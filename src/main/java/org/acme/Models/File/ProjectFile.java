package org.acme.Models.File;


public record ProjectFile(
        String fileName,
        String relativePath,
        String directory,
        String extension,
        String content,
        long sizeBytes
) {}
