package com.rippo.backend.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rippo.backend.mcp.dto.ToolDefinition;
import com.rippo.backend.mcp.dto.ToolRequest;
import com.rippo.backend.mcp.dto.ToolResponse;
import com.rippo.backend.mcp.registry.ToolRegistry;
import com.rippo.backend.mcp.tool.McpTool;
import com.rippo.backend.mcp.tool.ToolExecutionContext;
import com.rippo.backend.mcp.tool.ToolValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MCPServiceTests {

    private final ToolExecutionContext context = new ToolExecutionContext("token");

    @Test
    void executesRegisteredTool() {
        McpTool tool = mock(McpTool.class);
        when(tool.definition()).thenReturn(
                new ToolDefinition("sample_tool", "Description", Map.of())
        );
        when(tool.execute(Map.of("value", "test"), context)).thenReturn(Map.of("ok", true));
        MCPService service = new MCPService(new ToolRegistry(List.of(tool)));

        ToolResponse response = service.execute(
                new ToolRequest("sample_tool", Map.of("value", "test")),
                context
        );

        assertThat(response.success()).isTrue();
        assertThat(response.result()).isEqualTo(Map.of("ok", true));
        assertThat(response.error()).isNull();
    }

    @Test
    void returnsStructuredErrorForUnknownTool() {
        MCPService service = new MCPService(new ToolRegistry(List.of()));

        ToolResponse response = service.execute(
                new ToolRequest("missing_tool", Map.of()),
                context
        );

        assertThat(response.success()).isFalse();
        assertThat(response.error().code()).isEqualTo("UNKNOWN_TOOL");
    }

    @Test
    void returnsStructuredErrorForInvalidParameters() {
        McpTool tool = mock(McpTool.class);
        when(tool.definition()).thenReturn(
                new ToolDefinition("sample_tool", "Description", Map.of())
        );
        org.mockito.Mockito.doThrow(new ToolValidationException("value is required"))
                .when(tool)
                .validate(Map.of());
        MCPService service = new MCPService(new ToolRegistry(List.of(tool)));

        ToolResponse response = service.execute(
                new ToolRequest("sample_tool", Map.of()),
                context
        );

        assertThat(response.success()).isFalse();
        assertThat(response.error().code()).isEqualTo("INVALID_PARAMETERS");
        assertThat(response.error().message()).isEqualTo("value is required");
    }

    @Test
    void mapsMissingGitHubResourceWithoutReturningServerError() {
        McpTool tool = mock(McpTool.class);
        when(tool.definition()).thenReturn(
                new ToolDefinition("sample_tool", "Description", Map.of())
        );
        when(tool.execute(Map.of(), context)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
        );
        MCPService service = new MCPService(new ToolRegistry(List.of(tool)));

        ToolResponse response = service.execute(
                new ToolRequest("sample_tool", Map.of()),
                context
        );

        assertThat(response.success()).isFalse();
        assertThat(response.error().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
