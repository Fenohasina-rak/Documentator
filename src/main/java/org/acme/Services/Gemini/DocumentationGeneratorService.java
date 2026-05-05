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
import java.util.function.Function;


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
        String architectureHtml;
        String apiHtml;
        String erdHtml;
        // Step 1: Scan project files
        ProjectStructure structure = fileScanner.scan(sourcePath);
        if (structure.files().isEmpty()) {
            throw new IllegalStateException("No source files found at: " + sourcePath);
        }
        // Step 2: Generate Architecture documentation
        try{
            architectureHtml = geminiService.generateContent(geminiService.buildArchitecturePrompt(structure));
        } catch (Exception e) {
            try{
                architectureHtml = geminiService.generateContent(geminiService.buildArchitecturePrompt(structure));
            } catch (Exception ex) {
                architectureHtml = "<div class=\"error-section\"><p>Could not generate " + "Architecture" +
                        " documentation. Error: " + escapeHtml(e.getMessage()) + "</p>" +
                        "<p>Please check your GEMINI_API_KEY and network connection.</p></div>";
            }
        }
        // Step 3: Generate API Specification
        try{
            apiHtml = geminiService.generateContent(geminiService.buildApiSpecPrompt(structure));
        } catch (Exception e) {
            try{
                apiHtml = geminiService.generateContent(geminiService.buildApiSpecPrompt(structure));
            } catch (Exception ex) {
                apiHtml = "<div class=\"error-section\"><p>Could not generate " + "API Specification" +
                        " documentation. Error: " + escapeHtml(e.getMessage()) + "</p>" +
                        "<p>Please check your GEMINI_API_KEY and network connection.</p></div>";
            }
        }
        // Step 4: Generate ERD
        try{
            erdHtml = geminiService.generateContent(geminiService.buildErdPrompt(structure));
        } catch (Exception e) {
            try {
                erdHtml = geminiService.generateContent(geminiService.buildErdPrompt(structure));
            } catch (Exception ex) {
                erdHtml = "<div class=\"error-section\"><p>Could not generate " + "ERD" +
                        " documentation. Error: " + escapeHtml(e.getMessage()) + "</p>" +
                        "<p>Please check your GEMINI_API_KEY and network connection.</p></div>";
            }

        }

        // Assemble full HTML document
        String fullHtml = htmlAssembler.assemble(structure, architectureHtml, apiHtml, erdHtml);

        // Write output
        Files.writeString(outputPath, fullHtml, StandardCharsets.UTF_8);
    }



    private String escapeHtml(String text) {
        if (text == null) return "Unknown error";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
