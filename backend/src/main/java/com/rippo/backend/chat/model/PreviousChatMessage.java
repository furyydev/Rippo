package com.rippo.backend.chat.model;

import com.rippo.backend.entity.ChatMessageRole;

public record PreviousChatMessage(ChatMessageRole role, String content) {
}
