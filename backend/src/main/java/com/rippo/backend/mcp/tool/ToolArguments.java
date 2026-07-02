package com.rippo.backend.mcp.tool;

import java.util.Map;

public final class ToolArguments {

    public static final String REPOSITORY_OWNER = "repositoryOwner";
    public static final String REPOSITORY_NAME = "repositoryName";
    public static final String FILE_PATH = "filePath";
    public static final String DIRECTORY_PATH = "directoryPath";

    private ToolArguments() {
    }

    public static String requireText(Map<String, Object> arguments, String parameter) {
        Object value = arguments.get(parameter);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ToolValidationException(
                    "Parameter '" + parameter + "' must be a non-blank string"
            );
        }
        return text;
    }

    public static String requireString(Map<String, Object> arguments, String parameter) {
        Object value = arguments.get(parameter);
        if (!(value instanceof String text)) {
            throw new ToolValidationException(
                    "Parameter '" + parameter + "' must be a string"
            );
        }
        return text;
    }
}
