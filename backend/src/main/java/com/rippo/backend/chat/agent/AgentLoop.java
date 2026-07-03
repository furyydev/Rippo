package com.rippo.backend.chat.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rippo.backend.ai.service.GeminiService;
import com.rippo.backend.mcp.dto.ToolRequest;
import com.rippo.backend.mcp.dto.ToolResponse;
import com.rippo.backend.mcp.service.MCPService;
import com.rippo.backend.mcp.tool.ToolArguments;
import com.rippo.backend.mcp.tool.ToolExecutionContext;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentLoop {

    static final int MAX_TOOL_EXECUTIONS = 5;
    static final int MAX_MODEL_TURNS = 10;
    static final Duration EXECUTION_DEADLINE = Duration.ofSeconds(90);

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentLoop.class);
    private static final String GRACEFUL_FALLBACK =
            "I could not finish analysing this repository within the available time. "
                    + "Please narrow the question and try again.";

    private final GeminiService geminiService;
    private final MCPService mcpService;
    private final AgentResponseParser responseParser;
    private final ObjectMapper objectMapper;

    public AgentLoop(
            GeminiService geminiService,
            MCPService mcpService,
            AgentResponseParser responseParser,
            ObjectMapper objectMapper
    ) {
        this.geminiService = geminiService;
        this.mcpService = mcpService;
        this.responseParser = responseParser;
        this.objectMapper = objectMapper;
    }

    public String run(
            String initialPrompt,
            String githubAccessToken,
            String repositoryOwner,
            String repositoryName
    ) {
        StringBuilder conversation = new StringBuilder(initialPrompt);
        Set<ToolRequest> executedRequests = new HashSet<>();
        int executionCount = 0;
        long deadlineNanos = System.nanoTime() + EXECUTION_DEADLINE.toNanos();

        for (int turn = 1; turn <= MAX_MODEL_TURNS; turn++) {
            if (deadlineExceeded(deadlineNanos)) {
                LOGGER.warn(
                        "Agent loop stopped: outcome=DEADLINE_EXCEEDED turns={} toolExecutions={}",
                        turn - 1,
                        executionCount
                );
                return GRACEFUL_FALLBACK;
            }

            LOGGER.info("Agent loop calling Gemini: iteration={}", turn);
            String geminiResponse = geminiService.generateJson(conversation.toString());
            AgentDecision decision = responseParser.parse(geminiResponse);

            if (decision.type() == AgentDecision.Type.FINAL_ANSWER) {
                LOGGER.info(
                        "Agent loop completed: outcome=FINAL_ANSWER iterations={} toolExecutions={}",
                        turn,
                        executionCount
                );
                return sanitizeAnswer(decision.answer());
            }

            ToolRequest requestedTool = decision.toolRequest();
            LOGGER.info(
                    "Agent selected tool: name={} iteration={}",
                    requestedTool.name(),
                    turn
            );

            if (targetsDifferentRepository(
                    requestedTool,
                    repositoryOwner,
                    repositoryName
            )) {
                LOGGER.warn(
                        "Agent tool rejected: name={} reason=REPOSITORY_SCOPE_VIOLATION",
                        requestedTool.name()
                );
                appendToolFeedback(
                        conversation,
                        geminiResponse,
                        ToolResponse.failure(
                                requestedTool.name(),
                                "REPOSITORY_SCOPE_VIOLATION",
                                "Tools may only inspect the repository selected for this chat"
                        )
                );
                continue;
            }

            // Never rely solely on model-supplied parameters: force the active repository.
            ToolRequest toolRequest = pinToActiveRepository(
                    requestedTool,
                    repositoryOwner,
                    repositoryName
            );

            if (executedRequests.contains(toolRequest)) {
                appendToolFeedback(
                        conversation,
                        geminiResponse,
                        ToolResponse.failure(
                                toolRequest.name(),
                                "DUPLICATE_TOOL_REQUEST",
                                "This exact tool request was already executed in this conversation"
                        )
                );
                continue;
            }

            if (executionCount >= MAX_TOOL_EXECUTIONS) {
                return requestFinalAnswer(
                        conversation,
                        "The maximum of " + MAX_TOOL_EXECUTIONS
                                + " tool executions has been reached. Use the collected results.",
                        "TOOL_LIMIT_REACHED"
                );
            }

            executedRequests.add(toolRequest);
            executionCount++;
            long toolStartedAt = System.nanoTime();
            ToolResponse toolResponse = mcpService.execute(
                    toolRequest,
                    new ToolExecutionContext(githubAccessToken)
            );
            appendToolFeedback(conversation, geminiResponse, toolResponse);
            LOGGER.info(
                    "Agent tool finished: name={} success={} durationMs={} execution={}/{}",
                    toolRequest.name(),
                    toolResponse.success(),
                    elapsedMillis(toolStartedAt),
                    executionCount,
                    MAX_TOOL_EXECUTIONS
            );
        }

        return requestFinalAnswer(
                conversation,
                "The reasoning turn limit has been reached. Answer with the available context.",
                "TURN_LIMIT_REACHED"
        );
    }

    private void appendToolFeedback(
            StringBuilder conversation,
            String geminiResponse,
            ToolResponse toolResponse
    ) {
        conversation.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("ASSISTANT TOOL REQUEST")
                .append(System.lineSeparator())
                .append(geminiResponse)
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("TOOL RESULT")
                .append(System.lineSeparator())
                .append(toJson(toolResponse))
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("Continue. Request another tool only if needed; "
                        + "otherwise return the final_answer JSON.");
    }

    private String requestFinalAnswer(
            StringBuilder conversation,
            String reason,
            String outcome
    ) {
        LOGGER.warn("Agent loop forcing final answer: outcome={} reason={}", outcome, reason);
        conversation.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("FINAL ANSWER REQUIRED")
                .append(System.lineSeparator())
                .append(reason)
                .append(System.lineSeparator())
                .append("Do not request another tool. "
                        + "Return the best final_answer JSON now.");

        AgentDecision decision = responseParser.parse(
                geminiService.generateJson(conversation.toString())
        );
        if (decision.type() == AgentDecision.Type.FINAL_ANSWER) {
            return sanitizeAnswer(decision.answer());
        }
        return "I could not complete the repository analysis within the tool execution limit.";
    }

    /**
     * Guards against protocol JSON or empty payloads reaching the user when the model
     * ignores the response contract.
     */
    private String sanitizeAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return GRACEFUL_FALLBACK;
        }
        String trimmed = answer.trim();
        if (looksLikeProtocolJson(trimmed)) {
            LOGGER.warn("Agent loop suppressed protocol JSON leaking into the final answer");
            return GRACEFUL_FALLBACK;
        }
        return answer;
    }

    private boolean looksLikeProtocolJson(String text) {
        return text.startsWith("{")
                && text.contains("\"type\"")
                && (text.contains("tool_request") || text.contains("final_answer"));
    }

    private String toJson(ToolResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            return "{\"success\":false,\"error\":{\"code\":\"RESULT_SERIALIZATION_FAILURE\","
                    + "\"message\":\"The tool result could not be added to the conversation\"}}";
        }
    }

    private ToolRequest pinToActiveRepository(
            ToolRequest request,
            String repositoryOwner,
            String repositoryName
    ) {
        Map<String, Object> arguments = new LinkedHashMap<>(request.arguments());
        arguments.put(ToolArguments.REPOSITORY_OWNER, repositoryOwner);
        arguments.put(ToolArguments.REPOSITORY_NAME, repositoryName);
        return new ToolRequest(request.name(), arguments);
    }

    private boolean targetsDifferentRepository(
            ToolRequest request,
            String repositoryOwner,
            String repositoryName
    ) {
        return differs(request, ToolArguments.REPOSITORY_OWNER, repositoryOwner)
                || differs(request, ToolArguments.REPOSITORY_NAME, repositoryName);
    }

    private boolean differs(ToolRequest request, String key, String expectedValue) {
        Object requestedValue = request.arguments().get(key);
        return requestedValue instanceof String text
                && !text.equalsIgnoreCase(expectedValue);
    }

    private boolean deadlineExceeded(long deadlineNanos) {
        return System.nanoTime() >= deadlineNanos;
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }
}
