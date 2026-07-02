package com.rippo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import reactor.core.publisher.Mono;

class GitHubServiceReadmeCacheTests {

    private static final String OWNER = "octocat";
    private static final String REPO = "rippo";
    private static final String CACHE_KEY = "repo:octocat:rippo:readme";
    private static final String ACCESS_TOKEN = "token";
    // "Hello Rippo" encoded as base64, matching GitHub's readme payload format.
    private static final String README_JSON = "{"
            + "\"name\":\"README.md\","
            + "\"path\":\"README.md\","
            + "\"encoding\":\"base64\","
            + "\"content\":\"SGVsbG8gUmlwcG8=\"}";

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
    void cacheHitReturnsCachedReadmeWithoutCallingGitHub() {
        Map<String, Object> cachedReadme = Map.of(
                "name", "README.md",
                "path", "README.md",
                "content", "Hello Rippo"
        );
        when(cacheService.get(eq(CACHE_KEY), eq(Map.class)))
                .thenReturn(Optional.of(cachedReadme));

        Map<String, Object> result = gitHubService.getRepositoryReadme(OWNER, REPO, ACCESS_TOKEN);

        assertThat(result).isEqualTo(cachedReadme);
        verify(webClient, never()).get();
        verify(cacheService, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissFetchesFromGitHubAndStoresWithReadmeTtl() {
        cacheProperties.setReadmeTtl(Duration.ofHours(6));
        when(cacheService.get(eq(CACHE_KEY), eq(Map.class))).thenReturn(Optional.empty());
        stubGitHubResponse();
        when(cacheService.put(eq(CACHE_KEY), any(), eq(Duration.ofHours(6)))).thenReturn(true);

        Map<String, Object> result = gitHubService.getRepositoryReadme(OWNER, REPO, ACCESS_TOKEN);

        assertThat(result)
                .containsEntry("name", "README.md")
                .containsEntry("path", "README.md")
                .containsEntry("content", "Hello Rippo");
        verify(cacheService).put(eq(CACHE_KEY), eq(result), eq(Duration.ofHours(6)));
    }

    @Test
    void redisUnavailableStillReturnsReadmeFromGitHub() {
        // CacheService fails open: get returns empty and put returns false when Redis is down.
        when(cacheService.get(eq(CACHE_KEY), eq(Map.class))).thenReturn(Optional.empty());
        stubGitHubResponse();
        when(cacheService.put(eq(CACHE_KEY), any(), any())).thenReturn(false);

        Map<String, Object> result = gitHubService.getRepositoryReadme(OWNER, REPO, ACCESS_TOKEN);

        assertThat(result).containsEntry("content", "Hello Rippo");
        verify(cacheService).put(eq(CACHE_KEY), any(), any());
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
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(README_JSON));
    }
}
