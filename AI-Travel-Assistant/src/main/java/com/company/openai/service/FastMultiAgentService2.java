package com.company.openai.service;

import com.company.openai.dto.TripPlanRequest;
import com.company.openai.enums.TravelEvent;
import com.company.openai.enums.TravelState;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FastMultiAgentService2 {

    // implementing multiagent service based on FastMultiAgentService class
    // using Spring State machine
    private final StateMachineFactory<TravelState, TravelEvent> stateMachineFactory;
    private final MeterRegistry meterRegistry;

    public FastMultiAgentService2(StateMachineFactory<TravelState, TravelEvent> stateMachineFactory, MeterRegistry meterRegistry) {
        this.stateMachineFactory = stateMachineFactory;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public String planTripFast(TripPlanRequest tripPlanRequest) {

        System.out.println("\n========================================");
        System.out.println("🔥🔥🔥 PLAN TRIP FAST CALLED 🔥🔥🔥");
        System.out.println("========================================");

        Timer.Sample overallSample = Timer.start(meterRegistry);

        String conversationId = tripPlanRequest.getConversationId();

        System.out.println("🔥 Conversation ID: " + conversationId);

        StateMachine<TravelState, TravelEvent> stateMachine =
                stateMachineFactory.getStateMachine(conversationId);

        System.out.println("🔥 State machine created");
        System.out.println("🔥 State machine ID: " + stateMachine.getId());

        try {

            // 1. START STATE MACHINE
            System.out.println("\n🔥 [1] Starting state machine...");
            stateMachine.startReactively().block();
            System.out.println("✅ [1] State machine started successfully");
            System.out.println(
                    "🔥 [1] Current state after start: "
                            + stateMachine.getState().getId()
            );

            System.out.println("\n🔥 [2] Storing TripPlanRequest...");

            stateMachine.getExtendedState()
                    .getVariables()
                    .put("request", tripPlanRequest);

            System.out.println("✅ [2] TripPlanRequest stored in extended state");
            System.out.println("\n🔥 [3] Sending START_PLANNING event...");

            Message<TravelEvent> startPlanningMessage =
                    MessageBuilder
                            .withPayload(TravelEvent.START_PLANNING)
                            .build();

            boolean accepted =
                    stateMachine.sendEvent(startPlanningMessage);

            System.out.println(
                    "🔥 [3] START_PLANNING accepted? " + accepted
            );

            System.out.println(
                    "🔥 [3] Current state after START_PLANNING: "
                            + stateMachine.getState().getId()
            );

            System.out.println("\n🔥 [4] Checking for early exit...");

            String earlyExitResult =
                    stateMachine.getExtendedState()
                            .get("earlyExitResult", String.class);

            if (earlyExitResult != null) {

                System.out.println(
                        "⚠️ [4] Early exit detected:"
                );

                System.out.println(earlyExitResult);

                return earlyExitResult;
            }

            System.out.println("✅ [4] No early exit");

            System.out.println("\n🔥 [5] Checking state...");

            TravelState currentState =
                    stateMachine.getState().getId();

            System.out.println(
                    "🔥 [5] Current state: " + currentState
            );

            if (currentState == TravelState.FAILED) {

                System.out.println(
                        "❌ [5] State machine entered FAILED state"
                );

                return "Failed to complete travel plan workflow due to tool or execution errors.";
            }


            System.out.println("\n🔥 [6] Checking final result...");

            String finalResult =
                    stateMachine.getExtendedState()
                            .get("finalResult", String.class);

            if (finalResult != null) {

                System.out.println(
                        "✅ [6] Final result found"
                );

                return finalResult;
            }

            System.out.println(
                    "⚠️ [6] Final result is currently NULL"
            );

            System.out.println(
                    "🔥 [6] Current state: "
                            + stateMachine.getState().getId()
            );


            System.out.println(
                    "\n⚠️ Workflow has not produced finalResult yet."
            );

            return "Travel workflow started but has not produced a final result yet.";

        } catch (Exception e) {

            System.err.println("\n========================================");
            System.err.println("❌❌❌ ERROR IN PLAN TRIP FAST ❌❌❌");
            System.err.println("========================================");

            e.printStackTrace();

            throw e;

        } finally {

            overallSample.stop(
                    meterRegistry.timer(
                            "ai.pipeline.stage",
                            "step",
                            "total_pipeline"
                    )
            );

            System.out.println(
                    "\n🔥 Stopping state machine..."
            );

            try {

                stateMachine.stopReactively().block();

                System.out.println(
                        "✅ State machine stopped successfully"
                );

            } catch (Exception e) {

                System.err.println(
                        "❌ Error while stopping state machine"
                );

                e.printStackTrace();
            }

            System.out.println("========================================");
            System.out.println("🔥🔥🔥 PLAN TRIP FAST FINISHED 🔥🔥🔥");
            System.out.println("========================================\n");
        }
    }
}
