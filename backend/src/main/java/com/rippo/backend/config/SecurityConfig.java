package com.rippo.backend.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {
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
