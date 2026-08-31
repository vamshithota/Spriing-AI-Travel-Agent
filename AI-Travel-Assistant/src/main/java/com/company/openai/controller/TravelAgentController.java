package com.company.openai.controller;

import com.company.openai.service.TravelAgentService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.ai.chat.messages.Message;


import java.util.List;

@RestController
@RequestMapping("/api/v1/travel")
public class TravelAgentController {

    private final TravelAgentService travelAgentService;
    private final ChatMemory chatMemory;
    public TravelAgentController(TravelAgentService travelAgentService, ChatMemory chatMemory) {
        this.travelAgentService = travelAgentService;
        this.chatMemory = chatMemory;
    }

    public record ChatRequest(String prompt, String conversationId) {}
    public record ChatResponse(String response, String conversationId) {}
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest chatRequest){
        String result = travelAgentService.processUserRequest(chatRequest.prompt(), chatRequest.conversationId);
        return ResponseEntity.ok(new ChatResponse(result, chatRequest.conversationId()));
    }

    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<String>> getHistory(@PathVariable String conversationId){
        List<Message> history = chatMemory.get(conversationId);
        List<String> formatted = history.stream()
                .map(msg -> msg.getMessageType() + ": " + msg.getText())
                .toList();
        return ResponseEntity.ok(formatted);
    }

}
