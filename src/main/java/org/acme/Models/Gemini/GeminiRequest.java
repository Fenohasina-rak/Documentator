package org.acme.Models.Gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public record GeminiRequest(
        List<Content> contents,
        @JsonProperty("generationConfig") GenerationConfig generationConfig
) {
    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(
            @JsonProperty("maxOutputTokens") int maxOutputTokens,
            float temperature
    ) {}
}
