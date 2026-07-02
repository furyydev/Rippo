package com.rippo.backend.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rippo.backend.chat.dto.ChatSessionResponse;
import com.rippo.backend.chat.dto.CreateChatSessionRequest;
import com.rippo.backend.entity.ChatSession;
import com.rippo.backend.entity.User;
import com.rippo.backend.service.ChatPersistenceService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChatSessionServiceTests {

    @Test
    void createsPersistedSessionWithDefaultTitle() {
        ChatPersistenceService persistence = mock(ChatPersistenceService.class);
        ChatSessionService service = new ChatSessionService(persistence);
        User user = mock(User.class);
        ChatSession session = mock(ChatSession.class);
        Instant createdAt = Instant.parse("2026-07-02T00:00:00Z");

        when(persistence.createSession(
                user,
                "octocat",
                "rippo",
                "Chat about octocat/rippo"
        )).thenReturn(session);
        when(session.getId()).thenReturn(42L);
        when(session.getRepositoryOwner()).thenReturn("octocat");
        when(session.getRepositoryName()).thenReturn("rippo");
        when(session.getTitle()).thenReturn("Chat about octocat/rippo");
        when(session.getCreatedAt()).thenReturn(createdAt);

        ChatSessionResponse response = service.create(
                new CreateChatSessionRequest(" octocat ", " rippo ", null),
                user
        );

        assertThat(response.chatSessionId()).isEqualTo(42L);
        assertThat(response.title()).isEqualTo("Chat about octocat/rippo");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
