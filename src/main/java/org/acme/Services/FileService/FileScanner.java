package org.acme.Services.FileService;


import jakarta.enterprise.context.ApplicationScoped;
import org.acme.Models.File.ProjectFile;
import org.acme.Models.File.ProjectStructure;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@ApplicationScoped
public class FileScanner {

    private static final Logger LOG = Logger.getLogger(FileScanner.class);

    @ConfigProperty(name = "docgen.include.extensions")
    String includeExtensions;

    @ConfigProperty(name = "docgen.skip.directories")
    String skipDirectories;

    @ConfigProperty(name = "docgen.max.file.size", defaultValue = "512000")
    long maxFileSize;

    public ProjectStructure scan(Path rootPath) throws IOException {
        HashSet<String> extensions = new HashSet<>(Arrays.asList(includeExtensions.split(",")));
        HashSet<String> skipDirs = new HashSet<>(Arrays.asList(skipDirectories.split(",")));

        List<ProjectFile> files = new ArrayList<>();
        Map<String, List<ProjectFile>> byDirectory = new LinkedHashMap<>();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                if (skipDirs.contains(dirName) && !dir.equals(rootPath)) {
                    LOG.debugf("Skipping directory: %s", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();
                String ext = getExtension(fileName);

                if (!extensions.contains(ext)) {
                    return FileVisitResult.CONTINUE;
                }

                if (attrs.size() > maxFileSize) {
                    LOG.warnf("Skipping large file (%d bytes): %s", attrs.size(), file);
                    return FileVisitResult.CONTINUE;
                }

                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    String relativePath = rootPath.relativize(file).toString();
                    String directory = rootPath.relativize(file.getParent()).toString();

                    ProjectFile projectFile = new ProjectFile(
                            fileName,
                            relativePath,
                            directory.isEmpty() ? "root" : directory,
                            ext,
                            content,
                            attrs.size()
                    );

                    files.add(projectFile);
                    byDirectory.computeIfAbsent(projectFile.directory(), k -> new ArrayList<>()).add(projectFile);
                    LOG.debugf(" %s (%d bytes)", relativePath, attrs.size());

                } catch (IOException e) {
                    LOG.warnf("Could not read file: %s — %s", file, e.getMessage());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        LOG.infof("   Found %d files across %d directories", files.size(), byDirectory.size());
        return new ProjectStructure(rootPath.getFileName().toString(), files, byDirectory);
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot) : "";
    }
}
