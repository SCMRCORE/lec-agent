package com.example.lecagent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class SummaryFeedbackClassifierNode implements NodeAction {

    private final ChatClient chatClient;

    public SummaryFeedbackClassifierNode(ChatClient chatClient) {
        this.chatClient=chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState t) throws Exception {
        String summary = (String) t.value("summary").orElse("");
        if(!StringUtils.hasText(summary)){
            throw new RuntimeException("summary is empty");
        }

        String prompt = """
                以下是一个自动生成的摘要，请判断是否让用户满意。如果满意，请回复"positive"，否则返回 "negative"：
                摘要内容：
                %s
                """.formatted(summary);
        ChatResponse chatResponse = chatClient
                .prompt(prompt)
                .call()
                .chatResponse();
        String output = chatResponse.getResult().getOutput().getText();


        String classification = output
                //先将可能的大写转为小写，然后通过contains来进行判断，类似于断言
                .toLowerCase()
                .contains("positive") ? "positive" : "negative";

        Map<String, Object> updated = new HashMap<>();
        updated.put("summary_feedback", classification);

        return updated;
    }
}
