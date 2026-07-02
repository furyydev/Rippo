package com.rippo.backend.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rippo.backend.service.GitHubService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReadmeToolTests {

    @Test
    void readsReadmeThroughExistingGitHubServiceLogic() {
        GitHubService gitHubService = mock(GitHubService.class);
        ReadmeTool tool = new ReadmeTool(gitHubService);
        Map<String, Object> readme = Map.of(
                "path", "README.md",
                "content", "# Rippo"
        );
        when(gitHubService.getRepositoryReadme("owner", "repo", "token"))
                .thenReturn(readme);
        Map<String, Object> arguments = Map.of(
                "repositoryOwner", "owner",
                "repositoryName", "repo"
        );

        tool.validate(arguments);
        Object result = tool.execute(arguments, new ToolExecutionContext("token"));

        assertThat(result).isEqualTo(readme);
        verify(gitHubService).getRepositoryReadme("owner", "repo", "token");
    }
}
