package com.company.openai.service;

import com.company.openai.dto.TripPlanRequest;
import com.company.openai.dto.TripPlanTasks;
import com.company.openai.entity.TripPlanEntity;
import com.company.openai.repository.TripPlanRepository;
import com.company.openai.tools.TravelTools;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

//Parallel Orchestrator Service
@Service
public class FastMultiAgentService {

    private final ChatClient chatClient;
    private final TravelTools travelTools;
    private final MeterRegistry meterRegistry;
    private final TripPlanRepository tripPlanRepository;
    private final Executor travelToolExecutor;

    public FastMultiAgentService(ChatClient.Builder chatClientBuilder,
                                 TravelTools travelTools, MeterRegistry meterRegistry,
                                 TripPlanRepository repository,  @Qualifier("travelToolExecutor") Executor travelToolExecutor) {
        this.chatClient = chatClientBuilder.build();
        this.travelTools = travelTools;
        this.meterRegistry = meterRegistry;
        this.tripPlanRepository = repository;
        this.travelToolExecutor = travelToolExecutor;
    }
//    @Cacheable(
//            value = "tripPlans",
//            key = "#tripPlanRequest.conversationId + ':' + #tripPlanRequest.prompt?.toLowerCase()?.trim()"
//    )
    @Transactional
    public String planTripFast(TripPlanRequest tripPlanRequest) {
        String conversationId = tripPlanRequest.getConversationId();
        // METRIC 1: Overall Pipeline Timer
        // =========================================================================
        Timer.Sample overallSample = Timer.start(meterRegistry);

        // -------------------------------------------------------------------------
        // STAGE 1: Planner Agent (LLM Call 1)
        // -------------------------------------------------------------------------
        Timer.Sample plannerSample = Timer.start(meterRegistry);
        TripPlanTasks tasks = chatClient.prompt()
                .options(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .temperature(0.0).build()
                        )
                .system("""
            You are a Travel Planner Assistant.
            
            BUDGET & FEASIBILITY RULES:
            1. If the user DOES NOT mention a budget constraint in their prompt:
               - Set totalBudgetUSD to 0.0 (or leave empty).
               - Set 'isBudgetSufficient' to TRUE. Do NOT flag an insufficient budget error if no budget was specified.
            
            2. If the user explicitly DOES mention a numeric budget constraint:
               - Set totalBudgetUSD to that amount.
               - Evaluate whether totalBudgetUSD is realistically sufficient for the requested destination and days.
               - Set 'isBudgetSufficient' to FALSE ONLY IF the specified budget is clearly far below realistic minimums.
               - If 'isBudgetSufficient' is false, set 'rejectionReason' to explain why the budget is insufficient.
            """)
                .user(tripPlanRequest.getPrompt())
                .call()
                .entity(TripPlanTasks.class);

        // 1. Domain Intent Check
        if (Boolean.FALSE.equals(tasks.isValidTravelQuery())) {
            return tasks.rejectionReason() != null
                    ? tasks.rejectionReason()
                    : "I can only assist with travel planning logistics.";
        }

        // 2. Budget Sufficiency Check (ONLY run if a budget was explicitly provided)
        if (tasks.totalBudgetUSD() > 0.0 && Boolean.FALSE.equals(tasks.isBudgetSufficient())) {
            return String.format(
                    "⚠️ **Insufficient Budget Warning**\n\n%s\n\n*Tip: Try increasing your budget or reducing the trip duration.*",
                    tasks.rejectionReason()
            );
        }

        plannerSample.stop(meterRegistry.timer("ai.pipeline.stage", "step", "planner_llm"));
        Timer.Sample toolsSample = Timer.start(meterRegistry);
        // STAGE 2: Parallel Direct Tool Execution (0 LLM Calls)
        // Runs all backend Java tools in parallel using CompletableFuture
        // 1. Weather Tool Call
        CompletableFuture<String> weatherTask = CompletableFuture.supplyAsync(() ->
                travelTools.getWeather(tasks.destination()),travelToolExecutor
        );
// 2. Flight Tool Call (With Strategy Pattern for inline budget filtering)
        CompletableFuture<String> flightTask = CompletableFuture.supplyAsync((()
                -> {
            List<String> rawFlights = travelTools.searchFlights(
                    tasks.originAirport(),
                    tasks.destinationAirport(),
                    tasks.checkInDate()
            );

            // Strategy Pattern: Inline filtering based on maxFlightBudgetUSD constraint
            List<String> filteredFlights = rawFlights.stream()
                    .filter(f -> extractPrice(f) <= tasks.maxFlightBudgetUSD())
                    .toList();

            if (filteredFlights.isEmpty()) {
                return String.join("\n", rawFlights) + " (Note: No flights strictly under $" + tasks.maxFlightBudgetUSD() + ")";
            }
            return String.join("\n", filteredFlights);
        }), travelToolExecutor);
        // 3. Hotel Tool Call
        CompletableFuture<String> hotelTask = CompletableFuture.supplyAsync((() -> {
            List<String> hotels = travelTools.searchHotels(
                    tasks.destination(),
                    tasks.checkInDate(),
                    tasks.checkOutDate(),
                    tasks.maxHotelNightlyUSD()
            );
            return String.join("\n", hotels);
        }), travelToolExecutor);
        // 4. Currency Tool Call
        CompletableFuture<String> currencyTask =
                CompletableFuture.supplyAsync((() ->
                travelTools.calculateCurrency(
                        tasks.totalBudgetUSD(),
                        "USD",
                        tasks.targetCurrency()
                )
        ),travelToolExecutor);
        // 5. Directions Tool Call
        CompletableFuture<String> directionsTask = CompletableFuture.supplyAsync((() ->
                travelTools.getDirections(
                        tasks.destinationAirport(),
                        tasks.destination() + " City Center",
                        "transit"
                )
        ),travelToolExecutor );

        // Wait for all parallel worker threads to complete simultaneously
        CompletableFuture.allOf(weatherTask, flightTask, hotelTask, currencyTask, directionsTask).join();
        toolsSample.stop(meterRegistry.timer("ai.pipeline.stage", "step", "parallel_tools"));

        Timer.Sample aggregatorSample = Timer.start(meterRegistry);
        // Extract raw String results
        String weatherData = weatherTask.join();
        String flightData = flightTask.join();
        String hotelData = hotelTask.join();
        String currencyData = currencyTask.join();
        String directionsData = directionsTask.join();
        // PATTERN 3: AGGREGATOR PATTERN (Aggregator Agent - 2nd LLM Call)

        String finalMarkdownPrompt = String.format("""
        Synthesize this travel data into a CONCISE Markdown report:
        
        USER GOAL: %s
        DESTINATION: %s
        DATES: %s to %s
        BUDGET: $%.2f USD
        
        FETCHED DATA:
        - Weather: %s
        - Currency: %s
        - Flights: %s
        - Hotels: %s
        - Directions: %s
        
        STRICT RULES:
        - Keep the entire report under 250 words.
        - Use bullet points and a single summary table for budget.
        - Do NOT include long narrative descriptions.
        """,
                tripPlanRequest.getPrompt(), tasks.destination(), tasks.checkInDate(), tasks.checkOutDate(),
                tasks.totalBudgetUSD(), weatherData, currencyData, flightData, hotelData, directionsData
        );

        String result = chatClient.prompt()
                .options(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .maxTokens(500)
                        .temperature(0.3).build()
                )
                .system("You are an expert Travel Assistant. Produce a polished Markdown travel report.")
                .user(finalMarkdownPrompt)
                .advisors( a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        TripPlanEntity entity = new TripPlanEntity();
        entity.setConversationId(conversationId);
        entity.setDestination(tasks.destination());
        entity.setDays(tasks.days());
        entity.setBudgetUSD(tasks.totalBudgetUSD());
        entity.setGeneratedItinerary(result);

        tripPlanRepository.save(entity);

        aggregatorSample.stop(meterRegistry.timer("ai.pipeline.stage", "step", "aggregator_llm"));
        overallSample.stop(meterRegistry.timer("ai.pipeline.stage", "step", "total_pipeline"));
        return result;
    }

    private double extractPrice(String flightText) {
        try {
            int dollarSignIdx = flightText.indexOf('$');
            if (dollarSignIdx != -1) {
                String pricePart = flightText.substring(dollarSignIdx + 1).split(" ")[0];
                return Double.parseDouble(pricePart);
            }
        } catch (Exception ignored) {
            // Fallback default if string parsing fails
        }
        return 0.0;
    }
}
// TODO -- implement guard rails, -- DONE
// TODO -- SQL integration -- INTEGRATED WITH H2 DONE
// TODO -- Implement Oauth2 -- DONE
// TODO -- explore different llm models for space and time complexity
// TODO -- AI agent observability, eval
// TODO -- IMPLEMENT DASHBOARD METRICS SHOWCASING PERFORMANCE OF
            //  EACH TOOL AND API CALL (Done)
// TODO -- REPLICATE FAILING TOOL AND CHECK LLM BEHAVIOUR
// TODO -- Implement semantic cacheing
// TODO -- Integrate currency converter API
// TODO -- Integrate flight Search API
// TODO -- AWS integration