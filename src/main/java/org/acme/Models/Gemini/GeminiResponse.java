package org.acme.Models.Gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<Candidate> candidates,
        @JsonProperty("usageMetadata") UsageMetadata usageMetadata
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
            Content content,
            @JsonProperty("finishReason") String finishReason,
            int index
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            List<Part> parts,
            String role
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsageMetadata(
            @JsonProperty("promptTokenCount") int promptTokenCount,
            @JsonProperty("candidatesTokenCount") int candidatesTokenCount,
            @JsonProperty("totalTokenCount") int totalTokenCount
    ) {}
}
