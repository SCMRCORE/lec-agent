package com.example.lecagent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.HashMap;
import java.util.Map;

public class RewordNode implements NodeAction {
    private final ChatClient chatClient;

    public RewordNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState t) throws Exception {
        String summary = (String) t.value("summary").orElse("");
        String prompt = "请将以下摘要，以更加生动优美的语言改写，同时保持信息不变:\n\n"+summary;

        ChatResponse chatResponse = chatClient
                .prompt(prompt)
                .call()
                .chatResponse();
        String output = chatResponse.getResult().getOutput().getText();

        Map<String, Object> result = new HashMap<>();
        result.put("reword", output);
        return result;
    }
}
