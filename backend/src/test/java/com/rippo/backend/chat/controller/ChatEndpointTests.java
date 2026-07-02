package com.rippo.backend.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rippo.backend.chat.dto.ChatRequest;
import com.rippo.backend.chat.dto.ChatResponse;
import com.rippo.backend.chat.dto.ChatSessionResponse;
import com.rippo.backend.chat.dto.CreateChatSessionRequest;
import com.rippo.backend.chat.service.ChatService;
import com.rippo.backend.chat.service.ChatSessionService;
import com.rippo.backend.entity.User;
import com.rippo.backend.service.GitHubAuthenticationService;
import com.rippo.backend.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ChatSessionService chatSessionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GitHubAuthenticationService githubAuthenticationService;

    @Test
    void authenticatedPostReturnsRepositoryAwareChatResponseWithoutCsrfToken()
            throws Exception {
        User user = new User(123L, "octocat", null, null);
        Instant timestamp = Instant.parse("2026-07-02T00:00:00Z");

        when(githubAuthenticationService.getAccessToken(
                any(OAuth2AuthenticationToken.class),
                any(OAuth2User.class)
        )).thenReturn("github-token");
        when(userService.findOrCreateFromGitHub(any(OAuth2User.class))).thenReturn(user);
        when(chatService.chat(any(ChatRequest.class), any(User.class), any(String.class)))
                .thenReturn(new ChatResponse("Repository-aware answer", 1L, timestamp));

        mockMvc.perform(post("/chat")
                        .with(oauth2Login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repositoryOwner": "octocat",
                                  "repositoryName": "rippo",
                                  "chatSessionId": 1,
                                  "message": "What is this repository about?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantMessage")
                        .value("Repository-aware answer"))
                .andExpect(jsonPath("$.chatSessionId").value(1))
                .andExpect(jsonPath("$.timestamp").value(timestamp.toString()));
    }

    @Test
    void authenticatedPostCreatesPersistedChatSessionWithoutCsrfToken()
            throws Exception {
        User user = new User(123L, "octocat", null, null);
        Instant timestamp = Instant.parse("2026-07-02T00:00:00Z");

        when(userService.findOrCreateFromGitHub(any(OAuth2User.class))).thenReturn(user);
        when(chatSessionService.create(
                any(CreateChatSessionRequest.class),
                any(User.class)
        )).thenReturn(new ChatSessionResponse(
                42L,
                "octocat",
                "rippo",
                "Chat about octocat/rippo",
                timestamp
        ));

        mockMvc.perform(post("/chat/sessions")
                        .with(oauth2Login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repositoryOwner": "octocat",
                                  "repositoryName": "rippo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatSessionId").value(42))
                .andExpect(jsonPath("$.repositoryOwner").value("octocat"))
                .andExpect(jsonPath("$.repositoryName").value("rippo"));
    }
}
