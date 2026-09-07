package com.company.openai.evals.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

@TestConfiguration
public class TestEvaluatorConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean(name = "highReasoningJudgeClient")
    public ChatClient highReasoningJudgeClient() {
        var openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
       var judgeOptions = OpenAiChatOptions.builder()
               .model("gpt-4o")
               .temperature(0.0)
               .build();
        var judgeModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(judgeOptions)
                .build();

       return ChatClient.builder(judgeModel)
               .defaultSystem("""
                    You are an expert, unbiased AI Judge evaluating end-to-end multi-agent system performance.
                    Evaluate strictly for correctness, policy adherence, and constraint satisfaction.
                    """)
               .build();
    }
}