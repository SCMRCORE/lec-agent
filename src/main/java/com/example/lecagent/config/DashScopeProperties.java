package com.example.lecagent.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.dashscope")
public class DashScopeProperties {
    private String apiKey;
}
