import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LlmJudgeEvaluator {

    @Autowired
    private final ChatClient judgeChatClient;

    public LlmJudgeEvaluator(@Qualifier("highReasoningJudgeClient") ChatClient judgeChatClient) {

        this.judgeChatClient = judgeChatClient;
    }

    public record EvalResult(double score, String reasoning, boolean passed) {}

    public EvalResult evaluateCorrectness(String inputPrompt, String actualOutput, String groundTruthContext) {
        String judgePrompt = """
                Evaluate the actual output against the input prompt and baseline context.
                
                Input Prompt: %s
                Retrieved Context / Tool Output: %s
                Actual AI Output: %s
                
                Provide your evaluation in the following format:
                Score: [0.0 to 1.0]
                Reasoning: [Explanation of score]
                """.formatted(inputPrompt, groundTruthContext, actualOutput);

        String response = judgeChatClient.prompt(judgePrompt).call().content();

        double score = extractScore(response);
        return new EvalResult(score, response, score >= 0.8);
    }

    private double extractScore(String response) {
        // Simple regex parser for "Score: X.X"
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().startsWith("score:")) {
                    return Double.parseDouble(line.split(":")[1].trim());
                }
            }
        } catch (Exception e) {
            return 0.0;
        }
        return 0.0;
    }
}