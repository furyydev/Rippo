package com.rippo.backend.config;

import com.rippo.backend.service.UserService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/ai/test").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ai/test", "/chat", "/chat/**")
                )
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {
                            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                                userService.findOrCreateFromGitHub(oauth2User);
                            }

                            String sessionId = request.getSession().getId();
                            String encodedSessionId = URLEncoder.encode(
                                    sessionId,
                                    StandardCharsets.UTF_8
                            );

                            response.sendRedirect("rippo://auth/success?sessionId=" + encodedSessionId);
                        })
                );

        return http.build();
    }
}
