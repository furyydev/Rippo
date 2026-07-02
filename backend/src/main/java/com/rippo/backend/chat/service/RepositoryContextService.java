package com.rippo.backend.chat.service;

import com.rippo.backend.chat.model.PreviousChatMessage;
import com.rippo.backend.chat.model.RepositoryContext;
import com.rippo.backend.entity.ChatMessage;
import com.rippo.backend.entity.ChatSession;
import com.rippo.backend.service.ChatPersistenceService;
import com.rippo.backend.service.GitHubService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RepositoryContextService {

    private static final String MISSING_README =
            "No README is available for this repository.";

    private final GitHubService gitHubService;
    private final ChatPersistenceService chatPersistenceService;

    public RepositoryContextService(
            GitHubService gitHubService,
            ChatPersistenceService chatPersistenceService
    ) {
        this.gitHubService = gitHubService;
        this.chatPersistenceService = chatPersistenceService;
    }

    public RepositoryContext loadContext(
            ChatSession chatSession,
            String repositoryOwner,
            String repositoryName,
            String accessToken
    ) {
        String defaultBranch = loadDefaultBranch(
                repositoryOwner,
                repositoryName,
                accessToken
        );
        String readmeContent = loadReadme(
                repositoryOwner,
                repositoryName,
                accessToken
        );
        List<PreviousChatMessage> previousMessages =
                chatPersistenceService.getPreviousFiveMessages(chatSession).stream()
                        .map(this::toPreviousMessage)
                        .toList();

        return new RepositoryContext(
                repositoryOwner,
                repositoryName,
                defaultBranch,
                readmeContent,
                previousMessages
        );
    }

    private String loadDefaultBranch(
            String repositoryOwner,
            String repositoryName,
            String accessToken
    ) {
        try {
            return gitHubService.getDefaultBranch(
                    repositoryOwner,
                    repositoryName,
                    accessToken
            );
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ChatException(
                        HttpStatus.NOT_FOUND,
                        "Repository was not found or is not accessible",
                        exception
                );
            }
            throw githubFailure(exception);
        }
    }

    private String loadReadme(
            String repositoryOwner,
            String repositoryName,
            String accessToken
    ) {
        try {
            Map<String, Object> readme = gitHubService.getRepositoryReadme(
                    repositoryOwner,
                    repositoryName,
                    accessToken
            );
            String content = Objects.toString(readme.get("content"), "");
            return content.isBlank() ? MISSING_README : content;
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == 404) {
                return MISSING_README;
            }
            throw githubFailure(exception);
        }
    }

    private ChatException githubFailure(ResponseStatusException exception) {
        HttpStatus status = exception.getStatusCode().is5xxServerError()
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.BAD_GATEWAY;
        return new ChatException(
                status,
                "Could not load repository context from GitHub",
                exception
        );
    }

    private PreviousChatMessage toPreviousMessage(ChatMessage message) {
        return new PreviousChatMessage(message.getRole(), message.getContent());
    }
}
