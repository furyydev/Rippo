package com.rippo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rippo.backend.cache.config.CacheProperties;
import com.rippo.backend.cache.service.CacheService;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class GitHubServiceFileCacheTests {

    private static final String OWNER = "octocat";
    private static final String REPO = "rippo";
    private static final String FILE_KEY = "repo:octocat:rippo:file:src/main/App.java";
    private static final String ACCESS_TOKEN = "token";
    // Successful file payload; "Hello Rippo" encoded as base64, matching GitHub's format.
    private static final String FILE_JSON = "{"
            + "\"type\":\"file\","
            + "\"name\":\"App.java\","
            + "\"path\":\"src/main/App.java\","
            + "\"size\":11,"
            + "\"encoding\":\"base64\","
            + "\"content\":\"SGVsbG8gUmlwcG8=\"}";
    // Oversized file payload (> 1 MB limit) that must never be cached.
    private static final String OVERSIZED_JSON = "{"
            + "\"type\":\"file\","
            + "\"name\":\"App.java\","
            + "\"path\":\"src/main/App.java\","
            + "\"size\":2000000,"
            + "\"encoding\":\"base64\","
            + "\"content\":\"\"}";

    private final WebClient webClient = mock(WebClient.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final CacheProperties cacheProperties = new CacheProperties();
    private final GitHubService gitHubService = new GitHubService(
            webClient,
            new ObjectMapper(),
            cacheService,
            cacheProperties
    );

    @Test
    void cacheHitReturnsCachedFileWithoutCallingGitHub() {
        Map<String, Object> cachedFile = Map.of(
                "name", "App.java",
                "path", "src/main/App.java",
                "size", 11,
                "content", "Hello Rippo"
        );
        when(cacheService.get(eq(FILE_KEY), eq(Map.class)))
                .thenReturn(Optional.of(cachedFile));

        Map<String, Object> result =
                gitHubService.getFileContent(OWNER, REPO, "src/main/App.java", ACCESS_TOKEN);

        assertThat(result).isEqualTo(cachedFile);
        verify(webClient, never()).get();
        verify(cacheService, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissFetchesFromGitHubAndStoresWithFileTtl() {
        cacheProperties.setFileTtl(Duration.ofHours(6));
        when(cacheService.get(eq(FILE_KEY), eq(Map.class))).thenReturn(Optional.empty());
        stubGitHubResponse(FILE_JSON);
        when(cacheService.put(eq(FILE_KEY), any(), eq(Duration.ofHours(6)))).thenReturn(true);

        Map<String, Object> result =
                gitHubService.getFileContent(OWNER, REPO, "src/main/App.java", ACCESS_TOKEN);

        assertThat(result)
                .containsEntry("name", "App.java")
                .containsEntry("path", "src/main/App.java")
                .containsEntry("size", 11)
                .containsEntry("content", "Hello Rippo");
        verify(cacheService).put(eq(FILE_KEY), eq(result), eq(Duration.ofHours(6)));
    }

    @Test
    void redisUnavailableStillReturnsFileFromGitHub() {
        // CacheService fails open: get returns empty and put returns false when Redis is down.
        when(cacheService.get(eq(FILE_KEY), eq(Map.class))).thenReturn(Optional.empty());
        stubGitHubResponse(FILE_JSON);
        when(cacheService.put(eq(FILE_KEY), any(), any())).thenReturn(false);

        Map<String, Object> result =
                gitHubService.getFileContent(OWNER, REPO, "src/main/App.java", ACCESS_TOKEN);

        assertThat(result).containsEntry("content", "Hello Rippo");
        verify(cacheService).put(eq(FILE_KEY), any(), any());
    }

    @Test
    void normalizesEquivalentFilePathsToSameCacheKey() {
        when(cacheService.get(anyString(), eq(Map.class))).thenReturn(Optional.empty());
        stubGitHubResponse(FILE_JSON);
        when(cacheService.put(anyString(), any(), any())).thenReturn(true);

        for (String variant : java.util.List.of(
                "src/main/App.java", "/src/main/App.java", "src/main/App.java/", "src//main/App.java")) {
            gitHubService.getFileContent(OWNER, REPO, variant, ACCESS_TOKEN);
        }

        verify(cacheService, times(4)).get(FILE_KEY, Map.class);
    }

    @Test
    void doesNotCacheOversizedFileResponse() {
        when(cacheService.get(eq(FILE_KEY), eq(Map.class))).thenReturn(Optional.empty());
        stubGitHubResponse(OVERSIZED_JSON);

        assertThatThrownBy(() ->
                gitHubService.getFileContent(OWNER, REPO, "src/main/App.java", ACCESS_TOKEN))
                .isInstanceOf(ResponseStatusException.class);

        verify(cacheService, never()).put(anyString(), any(), any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubGitHubResponse(String json) {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));
    }
}
