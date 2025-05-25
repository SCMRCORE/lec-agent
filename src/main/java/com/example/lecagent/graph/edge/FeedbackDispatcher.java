package com.example.lecagent.graph.edge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class FeedbackDispatcher implements EdgeAction {
    @Override
    public String apply(OverAllState t) throws Exception {
        String feedback = (String) t.value("summary_feedback").orElse("");
        if (feedback.contains("positive")) {
            return "positive";
        }
        return "negative";
    }
}
