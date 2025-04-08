package com.example.lecagent.service;

import com.example.lecagent.entity.pojo.Result;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

public interface LecMcpService {

    Flux<String> mcpChat(Long chatId, String userMessage);

}
