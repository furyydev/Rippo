package com.rippo.backend.mcp.tool;

import static com.rippo.backend.mcp.tool.ToolArguments.DIRECTORY_PATH;
import static com.rippo.backend.mcp.tool.ToolArguments.REPOSITORY_NAME;
import static com.rippo.backend.mcp.tool.ToolArguments.REPOSITORY_OWNER;

import com.rippo.backend.mcp.config.McpConstants;
import com.rippo.backend.mcp.dto.ToolDefinition;
import com.rippo.backend.service.GitHubService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ListDirectoryTool implements McpTool {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            McpConstants.LIST_DIRECTORY_TOOL,
            "Lists the immediate children of a GitHub repository directory.",
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            REPOSITORY_OWNER, Map.of("type", "string"),
                            REPOSITORY_NAME, Map.of("type", "string"),
                            DIRECTORY_PATH, Map.of(
                                    "type", "string",
                                    "description", "Directory path, or an empty string for the root"
                            )
                    ),
                    "required", List.of(REPOSITORY_OWNER, REPOSITORY_NAME, DIRECTORY_PATH),
                    "additionalProperties", false
            )
    );

    private final GitHubService gitHubService;

    public ListDirectoryTool(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(Map<String, Object> arguments) {
        ToolArguments.requireText(arguments, REPOSITORY_OWNER);
        ToolArguments.requireText(arguments, REPOSITORY_NAME);
        ToolArguments.requireString(arguments, DIRECTORY_PATH);
    }

    @Override
    public Object execute(Map<String, Object> arguments, ToolExecutionContext context) {
        return gitHubService.getDirectoryContents(
                (String) arguments.get(REPOSITORY_OWNER),
                (String) arguments.get(REPOSITORY_NAME),
                (String) arguments.get(DIRECTORY_PATH),
                context.githubAccessToken()
        );
    }
}
