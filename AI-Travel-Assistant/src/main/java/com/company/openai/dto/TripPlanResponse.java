package com.company.openai.dto;

public record TripPlanResponse(
        boolean success,
        String result,
        String errorMessage
) {
    public static TripPlanResponse success(String result) {
        return new TripPlanResponse(true, result, null);
    }

    public static TripPlanResponse error(String errorMessage) {
        return new TripPlanResponse(false, null, errorMessage);
    }
}
