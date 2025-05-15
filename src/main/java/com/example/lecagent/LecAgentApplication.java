package com.example.lecagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LecAgentApplication {
	public static void main(String[] args) {
		SpringApplication.run(LecAgentApplication.class, args);
	}

	@Bean
	public CommandLineRunner predefinedQuestions(
			ChatClient.Builder chatClientBuilder,
			ToolCallbackProvider tools,
			ConfigurableApplicationContext context) {
		return args -> {
			// 构建ChatClient并注入MCP工具
			var chatClient = chatClientBuilder
					.defaultTools(tools)
					.build();

			// 定义用户输入
			String userInput = "用高德地图查询北京今天的天气";
			// 打印问题
			System.out.println("\n>>> QUESTION: " + userInput);
			// 调用LLM并打印响应
			System.out.println("\n>>> ASSISTANT: " +
					chatClient.prompt(userInput).call().content());

			// 关闭应用上下文
			context.close();
		};
	}
}
