package com.rippo.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GitHubService {

    private static final int MAX_FILE_SIZE_BYTES = 1_000_000;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GitHubService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public String getRepositories(String accessToken) {
        return makeGitHubGetRequest(
                "https://api.github.com/user/repos",
                accessToken
        );
    }

    public String getRepositoryContents(
            String owner,
            String repo,
            String path,
            String accessToken
    ) {
        String url = path == null || path.isBlank()
                ? buildRepoUrl(owner, repo, "contents")
                : buildRepoUrl(owner, repo, "contents", path);

        return makeGitHubGetRequest(url, accessToken);
    }

    public Map<String, Object> getRepositoryReadme(
            String owner,
            String repo,
            String accessToken
    ) {
        String url = buildRepoUrl(owner, repo, "readme");
        JsonNode readme = readJson(makeGitHubGetRequest(url, accessToken));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", readme.path("name").asText("README"));
        response.put("path", readme.path("path").asText("README"));
        response.put("content", decodeContent(readme));
        return response;
    }

    public String getRepositoryCommits(String owner, String repo, String accessToken) {
        String url = UriComponentsBuilder
                .fromUriString(buildRepoUrl(owner, repo, "commits"))
                .queryParam("per_page", 20)
                .build()
                .encode()
                .toUriString();

        return makeGitHubGetRequest(url, accessToken);
    }

    public Map<String, Object> getFileContent(
            String owner,
            String repo,
            String path,
            String accessToken
    ) {
        if (path == null || path.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File path is required"
            );
        }

        String url = buildRepoUrl(owner, repo, "contents", path);
        JsonNode file = readJson(makeGitHubGetRequest(url, accessToken));

        if (!"file".equals(file.path("type").asText())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The requested path is not a file"
            );
        }

        int size = file.path("size").asInt();
        if (size > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "This file is too large to display. The limit is 1 MB."
            );
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", file.path("name").asText());
        response.put("path", file.path("path").asText(path));
        response.put("size", size);
        response.put("content", decodeContent(file));
        return response;
    }

    private String buildRepoUrl(
            String owner,
            String repo,
            String endpoint,
            String... remainingPath
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://api.github.com")
                .pathSegment("repos", owner, repo, endpoint);

        for (String path : remainingPath) {
            for (String segment : path.split("/")) {
                if (!segment.isBlank()) {
                    builder.pathSegment(segment);
                }
            }
        }

        return builder.build().encode().toUriString();
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "GitHub returned an unreadable response",
                    exception
            );
        }
    }

    private String decodeContent(JsonNode response) {
        String encoding = response.path("encoding").asText();
        String content = response.path("content").asText();

        if (!"base64".equalsIgnoreCase(encoding) || content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "GitHub did not return displayable text content"
            );
        }

        try {
            byte[] decodedBytes = Base64.getMimeDecoder().decode(content);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "GitHub returned invalid encoded content",
                    exception
            );
        }
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
