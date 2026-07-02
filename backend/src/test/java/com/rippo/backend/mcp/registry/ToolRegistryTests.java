package com.rippo.backend.mcp.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rippo.backend.mcp.dto.ToolDefinition;
import com.rippo.backend.mcp.tool.McpTool;
import com.rippo.backend.mcp.tool.ListDirectoryTool;
import com.rippo.backend.mcp.tool.ReadFileTool;
import com.rippo.backend.mcp.tool.ReadmeTool;
import com.rippo.backend.mcp.tool.RepositoryInfoTool;
import com.rippo.backend.service.GitHubService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolRegistryTests {

    @Test
    void discoversToolsByDefinitionName() {
        McpTool tool = toolNamed("sample_tool");
        ToolRegistry registry = new ToolRegistry(List.of(tool));

        assertThat(registry.findByName("sample_tool")).contains(tool);
        assertThat(registry.getDefinitions()).containsExactly(tool.definition());
    }

    @Test
    void rejectsDuplicateToolNames() {
        McpTool first = toolNamed("duplicate");
        McpTool second = toolNamed("duplicate");

        assertThatThrownBy(() -> new ToolRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void registersEveryRepositoryExplorationToolWithoutManualMappings() {
        GitHubService gitHubService = mock(GitHubService.class);
        ToolRegistry registry = new ToolRegistry(List.of(
                new RepositoryInfoTool(gitHubService),
                new ReadFileTool(gitHubService),
                new ListDirectoryTool(gitHubService),
                new ReadmeTool(gitHubService)
        ));

        assertThat(registry.getDefinitions())
                .extracting(ToolDefinition::name)
                .containsExactly(
                        "repository_info",
                        "read_file",
                        "list_directory",
                        "readme"
                );
    }

    private McpTool toolNamed(String name) {
        McpTool tool = mock(McpTool.class);
        when(tool.definition()).thenReturn(new ToolDefinition(name, "Description", Map.of()));
        return tool;
    }
}
