package com.rippo.backend.chat.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AgentResponseParserTests {

    private final AgentResponseParser parser = new AgentResponseParser(new ObjectMapper());

    @Test
    void parsesToolRequestJsonInsideMarkdownFence() {
        AgentDecision decision = parser.parse("""
                ```json
                {"type":"tool_request","toolName":"read_file",
                 "arguments":{"repositoryOwner":"owner","filePath":"App.java"}}
                ```
                """);

        assertThat(decision.type()).isEqualTo(AgentDecision.Type.TOOL_REQUEST);
        assertThat(decision.toolRequest().name()).isEqualTo("read_file");
        assertThat(decision.toolRequest().arguments())
                .containsEntry("repositoryOwner", "owner")
                .containsEntry("filePath", "App.java");
    }

    @Test
    void treatsNormalTextAsFinalAnswer() {
        AgentDecision decision = parser.parse("Authentication uses GitHub OAuth.");

        assertThat(decision.type()).isEqualTo(AgentDecision.Type.FINAL_ANSWER);
        assertThat(decision.answer()).isEqualTo("Authentication uses GitHub OAuth.");
    }

    @Test
    void parsesExplicitFinalAnswer() {
        AgentDecision decision = parser.parse(
                "{\"type\":\"final_answer\",\"answer\":\"The final answer\"}"
        );

        assertThat(decision.type()).isEqualTo(AgentDecision.Type.FINAL_ANSWER);
        assertThat(decision.answer()).isEqualTo("The final answer");
    }
}
