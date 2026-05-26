package com.rippo.backend.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/user")
    public Object user(OAuth2AuthenticationToken token) {
        return token.getPrincipal().getAttributes();
    }
}