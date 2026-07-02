package com.rippo.backend.chat.model;

import java.util.List;

public record RepositoryContext(
        String repositoryOwner,
        String repositoryName,
        String defaultBranch,
        String readmeContent,
        List<PreviousChatMessage> previousChatMessages
) {
    public RepositoryContext {
        previousChatMessages = List.copyOf(previousChatMessages);
    }
}
