package com.rippo.backend.ai.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.util.retry.Retry;

@Service
public class GeminiService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String JSON_MIME_TYPE = "application/json";

    private final WebClient webClient;
    private final String model;
    private final double temperature;
    private final int maxOutputTokens;
    private final int maxRetryAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;

    public GeminiService(WebClient webClient, String model) {
        this(webClient, model, 0.1, 4096, 3, 500, 4000);
    }

    @Autowired
    public GeminiService(
            @Qualifier("geminiWebClient") WebClient webClient,
            @Value("${gemini.model}") String model,
            @Value("${gemini.temperature:0.1}") double temperature,
            @Value("${gemini.max-output-tokens:4096}") int maxOutputTokens,
            @Value("${gemini.retry.max-attempts:3}") int maxRetryAttempts,
            @Value("${gemini.retry.initial-backoff-ms:500}") long initialBackoffMillis,
            @Value("${gemini.retry.max-backoff-ms:4000}") long maxBackoffMillis
    ) {
        this.webClient = webClient;
        this.model = model;
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;
        this.maxRetryAttempts = maxRetryAttempts;
        this.initialBackoff = Duration.ofMillis(initialBackoffMillis);
        this.maxBackoff = Duration.ofMillis(maxBackoffMillis);
    }

    /**
     * Generates free-form text. Used by the plain AI test endpoint.
     */
    public String generateText(String prompt) {
        return generate(prompt, null);
    }

    /**
     * Generates a response constrained to JSON output. Used by the agent loop so the
     * MCP tool-calling protocol is emitted deterministically.
     */
    public String generateJson(String prompt) {
        return generate(prompt, JSON_MIME_TYPE);
    }

    private String generate(String prompt, String responseMimeType) {
        if (prompt == null || prompt.isBlank()) {
            throw new GeminiException(HttpStatus.BAD_REQUEST, "Message must not be empty");
        }

        GeminiRequest request = new GeminiRequest(
                List.of(new Content(List.of(new Part(prompt.trim())))),
                new GenerationConfig(temperature, maxOutputTokens, responseMimeType)
        );

        try {
            GeminiResponse response = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .retryWhen(transientRetry())
                    .block();

            return extractText(response);
        } catch (WebClientResponseException exception) {
            throw mapApiError(exception);
        } catch (WebClientRequestException exception) {
            throw new GeminiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not connect to the Gemini API",
                    exception
            );
        } catch (GeminiException exception) {
            throw exception;
        } catch (Exception exception) {
            if (Exceptions.unwrap(exception) instanceof TimeoutException) {
                throw new GeminiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "The Gemini API request timed out",
                        exception
                );
            }
            throw new GeminiException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini returned an invalid response",
                    exception
            );
        }
    }

    private Retry transientRetry() {
        return Retry.backoff(maxRetryAttempts, initialBackoff)
                .maxBackoff(maxBackoff)
                .filter(this::isTransientFailure)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private boolean isTransientFailure(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            return status == 429 || responseException.getStatusCode().is5xxServerError();
        }
        // Connection resets and other transport-level faults are transient.
        return throwable instanceof WebClientRequestException;
    }

    private String extractText(GeminiResponse response) {
        if (response == null) {
            throw invalidResponse();
        }

        if (response.promptFeedback() != null
                && response.promptFeedback().blockReason() != null
                && !response.promptFeedback().blockReason().isBlank()) {
            throw blockedByFilters(
                    "Gemini blocked the request before answering (reason: "
                            + response.promptFeedback().blockReason() + ")"
            );
        }

        if (response.candidates() == null || response.candidates().isEmpty()) {
            throw invalidResponse();
        }

        Candidate candidate = response.candidates().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(this::invalidResponse);

        verifyFinishReason(candidate.finishReason());

        String text = response.candidates().stream()
                .filter(Objects::nonNull)
                .map(Candidate::content)
                .filter(Objects::nonNull)
                .map(Content::parts)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(Part::text)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining());

        if (text.isBlank()) {
            throw invalidResponse();
        }

        return text;
    }

    private void verifyFinishReason(String finishReason) {
        if (finishReason == null || finishReason.isBlank()) {
            return;
        }
        switch (finishReason.toUpperCase(Locale.ROOT)) {
            case "STOP" -> {
                // Normal completion.
            }
            case "MAX_TOKENS" -> throw new GeminiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The Gemini response was cut off after reaching the output token limit"
            );
            case "SAFETY" -> throw blockedByFilters(
                    "Gemini blocked the response because of its safety filters"
            );
            case "RECITATION" -> throw blockedByFilters(
                    "Gemini blocked the response because of recitation constraints"
            );
            default -> {
                // OTHER / unspecified reasons still allow any produced text to be used.
            }
        }
    }

    private GeminiException mapApiError(WebClientResponseException exception) {
        int status = exception.getStatusCode().value();

        if (status == 401
                || status == 403
                || exception.getResponseBodyAsString().contains("API_KEY_INVALID")) {
            return new GeminiException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini API key is invalid or unauthorized",
                    exception
            );
        }
        if (status == 400) {
            return new GeminiException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini rejected the request; check the configured model",
                    exception
            );
        }
        if (status == 429) {
            return new GeminiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini is temporarily rate-limited",
                    exception
            );
        }
        if (exception.getStatusCode().is5xxServerError()) {
            return new GeminiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini is temporarily unavailable",
                    exception
            );
        }
        return new GeminiException(
                HttpStatus.BAD_GATEWAY,
                "Gemini could not process the request",
                exception
        );
    }

    private GeminiException invalidResponse() {
        return new GeminiException(
                HttpStatus.BAD_GATEWAY,
                "Gemini returned an invalid response"
        );
    }

    private GeminiException blockedByFilters(String message) {
        return new GeminiException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GenerationConfig(
            Double temperature,
            Integer maxOutputTokens,
            String responseMimeType
    ) {
    }

    private record GeminiResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {
    }

    private record PromptFeedback(String blockReason) {
    }

    private record Candidate(Content content, String finishReason) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }
}
