package com.company.openai.security;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PromptSanitizer {

    private static final int MAX_PROMPT_LENGTH = 1000;

    // Hardcoded structural & system override rules
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(all\\s+)?previous\\s+instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+prior\\s+system\\s+prompts", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now\\s+in\\s+DAN\\s+mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script.*?>.*?</script>", Pattern.CASE_INSENSITIVE)
    );

    // Proprietary / Company PII patterns (Example: Internal Employee IDs or SSN)
    private static final List<Pattern> PII_PATTERNS = List.of(
            Pattern.compile("\\b[A-Z]{2}-\\d{6}\\b"), // Example internal project code format
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b") // SSN format
    );

    public ValidationResult validate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return ValidationResult.invalid("Prompt cannot be empty.");
        }

        if (prompt.length() > MAX_PROMPT_LENGTH) {
            return ValidationResult.invalid("Prompt exceeds maximum length limit.");
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                return ValidationResult.invalid("Security Violation: Restricted prompt injection pattern detected.");
            }
        }

        for (Pattern pattern : PII_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                return ValidationResult.invalid("Compliance Violation: Restricted personal or internal data detected.");
            }
        }

        return ValidationResult.valid();
    }

    public record ValidationResult(boolean isValid, String reason) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}