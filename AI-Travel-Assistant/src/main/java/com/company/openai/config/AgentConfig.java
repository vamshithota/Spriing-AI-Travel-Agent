package com.company.openai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
@Configuration
public class AgentConfig {

    @Bean
    public ChatMemory chatMemory2() {
        // In-memory chat storage for session persistence without DB
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20) // Retains last 20 messages in memory per session
                .build();
    }

    @Bean
    public ChatClient travelAgentClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory){
        return chatClientBuilder
                .defaultSystem("""
                You are an expert Travel Assistant AI running a ReAct reasoning loop.
                Plan multi-step requests methodically:
                1. Break complex requests into clear steps.
                2. Check available tools before asking the user for missing info.
                3. Handle tool errors gracefully by suggesting alternatives.
                """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                ).build();
    }
}
