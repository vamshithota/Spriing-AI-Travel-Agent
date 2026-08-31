package com.company.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenWeatherResponse(
        List<WeatherCondition> weather,
        MainData main,
        WindData wind,
        String name
) {
    public record WeatherCondition(
            String main,
            String description
    ) {}

    public record MainData(
            double temp,
            @JsonProperty("feels_like") double feelsLike,
            int humidity
    ) {}

    public record WindData(
            double speed
    ) {}
}
