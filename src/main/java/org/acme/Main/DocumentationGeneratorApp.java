package org.acme.Main;


import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.acme.Services.Gemini.DocumentationGeneratorService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@QuarkusMain
public class DocumentationGeneratorApp implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(DocumentationGeneratorApp.class);

    @Inject
    DocumentationGeneratorService generatorService;

    @ConfigProperty(name = "docgen.source.path")
    String sourcePath;

    @ConfigProperty(name = "docgen.output.filename", defaultValue = "documentation.html")
    String outputFilename;

    @Override
    public int run(String... args) throws Exception {
        // Allow overriding source path via CLI argument
        String targetPath = (args.length > 0) ? args[0] : sourcePath;

        LOG.infof("Target path: %s", targetPath);

        Path path = Paths.get(targetPath);
        if (!Files.exists(path)) {
            LOG.errorf("Path does not exist: %s", targetPath);
            return 1;
        }
        if (!Files.isDirectory(path)) {
            LOG.errorf("Path is not a directory: %s", targetPath);
            return 1;
        }

        try {
            Path outputPath = path.resolve(outputFilename);

            generatorService.generateDocumentation(path, outputPath);

            LOG.infof(" Output: %s", outputPath.toAbsolutePath());
            return 0;
        } catch (Exception e) {
            LOG.error("Stack trace:", e);
            return 1;
        }
    }
}
