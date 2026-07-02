package com.rippo.backend.ai.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final String model;

    public GeminiService(
            @Qualifier("geminiWebClient") WebClient webClient,
            @Value("${gemini.model}") String model
    ) {
        this.webClient = webClient;
        this.model = model;
    }

    public String generateText(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new GeminiException(HttpStatus.BAD_REQUEST, "Message must not be empty");
        }

        GeminiRequest request = new GeminiRequest(
                List.of(new Content(List.of(new Part(prompt.trim()))))
        );

        try {
            GeminiResponse response = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(30))
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

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null) {
            throw invalidResponse();
        }

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

    private record GeminiRequest(List<Content> contents) {
    }

    private record GeminiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }
}
