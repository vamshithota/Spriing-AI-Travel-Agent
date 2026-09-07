

import com.company.openai.TravelAgentAiApplication;
import com.company.openai.service.TravelAgentService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

//@SpringBootTest(classes = {SpringBootApplication.class,
//        com.company.openai.evals.config.TestEvaluatorConfig.class,
//        TravelAgentService.class
//})
@SpringBootTest(classes = TravelAgentAiApplication.class)
@Import(com.company.openai.evals.config.TestEvaluatorConfig.class)
//@Disabled("Temporarily disabled for debugging")
public class TravelAgentE2EEvalTest {

    @Autowired
    private TravelAgentService travelAgentService;

    @Autowired
    private LlmJudgeEvaluator judgeEvaluator;

    @Test
    @DisplayName("Eval: Flight & Hotel Budget Constraint Adherence")
    void evalBudgetConstraintsAndResponseQuality() {
        String userQuery = "Find flights from JFK to LHR under $800 and hotels under $200/night for 2026-09-15.";

        // 1. Execute full agent workflow
        String conversationId = "test-session-" + UUID.randomUUID();
        String actualResponse = travelAgentService.processTravelRequest(userQuery, conversationId);

        // 2. Deterministic Checks (Assertion Layer)
        assertTrue(actualResponse.contains("JFK"), "Response missing origin airport");
        assertTrue(actualResponse.contains("LHR"), "Response missing destination airport");

        // 3. LLM-as-a-Judge Evaluation (Semantic Layer)
        var evalResult = judgeEvaluator.evaluateCorrectness(
                userQuery,
                actualResponse,
                "User requested flights under $800 and hotels under $200. Check if constraints were respected."
        );

        System.out.println("Eval Score: " + evalResult.score());
        System.out.println("Eval Reasoning:\n" + evalResult.reasoning());

        assertTrue(evalResult.passed(), "Eval failed with score < 0.8: " + evalResult.reasoning());
    }
}