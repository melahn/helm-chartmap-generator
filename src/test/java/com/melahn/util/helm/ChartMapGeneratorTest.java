package com.melahn.util.helm;

import static com.melahn.util.test.ChartMapGeneratorTestUtil.cleanDirectory;
import static com.melahn.util.test.ChartMapGeneratorTestUtil.createTestMapGenerator;
import static com.melahn.util.test.ChartMapGeneratorTestUtil.isWindows;
import static com.melahn.util.test.ChartMapGeneratorTestUtil.streamContains;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

class ChartMapGeneratorTest {

    private static final String CSS_FILENAME = "style.css";
    private static final String DIVIDER = "-------------------------------------";
    private static final String FORMAT_JSON = "j";
    private static final String FORMAT_PUML = "p";
    private static final String FORMAT_TEXT = "t";
    private static final String INDEX_FILENAME = "index.html";
    private static final int PRINT_ONE_VERSION = 1;
    private static final int PRINT_ALL_VERSIONS = 99;
    private static final String TARGET_TEST_DIR_NAME = "target/test";
    private static final String TEST_REPO_NAME = "melahn";
    private static final String TEST_ENV_SPEC = "./resource/example/example-env-spec.yaml";
    private static final String TEST_ENV_SPEC_NOT_EXIST = "./resource/example/no-spec-here.yaml";
    private static final boolean VERBOSE_FALSE = false;
    private static final boolean VERBOSE_TRUE = true;

    private static String targetTest = "target/test";
    private static String targetTestDirectory = Paths.get(targetTest).toString();

    private final PrintStream initialOut = System.out;

    private String testDirectoryName = null;
    private int testVariation = 0; // used to generate unique subdirectory names

