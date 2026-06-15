package com.rippo.backend.controller;

import com.rippo.backend.service.GitHubService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
        String accessToken = getAccessToken(authenticationToken, oauth2User);

        return gitHubService.getRepositories(accessToken);
    }

    @GetMapping("/repo/{owner}/{repo}/contents")
    public String getRepositoryContents(
            @PathVariable String owner,
            @PathVariable String repo,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = getAccessToken(authenticationToken, oauth2User);

        return gitHubService.getRepositoryContents(owner, repo, accessToken);
    }

    @GetMapping("/repo/{owner}/{repo}/readme")
    public String getRepositoryReadme(
            @PathVariable String owner,
            @PathVariable String repo,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = getAccessToken(authenticationToken, oauth2User);

        return gitHubService.getRepositoryReadme(owner, repo, accessToken);
    }

    @GetMapping("/repo/{owner}/{repo}/commits")
    public String getRepositoryCommits(
            @PathVariable String owner,
            @PathVariable String repo,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = getAccessToken(authenticationToken, oauth2User);

        return gitHubService.getRepositoryCommits(owner, repo, accessToken);
    }

    private String getAccessToken(
            OAuth2AuthenticationToken authenticationToken,
            OAuth2User oauth2User
    ) {
        if (authenticationToken == null || oauth2User == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "You must be logged in with GitHub"
            );
        }

        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(
                        authenticationToken.getAuthorizedClientRegistrationId(),
                        oauth2User.getName()
                );

        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "GitHub access token was not found"
            );
        }

        return client.getAccessToken().getTokenValue();
    }
}
