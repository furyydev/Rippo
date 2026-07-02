package com.rippo.backend.mcp.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ToolRequest(String name, Map<String, Object> arguments) {

    public ToolRequest {
        arguments = arguments == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }
}
