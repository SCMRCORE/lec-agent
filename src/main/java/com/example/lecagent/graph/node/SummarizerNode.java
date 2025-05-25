package com.example.lecagent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.HashMap;
import java.util.Map;

public class SummarizerNode implements NodeAction {

    private final ChatClient chatClient;

    public SummarizerNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState t) throws Exception {
        String text = (String) t.value("original_text").orElse("");
        String prompt = "请对以下中文进行简明摘要：\n" + text;

        ChatResponse chatResponse = chatClient.prompt(prompt)
                .call()
                .chatResponse();
        String summary = chatResponse.getResult().getOutput().getText();

        Map<String, Object> res = new HashMap<>();
        res.put("summary", summary);
        return res;
    }
}
