package com.company.openai.controller;

import com.company.openai.service.TripPlannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/travel")
public class TripPlanningAgentController {

    private final TripPlannerService tripPlannerService;

    public TripPlanningAgentController(TripPlannerService tripPlannerService) {
        this.tripPlannerService = tripPlannerService;
    }

    public record PlannerRequest(String destination, int days, double budgetUSD, String conversationId) {}
    public record PlannerResponse(String itinerary, String conversationId) {}

    @PostMapping("/plan-structured")
    public ResponseEntity<String> planStructuredTrip(@RequestBody PlannerRequest request) {
       String finalItinerary = tripPlannerService.planStructuredTrip(
                request.destination(), request.days(), request.budgetUSD(), request.conversationId()
        );
       return  ResponseEntity.ok(finalItinerary);
    }

}
