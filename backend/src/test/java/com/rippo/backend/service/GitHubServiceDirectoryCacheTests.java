package com.rippo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class GitHubServiceDirectoryCacheTests {

    private static final String OWNER = "octocat";
    private static final String REPO = "rippo";
    private static final String SRC_KEY = "repo:octocat:rippo:dir:src";
    private static final String SRC_MAIN_KEY = "repo:octocat:rippo:dir:src/main";
    private static final String ROOT_KEY = "repo:octocat:rippo:dir:root";
    private static final String ACCESS_TOKEN = "token";
    private static final String DIR_JSON = "["
            + "{\"name\":\"main\",\"path\":\"src/main\",\"type\":\"dir\"},"
            + "{\"name\":\"App.java\",\"path\":\"src/App.java\",\"type\":\"file\",\"size\":12}"
            + "]";

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
    void cacheHitReturnsCachedListingWithoutCallingGitHub() {
        List<Map<String, Object>> cachedListing = List.of(
                Map.of("name", "main", "path", "src/main", "type", "folder")
        );
        when(cacheService.get(eq(SRC_KEY), eq(List.class)))
                .thenReturn(Optional.of(cachedListing));

        List<Map<String, Object>> result =
                gitHubService.getDirectoryContents(OWNER, REPO, "src", ACCESS_TOKEN);

        assertThat(result).isEqualTo(cachedListing);
        verify(webClient, never()).get();
        verify(cacheService, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissFetchesFromGitHubAndStoresWithDirectoryTtl() {
        cacheProperties.setDirectoryTtl(Duration.ofHours(1));
        when(cacheService.get(eq(SRC_KEY), eq(List.class))).thenReturn(Optional.empty());
        stubGitHubResponse();
        when(cacheService.put(eq(SRC_KEY), any(), eq(Duration.ofHours(1)))).thenReturn(true);

        List<Map<String, Object>> result =
                gitHubService.getDirectoryContents(OWNER, REPO, "src", ACCESS_TOKEN);

        assertThat(result).hasSize(2);
        assertThat(result.get(0))
                .containsEntry("name", "main")
                .containsEntry("path", "src/main")
                .containsEntry("type", "folder");
        assertThat(result.get(1))
                .containsEntry("name", "App.java")
                .containsEntry("type", "file")
                .containsEntry("size", 12);
        verify(cacheService).put(eq(SRC_KEY), eq(result), eq(Duration.ofHours(1)));
    }

    @Test
    void redisUnavailableStillReturnsListingFromGitHub() {
        // CacheService fails open: get returns empty and put returns false when Redis is down.
        when(cacheService.get(eq(SRC_KEY), eq(List.class))).thenReturn(Optional.empty());
        stubGitHubResponse();
        when(cacheService.put(anyString(), any(), any())).thenReturn(false);

        List<Map<String, Object>> result =
                gitHubService.getDirectoryContents(OWNER, REPO, "src", ACCESS_TOKEN);

        assertThat(result).hasSize(2);
        verify(cacheService).put(eq(SRC_KEY), any(), any());
    }

    @Test
    void normalizesEquivalentNestedPathsToSameCacheKey() {
        when(cacheService.get(anyString(), eq(List.class))).thenReturn(Optional.empty());
        stubGitHubResponse();
        when(cacheService.put(anyString(), any(), any())).thenReturn(true);

        for (String variant : List.of("src/main", "/src/main", "src/main/", "src//main")) {
            gitHubService.getDirectoryContents(OWNER, REPO, variant, ACCESS_TOKEN);
        }

        verify(cacheService, times(4)).get(SRC_MAIN_KEY, List.class);
    }

    @Test
    void treatsBlankSlashAndNullPathsAsRootKey() {
        when(cacheService.get(anyString(), eq(List.class))).thenReturn(Optional.empty());
        stubGitHubResponse();
        when(cacheService.put(anyString(), any(), any())).thenReturn(true);

        gitHubService.getDirectoryContents(OWNER, REPO, "", ACCESS_TOKEN);
        gitHubService.getDirectoryContents(OWNER, REPO, "/", ACCESS_TOKEN);
        gitHubService.getDirectoryContents(OWNER, REPO, null, ACCESS_TOKEN);

        verify(cacheService, times(3)).get(ROOT_KEY, List.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubGitHubResponse() {
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(DIR_JSON));
    }
}
