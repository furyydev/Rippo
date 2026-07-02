package com.rippo.backend.chat.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rippo.backend.mcp.dto.ToolRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AgentResponseParser {

    private static final String TOOL_REQUEST = "tool_request";
    private static final String FINAL_ANSWER = "final_answer";

    private final ObjectMapper objectMapper;

    public AgentResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AgentDecision parse(String response) {
        String normalized = stripCodeFence(response == null ? "" : response.trim());
        if (!normalized.startsWith("{")) {
            return AgentDecision.finalAnswer(normalized);
        }

        try {
            JsonNode root = objectMapper.readTree(normalized);
            String type = root.path("type").asText();
            if (TOOL_REQUEST.equals(type)) {
                String toolName = root.path("toolName").asText();
                Map<String, Object> arguments = root.has("arguments")
                        ? objectMapper.convertValue(
                                root.get("arguments"),
                                objectMapper.getTypeFactory().constructMapType(
                                        LinkedHashMap.class,
                                        String.class,
                                        Object.class
                                )
                        )
                        : Map.of();
                return AgentDecision.toolRequest(new ToolRequest(toolName, arguments));
            }
            if (FINAL_ANSWER.equals(type)) {
                if (root.path("answer").isTextual()) {
                    return AgentDecision.finalAnswer(root.path("answer").asText());
                }
                // Recognised protocol shape but a malformed payload: never expose the
                // raw JSON to the user. An empty answer is converted to a safe fallback
                // by the agent loop.
                return AgentDecision.finalAnswer("");
            }
        } catch (Exception ignored) {
            // A malformed protocol response is still useful as a plain-text answer.
        }

        return AgentDecision.finalAnswer(response.trim());
    }

    private String stripCodeFence(String response) {
        if (!response.startsWith("```") || !response.endsWith("```")) {
            return response;
        }
        int firstLineEnd = response.indexOf('\n');
        if (firstLineEnd < 0) {
            return response;
        }
        return response.substring(firstLineEnd + 1, response.length() - 3).trim();
    }
}
