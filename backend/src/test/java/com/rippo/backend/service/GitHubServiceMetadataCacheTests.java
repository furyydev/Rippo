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

class GitHubServiceMetadataCacheTests {

    private static final String OWNER = "octocat";
    private static final String REPO = "rippo";
    private static final String CACHE_KEY = "repo:octocat:rippo:metadata";
    private static final String ACCESS_TOKEN = "token";
    private static final String REPO_JSON = "{"
            + "\"name\":\"rippo\","
            + "\"full_name\":\"octocat/rippo\","
            + "\"owner\":{\"login\":\"octocat\"},"
            + "\"description\":\"An AI assistant\","
            + "\"default_branch\":\"main\","
            + "\"private\":false,"
            + "\"html_url\":\"https://github.com/octocat/rippo\","
            + "\"language\":\"Java\","
            + "\"stargazers_count\":10,"
            + "\"forks_count\":2,"
            + "\"open_issues_count\":3}";

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
    void cacheHitReturnsCachedMetadataWithoutCallingGitHub() {
        Map<String, Object> cachedMetadata = Map.of(
                "name", "rippo",
                "owner", "octocat",
                "stars", 10
        );
        when(cacheService.get(eq(CACHE_KEY), eq(Map.class)))
                .thenReturn(Optional.of(cachedMetadata));

        Map<String, Object> result = gitHubService.getRepositoryInfo(OWNER, REPO, ACCESS_TOKEN);

        assertThat(result).isEqualTo(cachedMetadata);
        verify(webClient, never()).get();
        verify(cacheService, never()).put(anyString(), any(), any());
    }

    @Test
    void cacheMissFetchesFromGitHubAndStoresWithMetadataTtl() {
        cacheProperties.setRepositoryMetadataTtl(Duration.ofHours(1));
        when(cacheService.get(eq(CACHE_KEY), eq(Map.class))).thenReturn(Optional.empty());
        stubGitHubResponse();
        when(cacheService.put(eq(CACHE_KEY), any(), eq(Duration.ofHours(1)))).thenReturn(true);

        Map<String, Object> result = gitHubService.getRepositoryInfo(OWNER, REPO, ACCESS_TOKEN);

        assertThat(result)
                .containsEntry("name", "rippo")
                .containsEntry("owner", "octocat")
                .containsEntry("defaultBranch", "main")
                .containsEntry("language", "Java")
                .containsEntry("stars", 10)
                .containsEntry("forks", 2)
                .containsEntry("openIssues", 3)
                .containsEntry("private", false);
        verify(cacheService).put(eq(CACHE_KEY), eq(result), eq(Duration.ofHours(1)));
    }

    @Test
    void redisUnavailableStillReturnsMetadataFromGitHub() {
        // CacheService fails open: get returns empty and put returns false when Redis is down.
        when(cacheService.get(eq(CACHE_KEY), eq(Map.class))).thenReturn(Optional.empty());
        stubGitHubResponse();
        when(cacheService.put(eq(CACHE_KEY), any(), any())).thenReturn(false);

        Map<String, Object> result = gitHubService.getRepositoryInfo(OWNER, REPO, ACCESS_TOKEN);

        assertThat(result).containsEntry("name", "rippo");
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
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(REPO_JSON));
    }
}
