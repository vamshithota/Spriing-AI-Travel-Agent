package com.company.openai.config;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtelTracingConfig {

    @Value("${management.otlp.tracing.endpoint:http://localhost:4318/v1/traces}")
    private String url;

    @Bean
    public OtlpHttpSpanExporter otlpHttpSpanExporter() {
        return OtlpHttpSpanExporter.builder()
                .setEndpoint(url)
                .build();
    }
}