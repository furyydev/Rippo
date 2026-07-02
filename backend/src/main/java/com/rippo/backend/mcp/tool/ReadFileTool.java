package com.rippo.backend.mcp.tool;

import static com.rippo.backend.mcp.tool.ToolArguments.FILE_PATH;
import static com.rippo.backend.mcp.tool.ToolArguments.REPOSITORY_NAME;
import static com.rippo.backend.mcp.tool.ToolArguments.REPOSITORY_OWNER;

import com.rippo.backend.mcp.config.McpConstants;
import com.rippo.backend.mcp.dto.ToolDefinition;
import com.rippo.backend.service.GitHubService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ReadFileTool implements McpTool {

    private static final String UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "java", "kt", "kts", "xml", "yaml", "yml", "json", "md", "markdown",
            "gradle", "properties", "dart", "ts", "tsx", "js", "jsx", "txt", "text"
    );

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            McpConstants.READ_FILE_TOOL,
            "Reads a supported text file from a GitHub repository.",
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            REPOSITORY_OWNER, Map.of("type", "string"),
                            REPOSITORY_NAME, Map.of("type", "string"),
                            FILE_PATH, Map.of("type", "string")
                    ),
                    "required", List.of(REPOSITORY_OWNER, REPOSITORY_NAME, FILE_PATH),
                    "additionalProperties", false
            )
    );

    private final GitHubService gitHubService;

    public ReadFileTool(GitHubService gitHubService) {
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
        String filePath = ToolArguments.requireText(arguments, FILE_PATH);

        if (!hasSupportedExtension(filePath)) {
            throw new ToolExecutionException(
                    UNSUPPORTED_FILE_TYPE,
                    "The requested file type is not supported"
            );
        }
    }

    @Override
    public Object execute(Map<String, Object> arguments, ToolExecutionContext context) {
        return gitHubService.getFileContent(
                (String) arguments.get(REPOSITORY_OWNER),
                (String) arguments.get(REPOSITORY_NAME),
                (String) arguments.get(FILE_PATH),
                context.githubAccessToken()
        );
    }

    private boolean hasSupportedExtension(String path) {
        String fileName = path.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator < 0 || extensionSeparator == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }
}
