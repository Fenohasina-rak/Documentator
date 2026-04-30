package org.acme.Services.Gemini;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.Helpers.HtmlAssembler;
import org.acme.Models.File.ProjectStructure;
import org.acme.Services.FileService.FileScanner;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


@ApplicationScoped
public class DocumentationGeneratorService {

    private static final Logger LOG = Logger.getLogger(DocumentationGeneratorService.class);

    @Inject
    FileScanner fileScanner;

    @Inject
    GeminiService geminiService;

    @Inject
    HtmlAssembler htmlAssembler;

    public void generateDocumentation(Path sourcePath, Path outputPath) throws IOException {
        // Step 1: Scan project files

        ProjectStructure structure = fileScanner.scan(sourcePath);

        if (structure.files().isEmpty()) {
            throw new IllegalStateException("No source files found at: " + sourcePath);
        }


        // Step 2: Generate Architecture documentation
        String architectureHtml = callGeminiSafely(
                "Architecture",
                () -> geminiService.generateContent(geminiService.buildArchitecturePrompt(structure))
        );

        // Step 3: Generate API Specification
        String apiHtml = callGeminiSafely(
                "API Specification",
                () -> geminiService.generateContent(geminiService.buildApiSpecPrompt(structure))
        );

        // Step 4: Generate ERD
        String erdHtml = callGeminiSafely(
                "ERD",
                () -> geminiService.generateContent(geminiService.buildErdPrompt(structure))
        );

        // Assemble full HTML document
        String fullHtml = htmlAssembler.assemble(structure, architectureHtml, apiHtml, erdHtml);

        // Write output
        Files.writeString(outputPath, fullHtml, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    interface GeminiCall {
        String call() throws Exception;
    }

    private String callGeminiSafely(String section, GeminiCall call) {
        try {
            String result = call.call();
            return result;
        } catch (Exception e) {
            return "<div class=\"error-section\"><p>Could not generate " + section +
                    " documentation. Error: " + escapeHtml(e.getMessage()) + "</p>" +
                    "<p>Please check your GEMINI_API_KEY and network connection.</p></div>";
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "Unknown error";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
