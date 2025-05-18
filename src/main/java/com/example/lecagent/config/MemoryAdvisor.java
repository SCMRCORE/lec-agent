package com.example.lecagent.config;

import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryAdvisor {

    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository(RedisProperties redisProperties) {
        String host = redisProperties.getHost();
        int port = Integer.parseInt(redisProperties.getPort());
        String password = redisProperties.getPassword();

        //TODO 默认过期时间86400秒
        return new RedisChatMemoryRepository.RedisBuilder()
                .host(host)
                .port(port)
                .password(password)
                .timeout(86400)
                .build();
    }

    @Bean
    public ChatMemory redisChatMemory(RedisChatMemoryRepository redisChatMemoryRepository){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(10)
                .build();
    }
}
