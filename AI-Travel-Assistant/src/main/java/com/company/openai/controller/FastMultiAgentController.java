package com.company.openai.controller;

import com.company.openai.dto.TripPlanRequest;
import com.company.openai.dto.TripPlanResponse;
import com.company.openai.service.FastMultiAgentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/multi-agents")
public class FastMultiAgentController {
    private final FastMultiAgentService fastMultiAgentService;

    public FastMultiAgentController(FastMultiAgentService fastMultiAgentService) {
        this.fastMultiAgentService = fastMultiAgentService;
    }

   @PostMapping("/plan-direct")
    public ResponseEntity<String> executeAgents(@Valid @RequestBody
                                                          TripPlanRequest request) {
       String result = fastMultiAgentService.planTripFast(request);
       return ResponseEntity.ok(result);
    }

}
