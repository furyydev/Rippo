package com.rippo.backend.mcp.tool;

import static com.rippo.backend.mcp.tool.ToolArguments.REPOSITORY_NAME;
import static com.rippo.backend.mcp.tool.ToolArguments.REPOSITORY_OWNER;

import com.rippo.backend.mcp.config.McpConstants;
import com.rippo.backend.mcp.dto.ToolDefinition;
import com.rippo.backend.service.GitHubService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReadmeTool implements McpTool {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            McpConstants.README_TOOL,
            "Returns the README for a GitHub repository.",
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            REPOSITORY_OWNER, Map.of("type", "string"),
                            REPOSITORY_NAME, Map.of("type", "string")
                    ),
                    "required", List.of(REPOSITORY_OWNER, REPOSITORY_NAME),
                    "additionalProperties", false
            )
    );

    private final GitHubService gitHubService;

    public ReadmeTool(GitHubService gitHubService) {
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
    }

    @Override
    public Object execute(Map<String, Object> arguments, ToolExecutionContext context) {
        return gitHubService.getRepositoryReadme(
                (String) arguments.get(REPOSITORY_OWNER),
                (String) arguments.get(REPOSITORY_NAME),
                context.githubAccessToken()
        );
    }
}
