package com.rippo.backend.chat.dto;

import java.time.Instant;

public record ChatResponse(
        String assistantMessage,
        Long chatSessionId,
        Instant timestamp
) {
}
