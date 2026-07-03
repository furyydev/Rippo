package com.rippo.backend.mcp.dto;

public record ToolResponse(
        String toolName,
        boolean success,
        Object result,
        ToolError error
) {

    public static ToolResponse success(String toolName, Object result) {
        return new ToolResponse(toolName, true, result, null);
    }

    public static ToolResponse failure(String toolName, String code, String message) {
        return new ToolResponse(toolName, false, null, new ToolError(code, message));
    }

    public record ToolError(String code, String message) {
    }
}
