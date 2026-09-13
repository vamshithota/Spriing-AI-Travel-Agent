package com.company.openai.orchestrators;

import com.company.openai.dto.TripPlanRequest;
import com.company.openai.dto.TripPlanTasks;
import com.company.openai.entity.TripPlanEntity;
import com.company.openai.enums.TravelEvent;
import com.company.openai.enums.TravelState;
import com.company.openai.repository.TripPlanRepository;
import com.company.openai.tools.TravelTools;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
//@WithStateMachine
public class TravelOrchestratorListener {

    private final ChatClient chatClient;
    private final TravelTools travelTools;
    private final MeterRegistry meterRegistry;
    private final TripPlanRepository tripPlanRepository;
    private final Executor travelToolExecutor;

    public TravelOrchestratorListener(ChatClient.Builder chatClientBuilder,
                                      TravelTools travelTools,
                                      MeterRegistry meterRegistry,
                                      TripPlanRepository repository,
                                      @Qualifier("travelToolExecutor") Executor travelToolExecutor) {
        System.out.println("🔥🔥🔥 TravelOrchestratorListener CREATED 🔥🔥🔥");
        this.chatClient = chatClientBuilder.build();
        this.travelTools = travelTools;
        this.meterRegistry = meterRegistry;
        this.tripPlanRepository = repository;
        this.travelToolExecutor = travelToolExecutor;
    }
    //@OnTransition(source = "INITIAL", target = "PLANNING")
   // @OnStateEntry(source = "PLANNING")
    public void handlePlannerStage(StateContext<TravelState, TravelEvent> context)
    {
        System.out.println("========== ENTERED PLANNING ==========");
        try{
                TripPlanRequest request =  context.getExtendedState().get("request", TripPlanRequest.class);
                Timer.Sample plannerSample = Timer.start(meterRegistry);
                // planner llm
                TripPlanTasks tasks  = chatClient.prompt().options(OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.0).build())
                        .system("""
                    You are a Travel Planner Assistant.
                    BUDGET & FEASIBILITY RULES:
                    1. If no budget is specified: totalBudgetUSD = 0.0, isBudgetSufficient = TRUE.
                    2. If a numeric budget is specified: evaluate sufficiency. Set isBudgetSufficient = FALSE if clearly insufficient.
                    """)
                        .user(request.getPrompt())
                        .call()
                        .entity(TripPlanTasks.class);

                plannerSample.stop(meterRegistry.timer("ai.pipeline.stage", "step", "planner_llm"));
            if (tasks == null) {
                System.err.println("❌ OpenAI call completed but returned a null payload. Check API Key or Model access.");
                failWorkflow(context, "OpenAI API call failed to produce a structured response. Verify API key.");
                return;
            }
                // Domain & Budget validation checks
                if(Boolean.FALSE.equals(tasks.isValidTravelQuery())){
                    String errorMsg = tasks.rejectionReason()!= null ? tasks.rejectionReason(): "I can only assist with travel planning logistics.";
                    context.getExtendedState().getVariables().put("earlyExitResult", errorMsg);
                    context.getStateMachine().sendEvent(TravelEvent.ERROR_ENCOUNTERED);
                    return;
                }

                if (tasks.totalBudgetUSD() > 0.0 && Boolean.FALSE.equals(tasks.isBudgetSufficient())) {
                    String warningMsg = String.format("⚠️ **Insufficient Budget Warning**\n\n%s\n\n*Tip: Try increasing your budget or reducing the trip duration.*", tasks.rejectionReason());
                    context.getExtendedState().getVariables().put("earlyExitResult", warningMsg);
                    context.getStateMachine().sendEvent(TravelEvent.ERROR_ENCOUNTERED);
                    return;
                }

            context.getExtendedState().getVariables().put("tasks", tasks);
            boolean accepted =
                    context.getStateMachine()
                            .sendEvent(TravelEvent.TASKS_GENERATED);

            System.out.println(
                    "🔥 TASKS_GENERATED accepted? " + accepted
            );
            } catch (Exception e) {
            context.getExtendedState().getVariables().put("earlyExitResult", "Error in PLANNING: " + e.getMessage());
            context.getStateMachine().sendEvent(TravelEvent.ERROR_ENCOUNTERED);
            e.printStackTrace();
        }

    }
    //@OnTransition(source = "PLANNING", target = "FETCHING_TOOLS")
   // @OnStateEntry(source = "FETCHING_TOOLS")
    public void handleParallelToolsStage(StateContext<TravelState, TravelEvent> context) {
        System.out.println("========== ENTERED FETCHING_TOOLS ==========");
        TripPlanTasks tasks = context.getExtendedState().get("tasks", TripPlanTasks.class);
        Timer.Sample toolsSample = Timer.start(meterRegistry);
        try {
            // Check weather
            CompletableFuture<String> weatherTask = CompletableFuture.supplyAsync(
                    () -> travelTools.getWeather(tasks.destination()), travelToolExecutor);

            //Flight search
            CompletableFuture<String> flightsTask = CompletableFuture.supplyAsync(() -> {
                List<String> rawFlights = travelTools.searchFlights(tasks.originAirport(), tasks.destinationAirport(), tasks.checkInDate());
                List<String> filteredFlights = rawFlights.stream()
                        .filter(f -> extractPrice(f) <= tasks.maxFlightBudgetUSD())
                        .toList();
                return filteredFlights.isEmpty() ? String.join("\n", rawFlights) + " (Note: No flights strictly under $" +
                        tasks.maxFlightBudgetUSD() + ")" : String.join("\n", filteredFlights);
            }, travelToolExecutor);

            //Flight search
            CompletableFuture<String> hotelTask = CompletableFuture.supplyAsync(() -> {
                List<String> hotels = travelTools.searchHotels(tasks.destination(), tasks.checkInDate(), tasks.checkOutDate(), tasks.maxHotelNightlyUSD());
                return String.join("\n", hotels);
            }, travelToolExecutor);

            CompletableFuture<String> currencyTask = CompletableFuture.supplyAsync(() ->
                    travelTools.calculateCurrency(tasks.totalBudgetUSD(),
                            "USD", tasks.targetCurrency()), travelToolExecutor);

            CompletableFuture<String> directionsTask = CompletableFuture.supplyAsync(() ->
                    travelTools.getDirections(tasks.destinationAirport(), tasks.destination() + " City Center", "transit"), travelToolExecutor);

            CompletableFuture.allOf(weatherTask, flightsTask, hotelTask, currencyTask, directionsTask).join();
            toolsSample.stop(meterRegistry.timer("ai.pipeline.stage", "step", "parallel_tools"));

            context.getExtendedState().getVariables().put("weatherData", weatherTask.join());
            context.getExtendedState().getVariables().put("flightData", flightsTask.join());
            context.getExtendedState().getVariables().put("hotelData", hotelTask.join());
            context.getExtendedState().getVariables().put("currencyData", currencyTask.join());
            context.getExtendedState().getVariables().put("directionsData", directionsTask.join());

            boolean accepted =
                    context.getStateMachine()
                            .sendEvent(TravelEvent.TOOLS_COMPLETED);

            System.out.println(
                    "🔥 TOOLS_COMPLETED accepted? " + accepted
            );
        } catch (Exception ex) {
            context.getStateMachine().sendEvent(MessageBuilder.withPayload(TravelEvent.ERROR_ENCOUNTERED).build());
        }
    }

  //  @OnStateEntry(source = "AGGREGATING")
    public void handleAggregatorStage(StateContext<TravelState, TravelEvent> context) {
        System.out.println("========== ENTERED AGGREGATING ==========");
        TripPlanRequest request = context.getExtendedState().get("request", TripPlanRequest.class);
        TripPlanTasks tasks = context.getExtendedState().get("tasks", TripPlanTasks.class);
        Timer.Sample aggregatorSample = Timer.start(meterRegistry);


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
            """,
                request.getPrompt(), tasks.destination(), tasks.checkInDate(), tasks.checkOutDate(),
                tasks.totalBudgetUSD(),
                context.getExtendedState().get("weatherData", String.class),
                context.getExtendedState().get("currencyData", String.class),
                context.getExtendedState().get("flightData", String.class),
                context.getExtendedState().get("hotelData", String.class),
                context.getExtendedState().get("directionsData", String.class)
        );

        String result =   chatClient.prompt().options(OpenAiChatOptions.builder().model("gpt-4o-mini").maxTokens(500).temperature(0.3).build())
                .system("You are an expert Travel Assistant. Produce a polished Markdown travel report.")
                .user(finalMarkdownPrompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getConversationId()))
                .call()
                .content();
        // Save Entity
        TripPlanEntity entity = new TripPlanEntity();
        entity.setConversationId(request.getConversationId());
        entity.setDestination(tasks.destination());
        entity.setDays(tasks.days());
        entity.setBudgetUSD(tasks.totalBudgetUSD());
        entity.setGeneratedItinerary(result);
        tripPlanRepository.save(entity);

        aggregatorSample.stop(meterRegistry.timer("ai.pipeline.stage", "step", "aggregator_llm"));
        context.getExtendedState().getVariables().put("finalResult", result);

        boolean accepted =
                context.getStateMachine()
                        .sendEvent(TravelEvent.AGGREGATION_COMPLETED);

        System.out.println(
                "🔥 AGGREGATION_COMPLETED accepted? " + accepted
        );
    }

    private double extractPrice(String flightText) {
        try {
            int dollarSignIdx = flightText.indexOf('$');
            if (dollarSignIdx != -1) {
                String pricePart = flightText.substring(dollarSignIdx + 1).split(" ")[0];
                return Double.parseDouble(pricePart);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private void failWorkflow(StateContext<TravelState, TravelEvent> context, String reason) {
        context.getExtendedState().getVariables().put("earlyExitResult", reason);
        context.getExtendedState().getVariables().put("finalResult", reason);
        boolean accepted = context.getStateMachine().sendEvent(
                MessageBuilder
                        .withPayload(TravelEvent.ERROR_ENCOUNTERED)
                        .build()
        );
        System.out.println(
                "🔥 ERROR_ENCOUNTERED accepted? " + accepted
        );
    }
}
