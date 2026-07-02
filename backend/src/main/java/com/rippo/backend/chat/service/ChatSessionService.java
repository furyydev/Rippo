package com.rippo.backend.chat.service;

import com.rippo.backend.chat.dto.ChatSessionResponse;
import com.rippo.backend.chat.dto.CreateChatSessionRequest;
import com.rippo.backend.entity.ChatSession;
import com.rippo.backend.entity.User;
import com.rippo.backend.service.ChatPersistenceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionService {

    private final ChatPersistenceService chatPersistenceService;

    public ChatSessionService(ChatPersistenceService chatPersistenceService) {
        this.chatPersistenceService = chatPersistenceService;
    }

    public ChatSessionResponse create(
            CreateChatSessionRequest request,
            User user
    ) {
        if (request == null) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.repositoryOwner() == null || request.repositoryOwner().isBlank()) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Repository owner is required");
        }
        if (request.repositoryName() == null || request.repositoryName().isBlank()) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Repository name is required");
        }

        String owner = request.repositoryOwner().trim();
        String repository = request.repositoryName().trim();
        String title = request.title() == null || request.title().isBlank()
                ? "Chat about " + owner + "/" + repository
                : request.title().trim();

        ChatSession session = chatPersistenceService.createSession(
                user,
                owner,
                repository,
                title
        );
        return ChatSessionResponse.from(session);
    }
}
