package com.rippo.backend.mcp.controller;

import com.rippo.backend.mcp.dto.ToolDefinition;
import com.rippo.backend.mcp.dto.ToolRequest;
import com.rippo.backend.mcp.dto.ToolResponse;
import com.rippo.backend.mcp.service.MCPService;
import com.rippo.backend.mcp.tool.ToolExecutionContext;
import com.rippo.backend.service.GitHubAuthenticationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp/tools")
public class MCPController {

    private final MCPService mcpService;
    private final GitHubAuthenticationService githubAuthenticationService;

    public MCPController(
            MCPService mcpService,
            GitHubAuthenticationService githubAuthenticationService
    ) {
        this.mcpService = mcpService;
        this.githubAuthenticationService = githubAuthenticationService;
    }

    @GetMapping
    public List<ToolDefinition> getTools() {
        return mcpService.getToolDefinitions();
    }

    @PostMapping("/execute")
    public ToolResponse execute(
            @RequestBody ToolRequest request,
            OAuth2AuthenticationToken authenticationToken,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String accessToken = githubAuthenticationService.getAccessToken(
                authenticationToken,
                oauth2User
        );
        return mcpService.execute(request, new ToolExecutionContext(accessToken));
    }
}
