package com.rippo.backend.chat.service;

import com.rippo.backend.ai.service.GeminiService;
import com.rippo.backend.chat.dto.ChatRequest;
import com.rippo.backend.chat.dto.ChatResponse;
import com.rippo.backend.chat.model.RepositoryContext;
import com.rippo.backend.entity.ChatMessage;
import com.rippo.backend.entity.ChatMessageRole;
import com.rippo.backend.entity.ChatSession;
import com.rippo.backend.entity.User;
import com.rippo.backend.service.ChatPersistenceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatPersistenceService chatPersistenceService;
    private final RepositoryContextService repositoryContextService;
    private final PromptBuilder promptBuilder;
    private final GeminiService geminiService;

    public ChatService(
            ChatPersistenceService chatPersistenceService,
            RepositoryContextService repositoryContextService,
            PromptBuilder promptBuilder,
            GeminiService geminiService
    ) {
        this.chatPersistenceService = chatPersistenceService;
        this.repositoryContextService = repositoryContextService;
        this.promptBuilder = promptBuilder;
        this.geminiService = geminiService;
    }

    public ChatResponse chat(ChatRequest request, User user, String accessToken) {
        validateRequest(request);

        String repositoryOwner = request.repositoryOwner().trim();
        String repositoryName = request.repositoryName().trim();
        String userMessage = request.message().trim();

        ChatSession chatSession = chatPersistenceService.findSessionForUser(
                        request.chatSessionId(),
                        user.getId()
                )
                .orElseThrow(() -> new ChatException(
                        HttpStatus.NOT_FOUND,
                        "Chat session was not found"
                ));

        validateSessionRepository(chatSession, repositoryOwner, repositoryName);

        RepositoryContext context = repositoryContextService.loadContext(
                chatSession,
                repositoryOwner,
                repositoryName,
                accessToken
        );
        String prompt = promptBuilder.build(context, userMessage);

        chatPersistenceService.addMessage(
                chatSession,
                ChatMessageRole.USER,
                userMessage
        );
        String assistantText = geminiService.generateText(prompt);
        ChatMessage assistantMessage = chatPersistenceService.addMessage(
                chatSession,
                ChatMessageRole.ASSISTANT,
                assistantText
        );

        return new ChatResponse(
                assistantMessage.getContent(),
                chatSession.getId(),
                assistantMessage.getCreatedAt()
        );
    }

    private void validateRequest(ChatRequest request) {
        if (request == null) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.repositoryOwner() == null || request.repositoryOwner().isBlank()) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Repository owner is required");
        }
        if (request.repositoryName() == null || request.repositoryName().isBlank()) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Repository name is required");
        }
        if (request.chatSessionId() == null) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Chat session ID is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Message must not be empty");
        }
    }

    private void validateSessionRepository(
            ChatSession chatSession,
            String repositoryOwner,
            String repositoryName
    ) {
        boolean ownerMatches = chatSession.getRepositoryOwner()
                .equalsIgnoreCase(repositoryOwner);
        boolean nameMatches = chatSession.getRepositoryName()
                .equalsIgnoreCase(repositoryName);

        if (!ownerMatches || !nameMatches) {
            throw new ChatException(
                    HttpStatus.BAD_REQUEST,
                    "Chat session does not belong to the requested repository"
            );
        }
    }
}
