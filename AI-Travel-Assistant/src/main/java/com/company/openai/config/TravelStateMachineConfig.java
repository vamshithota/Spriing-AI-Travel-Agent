package com.company.openai.config;

import com.company.openai.enums.TravelEvent;
import com.company.openai.enums.TravelState;
import com.company.openai.orchestrators.TravelOrchestratorListener;
import com.company.openai.security.TravelGuards;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.action.Action;

@Configuration
@EnableStateMachineFactory
public class TravelStateMachineConfig extends EnumStateMachineConfigurerAdapter<TravelState, TravelEvent> {
    @Autowired
    private final TravelGuards travelGuards;

    @Autowired
    private final TravelOrchestratorListener orchestrator;

    public TravelStateMachineConfig(
            TravelGuards travelGuards,
            TravelOrchestratorListener orchestrator) {

            this.travelGuards = travelGuards;
            this.orchestrator = orchestrator;
        }

@Override
public void configure(
        StateMachineStateConfigurer<TravelState, TravelEvent> states
) throws Exception {
    states
            .withStates()
            .initial(TravelState.INITIAL)
            .state(
                    TravelState.PLANNING,
                    plannerAction(),
                    null
            )
            .state(
                    TravelState.FETCHING_TOOLS,
                    toolsAction(),
                    null
            )
            .state(
                    TravelState.AGGREGATING,
                    aggregatorAction(),
                    null
            )
            .end(TravelState.COMPLETED)
            .end(TravelState.FAILED);
}
    @Override
    public void configure(
            StateMachineTransitionConfigurer<TravelState, TravelEvent> transitions
    ) throws Exception {

        transitions

                // INITIAL -> PLANNING
                .withExternal()
                .source(TravelState.INITIAL)
                .target(TravelState.PLANNING)
                .event(TravelEvent.START_PLANNING)


                // PLANNING -> FETCHING_TOOLS
                .and()
                .withExternal()
                .source(TravelState.PLANNING)
                .target(TravelState.FETCHING_TOOLS)
                .event(TravelEvent.TASKS_GENERATED)


                // PLANNING -> FAILED
                .and()
                .withExternal()
                .source(TravelState.PLANNING)
                .target(TravelState.FAILED)
                .event(TravelEvent.ERROR_ENCOUNTERED)

                // FETCHING_TOOLS -> AGGREGATING
                .and()
                .withExternal()
                .source(TravelState.FETCHING_TOOLS)
                .target(TravelState.AGGREGATING)
                .event(TravelEvent.TOOLS_COMPLETED)


                // FETCHING_TOOLS -> FETCHING_TOOLS
                .and()
                .withInternal()
                .source(TravelState.FETCHING_TOOLS)
                .event(TravelEvent.RETRY)
                .guard(travelGuards.retryAllowedGuard())

                // FETCHING_TOOLS -> FAILED
                .and()
                .withExternal()
                .source(TravelState.FETCHING_TOOLS)
                .target(TravelState.FAILED)
                .event(TravelEvent.ERROR_ENCOUNTERED)
                .guard(travelGuards.retryExhaustedGuard())

                // AGGREGATING -> COMPLETED
                .and()
                .withExternal()
                .source(TravelState.AGGREGATING)
                .target(TravelState.COMPLETED)
                .event(TravelEvent.AGGREGATION_COMPLETED);
    }
    @Bean
    public Action<TravelState, TravelEvent> plannerAction() {
        return context -> orchestrator.handlePlannerStage(context);
    }

    @Bean
    public Action<TravelState, TravelEvent> toolsAction() {
        return context -> orchestrator.handleParallelToolsStage(context);
    }

    @Bean
    public Action<TravelState, TravelEvent> aggregatorAction() {
        return context -> orchestrator.handleAggregatorStage(context);
    }
}
