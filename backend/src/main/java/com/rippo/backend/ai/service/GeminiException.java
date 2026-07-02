package com.rippo.backend.ai.service;

import org.springframework.http.HttpStatus;

public class GeminiException extends RuntimeException {

    private final HttpStatus status;

    public GeminiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public GeminiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
