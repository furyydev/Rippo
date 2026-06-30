package com.rippo.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rippo.backend.entity.User;
import java.time.Instant;

public class UserResponse {

    private final Long id;
    private final Long githubId;
    private final String username;
    private final String avatarUrl;
    private final String email;
    private final String htmlUrl;
    private final Instant createdAt;
    private final Instant updatedAt;

    private UserResponse(
            Long id,
            Long githubId,
            String username,
            String avatarUrl,
            String email,
            String htmlUrl,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.githubId = githubId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.email = email;
        this.htmlUrl = htmlUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getGithubId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getEmail(),
                "https://github.com/" + user.getUsername(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getGithubId() {
        return githubId;
    }

    public String getUsername() {
        return username;
    }

    @JsonProperty("login")
    public String getLogin() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    @JsonProperty("avatar_url")
    public String getAvatarUrlSnakeCase() {
        return avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    @JsonProperty("html_url")
    public String getHtmlUrlSnakeCase() {
        return htmlUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
