package com.rippo.backend.chat.controller;

import com.rippo.backend.chat.dto.ChatRequest;
import com.rippo.backend.chat.dto.ChatResponse;
import com.rippo.backend.chat.dto.ChatSessionResponse;
import com.rippo.backend.chat.dto.CreateChatSessionRequest;
import com.rippo.backend.chat.service.ChatService;
import com.rippo.backend.chat.service.ChatSessionService;
import com.rippo.backend.entity.User;
import com.rippo.backend.service.GitHubAuthenticationService;
import com.rippo.backend.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionService chatSessionService;
    private final UserService userService;
    private final GitHubAuthenticationService githubAuthenticationService;

    public ChatController(
            ChatService chatService,
            ChatSessionService chatSessionService,
            UserService userService,
            GitHubAuthenticationService githubAuthenticationService
    ) {
        this.chatService = chatService;
        this.chatSessionService = chatSessionService;
        this.userService = userService;
        this.githubAuthenticationService = githubAuthenticationService;
    }

    @PostMapping(
            value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ChatResponse chat(
            @RequestBody ChatRequest request,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = githubAuthenticationService.getAccessToken(
                authenticationToken,
                oauth2User
        );
        User user = userService.findOrCreateFromGitHub(oauth2User);

        return chatService.chat(request, user, accessToken);
    }

    @PostMapping(
            value = "/chat/sessions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ChatSessionResponse createSession(
            @RequestBody CreateChatSessionRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        User user = userService.findOrCreateFromGitHub(oauth2User);
        return chatSessionService.create(request, user);
    }
}
