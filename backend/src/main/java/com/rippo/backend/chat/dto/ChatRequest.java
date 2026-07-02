package com.rippo.backend.chat.dto;

public record ChatRequest(
        String repositoryOwner,
        String repositoryName,
        Long chatSessionId,
        String message
) {
}
