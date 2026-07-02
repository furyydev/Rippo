package com.rippo.backend.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rippo.backend.chat.model.PreviousChatMessage;
import com.rippo.backend.chat.model.RepositoryContext;
import com.rippo.backend.entity.ChatMessageRole;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBuilderTests {

    private final PromptBuilder promptBuilder = new PromptBuilder();

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

        String prompt = promptBuilder.build(context, "How does authentication work?");

        assertThat(prompt)
                .contains("Owner: rippo-owner")
                .contains("Name: rippo")
                .contains("Default branch: main")
                .contains("Rippo is a repository assistant.")
                .contains("USER: What is this project?")
                .contains("ASSISTANT: It is a repository assistant.")
                .contains("How does authentication work?")
                .contains("Do not invent repository details");
    }
}
