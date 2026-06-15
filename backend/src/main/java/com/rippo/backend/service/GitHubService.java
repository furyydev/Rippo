package com.rippo.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GitHubService {

    private final WebClient webClient;

    public GitHubService(WebClient webClient) {
        this.webClient = webClient;
    }

    public String getRepositories(String accessToken) {
        return makeGitHubGetRequest("https://api.github.com/user/repos", accessToken);
    }

    public String getRepositoryContents(String owner, String repo, String accessToken) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents";

        return makeGitHubGetRequest(url, accessToken);
    }

    public String getRepositoryReadme(String owner, String repo, String accessToken) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/readme";

        return makeGitHubGetRequest(url, accessToken);
    }

    public String getRepositoryCommits(String owner, String repo, String accessToken) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/commits";

        return makeGitHubGetRequest(url, accessToken);
    }

    private String makeGitHubGetRequest(String url, String accessToken) {
        try {
            return webClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException exception) {
            throw new ResponseStatusException(
                    exception.getStatusCode(),
                    "GitHub API request failed: " + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Something went wrong while calling GitHub",
                    exception
            );
        }
    }
}
