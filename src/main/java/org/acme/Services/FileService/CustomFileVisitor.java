package org.acme.Services.FileService;

import org.acme.Models.File.ProjectFile;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class CustomFileVisitor implements  FileVisitor{
    private Path rootPath;
    private List<ProjectFile> files;
    private Map<String, List<ProjectFile>> byDirectory ;
    private static final Logger LOG = Logger.getLogger(FileScanner.class);

    private final String includeExtensions = ConfigProvider.getConfig().getValue("docgen.include.extensions", String.class);

    private final String skipDirectories = ConfigProvider.getConfig().getValue("docgen.skip.directories", String.class);

    private final long maxFileSize = ConfigProvider.getConfig().getValue("docgen.max.file.size", long.class);

    public CustomFileVisitor(Path rootPath, List<ProjectFile> files, Map<String, List<ProjectFile>> byDirectory) {
        this.rootPath = rootPath;
        this.byDirectory = byDirectory;
        this.files = files;
    }



    @Override
    public FileVisitResult preVisitDirectory(Object dir, BasicFileAttributes attrs) throws IOException {
        Path dirPath = (Path) dir;
        HashSet<String> skipDirs = new HashSet<>(Arrays.asList(skipDirectories.split(",")));
        String dirName = dirPath.getFileName() != null ? dirPath.getFileName().toString() : "";
        if (skipDirs.contains(dirName) && !dir.equals(rootPath)) {
            LOG.debugf("Skipping directory: %s", dir);
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }





    @Override
    public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) {
        Path filePath = (Path) file;
        HashSet<String> extensions = new HashSet<>(Arrays.asList(includeExtensions.split(",")));
        String fileName = filePath.getFileName().toString();
        String ext = getExtension(fileName);

        if (!extensions.contains(ext)) {
            return FileVisitResult.CONTINUE;
        }

        if (attrs.size() > maxFileSize) {
            LOG.warnf("Skipping large file (%d bytes): %s", attrs.size(), file);
            return FileVisitResult.CONTINUE;
        }

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            String relativePath = rootPath.relativize(filePath).toString();
            String directory = rootPath.relativize(filePath.getParent()).toString();

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

    @Override
    public FileVisitResult visitFileFailed(Object file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Object dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot) : "";
    }
}
