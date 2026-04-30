package org.acme.Models.File;

import java.util.List;
import java.util.Map;

public record ProjectStructure(
        String projectName,
        List<ProjectFile> files,
        Map<String, List<ProjectFile>> byDirectory
) {}
