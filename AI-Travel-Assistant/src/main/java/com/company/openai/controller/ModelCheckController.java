package com.company.openai.controller;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ModelCheckController {

    private final ChatModel chatModel;

    public ModelCheckController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/check-model")
    public String getModelName() {
        ChatResponse response = chatModel.call(new Prompt("Hello"));

        // Retrieves the model string directly from the OpenAI response header/body
        String actualModel = response.getMetadata().getModel();

        return "Model used for execution: " + actualModel;
    }
}
