package com.rippo.backend.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rippo.backend.service.GitHubService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListDirectoryToolTests {

    @Test
    void listsDirectoryThroughGitHubServiceAndAllowsRepositoryRoot() {
        GitHubService gitHubService = mock(GitHubService.class);
        ListDirectoryTool tool = new ListDirectoryTool(gitHubService);
        List<Map<String, Object>> listing = List.of(
                Map.of("name", "src", "path", "src", "type", "folder", "size", 0)
        );
        when(gitHubService.getDirectoryContents("owner", "repo", "", "token"))
                .thenReturn(listing);
        Map<String, Object> arguments = Map.of(
                "repositoryOwner", "owner",
                "repositoryName", "repo",
                "directoryPath", ""
        );

        tool.validate(arguments);
        Object result = tool.execute(arguments, new ToolExecutionContext("token"));

        assertThat(result).isEqualTo(listing);
        verify(gitHubService).getDirectoryContents("owner", "repo", "", "token");
    }
}
