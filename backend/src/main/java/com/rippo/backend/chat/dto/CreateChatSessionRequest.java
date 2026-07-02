package com.rippo.backend.chat.dto;

public record CreateChatSessionRequest(
        String repositoryOwner,
        String repositoryName,
        String title
) {
}
