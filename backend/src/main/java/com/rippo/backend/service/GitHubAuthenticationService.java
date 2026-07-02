package com.rippo.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GitHubAuthenticationService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GitHubAuthenticationService(
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        this.authorizedClientService = authorizedClientService;
    }

    public String getAccessToken(
            OAuth2AuthenticationToken authenticationToken,
            OAuth2User oauth2User
    ) {
        if (authenticationToken == null || oauth2User == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "You must be logged in with GitHub"
            );
        }

        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(
                        authenticationToken.getAuthorizedClientRegistrationId(),
                        oauth2User.getName()
                );

        if (client == null || client.getAccessToken() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "GitHub access token was not found"
            );
        }

        return client.getAccessToken().getTokenValue();
    }
}
