package com.example.lecagent.mcpserver;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeatherProperties {
//    @Bean
//    public CommandLineRunner weatherPredefinedQuestions(
//            ChatClient.Builder chatClientBuilder,
//            ToolCallbackProvider tools,
//            ConfigurableApplicationContext context) {
//        return args -> {
//            // 构建ChatClient并注入MCP工具
//            var chatClient = chatClientBuilder
//                    .defaultTools(tools)
//                    .build();
//
//            // 定义用户输入
//            String userInput = "北京的天气如何？";
//            // 打印问题
//            System.out.println("\n>>> QUESTION: " + userInput);
//            // 调用LLM并打印响应
//            System.out.println("\n>>> ASSISTANT: " +
//                    chatClient.prompt(userInput).call().content());
//
//            // 关闭应用上下文
//            context.close();
//        };
//    }
}
