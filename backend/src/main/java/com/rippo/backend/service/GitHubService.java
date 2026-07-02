package com.rippo.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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

    public Map<String, Object> getRepositoryInfo(
            String owner,
            String repo,
            String accessToken
    ) {
        JsonNode repository = readJson(
                makeGitHubGetRequest(buildRepoUrl(owner, repo, ""), accessToken)
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", repository.path("name").asText());
        response.put("fullName", repository.path("full_name").asText());
        response.put("owner", repository.path("owner").path("login").asText());
        response.put("description", repository.path("description").isNull()
                ? null
                : repository.path("description").asText());
        response.put("defaultBranch", repository.path("default_branch").asText());
        response.put("private", repository.path("private").asBoolean());
        response.put("htmlUrl", repository.path("html_url").asText());
        response.put("language", repository.path("language").isNull()
                ? null
                : repository.path("language").asText());
        response.put("stars", repository.path("stargazers_count").asInt());
        response.put("forks", repository.path("forks_count").asInt());
        response.put("openIssues", repository.path("open_issues_count").asInt());
        return response;
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

    public List<Map<String, Object>> getDirectoryContents(
            String owner,
            String repo,
            String path,
            String accessToken
    ) {
        JsonNode contents = readJson(getRepositoryContents(owner, repo, path, accessToken));
        if (!contents.isArray()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The requested path is not a directory"
            );
        }

        List<Map<String, Object>> response = new ArrayList<>();
        for (JsonNode item : contents) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", item.path("name").asText());
            entry.put("path", item.path("path").asText());
            entry.put("type", "dir".equals(item.path("type").asText()) ? "folder" : "file");
            if (item.hasNonNull("size")) {
                entry.put("size", item.path("size").asInt());
            }
            response.add(entry);
        }
        return response;
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

    public String getDefaultBranch(String owner, String repo, String accessToken) {
        String url = buildRepoUrl(owner, repo, "");
        JsonNode repository = readJson(makeGitHubGetRequest(url, accessToken));
        String defaultBranch = repository.path("default_branch").asText();

        if (defaultBranch.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "GitHub did not return the repository default branch"
            );
        }

        return defaultBranch;
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
                .pathSegment("repos", owner, repo);

        if (endpoint != null && !endpoint.isBlank()) {
            builder.pathSegment(endpoint);
        }

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
            for (byte decodedByte : decodedBytes) {
                if (decodedByte == 0) {
                    throw new ResponseStatusException(
                            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            "The requested file contains binary data"
                    );
                }
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(decodedBytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "The requested file is not UTF-8 text",
                    exception
            );
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
            String message = exception.getStatusCode().value() == 404
                    ? "GitHub resource was not found"
                    : "GitHub API request failed";
            throw new ResponseStatusException(
                    exception.getStatusCode(),
                    message,
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
