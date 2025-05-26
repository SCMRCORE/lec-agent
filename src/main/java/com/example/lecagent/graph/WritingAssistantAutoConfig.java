package com.example.lecagent.graph;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.example.lecagent.graph.edge.FeedbackDispatcher;
import com.example.lecagent.graph.node.RewordNode;
import com.example.lecagent.graph.node.SummarizerNode;
import com.example.lecagent.graph.node.SummaryFeedbackClassifierNode;
import com.example.lecagent.graph.node.TitleGeneratorNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class WritingAssistantAutoConfig {
    @Bean
    public StateGraph WritingAssistant(ChatModel chatModel) throws GraphStateException {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        OverAllStateFactory overAllStateFactory = () ->{
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy("original_text", new ReplaceStrategy());
            state.registerKeyAndStrategy("reword", new ReplaceStrategy());
            state.registerKeyAndStrategy("summary", new ReplaceStrategy());
            state.registerKeyAndStrategy("summary_feedback", new ReplaceStrategy());
            state.registerKeyAndStrategy("title", new ReplaceStrategy());
            return state;
        };

        StateGraph graph = new StateGraph("Writing Assistant with Feedback Loop", overAllStateFactory.create())
                .addNode("reword", node_async(new RewordNode(chatClient)))
                .addNode("summarizer", node_async(new SummarizerNode(chatClient)))
                .addNode("feedbackClassifier", node_async(new SummaryFeedbackClassifierNode(chatClient)))
                .addNode("titleGenerator", node_async(new TitleGeneratorNode(chatClient)))

                .addEdge(START, "summarizer")
                .addEdge("summarizer", "feedbackClassifier")
                .addConditionalEdges("feedbackClassifier", edge_async(new FeedbackDispatcher()),
                        Map.of("positive", "reword", "negative", "summarizer"))
                .addEdge("reword", "titleGenerator")
                .addEdge("titleGenerator", END);

        // 添加 PlantUML 打印
        GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "writing assistant flow");
        System.out.println("\n=== Writing Assistant UML Flow ===");
        System.out.println(representation.content());
        System.out.println("==================================\n");

        return graph;
    }


}
