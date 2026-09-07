package com.company.openai.security;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.memory.ChatMemory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionLimitAdvisor implements CallAdvisor {

    private final int maxIterations;
    private final ConcurrentHashMap<String, AtomicInteger> executionCounts = new ConcurrentHashMap<>();

    public ExecutionLimitAdvisor(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

        String conversationId = (String) chatClientRequest.context().getOrDefault(ChatMemory.CONVERSATION_ID,"default");

        int currentCount = executionCounts.computeIfAbsent(conversationId , k -> new AtomicInteger(0)).incrementAndGet();

        if(currentCount > maxIterations){
            executionCounts.remove(conversationId);
            throw new IllegalStateException("FORCED TERMINATION: Maximum agent iterations ("
                    + maxIterations + ") exceeded for session: " + conversationId);
        }
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        if(response!= null){
            executionCounts.remove(conversationId);
        }
        return response;
    }

    @Override
    public String getName() {
        return "ExecutionLimitAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
