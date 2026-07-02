package com.rippo.backend.ai.dto;

import java.time.Instant;

public record AiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
