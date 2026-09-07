package com.company.openai.config;

import com.company.openai.enums.TravelEvent;
import com.company.openai.enums.TravelState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class TravelStateMachineConfig extends EnumStateMachineConfigurerAdapter<TravelState, TravelEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<TravelState, TravelEvent> states) throws Exception {
        states.withStates()
                .initial(TravelState.INITIAL)
                .states(EnumSet.allOf(TravelState.class))
                .end(TravelState.COMPLETED)
                .end(TravelState.FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TravelState, TravelEvent> transitions) throws Exception {
        transitions
                .withExternal().source(TravelState.INITIAL).target(TravelState.SEARCHING_FLIGHTS).event(TravelEvent.START)
                .and()
                .withExternal().source(TravelState.SEARCHING_FLIGHTS).target(TravelState.SEARCHING_HOTELS).event(TravelEvent.FLIGHTS_FOUND)
                .and()
                .withExternal().source(TravelState.SEARCHING_HOTELS).target(TravelState.CALCULATING_BUDGET).event(TravelEvent.HOTELS_FOUND)
                .and()
                .withExternal().source(TravelState.CALCULATING_BUDGET).target(TravelState.COMPLETED).event(TravelEvent.BUDGET_CALCULATED)
                .and()
                .withExternal().source(TravelState.SEARCHING_FLIGHTS).target(TravelState.FAILED).event(TravelEvent.ERROR_ENCOUNTERED)
                .and()
                .withExternal().source(TravelState.SEARCHING_HOTELS).target(TravelState.FAILED).event(TravelEvent.ERROR_ENCOUNTERED)
                .and()
                .withExternal().source(TravelState.CALCULATING_BUDGET).target(TravelState.FAILED).event(TravelEvent.ERROR_ENCOUNTERED);
    }
}
