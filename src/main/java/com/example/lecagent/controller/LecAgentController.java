package com.example.lecagent.controller;


import com.example.lecagent.service.LecAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/lec")
public class LecAgentController {

    @Autowired
    private LecAgentService lecAgentService;

    @GetMapping(value="/simplechat",produces = "text/html;charset=utf-8")
    public Flux<String> simpleChat(String chatId, String userMessage){
        return lecAgentService.simpleChat(chatId, userMessage);
    }
}
