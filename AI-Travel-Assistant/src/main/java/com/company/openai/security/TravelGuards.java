package com.company.openai.security;

import com.company.openai.enums.TravelEvent;
import com.company.openai.enums.TravelState;
import org.springframework.stereotype.Component;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
public class TravelGuards {

    private static final int MAX_RETRIES = 3;

    public Guard<TravelState, TravelEvent> retryAllowedGuard() {
        return context -> {
            Integer retryCount = context.getExtendedState().get("retryCount", Integer.class);
            return retryCount != null && retryCount < MAX_RETRIES;
        };
    }

    public Guard<TravelState, TravelEvent> retryExhaustedGuard() {
        return context -> {
            Integer retryCount = context.getExtendedState().get("retryCount", Integer.class);
            return retryCount != null && retryCount >= MAX_RETRIES;
        };
    }


}
