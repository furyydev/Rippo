package com.rippo.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.rippo.backend.entity.ChatMessage;
import com.rippo.backend.entity.ChatMessageRole;
import com.rippo.backend.entity.ChatSession;
import com.rippo.backend.entity.User;
import com.rippo.backend.repository.UserRepository;
import com.rippo.backend.service.ChatPersistenceService;
import com.rippo.backend.service.UserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatPersistenceService chatPersistenceService;

    @Test
    void contextLoads() {
    }

    @Test
    void githubLoginCreatesAndReusesRippoUserByGithubId() {
        DefaultOAuth2User firstGitHubUser = githubUser(
                12345L,
                "octocat",
                "https://avatars.githubusercontent.com/u/12345",
                "octocat@example.com"
        );
        DefaultOAuth2User renamedGitHubUser = githubUser(
                12345L,
                "renamed-octocat",
                "https://avatars.githubusercontent.com/u/12345?v=2",
                null
        );

        User createdUser = userService.findOrCreateFromGitHub(firstGitHubUser);
        User reusedUser = userService.findOrCreateFromGitHub(renamedGitHubUser);

        assertThat(reusedUser.getId()).isEqualTo(createdUser.getId());
        assertThat(reusedUser.getGithubId()).isEqualTo(12345L);
        assertThat(reusedUser.getUsername()).isEqualTo("renamed-octocat");
        long matchingGithubUsers = userRepository.findAll().stream()
                .filter(user -> user.getGithubId().equals(12345L))
                .count();
        assertThat(matchingGithubUsers).isEqualTo(1);
    }

    @Test
    void chatSessionAndMessageCanBePersisted() {
        User user = userService.findOrCreateFromGitHub(githubUser(
                67890L,
                "repo-owner",
                null,
                null
        ));

        ChatSession chatSession = chatPersistenceService.createSession(
                user,
                "repo-owner",
                "rippo",
                "Initial repository conversation"
        );
        ChatMessage message = chatPersistenceService.addMessage(
                chatSession,
                ChatMessageRole.USER,
                "How is authentication wired?"
        );

        assertThat(chatSession.getId()).isNotNull();
        assertThat(message.getId()).isNotNull();
        assertThat(chatPersistenceService.getMessages(chatSession))
                .extracting(ChatMessage::getContent)
                .containsExactly("How is authentication wired?");
    }

    @Test
    void repositoryContextHistoryReturnsOnlyLatestFiveMessagesInConversationOrder() {
        User user = userService.findOrCreateFromGitHub(githubUser(
                98765L,
                "history-owner",
                null,
                null
        ));
        ChatSession chatSession = chatPersistenceService.createSession(
                user,
                "history-owner",
                "rippo",
                "History test"
        );

        for (int index = 1; index <= 7; index++) {
            chatPersistenceService.addMessage(
                    chatSession,
                    ChatMessageRole.USER,
                    "Message " + index
            );
        }

        assertThat(chatPersistenceService.getPreviousFiveMessages(chatSession))
                .extracting(ChatMessage::getContent)
                .containsExactly(
                        "Message 3",
                        "Message 4",
                        "Message 5",
                        "Message 6",
                        "Message 7"
                );
    }

    private DefaultOAuth2User githubUser(
            Long githubId,
            String username,
            String avatarUrl,
            String email
    ) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "id", githubId,
                        "login", username,
                        "avatar_url", avatarUrl == null ? "" : avatarUrl,
                        "email", email == null ? "" : email
                ),
                "id"
        );
    }
}
