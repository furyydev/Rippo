package com.rippo.backend.chat.agent;

import com.rippo.backend.mcp.dto.ToolRequest;

public record AgentDecision(Type type, String answer, ToolRequest toolRequest) {

    public enum Type {
        FINAL_ANSWER,
        TOOL_REQUEST
    }

    public static AgentDecision finalAnswer(String answer) {
        return new AgentDecision(Type.FINAL_ANSWER, answer, null);
    }

    public static AgentDecision toolRequest(ToolRequest request) {
        return new AgentDecision(Type.TOOL_REQUEST, null, request);
    }
}
