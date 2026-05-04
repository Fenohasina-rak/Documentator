package org.acme.Services.FileService;


import jakarta.enterprise.context.ApplicationScoped;
import org.acme.Models.File.ProjectFile;
import org.acme.Models.File.ProjectStructure;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@ApplicationScoped
public class FileScanner {
    public ProjectStructure scan(Path rootPath) throws IOException {
        List<ProjectFile> files = new ArrayList<>();
        Map<String, List<ProjectFile>> byDirectory = new LinkedHashMap<>();
        CustomFileVisitor customFileVisitor = new CustomFileVisitor(rootPath, files, byDirectory);
        Files.walkFileTree(rootPath, customFileVisitor);
        return new ProjectStructure(rootPath.getFileName().toString(), files, byDirectory);
    }
}
