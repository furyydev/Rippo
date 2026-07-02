package com.rippo.backend.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rippo.backend.chat.model.PreviousChatMessage;
import com.rippo.backend.chat.model.RepositoryContext;
import com.rippo.backend.entity.ChatMessageRole;
import com.rippo.backend.mcp.dto.ToolDefinition;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBuilderTests {

    private final PromptBuilder promptBuilder = new PromptBuilder(new ObjectMapper());

    @Test
    void includesRepositoryReadmeHistoryAndCurrentQuestion() {
        RepositoryContext context = new RepositoryContext(
                "rippo-owner",
                "rippo",
                "main",
                "Rippo is a repository assistant.",
                List.of(
                        new PreviousChatMessage(
                                ChatMessageRole.USER,
                                "What is this project?"
                        ),
                        new PreviousChatMessage(
                                ChatMessageRole.ASSISTANT,
                                "It is a repository assistant."
                        )
                )
        );

        String prompt = promptBuilder.build(
                context,
                "How does authentication work?",
                List.of(new ToolDefinition(
                        "read_file",
                        "Reads a repository file.",
                        Map.of("type", "object")
                ))
        );

        assertThat(prompt)
                .contains("Owner: rippo-owner")
                .contains("Name: rippo")
                .contains("Default branch: main")
                .contains("Rippo is a repository assistant.")
                .contains("USER: What is this project?")
                .contains("ASSISTANT: It is a repository assistant.")
                .contains("How does authentication work?")
                .contains("Do not invent repository details")
                .contains("read_file")
                .contains("Reads a repository file.")
                .contains("\"type\":\"tool_request\"")
                .contains("Do not repeat a tool request with identical arguments");
    }
}
