package com.rippo.backend.chat.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rippo.backend.ai.service.GeminiService;
import com.rippo.backend.mcp.dto.ToolRequest;
import com.rippo.backend.mcp.dto.ToolResponse;
import com.rippo.backend.mcp.service.MCPService;
import com.rippo.backend.mcp.tool.ToolExecutionContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentLoopTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentResponseParser parser = new AgentResponseParser(objectMapper);

    @Test
    void executesToolAndReturnsGeminiFinalAnswer() {
        GeminiService gemini = mock(GeminiService.class);
        MCPService mcpService = mock(MCPService.class);
        AgentLoop loop = new AgentLoop(gemini, mcpService, parser, objectMapper);
        ToolRequest request = request("src/SecurityConfig.java");
        when(gemini.generateJson("initial prompt")).thenReturn(toolJson(request));
        when(mcpService.execute(request, new ToolExecutionContext("token")))
                .thenReturn(ToolResponse.success("read_file", Map.of("content", "security")));
        when(gemini.generateJson(contains("\"content\":\"security\"")))
                .thenReturn("Authentication uses Spring Security.");

        String answer = run(loop, "initial prompt");

        assertThat(answer).isEqualTo("Authentication uses Spring Security.");
        verify(mcpService).execute(request, new ToolExecutionContext("token"));
    }

    @Test
    void sendsToolFailureBackToGeminiForRecovery() {
        GeminiService gemini = mock(GeminiService.class);
        MCPService mcpService = mock(MCPService.class);
        AgentLoop loop = new AgentLoop(gemini, mcpService, parser, objectMapper);
        ToolRequest request = request("Missing.java");
        when(gemini.generateJson("prompt")).thenReturn(toolJson(request));
        when(mcpService.execute(any(), any())).thenReturn(
                ToolResponse.failure("read_file", "RESOURCE_NOT_FOUND", "File not found")
        );
        when(gemini.generateJson(contains("RESOURCE_NOT_FOUND")))
                .thenReturn("That file was not present, so I used the available context.");

        String answer = run(loop, "prompt");

        assertThat(answer).contains("file was not present");
    }

    @Test
    void preventsDuplicateToolExecution() {
        GeminiService gemini = mock(GeminiService.class);
        MCPService mcpService = mock(MCPService.class);
        AgentLoop loop = new AgentLoop(gemini, mcpService, parser, objectMapper);
        ToolRequest request = request("App.java");
        when(gemini.generateJson(any(String.class)))
                .thenReturn(toolJson(request))
                .thenReturn(toolJson(request))
                .thenReturn("Final answer");
        when(mcpService.execute(any(), any()))
                .thenReturn(ToolResponse.success("read_file", Map.of("content", "app")));

        String answer = run(loop, "prompt");

        assertThat(answer).isEqualTo("Final answer");
        verify(mcpService, times(1)).execute(any(), any());
        verify(gemini).generateJson(contains("DUPLICATE_TOOL_REQUEST"));
    }

    @Test
    void limitsToolExecutionsToFiveAndRequestsBestFinalAnswer() {
        GeminiService gemini = mock(GeminiService.class);
        MCPService mcpService = mock(MCPService.class);
        AgentLoop loop = new AgentLoop(gemini, mcpService, parser, objectMapper);
        when(gemini.generateJson(any(String.class)))
                .thenReturn(toolJson(request("File0.java")))
                .thenReturn(toolJson(request("File1.java")))
                .thenReturn(toolJson(request("File2.java")))
                .thenReturn(toolJson(request("File3.java")))
                .thenReturn(toolJson(request("File4.java")))
                .thenReturn(toolJson(request("File5.java")))
                .thenReturn("Best answer from five files");
        when(mcpService.execute(any(), any()))
                .thenReturn(ToolResponse.success("read_file", Map.of("content", "content")));

        String answer = run(loop, "prompt");

        assertThat(answer).isEqualTo("Best answer from five files");
        verify(mcpService, times(AgentLoop.MAX_TOOL_EXECUTIONS)).execute(any(), any());
        verify(gemini).generateJson(contains("maximum of 5 tool executions"));
    }

    @Test
    void preventsToolsFromChangingTheActiveRepository() {
        GeminiService gemini = mock(GeminiService.class);
        MCPService mcpService = mock(MCPService.class);
        AgentLoop loop = new AgentLoop(gemini, mcpService, parser, objectMapper);
        ToolRequest outsideRepository = new ToolRequest(
                "read_file",
                Map.of(
                        "repositoryOwner", "someone-else",
                        "repositoryName", "repo",
                        "filePath", "App.java"
                )
        );
        when(gemini.generateJson(any(String.class)))
                .thenReturn(toolJson(outsideRepository))
                .thenReturn("I stayed within the selected repository.");

        String answer = run(loop, "prompt");

        assertThat(answer).contains("selected repository");
        verify(gemini).generateJson(contains("REPOSITORY_SCOPE_VIOLATION"));
        verify(mcpService, times(0)).execute(any(), any());
    }

    private ToolRequest request(String path) {
        return new ToolRequest(
                "read_file",
                Map.of(
                        "repositoryOwner", "owner",
                        "repositoryName", "repo",
                        "filePath", path
                )
        );
    }

    private String toolJson(ToolRequest request) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "tool_request",
                    "toolName", request.name(),
                    "arguments", request.arguments()
            ));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private String run(AgentLoop loop, String prompt) {
        return loop.run(prompt, "token", "owner", "repo");
    }
}

