package com.company.openai.tools;

import com.company.openai.dto.OpenWeatherResponse;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class TravelTools {

    private final RestClient restClient;
    private final String apiKey;
    private final ObservationRegistry observationRegistry;

    public TravelTools(
            RestClient.Builder restClientBuilder,
            @Value("${openweather.api.url}") String baseUrl,
            @Value("${openweather.api.key}") String apiKey,
            ObservationRegistry observationRegistry) {

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
        this.observationRegistry = observationRegistry;
    }

    // ============================================================
    // WEATHER TOOL
    // ============================================================

    @Tool(description = "Get current real-time weather conditions and temperature for a given city.")
    public String getWeather(
            @ToolParam(description =
                    "City name and optional 2-letter country code (e.g. 'Paris,FR' or 'Tokyo')")
            String location) {

        Observation observation = Observation.createNotStarted(
                "travel.tool.weather",
                observationRegistry
        );

        observation.lowCardinalityKeyValue("tool", "weather");
        observation.lowCardinalityKeyValue("service", "openweather");

        return observation.observe(() -> {

            try {

                OpenWeatherResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/weather")
                                .queryParam("q", location)
                                .queryParam("units", "metric")
                                .queryParam("appid", apiKey)
                                .build())
                        .retrieve()
                        .body(OpenWeatherResponse.class);

                if (response == null || response.weather().isEmpty()) {
                    return "No weather data found for location: " + location;
                }

                String condition = response.weather().get(0).description();
                double temp = response.main().temp();
                double feelsLike = response.main().feelsLike();
                int humidity = response.main().humidity();

                return String.format(
                        "Current weather in %s: %s. Temperature: %.1f°C " +
                                "(feels like %.1f°C). Humidity: %d%%.",
                        response.name(),
                        condition,
                        temp,
                        feelsLike,
                        humidity
                );

            } catch (RestClientException ex) {

                observation.error(ex);

                return "Error retrieving weather for '" +
                        location + "': " + ex.getMessage();
            }
        });
    }


    // ============================================================
    // FLIGHT TOOL
    // ============================================================

    @Tool(description = "Search for available flights between two airports on a given date.")
    public List<String> searchFlights(
            @ToolParam(description = "Origin 3-letter airport code, e.g., 'JFK'")
            String origin,

            @ToolParam(description = "Destination 3-letter airport code, e.g., 'CDG'")
            String destination,

            @ToolParam(description = "Departure date in format YYYY-MM-DD")
            String date) {

        Observation observation = Observation.createNotStarted(
                "travel.tool.flights",
                observationRegistry
        );

        observation.lowCardinalityKeyValue("tool", "flights");

        return observation.observe(() -> {

            // Simulated external API call
            return List.of(
                    "Flight AA123: " + origin + " to " + destination +
                            " on " + date + " - $450 (Non-stop)",

                    "Flight DL456: " + origin + " to " + destination +
                            " on " + date + " - $380 (1 Stop)"
            );
        });
    }


    // ============================================================
    // HOTEL TOOL
    // ============================================================

    @Tool(description = "Search for available hotel accommodations in a given destination.")
    public List<String> searchHotels(

            @ToolParam(description = "City or neighborhood name")
            String destination,

            @ToolParam(description = "Check-in date YYYY-MM-DD")
            String checkIn,

            @ToolParam(description = "Check-out date YYYY-MM-DD")
            String checkOut,

            @ToolParam(description = "Maximum nightly budget in USD")
            Double maxBudget) {

        Observation observation = Observation.createNotStarted(
                "travel.tool.hotels",
                observationRegistry
        );

        observation.lowCardinalityKeyValue("tool", "hotels");

        return observation.observe(() -> {

            // Simulated external API call
            return List.of(
                    "Grand Plaza " + destination +
                            " - $180/night (Rating: 4.5/5)",

                    "City Center Hotel " + destination +
                            " - $120/night (Rating: 4.1/5)"
            );
        });
    }


    // ============================================================
    // CURRENCY TOOL
    // ============================================================

    @Tool(description = "Convert currency from one currency code to another using live conversion rates.")
    public String calculateCurrency(

            @ToolParam(description = "Amount to convert")
            Double amount,

            @ToolParam(description = "3-letter source currency code, e.g., 'USD'")
            String fromCurrency,

            @ToolParam(description = "3-letter target currency code, e.g., 'EUR'")
            String toCurrency) {

        Observation observation = Observation.createNotStarted(
                "travel.tool.currency",
                observationRegistry
        );

        observation.lowCardinalityKeyValue("tool", "currency");

        return observation.observe(() -> {

            double rate = 1.0;

            String from = fromCurrency.toUpperCase();
            String to = toCurrency.toUpperCase();

            if (from.equals("USD") && to.equals("EUR")) {
                rate = 0.92;
            } else if (from.equals("USD") && to.equals("JPY")) {
                rate = 155.0;
            } else if (from.equals("USD") && to.equals("GBP")) {
                rate = 0.78;
            }

            double converted = amount * rate;

            return String.format(
                    "%.2f %s = %.2f %s (Rate: %.2f)",
                    amount,
                    from,
                    converted,
                    to,
                    rate
            );
        });
    }


    // ============================================================
    // DIRECTIONS TOOL
    // ============================================================

    @Tool(description = "Get transit or driving directions between two locations.")
    public String getDirections(

            @ToolParam(description = "Starting address or landmark")
            String origin,

            @ToolParam(description = "Destination address or landmark")
            String destination,

            @ToolParam(description =
                    "Mode of transport: 'driving', 'transit', or 'walking'")
            String mode) {

        Observation observation = Observation.createNotStarted(
                "travel.tool.directions",
                observationRegistry
        );

        observation.lowCardinalityKeyValue("tool", "directions");

        return observation.observe(() -> {

            // Simulated external API call
            return "Directions from " +
                    origin +
                    " to " +
                    destination +
                    " via " +
                    mode +
                    ": Take Metro Line 1 for 15 mins.";
        });
    }
}