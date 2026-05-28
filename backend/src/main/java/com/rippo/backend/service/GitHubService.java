package com.rippo.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GitHubService {

    private final WebClient webClient;

    public GitHubService(WebClient webClient) {
        this.webClient = webClient;
    }

    public String getRepositories(String accessToken) {

        return webClient
                .get()
                .uri("https://api.github.com/user/repos")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
