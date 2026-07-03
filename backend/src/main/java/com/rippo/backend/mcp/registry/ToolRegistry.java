package com.rippo.backend.mcp.registry;

import com.rippo.backend.mcp.dto.ToolDefinition;
import com.rippo.backend.mcp.tool.McpTool;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final Map<String, McpTool> tools;

    public ToolRegistry(List<McpTool> discoveredTools) {
        Map<String, McpTool> registeredTools = new LinkedHashMap<>();

        for (McpTool tool : discoveredTools) {
            String name = tool.definition().name();
            if (registeredTools.putIfAbsent(name, tool) != null) {
                throw new IllegalStateException("Duplicate MCP tool name: " + name);
            }
        }

        this.tools = Collections.unmodifiableMap(registeredTools);
    }

    public Optional<McpTool> findByName(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<ToolDefinition> getDefinitions() {
        return tools.values().stream()
                .map(McpTool::definition)
                .toList();
    }
}
