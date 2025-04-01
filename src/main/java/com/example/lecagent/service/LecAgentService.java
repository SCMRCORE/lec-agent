package com.example.lecagent.service;

import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

public interface LecAgentService {
    Flux<String> simpleChat(String chatId, String userMessage);

    void importDocuments(MultipartFile multipartFile) throws IOException;
}
