package com.example.lecagent.service.impl;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeCloudStore;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentCloudReader;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import com.example.lecagent.advisor.ReasoningContentAdvisor;
import com.example.lecagent.config.AppHttpCodeEnum;
import com.example.lecagent.config.DashScopeProperties;
import com.example.lecagent.entity.pojo.Result;
import com.example.lecagent.mapper.LecMapper;
import com.example.lecagent.service.LecAgentService;
import com.example.lecagent.util.MinioUtils;
import com.example.lecagent.util.SnowflakeIdWorker;
import com.example.lecagent.util.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
@Slf4j
public class LecAgentServiceImpl implements LecAgentService {

    private final ChatClient chatClient;

    private final DashScopeProperties dashScopeProperties;

    private final VectorStore vectorStore;
    ChatMemory chatMemory = new InMemoryChatMemory();
    private final String DEFAULT_SYSTEM = "你是乐程娘，涉及到你自己的时候用乐程娘称呼自己，语气可爱一点，擅长计算机专业相关，请用中文回答用户的问题，可以适当加一些emoji";

    //RAG高级组件
    private final MultiQueryExpander multiQueryExpander;
    private final QueryTransformer queryTransformer;
    private final QueryTransformer queryTranslation;
    private final QueryTransformer queryContext;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    //minio
    @Resource
    private  MinioUtils minioUtils;

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private LecMapper lecMapper;

    private final List<String> modelList = List.of(
            "qwen-max",
            "deepseek-r1",
            "deepseek-v3",
            "qwq-plus"
    );

    public LecAgentServiceImpl(DashScopeProperties dashScopeProperties, DashScopeChatModel chatModel, ToolCallbackProvider tools){
        ChatClient.Builder builder = ChatClient.builder(chatModel);

        this.dashScopeProperties=dashScopeProperties;
        DashScopeStoreOptions options = new DashScopeStoreOptions("lec-vector-store");
        DashScopeApi dashScopeApi = new DashScopeApi(dashScopeProperties.getApiKey());
        this.vectorStore = new DashScopeCloudStore(dashScopeApi, options);

        //初始化client
        this.chatClient = builder
                .defaultTools(tools)
                .defaultSystem(DEFAULT_SYSTEM)
                .defaultAdvisors(
                        new PromptChatMemoryAdvisor(chatMemory),
                        // 整合 QWQ 的思考过程到输出中
                        new ReasoningContentAdvisor(0)
                        )
                .build();

        //多查询拓展
        this.multiQueryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(builder)
                .includeOriginal(false)//不包含原始查询
                .numberOfQueries(3)//生成3个查询变体
                .build();

        //查询重写
        this.queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();

        //查询翻译
        this.queryTranslation = TranslationQueryTransformer.builder()
                .chatClientBuilder(builder)
                .targetLanguage("Chinese")
                .build();

        //上下文感知
        this.queryContext = CompressionQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();


        //检索增强顾问
        this.retrievalAugmentationAdvisor=RetrievalAugmentationAdvisor.builder()
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .queryExpander(multiQueryExpander)
                .queryTransformers(List.of(queryTransformer, queryTranslation, queryContext))
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)   //向量存储
                        .similarityThreshold(0.1) // 相似度阈值
                        .topK(3) // 返回文档数量
                        .build())
                .build();
    }


    /**
     * 新建对话
     *
     * @return
     */
    @Override
    public Long newChat() {
        Long userId = UserContext.getUser();
        //生成唯一ID
        Long chatId = snowflakeIdWorker.nextId();
        String checKey = "userId:"+userId+"chat:" + chatId;
        redisTemplate.opsForValue().set(checKey, chatId, 10, TimeUnit.MINUTES);
        log.info("新建对话："+chatId);
        return chatId;
    }

    public Boolean checkChatId(Long chatId){
        Long userId = UserContext.getUser();
        String checKey = "userId:"+userId+"chat:" + chatId;
        if(redisTemplate.opsForValue().get(checKey)==null && lecMapper.getChatId(chatId)==0){
            return false;
        }
        if(redisTemplate.opsForValue().get(checKey)!=null){
            lecMapper.storeChatId(userId, chatId);
            redisTemplate.delete(checKey);
        }
        return true;
    }

    /**
     * 简单对话
     * @param chatIdString
     * @param userMessage
     * @return
     */
    @Override
    public Flux<String> simpleChat(String chatIdString, String userMessage, int type) {
        Long chatId = Long.valueOf(chatIdString);
        if(!checkChatId(chatId)){
            throw new IllegalArgumentException("chatId无效");
        }

        //0qwen-max，1ds-r1, 2ds-v3, 3qwq-plus
        if(type>3 || type<0) {
            throw new IllegalArgumentException("type无效");
        }

        log.info("当前提问："+chatId+"："+userMessage);
        return this.chatClient.prompt()
                .options(DashScopeChatOptions.builder()
                        .withModel(modelList.get(type))
                        .build())
                .user(userMessage)
                .advisors(a->a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, String.valueOf(chatId))
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                )
                .advisors(retrievalAugmentationAdvisor)
                .stream()
                .content();
    }

