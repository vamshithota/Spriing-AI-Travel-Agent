package com.company.openai.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class LlamaGuardClient {

    private final RestClient restClient;

    public LlamaGuardClient(@Value("${guardrails.llama-guard.url:http://localhost:11434/v1}") String guardUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(guardUrl)
                .build();
    }

    public GuardResult checkSafety(String userPrompt) {
        try {
            // Standard OpenAI Chat Completion payload for Llama Guard
            Map<String, Object> requestBody = Map.of(
                    "model", "llama-guard3:1b",
                    "messages", List.of(
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.0
            );
            long beforeCall = System.currentTimeMillis();
            LlamaGuardResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(LlamaGuardResponse.class);
            long afterCall = System.currentTimeMillis();
            System.out.println("Llama Guard HTTP call: "
                    + (afterCall - beforeCall) + " ms");

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String content = response.choices().get(0).message().content().trim();

                // Llama Guard outputs "safe" or "unsafe\nS<category_code>"
                if (content.startsWith("safe")) {
                    return new GuardResult(true, "SAFE", null);
                } else {
                    return new GuardResult(false, "UNSAFE", content);
                }
            }
        } catch (Exception e) {
            // Failure policy: Log error and fail-closed or fail-open depending on enterprise policy
            return new GuardResult(false, "ERROR", "Guardrail service unreachable: " + e.getMessage());
        }

        return new GuardResult(true, "UNKNOWN", null);
    }

    public record GuardResult(boolean isSafe, String status, String details) {}
    private record LlamaGuardResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}
}