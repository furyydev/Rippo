package com.rippo.backend.chat.dto;

import com.rippo.backend.entity.ChatSession;
import java.time.Instant;

public record ChatSessionResponse(
        Long chatSessionId,
        String repositoryOwner,
        String repositoryName,
        String title,
        Instant createdAt
) {
    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getRepositoryOwner(),
                session.getRepositoryName(),
                session.getTitle(),
                session.getCreatedAt()
        );
    }
}
