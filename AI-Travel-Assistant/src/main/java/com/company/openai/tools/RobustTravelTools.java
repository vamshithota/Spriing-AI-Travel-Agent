package com.company.openai.tools;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RobustTravelTools {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public record FlightSearchResult(@NotNull String flightNumber, @NotNull Double price) {}

    public String searchFlightsSafe(String origin, String destination) {
        try{
            FlightSearchResult rawResult = fetchExternalFlightApi(origin, destination);
            Set<ConstraintViolation<FlightSearchResult>> violations = validator.validate(rawResult);
            if(!violations.isEmpty()){
                return "TOOL_ERROR: Flight service returned malformed data format. Agent must fallback.";
            }
            return rawResult.toString();
        } catch (Exception e) {
            return "TOOL_ERROR: Flight search service unavailable: " + e.getMessage();
        }
    }

    private FlightSearchResult fetchExternalFlightApi(String origin, String destination) {
        return new FlightSearchResult(null, 450.0); // Simulates bad external payload missing flightNumber
    }

}
