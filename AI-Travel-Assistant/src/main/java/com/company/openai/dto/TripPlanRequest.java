package com.company.openai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TripPlanRequest {
    @NotBlank(message = "Prompt must not be blank")
    @Size(min = 5, max = 1000, message = "Prompt must be between 5 and 1000 characters")
    private String prompt;
    @NotBlank(message = "Conversation ID is required")
    @NotBlank(message = "Conversation ID is required")
    // REMOVED @Pattern constraint to accept non-UUID strings like "vamshit93"
    private String conversationId;
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    public TripPlanRequest() {}
    public TripPlanRequest(String prompt) {
        this.prompt = prompt;
    }
    public String getPrompt() {
        return prompt;
    }
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
