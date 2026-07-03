package com.rippo.backend.chat.service;

import com.rippo.backend.chat.agent.AgentLoop;
import com.rippo.backend.chat.dto.ChatRequest;
import com.rippo.backend.chat.dto.ChatResponse;
import com.rippo.backend.chat.model.RepositoryContext;
import com.rippo.backend.entity.ChatMessage;
import com.rippo.backend.entity.ChatMessageRole;
import com.rippo.backend.entity.ChatSession;
import com.rippo.backend.entity.User;
import com.rippo.backend.service.ChatPersistenceService;
import com.rippo.backend.mcp.service.MCPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);
    private static final String MDC_CHAT_SESSION_ID = "chatSessionId";

    private final ChatPersistenceService chatPersistenceService;
    private final RepositoryContextService repositoryContextService;
    private final PromptBuilder promptBuilder;
    private final AgentLoop agentLoop;
    private final MCPService mcpService;

    public ChatService(
            ChatPersistenceService chatPersistenceService,
            RepositoryContextService repositoryContextService,
            PromptBuilder promptBuilder,
            AgentLoop agentLoop,
            MCPService mcpService
    ) {
        this.chatPersistenceService = chatPersistenceService;
        this.repositoryContextService = repositoryContextService;
        this.promptBuilder = promptBuilder;
        this.agentLoop = agentLoop;
        this.mcpService = mcpService;
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
        String prompt = promptBuilder.build(
                context,
                userMessage,
                mcpService.getToolDefinitions()
        );

        MDC.put(MDC_CHAT_SESSION_ID, String.valueOf(chatSession.getId()));
        try {
            LOGGER.info(
                    "Chat agent started: repository={}/{}",
                    repositoryOwner,
                    repositoryName
            );
            chatPersistenceService.addMessage(
                    chatSession,
                    ChatMessageRole.USER,
                    userMessage
            );
            String assistantText = agentLoop.run(
                    prompt,
                    accessToken,
                    repositoryOwner,
                    repositoryName
            );
            ChatMessage assistantMessage = chatPersistenceService.addMessage(
                    chatSession,
                    ChatMessageRole.ASSISTANT,
                    assistantText
            );
            LOGGER.info(
                    "Chat agent completed: repository={}/{}",
                    repositoryOwner,
                    repositoryName
            );

            return new ChatResponse(
                    assistantMessage.getContent(),
                    chatSession.getId(),
                    assistantMessage.getCreatedAt()
            );
        } finally {
            MDC.remove(MDC_CHAT_SESSION_ID);
        }
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
