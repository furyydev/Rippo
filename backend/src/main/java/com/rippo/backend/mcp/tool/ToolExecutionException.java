package com.rippo.backend.mcp.tool;

public class ToolExecutionException extends RuntimeException {

    private final String code;

    public ToolExecutionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
