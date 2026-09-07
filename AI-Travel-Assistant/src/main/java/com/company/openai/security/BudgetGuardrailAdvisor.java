package com.company.openai.security;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.ConcurrentHashMap;
public class BudgetGuardrailAdvisor implements CallAdvisor {

    private final double maxDollarBudget;
    private final ConcurrentHashMap<String, Double> sessionSpend = new ConcurrentHashMap<>();

    private static final double INPUT_COST_PER_1K = 0.00015;
    private static final double OUTPUT_COST_PER_1K = 0.00060;

    public BudgetGuardrailAdvisor(double maxDollarBudget) {
        this.maxDollarBudget = maxDollarBudget;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String conversationId = (String) request.context().getOrDefault(ChatMemory.CONVERSATION_ID, "default");
        double currentSpend = sessionSpend.getOrDefault(conversationId, 0.0);
        if (currentSpend >= maxDollarBudget) {
            throw new IllegalStateException(String.format(
                    "BUDGET EXCEEDED: Session %s hit dollar limit ($%.2f / $%.2f)",
                    conversationId, currentSpend, maxDollarBudget));
        }

        ChatClientResponse response = chain.nextCall(request);
        if (response != null && response.chatResponse() != null) {
            Usage usage = response.chatResponse().getMetadata().getUsage();
            if (usage != null) {
                double callCost = ((usage.getPromptTokens() / 1000.0) * INPUT_COST_PER_1K) +
                        ((usage.getCompletionTokens() / 1000.0) * OUTPUT_COST_PER_1K);

                sessionSpend.merge(conversationId, callCost, Double::sum);
            }
        }
        return response;
    }

    @Override
    public String getName() {
        return "BudgetGuardrailAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
