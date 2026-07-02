package com.rippo.backend.mcp.tool;

import com.rippo.backend.mcp.dto.ToolDefinition;
import java.util.Map;

public interface McpTool {

    ToolDefinition definition();

    void validate(Map<String, Object> arguments);

    Object execute(Map<String, Object> arguments, ToolExecutionContext context);
}
