package com.example.lecagent.service.impl;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeCloudStore;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentCloudReader;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentCloudReaderOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import com.alibaba.cloud.ai.parser.markdown.MarkdownDocumentParser;
import com.alibaba.cloud.ai.parser.markdown.config.MarkdownDocumentParserConfig;
import com.alibaba.cloud.ai.reader.obsidian.ObsidianDocumentReader;
import com.example.lecagent.config.DashScopeProperties;
import com.example.lecagent.service.LecAgentService;
import com.example.lecagent.util.MinioUtils;
import jakarta.annotation.Resource;
import kotlin.SinceKotlin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.error.Mark;
import reactor.core.publisher.Flux;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

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

    public LecAgentServiceImpl(ChatClient.Builder builder, DashScopeProperties dashScopeProperties){
        this.dashScopeProperties=dashScopeProperties;
        DashScopeStoreOptions options = new DashScopeStoreOptions("lec-vector-store");
        DashScopeApi dashScopeApi = new DashScopeApi(dashScopeProperties.getApiKey());
        this.vectorStore = new DashScopeCloudStore(dashScopeApi, options);

        //初始化client
        this.chatClient = builder
                .defaultAdvisors(new PromptChatMemoryAdvisor(chatMemory))
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


    @Override
    public Flux<String> simpleChat(String chatId, String userMessage) {
        log.info("当前提问："+chatId+"："+userMessage);
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM+"根据用户输入，在结尾生成三条用户可能想搜索的内容")
                .user(userMessage)
                .advisors(a->a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                )
                .advisors(retrievalAugmentationAdvisor)
                .stream()
                .content();
    }

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
