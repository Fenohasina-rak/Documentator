package org.acme.Services.Gemini;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.Models.File.ProjectFile;
import org.acme.Models.File.ProjectStructure;
import org.acme.Models.Gemini.GeminiRequest;
import org.acme.Models.Gemini.GeminiResponse;
import org.acme.Services.FileService.FileScanner;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import java.util.List;

@ApplicationScoped
public class GeminiService {
    private final GeminiClient geminiClient;
    private final FileScanner fileScanner;

    @Inject
    public GeminiService(@RestClient GeminiClient geminiClient, FileScanner fileScanner) {
        this.geminiClient = geminiClient;
        this.fileScanner = fileScanner;
    }

    private static final Logger LOG = Logger.getLogger(GeminiService.class);

    String apiKey = ConfigProvider.getConfig().getValue("gemini.api.key", String.class);

    String model = ConfigProvider.getConfig().getValue("gemini.model", String.class);

    Integer charactersLimit = ConfigProvider.getConfig().getValue("gemini.limit.characters", Integer.class);


    public String generateContent(String prompt) {
        GeminiRequest request = new GeminiRequest(
                List.of(new GeminiRequest.Content(
                        List.of(new GeminiRequest.Part(prompt))
                )),
                new GeminiRequest.GenerationConfig(8192, 0.2f)
        );

        LOG.debugf("Calling Gemini API (model: %s)...", model);
        GeminiResponse response = geminiClient.generateContent(model, apiKey, request);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new RuntimeException("Empty response from Gemini API");
        }

        GeminiResponse.Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            throw new RuntimeException("No content in Gemini response. Finish reason: " + candidate.finishReason());
        }

        return candidate.content().parts().stream()
                .map(GeminiResponse.Part::text)
                .reduce("", String::concat);
    }


    public String buildArchitecturePrompt(ProjectStructure structure) {
        StringBuilder sb = new StringBuilder();
        sb.append(FileScanner.ARCHITECTURE_PROMPT.formatted(structure.projectName(), structure.files().size()));

        for (ProjectFile f : structure.files()) {
            sb.append("  ").append(f.relativePath()).append(" (").append(f.sizeBytes()).append(" bytes)\n");
        }

        sb.append("\n\n=== SOURCE CODE ===\n\n");
        appendCodeContext(sb, structure, charactersLimit);

        return sb.toString();
    }


    public String buildApiSpecPrompt(ProjectStructure structure) {
        StringBuilder sb = new StringBuilder();
        sb.append(FileScanner.API_PROMPT.formatted(structure.projectName()));

        appendCodeContext(sb, structure, charactersLimit);
        return sb.toString();
    }


    public String buildErdPrompt(ProjectStructure structure) {
        StringBuilder sb = new StringBuilder();
        sb.append(FileScanner.ERD_PROMPT.formatted(structure.projectName()));

        appendCodeContext(sb, structure, charactersLimit);
        return sb.toString();
    }


    private void appendCodeContext(StringBuilder sb, ProjectStructure structure, int charLimit) {
        int used = 0;
        // Prioritize Java files, then config files
        List<ProjectFile> sorted = structure.files().stream()
                .sorted((a, b) -> {
                    int scoreA = fileImportance(a.extension());
                    int scoreB = fileImportance(b.extension());
                    return Integer.compare(scoreB, scoreA);
                })
                .toList();

        for (ProjectFile file : sorted) {
            String header = "\n--- FILE: " + file.relativePath() + " ---\n";
            String content = file.content();
            int needed = header.length() + content.length() + 4;

            if (used + needed > charLimit) {
                sb.append("\n[... ").append(structure.files().size() - sorted.indexOf(file))
                        .append(" more files truncated due to size limit ...]\n");
                break;
            }

            sb.append(header).append(content).append("\n");
            used += needed;
        }
    }

    private int fileImportance(String ext) {
        return switch (ext) {
            case ".java" -> 10;
            case ".kt" -> 10;
            case ".sql" -> 9;
            case ".xml" -> 7;
            case ".properties", ".yaml", ".yml" -> 6;
            case ".json" -> 5;
            case ".gradle" -> 4;
            case ".md" -> 2;
            default -> 1;
        };
    }
}
