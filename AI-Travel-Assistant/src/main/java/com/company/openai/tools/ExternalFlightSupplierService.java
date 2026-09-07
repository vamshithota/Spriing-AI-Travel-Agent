package com.company.openai.tools;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class ExternalFlightSupplierService {
    @CircuitBreaker(name="flightSupplierApi", fallbackMethod = "flightSupplierFallback")
    public String callFlightProviderApi(String origin , String destination){
        // Calls external flight supplier API
        throw new RuntimeException("External flight API timed out!");
    }

    // Deterministic fallback state when tool fails repeatedly
    public String flightSupplierFallback(String origin, String destination, Throwable t) {
        return "CIRCUIT_BREAKER_OPEN: Flight search tool is temporarily offline. Do not retry calling this tool.";
    }
}
