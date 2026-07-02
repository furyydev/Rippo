package com.rippo.backend.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rippo.backend.ai.service.GeminiService;
import com.rippo.backend.chat.dto.ChatRequest;
import com.rippo.backend.chat.dto.ChatResponse;
import com.rippo.backend.chat.model.RepositoryContext;
import com.rippo.backend.entity.ChatMessage;
import com.rippo.backend.entity.ChatMessageRole;
import com.rippo.backend.entity.ChatSession;
import com.rippo.backend.entity.User;
import com.rippo.backend.service.ChatPersistenceService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ChatServiceTests {

    @Test
    void orchestratesContextGeminiAndMessagePersistence() {
        ChatPersistenceService persistence = mock(ChatPersistenceService.class);
        RepositoryContextService contextService = mock(RepositoryContextService.class);
        PromptBuilder promptBuilder = mock(PromptBuilder.class);
        GeminiService geminiService = mock(GeminiService.class);
        ChatService chatService = new ChatService(
                persistence,
                contextService,
                promptBuilder,
                geminiService
        );

        ChatRequest request = new ChatRequest(
                "owner",
                "repo",
                10L,
                "Explain authentication"
        );
        User user = mock(User.class);
        ChatSession session = mock(ChatSession.class);
        ChatMessage userMessage = mock(ChatMessage.class);
        ChatMessage assistantMessage = mock(ChatMessage.class);
        RepositoryContext context = new RepositoryContext(
                "owner",
                "repo",
                "main",
                "README",
                List.of()
        );
        Instant responseTime = Instant.parse("2026-07-02T00:00:00Z");

        when(user.getId()).thenReturn(7L);
        when(session.getId()).thenReturn(10L);
        when(session.getRepositoryOwner()).thenReturn("owner");
        when(session.getRepositoryName()).thenReturn("repo");
        when(persistence.findSessionForUser(10L, 7L)).thenReturn(Optional.of(session));
        when(contextService.loadContext(session, "owner", "repo", "token"))
                .thenReturn(context);
        when(promptBuilder.build(context, "Explain authentication")).thenReturn("prompt");
        when(persistence.addMessage(session, ChatMessageRole.USER, "Explain authentication"))
                .thenReturn(userMessage);
        when(geminiService.generateText("prompt")).thenReturn("Authentication explanation");
        when(persistence.addMessage(
                session,
                ChatMessageRole.ASSISTANT,
                "Authentication explanation"
        )).thenReturn(assistantMessage);
        when(assistantMessage.getContent()).thenReturn("Authentication explanation");
        when(assistantMessage.getCreatedAt()).thenReturn(responseTime);

        ChatResponse response = chatService.chat(request, user, "token");

        assertThat(response.assistantMessage()).isEqualTo("Authentication explanation");
        assertThat(response.chatSessionId()).isEqualTo(10L);
        assertThat(response.timestamp()).isEqualTo(responseTime);

        InOrder order = inOrder(persistence, geminiService);
        order.verify(persistence).addMessage(
                session,
                ChatMessageRole.USER,
                "Explain authentication"
        );
        order.verify(geminiService).generateText("prompt");
        order.verify(persistence).addMessage(
                session,
                ChatMessageRole.ASSISTANT,
                "Authentication explanation"
        );
        verify(contextService).loadContext(session, "owner", "repo", "token");
    }
}
