package com.company.openai.dto;

public record TripPlanTasks(
        Boolean isValidTravelQuery,
        Boolean isBudgetSufficient,
        String rejectionReason,
        String destination,
        String originAirport,
        String destinationAirport,
        String checkInDate,
        String checkOutDate,
        Integer days,
        Double totalBudgetUSD,
        Double maxFlightBudgetUSD,
        Double maxHotelNightlyUSD,
        String targetCurrency
) {
    public TripPlanTasks {
        isValidTravelQuery = (isValidTravelQuery == null) ? true : isValidTravelQuery;
        isBudgetSufficient = (isBudgetSufficient == null) ? true : isBudgetSufficient; // Default to true
        days = (days == null) ? 5 : days;
        totalBudgetUSD = (totalBudgetUSD == null) ? 0.0 : totalBudgetUSD;
        maxFlightBudgetUSD = (maxFlightBudgetUSD == null) ? 0.0 : maxFlightBudgetUSD;
        maxHotelNightlyUSD = (maxHotelNightlyUSD == null) ? 0.0 : maxHotelNightlyUSD;
    }
}