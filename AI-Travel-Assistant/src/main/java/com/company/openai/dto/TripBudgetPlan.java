package com.company.openai.dto;

import java.util.List;

public record TripBudgetPlan(String destination,
                             int totalDays,
                             double totalBudgetUSD,
                             BudgetBreakdown budgetBreakdown,
                             List<String> plannedActivities) {
    public record BudgetBreakdown(
            double flightTargetUSD,
            double hotelNightlyTargetUSD,
            double dailyFoodAndTransitUSD,
            double emergencyBufferUSD
    ) {}
}
