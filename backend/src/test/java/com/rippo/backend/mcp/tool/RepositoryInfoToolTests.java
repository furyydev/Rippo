package com.rippo.backend.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rippo.backend.service.GitHubService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RepositoryInfoToolTests {

    private final GitHubService gitHubService = mock(GitHubService.class);
    private final RepositoryInfoTool tool = new RepositoryInfoTool(gitHubService);

    @Test
    void delegatesRepositoryLookupToGitHubService() {
        Map<String, Object> repository = Map.of("name", "rippo");
        when(gitHubService.getRepositoryInfo("owner", "rippo", "token"))
                .thenReturn(repository);

        Object result = tool.execute(
                Map.of("repositoryOwner", "owner", "repositoryName", "rippo"),
                new ToolExecutionContext("token")
        );

        assertThat(result).isEqualTo(repository);
        verify(gitHubService).getRepositoryInfo("owner", "rippo", "token");
    }

    @Test
    void rejectsMissingAndBlankParameters() {
        assertThatThrownBy(() -> tool.validate(Map.of("repositoryOwner", "owner")))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("repositoryName");

        assertThatThrownBy(() -> tool.validate(
                Map.of("repositoryOwner", " ", "repositoryName", "rippo")
        ))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("repositoryOwner");
    }
}
