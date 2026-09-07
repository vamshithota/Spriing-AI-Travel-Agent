package com.company.openai.orchestrators;

import com.company.openai.enums.TravelEvent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;
@Component
@WithStateMachine
public class TravelOrchestratorListener {

    private final ChatClient flightAgentClient;
    private final ChatClient hotelAgentClient;

    public TravelOrchestratorListener(
            @Qualifier("travelAgentClient") ChatClient flightAgentClient,
            @Qualifier("travelAgentClient") ChatClient hotelAgentClient) {
        this.flightAgentClient = flightAgentClient;
        this.hotelAgentClient = hotelAgentClient;
    }

    public void handleFlightSearch(Message<TravelEvent> message) {
        String prompt = (String) message.getHeaders().get("userPrompt");
        // Execute Flight Sub-Agent tool calls via Spring AI ChatClient
        String flightResult = flightAgentClient.prompt().user(prompt).call().content();

    }


}
