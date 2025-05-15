package com.example.lecagent.config;

import com.alibaba.cloud.ai.memory.redis.RedisChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryAdvisor {

    @Bean
    public RedisChatMemory redisChatMemory(RedisProperties redisProperties) {
        String host = redisProperties.getHost();
        int port = Integer.parseInt(redisProperties.getPort());
        String password = redisProperties.getPassword();

        return new RedisChatMemory(
                host,
                port,
                password
        );
    }
}
