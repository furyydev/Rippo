package com.rippo.backend.ai.controller;

import com.rippo.backend.ai.dto.AiErrorResponse;
import com.rippo.backend.ai.service.GeminiException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AiTestController.class)
public class AiExceptionHandler {

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<AiErrorResponse> handleGeminiException(
            GeminiException exception,
            HttpServletRequest request
    ) {
        AiErrorResponse response = new AiErrorResponse(
                Instant.now(),
                exception.getStatus().value(),
                exception.getStatus().getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(exception.getStatus()).body(response);
    }
}
