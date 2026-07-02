package com.rippo.backend.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiConfig {

    @Bean("geminiWebClient")
    public WebClient geminiWebClient(
            WebClient.Builder builder,
            @Value("${gemini.api-key}") String apiKey
    ) {
        return builder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }
}