//    @Override
//    public String mcpChat(Long chatId, String userMessage) {
//        if(!checkChatId(chatId)){
//            throw new IllegalArgumentException("chatId无效");
//        }
//
//        log.info("当前提问："+chatId+"："+userMessage);
//        return this.chatClient.prompt()
//                .user(userMessage)
//                .advisors(a->a
//                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, String.valueOf(chatId))
//                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
//                )
//                .advisors(retrievalAugmentationAdvisor)
//                .call()
//                .content();
//    }

    @Override
    public Result getNowHistory(String chatIdString) {
        Long chatId = Long.valueOf(chatIdString);
        List<Message> chatRecord =  chatMemory.get(String.valueOf(chatId), 10);
        return Result.okResult(chatRecord);
    }

    @Override
    public Result getHistory() {
        Long userId = UserContext.getUser();
        List<Long> chatHistoriesLong = lecMapper.getHistory(userId);
        List<String> chatHistories = new ArrayList<>();
        for(Long chatId:chatHistoriesLong){
            chatHistories.add(String.valueOf(chatId));
        }
        return Result.okResult(chatHistories);
    }

    @Override
    public Result deleteHistory(String chatIdString) {
        Long chatId = Long.valueOf(chatIdString);
        Long userId = UserContext.getUser();
        List<Long> chatHistories = lecMapper.getHistory(userId);
        if(!chatHistories.contains(chatId)){
            return Result.errorResult(AppHttpCodeEnum.INVALID_CHATID);
        }else{
            lecMapper.deleteHistory(chatId, userId);
            chatMemory.clear(String.valueOf(chatId));
            return Result.okResult(AppHttpCodeEnum.DELETE_SUCCESS);
        }
    }

    /**
     * 导入文档
     * @param multipartFile
     * @throws IOException
     */
    @Override
    public void importDocuments(MultipartFile multipartFile) throws IOException {
        String path = saveToTempFile(multipartFile);
//        备份到oss
        saveToMinIO(multipartFile);
        log.info(path);

        DocumentReader reader = new DashScopeDocumentCloudReader(path, new DashScopeApi(dashScopeProperties.getApiKey()), null);
        List<Document> documentList = reader.get();
        log.info(documentList.toString());

        vectorStore.add(documentList);
        log.info("{} documents loaded and split", documentList.size());
    }

    public String saveToTempFile(MultipartFile multipartFile) throws IOException {
        File tempFile = null;
        try{
            tempFile = File.createTempFile("ai-temp", ".pdf");
            tempFile.deleteOnExit();

            try (InputStream inputStream = multipartFile.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            if (tempFile != null) {
                tempFile.delete(); // 异常时尝试删除临时文件
            }
            throw new RuntimeException("保存临时文件失败", e);
        }
    }

    public String saveToMinIO(MultipartFile multipartFile){
        minioUtils.upload(multipartFile, multipartFile.getOriginalFilename());
        String url = minioUtils.getFileUrl(multipartFile.getOriginalFilename());
        log.info("备份到MinIO的文件的url为:{}",url);
        return url;
    }
}
