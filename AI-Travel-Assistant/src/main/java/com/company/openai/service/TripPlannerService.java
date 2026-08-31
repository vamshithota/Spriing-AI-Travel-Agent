package com.company.openai.service;

import com.company.openai.dto.TripBudgetPlan;
import com.company.openai.tools.TravelTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class TripPlannerService {

    private final ChatClient chatClient;
    private final TravelTools travelTools;

    public TripPlannerService(ChatClient.Builder builder, ChatMemory chatMemory, TravelTools travelTools) {
        this.travelTools = travelTools;
        this.chatClient = builder
                .defaultAdvisors(a -> a.param(ChatMemory.CONVERSATION_ID, "planner-session"))
                .build();
    }


    public String planStructuredTrip(String destination, int days, double totalBudgetUSD, String conversationId) {
        // STAGE 1: PLANNER (No tools, forces pure structured reasoning & math)
        TripBudgetPlan initialPlan = chatClient.prompt()
                .system("""
                    You are a Strategic Financial Trip Planner. 
                    Given a target destination, duration, and budget, create a realistic budget allocation.
                    Be conservative with estimates and always keep a safety buffer.
                    """)
                .user(String.format("Plan a budget breakdown for a %d-day" +
                                " trip to %s with a total budget of $%.2f USD.",
                        days, destination, totalBudgetUSD))
                .call()
                .entity(TripBudgetPlan.class);
        if(initialPlan== null){
            return "Failed to generate initial trip budget structure.";
        }
        // STAGE 2: EXECUTOR (Tool-enabled ReAct Loop to ground the plan in facts)
            String executionPrompt = String.format("""
                        Execute and ground this trip plan using real tools:
                                   \s
                                    Destination: %s | Days: %d | Total Budget: $%.2f USD
                                    Targets: Flight <= $%.2f | Hotel <= $%.2f/night | Daily Expenses <= $%.2f
                                    Highlights: %s
                                   \s
                                    RULES FOR OUTPUT FORMATTING:
                                    1. DO NOT ask the user clarifying questions or block execution. Assume origin airport is 'JFK', dates start 60 days from today, and default to 1 traveler in Economy.
                                    2. ALWAYS execute tools (`searchFlights`, `searchHotels`, `getWeather`, `calculateCurrency`) to verify prices and weather.
                                    3. Structure the final output strictly using bold section titles and concise tables/bullets:
                                       - **Executive Summary**
                                       - **Budget Allocation & Currency Breakdown** (Use a Markdown Table)
                                       - **Flight & Accommodation Options** (From Tool Data)
                                       - **Current Weather & Preparation**
                                       - **Day-by-Day Itinerary** (Day 1 to Day %d)
            """,
                initialPlan.destination(),
                initialPlan.totalDays(),
                initialPlan.totalBudgetUSD(),
                initialPlan.budgetBreakdown().flightTargetUSD(),
                initialPlan.budgetBreakdown().hotelNightlyTargetUSD(),
                initialPlan.budgetBreakdown().dailyFoodAndTransitUSD(),
                String.join(", ", initialPlan.plannedActivities()),
                initialPlan.totalDays()
        );
        return chatClient.prompt()
                .system("""
                        You are an expert Travel Execution Agent.
                        Generate clean, highly scannable Markdown responses.
                        Never print raw JSON or internal thought reasoning to the user.
                    """)
                .user(executionPrompt)
                .tools(travelTools)
                .advisors(a ->a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }


}
