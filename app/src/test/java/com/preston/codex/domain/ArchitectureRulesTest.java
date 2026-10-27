package com.preston.codex.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * ARCH-01: Verifies that domain/ and importer/ have ZERO android.* and org.json.* imports.
 */
public class ArchitectureRulesTest {

    @Test
    public void ARCH01_domainAndImporter_haveNoAndroidOrOrgJsonImports() throws IOException {
        File projectDir = new File("src/main/java/com/preston/codex");
        if (!projectDir.exists()) {
            projectDir = new File("app/src/main/java/com/preston/codex");
        }
        assertTrue("Source directory exists: " + projectDir.getAbsolutePath(), projectDir.exists());

        File domainDir = new File(projectDir, "domain");
        File importerDir = new File(projectDir, "importer");

        List<Path> filesToCheck = new ArrayList<>();
        if (domainDir.exists()) {
            try (Stream<Path> stream = Files.walk(domainDir.toPath())) {
                stream.filter(p -> p.toString().endsWith(".java")).forEach(filesToCheck::add);
            }
        }
        if (importerDir.exists()) {
            try (Stream<Path> stream = Files.walk(importerDir.toPath())) {
                stream.filter(p -> p.toString().endsWith(".java")).forEach(filesToCheck::add);
            }
        }

        assertTrue("Found Java files to check in domain/importer", !filesToCheck.isEmpty());

        for (Path file : filesToCheck) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("import android.")) {
                    throw new AssertionError("Forbidden android.* import in " + file + " at line " + (i + 1) + ": " + line);
                }
                if (line.startsWith("import org.json.")) {
                    throw new AssertionError("Forbidden org.json.* import in " + file + " at line " + (i + 1) + ": " + line);
                }
            }
        }
    }

    @Test
    public void ARCH02_integration_hasNoDataEntityOrDaoImports() throws IOException {
        File projectDir = new File("src/main/java/com/preston/codex");
        if (!projectDir.exists()) {
            projectDir = new File("app/src/main/java/com/preston/codex");
        }
        File integrationDir = new File(projectDir, "integration");
        assertTrue("Integration dir exists: " + integrationDir.getAbsolutePath(), integrationDir.exists());

        List<Path> filesToCheck = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(integrationDir.toPath())) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(filesToCheck::add);
        }
        assertTrue("Found Java files to check in integration", !filesToCheck.isEmpty());

        for (Path file : filesToCheck) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("import com.preston.codex.data.entity.")) {
                    throw new AssertionError("Forbidden data.entity import in " + file + " at line " + (i + 1) + ": " + line);
                }
                if (line.startsWith("import com.preston.codex.data.dao.")) {
                    throw new AssertionError("Forbidden data.dao import in " + file + " at line " + (i + 1) + ": " + line);
                }
            }
        }
    }
}
