package io.oxalate.backend.api;

import io.oxalate.backend.api.RestEndpointInventory.Endpoint;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ratchet for "the REST contract tests must cover 100% of existing REST endpoints, with the success status and every
 * declared error status".
 * <p>
 * The repository is a long way from that today: 164 endpoints declare 404 response codes between them, and the
 * {@code *RTC} suites reference roughly 31 paths. A hard gate would fail every build, so this test instead pins an
 * explicit per-endpoint inventory in {@value #BASELINE_RESOURCE} and enforces three properties:
 * <ol>
 *     <li><b>Completeness.</b> Every declared endpoint appears in the baseline. A new endpoint fails the build until
 *         it is classified, so no endpoint can be added unnoticed.</li>
 *     <li><b>Monotonicity.</b> The number of {@code UNTESTED} entries never grows.</li>
 *     <li><b>Non-triviality.</b> An endpoint marked {@code TESTED} must have its path referenced by at least one
 *         {@code *RTC} test source, so the marker cannot simply be asserted.</li>
 * </ol>
 * <p>
 * The non-triviality check is a source scan, which is an approximation: it proves a test mentions the endpoint, not
 * that the test asserts every declared status. The exact version records {@code (endpoint, status)} pairs from MockMvc
 * at runtime and compares them against {@code Endpoint#declaredStatusCodes()}; that is the intended end state and is
 * written up as ../TODO-20260916.md item 4. Until then, promoting an entry to {@code TESTED} is a review-visible claim
 * backed by a weak automatic check rather than a strong one.
 * <p>
 * To regenerate the baseline after a deliberate change, delete the file and run this test once: it writes a fresh
 * baseline and fails with instructions, so the new content lands in the diff for review.
 */
@DisplayName("REST contract coverage: every endpoint is classified and the untested count only falls")
class RestEndpointTestCoverageUTC {

    private static final String BASELINE_RESOURCE = "rest-endpoint-test-coverage-baseline.txt";
    private static final Path BASELINE_PATH = Path.of("src", "test", "resources", BASELINE_RESOURCE);
    private static final Path TEST_SOURCE_ROOT = Path.of("src", "test", "java");

    private static final String TESTED = "TESTED";
    private static final String UNTESTED = "UNTESTED";

    /**
     * Maximum number of endpoints that may lack a status-code test. Lower it whenever you add coverage; never raise
     * it. The target is 0.
     */
    private static final int MAX_UNTESTED_ENDPOINTS = 129;

    @Test
    void everyEndpointIsClassifiedInTheBaselineOk() {
        var baseline = loadBaselineOrGenerate();
        var missing = new TreeSet<String>();

        for (var endpoint : RestEndpointInventory.endpoints()) {
            if (!baseline.containsKey(endpoint.id())) {
                missing.add(endpoint.describe());
            }
        }

        assertTrue(missing.isEmpty(), () -> """
                These endpoints are not classified in %s:
                %s
                Add a line per endpoint. Prefer writing the status-code test and marking it %s; if you genuinely
                cannot, mark it %s and raise MAX_UNTESTED_ENDPOINTS -- which is a regression a reviewer should
                question.
                """.formatted(BASELINE_RESOURCE, join(missing), TESTED, UNTESTED));
    }

    @Test
    void theBaselineHasNoEntriesForEndpointsThatNoLongerExistOk() {
        var baseline = loadBaselineOrGenerate();
        var knownIds = RestEndpointInventory.endpoints()
                                            .stream()
                                            .map(Endpoint::id)
                                            .collect(Collectors.toSet());

        var stale = new TreeSet<String>();
        for (var id : baseline.keySet()) {
            if (!knownIds.contains(id)) {
                stale.add(id);
            }
        }

        assertTrue(stale.isEmpty(), () -> """
                %s lists endpoints that no longer exist. Remove these lines:
                %s
                """.formatted(BASELINE_RESOURCE, join(stale)));
    }

    @Test
    void theUntestedEndpointCountDoesNotGrowOk() {
        var baseline = loadBaselineOrGenerate();
        var untested = baseline.values()
                               .stream()
                               .filter(UNTESTED::equals)
                               .count();

        assertTrue(untested <= MAX_UNTESTED_ENDPOINTS, () -> """
                %d of %d endpoints have no status-code test, which is above the ratchet of %d.

                The ratchet only moves one way. Add the missing tests rather than raising
                MAX_UNTESTED_ENDPOINTS.
                """.formatted(untested, baseline.size(), MAX_UNTESTED_ENDPOINTS));

        if (untested < MAX_UNTESTED_ENDPOINTS) {
            System.out.printf(
                    "REST endpoint status-code coverage improved: %d untested, ratchet is %d. Lower MAX_UNTESTED_ENDPOINTS to %d.%n",
                    untested, MAX_UNTESTED_ENDPOINTS, untested);
        }
    }

    /**
     * Stops {@code TESTED} from becoming a label anyone can apply. It is a weak check by design; see the class
     * comment.
     */
    @Test
    void endpointsMarkedTestedAreReferencedByARestTestOk() {
        var baseline = loadBaselineOrGenerate();
        var testSources = readRestTestSources();
        var unsupported = new TreeSet<String>();

        for (var endpoint : RestEndpointInventory.endpoints()) {
            if (!TESTED.equals(baseline.get(endpoint.id()))) {
                continue;
            }
            if (!isReferencedByATest(endpoint, testSources)) {
                unsupported.add(endpoint.describe());
            }
        }

        assertTrue(unsupported.isEmpty(), () -> """
                These endpoints are marked %s in %s, but no *RTC test source mentions their path:
                %s
                Either write the test or change the marker back to %s.
                """.formatted(TESTED, BASELINE_RESOURCE, join(unsupported), UNTESTED));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Baseline handling
    // -------------------------------------------------------------------------------------------------------------

    private static Map<String, String> loadBaselineOrGenerate() {
        if (!Files.exists(BASELINE_PATH)) {
            generateBaseline();
            return fail("No baseline existed, so one was generated at " + BASELINE_PATH
                    + ". Review it and commit it, then re-run.");
        }

        var baseline = new LinkedHashMap<String, String>();

        try {
            for (var rawLine : Files.readAllLines(BASELINE_PATH, StandardCharsets.UTF_8)) {
                var line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                var parts = line.split("\\s+", 2);
                if (parts.length < 2) {
                    fail("Malformed line in " + BASELINE_RESOURCE + ": '" + rawLine + "'");
                }

                var status = parts[0];
                if (!TESTED.equals(status) && !UNTESTED.equals(status)) {
                    fail("Unknown marker '" + status + "' in " + BASELINE_RESOURCE + ", expected " + TESTED + " or " + UNTESTED);
                }

                baseline.put(parts[1].trim(), status);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + BASELINE_PATH, e);
        }

        return baseline;
    }

    private static void generateBaseline() {
        var testSources = readRestTestSources();
        var lines = new ArrayList<String>();

        lines.add("# REST endpoint status-code test coverage baseline.");
        lines.add("#");
        lines.add("# One line per declared REST endpoint: <TESTED|UNTESTED> <Interface.method>");
        lines.add("#");
        lines.add("# TESTED   an *RTC test exercises this endpoint and asserts its statuses.");
        lines.add("# UNTESTED no status-code test exists. The target is zero of these; see ../TODO-20260916.md item 4.");
        lines.add("#");
        lines.add("# Managed by RestEndpointTestCoverageUTC. Delete this file and run that test to regenerate it.");
        lines.add("");

        for (var endpoint : RestEndpointInventory.endpoints()) {
            var status = isReferencedByATest(endpoint, testSources) ? TESTED : UNTESTED;
            lines.add(status + " " + endpoint.id());
        }

        try {
            Files.createDirectories(BASELINE_PATH.getParent());
            Files.write(BASELINE_PATH, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + BASELINE_PATH, e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Test source scanning
    // -------------------------------------------------------------------------------------------------------------

    private static List<String> readRestTestSources() {
        if (!Files.isDirectory(TEST_SOURCE_ROOT)) {
            throw new IllegalStateException("Expected test sources at " + TEST_SOURCE_ROOT.toAbsolutePath()
                    + "; this test must run with the service module as its working directory");
        }

        try (Stream<Path> paths = Files.walk(TEST_SOURCE_ROOT)) {
            return paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName()
                                            .toString()
                                            .endsWith("RTC.java"))
                        .map(RestEndpointTestCoverageUTC::readFile)
                        .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not scan " + TEST_SOURCE_ROOT, e);
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }

    /**
     * True when some REST test source mentions this endpoint's path. Both the templated path and its literal prefix are
     * accepted, because tests address path variables either way: {@code get("/api/dive-groups/{id}", id)} and
     * {@code get("/api/memberships/" + id)} are both legitimate.
     */
    private static boolean isReferencedByATest(Endpoint endpoint, List<String> testSources) {
        // A LinkedHashSet, not Set.of: for a path with no variables the prefix equals the path itself.
        var candidates = new LinkedHashSet<>(List.of(endpoint.path(), endpoint.staticPathPrefix()));

        for (var source : testSources) {
            for (var candidate : candidates) {
                if (!candidate.isBlank() && source.contains("\"" + candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String join(Collection<String> lines) {
        return lines.stream()
                    .map(line -> "  - " + line)
                    .collect(Collectors.joining("\n"));
    }
}
