package com.rippo.backend.ai.controller;

import com.rippo.backend.ai.dto.AiTestRequest;
import com.rippo.backend.ai.dto.AiTestResponse;
import com.rippo.backend.ai.service.GeminiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiTestController {

    private final GeminiService geminiService;

    public AiTestController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping(
            value = "/test",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AiTestResponse testGemini(@RequestBody(required = false) AiTestRequest request) {
        if (request == null) {
            return new AiTestResponse(geminiService.generateText(null));
        }

        return new AiTestResponse(geminiService.generateText(request.message()));
    }
}
