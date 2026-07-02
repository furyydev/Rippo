package com.rippo.backend.controller;

import com.rippo.backend.service.GitHubService;
import com.rippo.backend.service.GitHubAuthenticationService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepoController {

    private final GitHubService gitHubService;
    private final GitHubAuthenticationService githubAuthenticationService;

    public RepoController(
            GitHubService gitHubService,
            GitHubAuthenticationService githubAuthenticationService
    ) {
        this.gitHubService = gitHubService;
        this.githubAuthenticationService = githubAuthenticationService;
    }

    @GetMapping("/repos")
    public String getRepos(
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = githubAuthenticationService.getAccessToken(
                authenticationToken,
                oauth2User
        );

        return gitHubService.getRepositories(accessToken);
    }

    @GetMapping("/repo/{owner}/{repo}/contents")
    public String getRepositoryContents(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "") String path,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = githubAuthenticationService.getAccessToken(
                authenticationToken,
                oauth2User
        );

        return gitHubService.getRepositoryContents(owner, repo, path, accessToken);
    }

    @GetMapping("/repo/{owner}/{repo}/readme")
    public Map<String, Object> getRepositoryReadme(
            @PathVariable String owner,
            @PathVariable String repo,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = githubAuthenticationService.getAccessToken(
                authenticationToken,
                oauth2User
        );

        return gitHubService.getRepositoryReadme(owner, repo, accessToken);
    }

    @GetMapping("/repo/{owner}/{repo}/commits")
    public String getRepositoryCommits(
            @PathVariable String owner,
            @PathVariable String repo,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = githubAuthenticationService.getAccessToken(
                authenticationToken,
                oauth2User
        );

        return gitHubService.getRepositoryCommits(owner, repo, accessToken);
    }

    @GetMapping("/repo/{owner}/{repo}/file")
    public Map<String, Object> getFileContent(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String path,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = githubAuthenticationService.getAccessToken(
                authenticationToken,
                oauth2User
        );

        return gitHubService.getFileContent(owner, repo, path, accessToken);
    }
}
