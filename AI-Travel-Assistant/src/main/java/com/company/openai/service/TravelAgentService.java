package com.company.openai.service;

import com.company.openai.tools.TravelTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class TravelAgentService {

    private final ChatClient chatClient;
    private final TravelTools travelTools;


    public TravelAgentService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, TravelTools travelTools){
       this.travelTools = travelTools;
       this.chatClient = chatClientBuilder.defaultSystem("""
                    You are an autonomous Travel Assistant Agent.
                    Your goal is to solve complex user requests by strategically selecting and calling tools.
                    
                    Rules:
                    1. If a prompt requires multiple actions (e.g. flight + hotel + weather), decide the logical sequence and execute the required tools sequentially.
                    2. Convert costs into local currency when helpful using `calculateCurrency`.
                    3. Handle tool output or missing info intelligently before asking the user for input.
                    """).defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
               .build();
    }

    public String processUserRequest(String userPrompt, String conversationId){
        return this.chatClient.prompt()
                .user(userPrompt)
                .tools(this.travelTools)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

}
