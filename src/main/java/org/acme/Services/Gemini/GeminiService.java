package org.acme.Services.Gemini;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.Models.File.ProjectFile;
import org.acme.Models.File.ProjectStructure;
import org.acme.Models.Gemini.GeminiRequest;
import org.acme.Models.Gemini.GeminiResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class GeminiService {

    private static final Logger LOG = Logger.getLogger(GeminiService.class);

    @Inject
    @RestClient
    GeminiClient geminiClient;

    @ConfigProperty(name = "gemini.api.key")
    String apiKey;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-1.5-flash")
    String model;

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
        sb.append("""
                You are a senior software architect. Analyze the following source code project and produce a detailed Architecture Documentation.
                
                Return ONLY a valid HTML fragment (no <html>, <head>, or <body> tags) that includes:
                1. Project Overview — what this project does, its purpose
                2. Technology Stack — frameworks, libraries, languages detected
                3. Architecture Style — monolith, microservice, layered, hexagonal, etc.
                4. Module/Package Breakdown — describe each package/module and its responsibility
                5. Component Interaction Diagram — use an ASCII diagram or describe the flow between layers
                6. Key Design Patterns — identify patterns used (Repository, Service, Factory, etc.)
                7. Configuration & Infrastructure — how the app is configured, any Docker/K8s/CI files
                
                Use semantic HTML with proper headings (h2, h3), paragraphs, tables, and code blocks (<pre><code>).
                Make it professional and developer-friendly.
                
                PROJECT NAME: %s
                TOTAL FILES: %d
                
                FILE LISTING (path → size in bytes):
                """.formatted(structure.projectName(), structure.files().size()));

        for (ProjectFile f : structure.files()) {
            sb.append("  ").append(f.relativePath()).append(" (").append(f.sizeBytes()).append(" bytes)\n");
        }

        sb.append("\n\n=== SOURCE CODE ===\n\n");
        appendCodeContext(sb, structure, 120000);

        return sb.toString();
    }


    public String buildApiSpecPrompt(ProjectStructure structure) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are an API documentation expert. Analyze the following source code and produce a comprehensive API Specification document.
                
                Return ONLY a valid HTML fragment (no <html>, <head>, or <body> tags) that includes:
                1. API Overview — base URL pattern, authentication method, content types
                2. Endpoints Table — list all REST endpoints with: Method, Path, Description, Request Body, Response, HTTP Status Codes
                3. Data Models — document all DTOs, request/response models with field names, types, required/optional, description
                4. Error Handling — document error responses and codes used
                5. Security — any roles, permissions, or JWT claims used
                6. OpenAPI-style detail for each endpoint (parameters, body schema, example responses)
                
                If the project has no REST endpoints, document the public API (public classes/methods) instead.
                Use semantic HTML with tables, code blocks, and proper headings.
                Use <code> tags for method names, paths, and field names.
                
                PROJECT NAME: %s
                
                === SOURCE CODE ===
                
                """.formatted(structure.projectName()));

        appendCodeContext(sb, structure, 120000);
        return sb.toString();
    }


    public String buildErdPrompt(ProjectStructure structure) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are a database architect. Analyze the following source code and produce an Entity Relationship Documentation.
                
                Return ONLY a valid HTML fragment (no <html>, <head>, or <body> tags) that includes:
                1. Entity Overview — list all entities/tables found (JPA @Entity, SQL schemas, or inferred from models)
                2. ERD as ASCII Art — draw a text-based ERD showing entities, their fields, and relationships
                   Format: use box-drawing characters like ┌─┐ │ └─┘ for boxes
                3. Entity Details Table — for each entity: field name, data type, constraints (PK, FK, NOT NULL, UNIQUE), description
                4. Relationships — describe all relationships (OneToMany, ManyToOne, ManyToMany, OneToOne) with cardinality
                5. Database Indexes — any indexes detected
                6. SQL DDL (if applicable) — reconstruct CREATE TABLE statements based on entities found
                
                If no JPA entities or SQL is found, document the core domain models and their relationships instead.
                Use semantic HTML with tables, code blocks (<pre><code>), and proper headings.
                
                PROJECT NAME: %s
                
                === SOURCE CODE ===
                
                """.formatted(structure.projectName()));

        appendCodeContext(sb, structure, 120000);
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
