package com.rippo.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

class GeminiServiceTests {

    @Test
    void returnsTextFromGeminiResponse() {
        WebClient webClient = clientReturning(
                HttpStatus.OK,
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {"text": "Spring Boot makes Spring applications easier to build."}
                        ]
                      }
                    }
                  ]
                }
                """
        );
        GeminiService service = new GeminiService(webClient, "gemini-2.5-flash");

        String response = service.generateText("Explain Spring Boot");

        assertThat(response)
                .isEqualTo("Spring Boot makes Spring applications easier to build.");
    }

    @Test
    void rejectsBlankPromptBeforeCallingGemini() {
        GeminiService service = new GeminiService(WebClient.builder().build(), "test-model");

        assertThatThrownBy(() -> service.generateText("  "))
                .isInstanceOfSatisfying(GeminiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("Message must not be empty");
                });
    }

    @Test
    void mapsInvalidApiKeyToBadGateway() {
        WebClient webClient = clientReturning(
                HttpStatus.UNAUTHORIZED,
                """
                {"error":{"message":"API key not valid"}}
                """
        );
        GeminiService service = new GeminiService(webClient, "test-model");

        assertThatThrownBy(() -> service.generateText("Hello"))
                .isInstanceOfSatisfying(GeminiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getMessage())
                            .isEqualTo("Gemini API key is invalid or unauthorized");
                });
    }

    @Test
    void rejectsResponseWithoutGeneratedText() {
        WebClient webClient = clientReturning(HttpStatus.OK, """
                {"candidates":[]}
                """);
        GeminiService service = new GeminiService(webClient, "test-model");

        assertThatThrownBy(() -> service.generateText("Hello"))
                .isInstanceOfSatisfying(GeminiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getMessage()).isEqualTo("Gemini returned an invalid response");
                });
    }

    private WebClient clientReturning(HttpStatus status, String body) {
        return WebClient.builder()
                .exchangeFunction(request -> reactor.core.publisher.Mono.just(
                        ClientResponse.create(status)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()
                ))
                .build();
    }
}
