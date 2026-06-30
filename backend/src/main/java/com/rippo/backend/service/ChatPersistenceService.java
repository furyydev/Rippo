package com.rippo.backend.service;

import com.rippo.backend.entity.ChatMessage;
import com.rippo.backend.entity.ChatMessageRole;
import com.rippo.backend.entity.ChatSession;
import com.rippo.backend.entity.User;
import com.rippo.backend.repository.ChatMessageRepository;
import com.rippo.backend.repository.ChatSessionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatPersistenceService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatPersistenceService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatSession createSession(
            User user,
            String repositoryOwner,
            String repositoryName,
            String title
    ) {
        return chatSessionRepository.save(
                new ChatSession(user, repositoryOwner, repositoryName, title)
        );
    }

    @Transactional
    public ChatMessage addMessage(
            ChatSession chatSession,
            ChatMessageRole role,
            String content
    ) {
        return chatMessageRepository.save(new ChatMessage(chatSession, role, content));
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(ChatSession chatSession) {
        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(chatSession.getId());
    }
}