     /**
     * Performs setup before the tests are run.
     */
    @BeforeAll
    static void setUp() {
        System.out.println(DIVIDER.concat(" UNIT TESTS START ").concat(DIVIDER));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Performs cleanup after the tests are run.
     */
    @AfterAll
    static void cleanUp() {
        /**
         * No cleanup to do after test. I don't delete the generated files because they
         * might be handy to have around to diagnose issues in test failures. They are
         * deleted anyway when the test is next run.
         */
        System.out.println(DIVIDER.concat(" UNIT TESTS END ").concat(DIVIDER));
    }

    /**
     * Test variations with the repo name parameter.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void RepoNameParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        // No repo name was provided. 
          assertThrows(ChartMapGeneratorException.class, () -> createTestMapGenerator(null, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC_NOT_EXIST, VERBOSE_FALSE));
        System.out.println("A ChartMapGeneratorException was thrown as expected since no repo name was provided");
        // A bogus repo name was provided.
        testDirectoryName = createTestDir(m, getTestVariation());
        String bogus = String.valueOf(System.currentTimeMillis());
        ChartMapGenerator cmg = createTestMapGenerator(bogus, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
        assertThrows(ChartMapGeneratorException.class, () -> cmg.generate());
        System.out.println(String.format("A ChartMapGeneratorException was thrown as expected since a bogus repo name \'%s\' was provided", bogus));
        System.out.println(m.concat(" completed"));
    }

    /**
     * Test variations with the environment spec file.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void EnvSpecParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        // An environment spec that does not exist.  Note that you can't reuse cmg1 because it must be final or effectively so for jupiter assertThrow
        ChartMapGenerator cmg1 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC_NOT_EXIST, VERBOSE_FALSE);
        assertThrows(ChartMapGeneratorException.class, () -> cmg1.generate());
        System.out.println("A ChartMapGeneratorException was thrown as expected since the env file does not exist");
        // An environment spec that does exist, print one version of each chart
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg2 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg2.generate();
        // validate that one and only one (the most recent) version of each of the charts is printed in text format
        assertTrue(Files.exists(Paths.get(testDirectoryName, "test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName, "test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName, "test-app-c-0.3.0.txt")));
        assertFalse(Files.exists(Paths.get(testDirectoryName, "test-app-b-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName, "test-app-c-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName, "test-app-c-0.2.0.txt"))); 
        System.out.println("One version of each chart was successfully printed in text format");
        System.out.println(m.concat(" completed"));
    }

       /**
     * Test variations with the output directory parameter.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void OutputDirParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        // No output parameter is provided so expect the files are generated in the target directory.
        ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, null, FORMAT_TEXT, PRINT_ONE_VERSION, null, VERBOSE_FALSE);
        Files.deleteIfExists(Paths.get(".", CSS_FILENAME));
        Files.deleteIfExists(Paths.get(".", INDEX_FILENAME));
        cmg.generate();
        // these are generated in the project root
        assertTrue(Files.exists(Paths.get(".", "test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(".", "test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(".", "test-app-c-0.3.0.txt")));
        assertTrue(Files.exists(Paths.get(".", CSS_FILENAME)));
        assertTrue(Files.exists(Paths.get(".", INDEX_FILENAME)));
        // don't clutter up the project root
        Files.deleteIfExists(Paths.get(".", "test-app-a-0.1.0.txt"));
        Files.deleteIfExists(Paths.get(".", "test-app-b-0.2.0.txt"));
        Files.deleteIfExists(Paths.get(".", "test-app-c-0.3.0.txt"));
        Files.deleteIfExists(Paths.get(".", CSS_FILENAME));
        Files.deleteIfExists(Paths.get(".", INDEX_FILENAME));
        System.out.println("No output directory was provided so the pwd was used");
        System.out.println(m.concat(" completed"));
    }

    /**
     * Test variations of the number of versions.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void versionsParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        // all versions of each of the charts are printed in text format
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.txt"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.txt")));
        System.out.println("All versions of each chart were successfully printed in text format");
        //one and only one (the most recent) version of each of the charts is printed in text format
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg .generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.txt")));
        assertFalse(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.txt"))); 
        System.out.println("One version of each chart was successfully printed in text format");
        System.out.println(m.concat(" completed"));
    }

 /**
     * Test variations using the verbose parameter.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void verboseParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        // all versions of each of the charts are printed in text format
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_TRUE);
            cmg.generate();
            assertTrue(streamContains(o, "Number of charts found in local helm repo"));
            System.setOut(initialOut);
            System.out.println("Verbose string found as expected");
        }
        System.out.println("All versions of each chart were successfully printed in puml and png format");
        System.out.println(m.concat(" completed"));
    }

    /**
     * Test variations on the format parameter.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     * 
     */
    @Test
    void formatParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        // all versions of each of the charts are printed in text format
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_TRUE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.txt"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.txt")));
        System.out.println("All versions of each chart were successfully printed in text format");
        // all versions of each of the charts are printed in json format 
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_JSON, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.json"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.json")));
        System.out.println("All versions of each chart were successfully printed in json format");
        // one version of each of the charts are printed in puml format with an accompanying image file
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_PUML, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.puml")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.puml"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.puml")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.png")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.png"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.png")));
        System.out.println("One version of each chart were successfully printed in puml and png format");
        // null format specifier
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), null, 1, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        System.out.println("null file format mask was handled correctly");
        // empty string format specifier
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), null, 1, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        System.out.println("empty file format mask was handled correctly");
        // mixed case format specifier
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), "tJp", 1, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.puml")));
        System.out.println("mixed case file format mask was handled correctly");
        System.out.println(m.concat(" completed"));
    }

    /**
     * Tests the ChartMap.checkHelmVersion method.
     * 
     * @throws ChartMapGeneratorException
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void checkHelmVersionTest() throws ChartMapGeneratorException, InterruptedException, IOException {
        // Test a bad helm command
        ChartMapGenerator cmg1 = createTestMapGenerator(TEST_REPO_NAME, targetTestDirectory, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg1 = spy(cmg1);
        // Use a command that is the same across all the OS's so it will run
        Process p1 = Runtime.getRuntime().exec(new String[] { "echo", "I am the foo process" });
        Process sp1 = spy(p1);
        doReturn(sp1).when(scmg1).getProcess(any(), eq(null));
        doReturn("helm").when(scmg1).getHelmCommand();
        // Return 1 to mimic a bad helm command forcing a ChartMapException
        doReturn(1).when(sp1).exitValue();
        assertThrows(ChartMapGeneratorException.class, () -> scmg1.checkHelmVersion());
        System.out.println("ChartMapGeneratorException thrown as expected");
        // Test not helm version 3
        ChartMapGenerator cmg2 = createTestMapGenerator(TEST_REPO_NAME, targetTestDirectory, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg2 = spy(cmg2);
        // Use a command that is the same across all the OS's to mimic a helm not v3
        Process p2 = Runtime.getRuntime().exec(new String[] { "echo", "I am not helm version 3" });
        doReturn(p2).when(scmg2).getProcess(any(), eq(null));
        assertThrows(ChartMapGeneratorException.class, () -> scmg2.checkHelmVersion());
        System.out.println("ChartMapGeneratorException thrown as expected");
        // Use a command that will cause the process' BufferedReader to return null and
        // force the ChartMapException.
        ChartMapGenerator cmg3 = createTestMapGenerator(TEST_REPO_NAME, targetTestDirectory, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg3 = spy(cmg3);
        String nullCommand = isWindows() ? "type" : "cat";
        String nullArgument = isWindows() ? "NUL" : "/dev/null";
        Process p3 = Runtime.getRuntime().exec(new String[] { nullCommand, nullArgument });
        doReturn(p3).when(scmg3).getProcess(any(), eq(null));
        assertThrows(ChartMapGeneratorException.class, () -> scmg3.checkHelmVersion());
        System.out.println("ChartMapGeneratorException thrown as expected");
        // Use a command that will cause the process' BufferedReader to just one
        // character and force the ChartMapException
        ChartMapGenerator cmg4 = createTestMapGenerator(TEST_REPO_NAME, targetTestDirectory, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg4 = spy(cmg4);
        Process p4 = Runtime.getRuntime().exec(new String[] { "echo", "1" });
        doReturn(p4).when(scmg4).getProcess(any(), eq(null));
        assertThrows(ChartMapGeneratorException.class, () -> scmg4.checkHelmVersion());
        System.out.println("ChartMapGeneratorException thrown as expected");
        // Cause an IOException -> ChartMapException on getProcess()
        ChartMapGenerator cmg5 = createTestMapGenerator(TEST_REPO_NAME, targetTestDirectory, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg5 = spy(cmg5);
        doThrow(IOException.class).when(scmg5).getProcess(any(), eq(null));
        assertThrows(ChartMapGeneratorException.class, () -> scmg5.checkHelmVersion());
        System.out.println("IOException -> ChartMapGeneratorException thrown as expected");
        // Cause an InterruptedException -> ChartMapException on waitFor()
        // Be careful to put InterruptedException case last in the test case since the
        // thread is not usable after that
        ChartMapGenerator cmg6 = createTestMapGenerator(TEST_REPO_NAME, targetTestDirectory, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg6 = spy(cmg6);
        Process p6 = Runtime.getRuntime()
                .exec(new String[] { "echo", "I am going to throw an InterruptedException!!" });
        Process sp6 = spy(p6);
        doReturn(sp6).when(scmg6).getProcess(any(), eq(null));
        doThrow(InterruptedException.class).when(sp6).waitFor(ChartMapGenerator.PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
        assertThrows(ChartMapGeneratorException.class, () -> scmg6.checkHelmVersion());
        System.out.println("InterruptedException -> ChartMapGeneratorException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the Help.
     * 
     * @throws IOException
     */
    @Test
    void helpTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        // help text is correct
        String helpTextExpected = "\nUsage:\n\n"
                .concat("java -jar helm-chartmap-generator-1.1.0-SNAPSHOT.jar\n")
                .concat("\nFlags:\n")
                .concat("\t-r\t<repo name>\t\tthe name of the local helm repo to use (required)\n")
                .concat("\t-o\t<directory name>\tthe output directory to use (default <pwd>) (optional)\n")
                .concat("\t-f\t<file format mask>\ta string containing any combination of 'j','p' and 't' (default 't') (optional)\n")
                .concat("\t-n\t<integer>\t\tthe maximum number of chart versions to print (default 1) (optional)\n")
                .concat("\t-e\t<file name>\t\tenvironment file (optional)\n")
                .concat("\t-v\tverbose\t\t\tprint verbose output (optional)\n")
                .concat("\t-h\thelp\t\t\tprovide help (optional)\n")
                .concat("\nSee https://github.com/melahn/helm-chartmap-generator for more information\n");
        String helpText = ChartMapGenerator.getHelp();
        assertEquals(helpText, helpTextExpected);
        System.out.println("Help text is what is expected");
        // no args
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMapGenerator.main(new String[0]);
            assertTrue(streamContains(o, "Usage:"));
            System.setOut(initialOut);
            System.out.println("Help was returned as expected when no parameters were passed");
        }
        System.out.println(m.concat(" completed"));
    }

    /**
     * Helper function to clean a subdirectory if it exists within
     * the target/test directory. then creates the directory for a test 
     * case to run in.
     * 
     * @param s the name of the subdirectory 
     * @param i a string that makes this subdirectory unique 
     */
    private String createTestDir(String s, String i) throws IOException {
        Path p = Paths.get(TARGET_TEST_DIR_NAME, s.concat(i));
        cleanDirectory(p);
        Files.createDirectories(p);
        return p.toString();
    }

    /**
     * Helper function to get a string to use for a subdirectory for storing test artifacts
     * separate from other test variations.
     * 
     * @return the name of the directory to use based on incrementing the testVariation number.
     */
    private String getTestVariation() {
        return String.format("-%d", ++testVariation);
    }
}
