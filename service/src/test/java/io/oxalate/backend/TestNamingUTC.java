package io.oxalate.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TestNamingUTC {

    @Test
    void everyTestSourceUsesSurefireNamingConventionOk() throws IOException {
        var offenders = Stream.of(Path.of("src/test/java"), Path.of("../api/src/test/java"))
                              .filter(Files::isDirectory)
                              .flatMap(this::javaFiles)
                              .filter(path -> containsTestAnnotation(path))
                              .filter(path -> !path.getFileName()
                                                   .toString()
                                                   .matches(".*(?:UTC|ITC|RTC|TC)\\.java"))
                              .toList();

        assertTrue(offenders.isEmpty(), () -> "Test sources containing @Test must match Surefire includes: " + offenders);
    }

    private Stream<Path> javaFiles(Path root) {
        try {
            return Files.walk(root)
                        .filter(path -> path.toString()
                                            .endsWith(".java"));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect test sources", exception);
        }
    }

    private boolean containsTestAnnotation(Path path) {
        try {
            return Files.readString(path)
                        .contains("@Test");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read test source " + path, exception);
        }
    }
}
