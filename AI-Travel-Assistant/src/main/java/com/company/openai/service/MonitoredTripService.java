package com.company.openai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;

@Service
public class MonitoredTripService {

    private static final Logger log = LoggerFactory.getLogger(MonitoredTripService.class);
    private final ChatClient chatClient;

    public MonitoredTripService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generateItineraryWithMetrics(String userPrompt) {
        ChatResponse response = chatClient.prompt()
                .user(userPrompt)
                .call()
                .chatResponse();

        // Extract raw token metrics from Spring AI response metadata
        if (response != null && response.getMetadata() != null) {
            Usage usage = response.getMetadata().getUsage();
            log.info("Prompt Tokens: {}", usage.getPromptTokens());
            log.info("Generation Tokens: {}", usage.getCompletionTokens());
            log.info("Total Tokens Used: {}", usage.getTotalTokens());
        }

        return response.getResult().getOutput().getText();
    }
}