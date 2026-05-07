package org.acme.Services.FileService;


import jakarta.enterprise.context.ApplicationScoped;
import org.acme.Models.File.ProjectFile;
import org.acme.Models.File.ProjectStructure;
import org.acme.Services.Gemini.GeminiService;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@ApplicationScoped
public class FileScanner {
    public static final String ARCHITECTURE_PROMPT = readFileProperty("src/main/resources/buildArchitecturePrompt.txt");
    public static final String API_PROMPT = readFileProperty("src/main/resources/buildApiSpecPrompt.txt");
    public static final String ERD_PROMPT = readFileProperty("src/main/resources/buildErdPrompt.txt");
    private static final Logger LOG = Logger.getLogger(FileScanner.class);
    public ProjectStructure scan(Path rootPath) throws IOException {
        List<ProjectFile> files = new ArrayList<>();
        Map<String, List<ProjectFile>> byDirectory = new LinkedHashMap<>();
        CustomFileVisitor customFileVisitor = new CustomFileVisitor(rootPath, files, byDirectory);
        Files.walkFileTree(rootPath, customFileVisitor);
        return new ProjectStructure(rootPath.getFileName().toString(), files, byDirectory);
    }

    public static String readFileProperty(String filePath) {
        try {
            Path path = Path.of(filePath);
            return  Files.readString(path);
        } catch (IOException e) {
            assert LOG != null;
            LOG.error(e.getMessage());
            return "";
        }
    }
}
