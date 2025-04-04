package com.example.lecagent.config;

import com.example.lecagent.util.SnowflakeIdWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeIdWorkerConfig {

    @Bean
    public long SnowWorkId(){
        return 1L;
    }

    @Bean
    public SnowflakeIdWorker snowflakeIdWorker(long SnowWorkId){
        return new SnowflakeIdWorker(SnowWorkId);
    }

}
