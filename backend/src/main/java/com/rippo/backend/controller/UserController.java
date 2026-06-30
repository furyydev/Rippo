package com.rippo.backend.controller;

import com.rippo.backend.dto.UserResponse;
import com.rippo.backend.entity.User;
import com.rippo.backend.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user")
    public UserResponse user(@AuthenticationPrincipal OAuth2User oauth2User) {
        User user = userService.findOrCreateFromGitHub(oauth2User);
        return UserResponse.from(user);
    }
}
