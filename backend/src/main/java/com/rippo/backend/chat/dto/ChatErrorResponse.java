package com.rippo.backend.chat.dto;

import java.time.Instant;

public record ChatErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
