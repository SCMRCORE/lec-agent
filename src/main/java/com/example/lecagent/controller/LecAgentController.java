package com.example.lecagent.controller;

import com.example.lecagent.entity.pojo.Result;
import com.example.lecagent.service.LecAgentService;
import com.example.lecagent.service.LecMcpService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.simpleframework.xml.Path;
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

    @Autowired
    private LecMcpService lecMcpService;

    @GetMapping(value="/newchat")
    public Result<String> newChat(){
        Long newChatId = lecAgentService.newChat();
        return Result.okResult(newChatId.toString());
    }

    @GetMapping(value="/simplechat",produces = "text/html;charset=utf-8")
    public Flux<String> simpleChat(String chatId, String userMessage, int type){
        return lecAgentService.simpleChat(chatId, userMessage, type);
    }

    @GetMapping(value="/mcpchat",produces = "text/html;charset=utf-8")
    public Flux<String> mcpChat(Long chatId, String userMessage){
        return lecMcpService.mcpChat(chatId, userMessage);
    }

    @PostMapping("/import")
    public void importDocuments(@RequestParam MultipartFile multipartFile) throws IOException {
        lecAgentService.importDocuments(multipartFile);
    }

    @GetMapping("/history/{chatId}")
    public Result getNowHistory(@PathVariable String chatId){
        return lecAgentService.getNowHistory(chatId);
    }

    @GetMapping("/history")
    public Result getChatHistory(){
        return lecAgentService.getHistory();
    }

    @GetMapping("/delHistory")
    public Result deleteHistory(String chatId){
        return lecAgentService.deleteHistory(chatId);
    }
}
