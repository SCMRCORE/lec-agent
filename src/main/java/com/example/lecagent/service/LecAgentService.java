package com.example.lecagent.service;

import reactor.core.publisher.Flux;

public interface LecAgentService {
    Flux<String> simpleChat(String chatId, String userMessage);
}
