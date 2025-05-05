package com.example.lecagent.service;

import com.example.lecagent.entity.pojo.Result;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

public interface LecAgentService {
    Flux<String> simpleChat(String chatId, String userMessage, int type);

    void importDocuments(MultipartFile multipartFile) throws IOException;
    Long newChat();

//    String mcpChat(Long chatId, String userMessage);

    Result getHistory();

    Result deleteHistory(String chatId);

    Result getNowHistory(String chatId);
}
