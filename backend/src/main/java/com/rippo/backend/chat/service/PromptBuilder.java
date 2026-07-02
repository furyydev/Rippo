package com.rippo.backend.chat.service;

import com.rippo.backend.chat.model.PreviousChatMessage;
import com.rippo.backend.chat.model.RepositoryContext;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String build(RepositoryContext context, String currentQuestion) {
        if (currentQuestion == null || currentQuestion.isBlank()) {
            throw new ChatException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Message must not be empty"
            );
        }

        StringBuilder history = new StringBuilder();
        if (context.previousChatMessages().isEmpty()) {
            history.append("No previous conversation.");
        } else {
            for (PreviousChatMessage message : context.previousChatMessages()) {
                history.append(message.role())
                        .append(": ")
                        .append(message.content())
                        .append(System.lineSeparator());
            }
        }

        return """
                SYSTEM INSTRUCTIONS
                You are Rippo, a professional GitHub repository assistant.
                Answer only from the repository context and conversation history below.
                Keep the answer concise, clear, and technically accurate.
                If the available context does not contain the answer, say so clearly.
                Do not invent repository details.

                REPOSITORY
                Owner: %s
                Name: %s
                Default branch: %s

                README
                <readme>
                %s
                </readme>

                PREVIOUS CONVERSATION
                <history>
                %s
                </history>

                CURRENT USER QUESTION
                <question>
                %s
                </question>
                """.formatted(
                context.repositoryOwner(),
                context.repositoryName(),
                context.defaultBranch(),
                context.readmeContent(),
                history.toString().stripTrailing(),
                currentQuestion.trim()
        );
    }
}
