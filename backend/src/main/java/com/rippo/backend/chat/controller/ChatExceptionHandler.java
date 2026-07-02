package com.rippo.backend.chat.controller;

import com.rippo.backend.ai.service.GeminiException;
import com.rippo.backend.chat.dto.ChatErrorResponse;
import com.rippo.backend.chat.service.ChatException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ChatController.class)
public class ChatExceptionHandler {

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ChatErrorResponse> handleChatException(
            ChatException exception,
            HttpServletRequest request
    ) {
        return error(
                exception.getStatus(),
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<ChatErrorResponse> handleGeminiException(
            GeminiException exception,
            HttpServletRequest request
    ) {
        return error(
                exception.getStatus(),
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ChatErrorResponse> error(
            HttpStatus status,
            String message,
            String path
    ) {
        ChatErrorResponse response = new ChatErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(response);
    }
}
