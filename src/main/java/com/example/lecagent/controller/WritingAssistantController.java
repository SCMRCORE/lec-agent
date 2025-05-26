package com.example.lecagent.controller;


import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/write")
public class WritingAssistantController {
    private final CompiledGraph compiledGraph;

    @Autowired
    public WritingAssistantController(@Qualifier("WritingAssistant") StateGraph writingAssistantConfig) throws GraphStateException {
        this.compiledGraph = writingAssistantConfig.compile();
    }

    //TODO 待测试
    @GetMapping
    public Map<String, Object> write(@RequestParam("text") String inputText){
        var resultFuture = compiledGraph.invoke(Map.of("original_text", inputText));
        var res = resultFuture.get();
        return res.data();
    }

}
