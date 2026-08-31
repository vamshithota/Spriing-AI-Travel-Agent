package com.company.openai.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HybridGuardrailInterceptor implements Filter {

    private final PromptSanitizer sanitizer;
    private final LlamaGuardClient llamaGuardClient;

    public HybridGuardrailInterceptor(
            PromptSanitizer sanitizer,
            LlamaGuardClient llamaGuardClient) {
        this.sanitizer = sanitizer;
        this.llamaGuardClient = llamaGuardClient;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();

        boolean targetEndpoint =
                httpRequest.getRequestURI().startsWith("/api/")
                        && ("POST".equalsIgnoreCase(method)
                        || "PUT".equalsIgnoreCase(method)
                        || "PATCH".equalsIgnoreCase(method));

        if (!targetEndpoint) {
            chain.doFilter(request, response);
            return;
        }

        // Cache request body
        CachedBodyHttpServletRequest wrappedRequest =
                new CachedBodyHttpServletRequest(httpRequest);

        String body = new String(
                wrappedRequest.getCachedBody(),
                StandardCharsets.UTF_8
        );

        if (!body.isBlank()) {

            // STAGE 1: Static rules
            PromptSanitizer.ValidationResult staticResult =
                    sanitizer.validate(body);

            if (!staticResult.isValid()) {
                rejectRequest(
                        httpResponse,
                        HttpStatus.BAD_REQUEST,
                        staticResult.reason()
                );
                return;
            }

            // STAGE 2: Llama Guard
            LlamaGuardClient.GuardResult guardResult =
                    llamaGuardClient.checkSafety(body);

            if (!guardResult.isSafe()) {
                rejectRequest(
                        httpResponse,
                        HttpStatus.FORBIDDEN,
                        "Security Violation: Content classified as unsafe ("
                                + guardResult.details() + ")"
                );
                return;
            }
        }

        // Safe → controller can read body
        chain.doFilter(wrappedRequest, response);
    }

    private void rejectRequest(
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType("application/json");

        String json = String.format(
                "{\"error\": \"%s\", \"message\": \"%s\"}",
                status.getReasonPhrase(),
                message
        );

        response.getWriter().write(json);
    }
}