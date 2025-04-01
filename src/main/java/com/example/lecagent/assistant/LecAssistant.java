//package com.example.lecagent.assistant;
//
//import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
//import com.alibaba.cloud.ai.dashscope.rag.DashScopeCloudStore;
//import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
//import com.example.lecagent.config.DashScopeProperties;
//import lombok.Data;
//import lombok.Getter;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.memory.InMemoryChatMemory;
//import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
//import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
//import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
//import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
//import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
//import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
//import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//
//@Configuration
//public class LecAssistant {
//
//    private final ChatClient chatClient;
//
//    private final DashScopeProperties dashScopeProperties;
//
//    private final VectorStore vectorStore;
//    ChatMemory chatMemory = new InMemoryChatMemory();
//    private final String DEFAULT_SYSTEM = "你是乐程娘，涉及到你自己的时候用乐程娘称呼自己，语气可爱一点，擅长计算机专业相关，请用中文回答用户的问题，可以适当加一些emoji";
//
//    //RAG高级组件
//    private final MultiQueryExpander multiQueryExpander;
//    private final QueryTransformer queryTransformer;
//    private final QueryTransformer queryTranslation;
//    private final QueryTransformer queryContext;
//    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
//    public LecAssistant(ChatClient.Builder builder, DashScopeProperties dashScopeProperties){
//        this.dashScopeProperties=dashScopeProperties;
//        DashScopeStoreOptions options = new DashScopeStoreOptions("lec-vector-store");
//        DashScopeApi dashScopeApi = new DashScopeApi(dashScopeProperties.getApiKey());
//        this.vectorStore = new DashScopeCloudStore(dashScopeApi, options);
//
//        //多查询拓展
//        this.multiQueryExpander = MultiQueryExpander.builder()
//                .chatClientBuilder(builder)
//                .includeOriginal(false)//不包含原始查询
//                .numberOfQueries(3)//生成3个查询变体
//                .build();
//
//        //查询重写
//        this.queryTransformer = RewriteQueryTransformer.builder()
//                .chatClientBuilder(builder)
//                .build();
//
//        //查询翻译
//        this.queryTranslation = TranslationQueryTransformer.builder()
//                .chatClientBuilder(builder)
//                .targetLanguage("Chinese")
//                .build();
//
//        //上下文感知
//        this.queryContext = CompressionQueryTransformer.builder()
//                .chatClientBuilder(builder)
//                .build();
//
//
//        //检索增强顾问
//        this.retrievalAugmentationAdvisor=RetrievalAugmentationAdvisor.builder()
//                .queryAugmenter(ContextualQueryAugmenter.builder()
//                        .allowEmptyContext(true)
//                        .build())
//                .queryExpander(multiQueryExpander)
//                .queryTransformers(List.of(queryTransformer, queryTranslation, queryContext))
//                .documentRetriever(VectorStoreDocumentRetriever.builder()
//                        .vectorStore(vectorStore)   //向量存储
//                        .similarityThreshold(0.1) // 相似度阈值
//                        .topK(3) // 返回文档数量
//                        .build())
//                .build();
//
//        //初始化client
//        this.chatClient = builder
//                .defaultSystem(DEFAULT_SYSTEM)
//                .defaultAdvisors(retrievalAugmentationAdvisor)
//                .build();
//    }
//
//}
