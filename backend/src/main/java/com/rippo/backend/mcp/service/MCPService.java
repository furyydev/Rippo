package com.rippo.backend.mcp.service;

import com.rippo.backend.mcp.dto.ToolDefinition;
import com.rippo.backend.mcp.dto.ToolRequest;
import com.rippo.backend.mcp.dto.ToolResponse;
import com.rippo.backend.mcp.registry.ToolRegistry;
import com.rippo.backend.mcp.tool.McpTool;
import com.rippo.backend.mcp.tool.ToolExecutionContext;
import com.rippo.backend.mcp.tool.ToolExecutionException;
import com.rippo.backend.mcp.tool.ToolValidationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MCPService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCPService.class);
    private static final String UNKNOWN_TOOL = "UNKNOWN_TOOL";
    private static final String INVALID_PARAMETERS = "INVALID_PARAMETERS";
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    private static final String UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE";
    private static final String GITHUB_API_FAILURE = "GITHUB_API_FAILURE";
    private static final String TOOL_EXECUTION_FAILURE = "TOOL_EXECUTION_FAILURE";

    private final ToolRegistry toolRegistry;

    public MCPService(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public ToolResponse execute(ToolRequest request, ToolExecutionContext context) {
        String toolName = request == null ? null : request.name();
        LOGGER.info(
                "MCP tool requested: {} arguments={}",
                toolName,
                request == null ? null : request.arguments()
        );
        long startedAt = System.nanoTime();

        if (toolName == null || toolName.isBlank()) {
            return failure(toolName, INVALID_PARAMETERS, "Tool name is required", startedAt, null);
        }

        McpTool tool = toolRegistry.findByName(toolName).orElse(null);
        if (tool == null) {
            return failure(
                    toolName,
                    UNKNOWN_TOOL,
                    "No tool is registered with the name '" + toolName + "'",
                    startedAt,
                    null
            );
        }

        try {
            tool.validate(request.arguments());
            Object result = tool.execute(request.arguments(), context);
            LOGGER.info(
                    "MCP tool executed: {} in {} ms",
                    toolName,
                    elapsedMilliseconds(startedAt)
            );
            return ToolResponse.success(toolName, result);
        } catch (ToolValidationException exception) {
            return failure(
                    toolName,
                    INVALID_PARAMETERS,
                    exception.getMessage(),
                    startedAt,
                    exception
            );
        } catch (ToolExecutionException exception) {
            return failure(
                    toolName,
                    exception.getCode(),
                    exception.getMessage(),
                    startedAt,
                    exception
            );
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return failure(
                        toolName,
                        RESOURCE_NOT_FOUND,
                        "The requested repository resource was not found",
                        startedAt,
                        exception
                );
            }
            if (exception.getStatusCode().value() == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
                return failure(
                        toolName,
                        UNSUPPORTED_FILE_TYPE,
                        "The requested file is not supported text content",
                        startedAt,
                        exception
                );
            }
            return failure(
                    toolName,
                    GITHUB_API_FAILURE,
                    "GitHub could not provide repository information",
                    startedAt,
                    exception
            );
        } catch (Exception exception) {
            return failure(
                    toolName,
                    TOOL_EXECUTION_FAILURE,
                    "The tool could not be executed",
                    startedAt,
                    exception
            );
        }
    }

    public List<ToolDefinition> getToolDefinitions() {
        return toolRegistry.getDefinitions();
    }

    private ToolResponse failure(
            String toolName,
            String code,
            String message,
            long startedAt,
            Exception exception
    ) {
        LOGGER.warn(
                "MCP tool failed: {} code={} durationMs={}",
                toolName,
                code,
                elapsedMilliseconds(startedAt)
        );
        if (exception != null) {
            LOGGER.debug("MCP tool failure details: {}", toolName, exception);
        }
        return ToolResponse.failure(toolName, code, message);
    }

    private long elapsedMilliseconds(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
