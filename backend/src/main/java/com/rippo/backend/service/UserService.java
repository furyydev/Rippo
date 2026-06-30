package com.rippo.backend.service;

import com.rippo.backend.entity.User;
import com.rippo.backend.repository.UserRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreateFromGitHub(OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "You must be logged in with GitHub"
            );
        }

        Map<String, Object> attributes = oauth2User.getAttributes();
        Long githubId = parseGithubId(attributes.get("id"));
        String username = parseRequiredString(attributes.get("login"), "GitHub username was not found");
        String avatarUrl = parseOptionalString(attributes.get("avatar_url"));
        String email = parseOptionalString(attributes.get("email"));

        return userRepository.findByGithubId(githubId)
                .map(existingUser -> {
                    existingUser.updateFromGitHub(username, avatarUrl, email);
                    logger.info(
                            "Loaded existing Rippo user id={} githubId={} username={}",
                            existingUser.getId(),
                            existingUser.getGithubId(),
                            existingUser.getUsername()
                    );
                    return existingUser;
                })
                .orElseGet(() -> {
                    User createdUser = userRepository.save(
                            new User(githubId, username, avatarUrl, email)
                    );
                    logger.info(
                            "Created Rippo user id={} githubId={} username={}",
                            createdUser.getId(),
                            createdUser.getGithubId(),
                            createdUser.getUsername()
                    );
                    return createdUser;
                });
    }

    private Long parseGithubId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "GitHub user ID was not a valid number",
                        exception
                );
            }
        }

        throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "GitHub user ID was not found"
        );
    }

    private String parseRequiredString(Object value, String errorMessage) {
        String stringValue = parseOptionalString(value);
        if (stringValue == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorMessage);
        }

        return stringValue;
    }

    private String parseOptionalString(Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString();
        return stringValue.isBlank() ? null : stringValue;
    }
}
