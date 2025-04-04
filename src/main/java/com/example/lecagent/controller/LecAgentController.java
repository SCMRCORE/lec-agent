package com.example.lecagent.controller;

import com.example.lecagent.entity.pojo.Result;
import com.example.lecagent.service.LecAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/lec")
public class LecAgentController {

    @Autowired
    private LecAgentService lecAgentService;

    @GetMapping(value="/newchat")
    public Result<Long> newChat(){
        return lecAgentService.newChat();
    }

    @GetMapping(value="/simplechat",produces = "text/html;charset=utf-8")
    public Flux<String> simpleChat(Long chatId, String userMessage){
        return lecAgentService.simpleChat(chatId, userMessage);
    }

    @PostMapping("/import")
    public void importDocuments(@RequestParam MultipartFile multipartFile) throws IOException {
        lecAgentService.importDocuments(multipartFile);
    }

}
