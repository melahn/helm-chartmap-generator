package com.melahn.util.helm;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.melahn.util.test.ChartMapGeneratorTestUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the shaded jar by running various test combinations of valid and
 * invalid parameters.
 */
class ChartMapIntegrationTest {

    private static final String DIVIDER = "-------------------------------------";
    private static String HELM_BIN_MESSAGE = "The helm command \'%s\' will be used to discover helm path information.";
    private static final String INDEX_FILENAME = "target/index.html";
    private static final String TARGET_TEST_INT_DIR_NAME = "target/integration-test";
    private static final Path   TARGET_TEST_INT_DIR_PATH = Paths.get(TARGET_TEST_INT_DIR_NAME);
    private static final String TEST_CHART_NAME = "test-app-a-0.1.0";
    private static final String TEST_FILE_FORMAT_MASK_ALL = "-jpt";
    private static final String TEST_MAX_VERSIONS = "3";
    private static final String TEST_REPO_NAME = "melahn";

    private static Path indexFilePath = Paths.get(INDEX_FILENAME);

    private List<String> args = null;
    private final String className = "com.melahn.util.helm.ChartMapGenerator";
    private final Path jaCocoAgentPath = Paths.get("", "lib/org.jacoco.agent-0.8.7-runtime").toAbsolutePath();
    private final String jaCocoAgentString = jaCocoAgentPath.toString()
            .concat(".jar=destfile=../../jacoco.exec,append=true");
    private final ChartMapGeneratorTestUtil utility = new ChartMapGeneratorTestUtil();

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

    @BeforeEach
    void testPrep() throws IOException {
        /**
         * Delete the index file in case the test wants to regenerate it
         */
        Files.deleteIfExists(indexFilePath);
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
        Path i = Paths.get(d, "index.html");
        // delete the index file if it already exists
        if (Files.exists(i)) {
            Files.delete(i);
        }
        args = Arrays.asList("-r", TEST_REPO_NAME, "-f", TEST_FILE_FORMAT_MASK_ALL, "-n", TEST_MAX_VERSIONS,
                "-v");
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null,
                jaCocoAgentString,
                className, p, l);
        assertTrue(true);
        assertTrue(Files.exists(Paths.get(d, TEST_CHART_NAME.concat(".json"))));
        assertTrue(Files.exists(Paths.get(d, TEST_CHART_NAME.concat(".png"))));
        assertTrue(Files.exists(Paths.get(d, TEST_CHART_NAME.concat(".puml"))));
        assertTrue(Files.exists(Paths.get(d, TEST_CHART_NAME.concat(".txt"))));
        System.out.println(m.concat(" completed"));
    }

    /**
     * Tests the use of the HELM_BIN environment variable.  
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void setHelmBinTest() throws InterruptedException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        // The HELM_BIN env var is set to a fake value and that value 
        // is tested for in the output log.  Note that the generation itself 
        // does not work but that is not important for the test
        Path p = Paths.get(TARGET_TEST_INT_DIR_NAME, m.concat("-0"));
        String d = p.toString();
        Path l = Paths.get(d, "sub-process-out.txt");
        Path i = Paths.get(d, "index.html");
        // delete the index file if it already exists
        if (Files.exists(i)) {
            Files.delete(i);
        }
        Files.createDirectories(p);
        args = Arrays.asList("-r", "foo-repo");
        String h = "fake-helm-bin-test-resolved";
        utility.createProcess(args, new String[][] { new String[] {"HELM_BIN", h}, new String[] {} }, null,
                jaCocoAgentString,
                className, p, l);
        assertTrue(ChartMapGeneratorTestUtil.fileContains(l, String.format(HELM_BIN_MESSAGE,h)));
        // The HELM_BIN env var is removed from the environment and the log is
        // is inspected to be sure the correct default value is used.
        p = Paths.get(TARGET_TEST_INT_DIR_NAME, m.concat("-1"));
        d = p.toString();
        l = Paths.get(d, "sub-process-out.txt");
        i = Paths.get(d, "index.html");
        // delete the index file if it already exists
        if (Files.exists(i)) {
            Files.delete(i);
        }
        Files.createDirectories(p);
        args = Arrays.asList("-r", ChartMapGeneratorTest.TEST_REPO_NAME,"-n", "1");
        h = "helm";
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {"HELM_BIN", h} }, null,
                jaCocoAgentString,
                className, p, l);
        assertTrue(ChartMapGeneratorTestUtil.fileContains(l, String.format(HELM_BIN_MESSAGE,h)));
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
        // Test the -h parm 
        Path p = Paths.get(TARGET_TEST_INT_DIR_NAME, m.concat("-0"));
        Files.createDirectories(p);
        String d = p.toString();
        Path l = Paths.get(d, "sub-process-out.txt");
        args = Arrays.asList("-h");
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null,
                jaCocoAgentString,
                className, p, l);
        assertTrue(ChartMapGeneratorTestUtil.fileContains(l, "Usage:"));
        System.out.println("Help in the shaded jar is shown with the -h parameter");
        // Test no parms 
        p = Paths.get(TARGET_TEST_INT_DIR_NAME, m.concat("-1"));
        Files.createDirectories(p);
        d = p.toString();
        l = Paths.get(d, "sub-process-out.txt");
        utility.createProcess(new ArrayList<String>(), new String[][] { new String[] {}, new String[] {} }, null,
                jaCocoAgentString,
                className, p, l);
        assertTrue(ChartMapGeneratorTestUtil.fileContains(l, "Usage:"));
        System.out.println("Help in the shaded jar is shown when no parameters are used");
        System.out.println(m.concat(" completed"));
    }
}