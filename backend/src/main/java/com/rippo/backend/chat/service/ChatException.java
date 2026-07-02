package com.rippo.backend.chat.service;

import org.springframework.http.HttpStatus;

public class ChatException extends RuntimeException {

    private final HttpStatus status;

    public ChatException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ChatException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
