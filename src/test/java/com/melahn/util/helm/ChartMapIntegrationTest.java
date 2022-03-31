package com.melahn.util.helm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.melahn.util.test.ChartMapGeneratorTestUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the shaded jar by running various test combinations of valid and
 * invalid parameters.
 */
class ChartMapIntegrationTest {

    private List<String> args = null;
    private final String className = "com.melahn.util.helm.ChartMapGenerator";
    private final Path JaCocoAgentPath = Paths.get("", "lib/org.jacoco.agent-0.8.7-runtime").toAbsolutePath();
    private final String JaCocoAgentString = JaCocoAgentPath.toString()
            .concat(".jar=destfile=../../jacoco.exec,append=true");
    private final ChartMapGeneratorTestUtil utility = new ChartMapGeneratorTestUtil();

    private static final String TARGET_TEST_INT_DIR_NAME = "target/integration-test";
    private static final Path TARGET_TEST_INT_DIR_PATH = Paths.get(TARGET_TEST_INT_DIR_NAME);
    private final static String TEST_FILE_FORMAT_MASK_ALL = "-jpt";
    private final static String TEST_FILE_FORMAT_MASK_JSON = "-j";
    private final static String TEST_FILE_FORMAT_MASK_PUML = "-p";
    private final static String TEST_FILE_FORMAT_MASK_TEXT = "-t";
    private static final String TEST_MAX_VERSIONS = "3";
    private static final String TEST_REPO_NAME = "melahn";
    private static final String TEST_CHART_NAME = "test-app-a-0.1.0";
    private static final String TEST_ENV_FILE_NAME = "../../../resource/example/example-env-spec.yaml";
    private static final String DIVIDER = "-------------------------------------";

    /**
     * Cleans the target test directory and creates one anew.
     * 
     */
    @BeforeAll
    static void setUp() {
            System.out.println(DIVIDER.concat(" INTEGRATION TESTS START ").concat(DIVIDER));
            try {
                    ChartMapGeneratorTestUtil.cleanDirectory(TARGET_TEST_INT_DIR_PATH);
                    Files.createDirectories(TARGET_TEST_INT_DIR_PATH);
            } catch (Exception e) {
                    fail("Test setup failed: " + e.getMessage());
            }
            System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @AfterAll
    static void cleanUp() {
        System.out.println(DIVIDER.concat(" INTEGRATION TESTS END ").concat(DIVIDER));
    }

    /**
     * Tests the no error, normal case in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void normalTest() throws InterruptedException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        Path p = Paths.get(TARGET_TEST_INT_DIR_NAME, m);
        Files.createDirectories(p);
        String d = p.toString();
        Path l = Paths.get(d, "sub-process-out.txt");
        Path i = Paths.get(d,"index.html");
        // delete the index file if it already exists
        if (Files.exists(i)) {
                Files.delete(i);
        }
        args = Arrays.asList("-r", TEST_REPO_NAME, "-f", TEST_FILE_FORMAT_MASK_ALL, "-n", TEST_MAX_VERSIONS, "-v");
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, p, l);
                assertTrue(true);
        assertTrue(Files.exists(Paths.get(d, TEST_CHART_NAME.concat(".json"))));
        assertTrue(Files.exists(Paths.get(d, TEST_CHART_NAME.concat(".png"))));
        assertTrue(Files.exists(Paths.get(d, TEST_CHART_NAME.concat(".puml"))));
        assertTrue(Files.exists(Paths.get(d, TEST_CHART_NAME.concat(".txt"))));
        System.out.println(m.concat(" completed"));
    }

    /**
     * Tests help in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void helpTest() throws InterruptedException, IOException {
            String m = new Throwable().getStackTrace()[0].getMethodName();
            Path p = Paths.get(TARGET_TEST_INT_DIR_NAME, m);
            Files.createDirectories(p);
            String d = p.toString();
            Path l = Paths.get(d, "sub-process-out.txt");
            args = Arrays.asList("-h");
            utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                            className, p, l);
            assertTrue(ChartMapGeneratorTestUtil.fileContains(l, "Usage:"));
            System.out.println(m.concat(" completed"));
    }
}