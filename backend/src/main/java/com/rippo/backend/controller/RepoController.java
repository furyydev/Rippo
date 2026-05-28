package com.rippo.backend.controller;

import com.rippo.backend.service.GitHubService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepoController {

    private final GitHubService gitHubService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public RepoController(
            GitHubService gitHubService,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        this.gitHubService = gitHubService;
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/repos")
    public String getRepos(
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {

        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(
                        authenticationToken.getAuthorizedClientRegistrationId(),
                        oauth2User.getName()
                );

        String accessToken = client
                .getAccessToken()
                .getTokenValue();

        return gitHubService.getRepositories(accessToken);
    }
}