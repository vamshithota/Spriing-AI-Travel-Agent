package com.company.openai.controller;

import com.company.openai.dto.TripPlanRequest;
import com.company.openai.dto.TripPlanResponse;
import com.company.openai.service.FastMultiAgentService2;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/travel")
public class TravelOrchestratorController {

    private final FastMultiAgentService2 orchestratorService;

    public TravelOrchestratorController(FastMultiAgentService2 orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/orch/plan-direct")
    public ResponseEntity<String> executeAgents(@Valid @RequestBody
                                                TripPlanRequest request) {
        String result = orchestratorService.planTripFast(request);
        return ResponseEntity.ok(result);
    }

  }
