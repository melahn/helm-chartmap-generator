package com.melahn.util.helm;

import static com.melahn.util.test.ChartMapGeneratorTestUtil.cleanDirectory;
import static com.melahn.util.test.ChartMapGeneratorTestUtil.createTestMapGenerator;
import static com.melahn.util.test.ChartMapGeneratorTestUtil.fileContains;
import static com.melahn.util.test.ChartMapGeneratorTestUtil.isWindows;
import static com.melahn.util.test.ChartMapGeneratorTestUtil.streamContains;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private static final String BAD_CHART_COUNT_STRING = "Bad chart count = %d as expected";
    private static final String DIVIDER = "-------------------------------------";
    private static final String FORMAT_ALL = "jpt";
    private static final String FORMAT_JSON = "j";
    private static final String FORMAT_PUML = "p";
    private static final String FORMAT_TEXT = "t";
    private static final String INDEX_FILENAME = "index.html";
    private static final int NUM_BAD_CHART_VERSIONS = 1;
    private static final int NUM_GOOD_CHART_VERSIONS = 8;
    private static final int NUM_ALL_CHART_VERSIONS = 9;
    private static final int PRINT_ONE_VERSION = 1;
    private static final int PRINT_ALL_VERSIONS = 99;
    private static final String TARGET_TEST_DIR_NAME = "target/test";
    protected static final String TEST_REPO_NAME = "melahn";
    private static final String TEST_ENV_SPEC = "./resource/example/example-env-spec.yaml";
    private static final String TEST_ENV_SPEC_NOT_EXIST = "./resource/example/no-spec-here.yaml";
    private static final boolean VERBOSE_FALSE = false;
    private static final boolean VERBOSE_TRUE = true;

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
     * Tests bad arguments.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
    */
    @Test
    void badArgsTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        // undefined argument
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMapGenerator.main(new String[] {"-x"});
            assertTrue(streamContains(o, "ChartMapGeneratorException: Unrecognized option: -x "));
            System.setOut(initialOut);
            System.out.println("Unrecognized option message found as expected");
        }
        System.out.println(m.concat(" completed"));
    }

    /**
     * Test variations with the repo name parameter.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void repoNameParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        // No repo name was provided. 
          assertThrows(ChartMapGeneratorException.class, () -> createTestMapGenerator(null, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC_NOT_EXIST, VERBOSE_FALSE));
        System.out.println("A ChartMapGeneratorException was thrown as expected since no repo name was provided");
        // A bogus repo name was provided.
        testDirectoryName = createTestDir(m, getTestVariation());
        String bogus = String.valueOf(System.currentTimeMillis());
        ChartMapGenerator cmg = createTestMapGenerator(bogus, testDirectoryName, FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
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
    void envSpecParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        // An environment spec that does exist, print one version of each chart
        ChartMapGenerator cmg2 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg2.generate();
        // validate that one and only one (the most recent) version of each of the charts is printed in text format
        assertTrue(Files.exists(Paths.get(testDirectoryName, "test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName, "test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName, "test-app-c-0.3.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName, "test-app-d-0.2.0.txt")));
        assertFalse(Files.exists(Paths.get(testDirectoryName, "test-app-b-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName, "test-app-c-0.2.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName, "test-app-c-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName, "test-app-d-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName, "test-app-g-0.1.0.txt"))); 
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
    void outputDirParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        // No output parameter is provided so expect the files are generated in the target directory.
        ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, null, FORMAT_TEXT, PRINT_ONE_VERSION, null, VERBOSE_FALSE);
        Files.deleteIfExists(Paths.get(".", CSS_FILENAME));
        Files.deleteIfExists(Paths.get(".", INDEX_FILENAME));
        cmg.generate();
        // these are generated in the project root since an output dir was not specified
        assertTrue(Files.exists(Paths.get(".", "test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(".", "test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(".", "test-app-c-0.3.0.txt")));
        assertTrue(Files.exists(Paths.get(".", "test-app-d-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(".", CSS_FILENAME)));
        assertTrue(Files.exists(Paths.get(".", INDEX_FILENAME)));
        // don't clutter up the project root
        Files.deleteIfExists(Paths.get(".", "test-app-a-0.1.0.txt"));
        Files.deleteIfExists(Paths.get(".", "test-app-b-0.2.0.txt"));
        Files.deleteIfExists(Paths.get(".", "test-app-c-0.3.0.txt"));
        Files.deleteIfExists(Paths.get(".", "test-app-d-0.2.0.txt"));
        Files.deleteIfExists(Paths.get(".", CSS_FILENAME));
        Files.deleteIfExists(Paths.get(".", INDEX_FILENAME));
        System.out.println("No output directory was provided so the pwd was used");
        System.out.println(m.concat(" completed"));
    }

    /**
     * Test the printChart methods.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void printChartTest() throws ChartMapException, ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        // test printChart(HelmChart) ChartMapGeneratorException
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg1 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg1.generate();
        assertEquals(NUM_BAD_CHART_VERSIONS, cmg1.getChartCountBad());
        assertEquals(NUM_GOOD_CHART_VERSIONS, cmg1.getChartCountGood());
        assertEquals(NUM_ALL_CHART_VERSIONS, cmg1.getChartCount());
        System.out.println(String.format(BAD_CHART_COUNT_STRING, NUM_BAD_CHART_VERSIONS));
        // test printChart(HelmChart, String) ChartMapGeneratorException
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg2 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg2.generate();
        assertEquals(1, cmg2.getChartCountBad());
        assertEquals(4, cmg2.getChartCountGood());
        assertEquals(5, cmg2.getChartCount());
        System.out.println(String.format(BAD_CHART_COUNT_STRING, 1));
        System.out.println(m.concat(" completed"));
    }

    /**
     * Tests the getDetails method.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void getDetailsTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        // test getDetails with a bad chart
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg1 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_TRUE);
        cmg1.generate();
        assertTrue(fileContains(Paths.get(testDirectoryName, INDEX_FILENAME), "Here is a list of the chart versions with errors"));
        System.out.println("A list of chart versions with errors is found, as expected");
        // test getDetails without a bad chart
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg2 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_TRUE);
        ChartMapGenerator scmg2 = spy(cmg2);
        doReturn(0).when(scmg2).getChartCountBad();
        scmg2.generate();
        assertFalse(fileContains(Paths.get(testDirectoryName, INDEX_FILENAME), "Here is a list of the chart versions with errors"));
        System.out.println(m.concat(" completed"));
    }

    /**
     * Test the ability of the generator to automatically retry using the refresh option.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void refreshRetryTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_ALL, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            cmg.generate();
            assertTrue(streamContains(o, "Printed chart: test-app-d:0.1.0 with the refresh option"));
            assertTrue(streamContains(o, "Printed chart: test-app-d:0.2.0 with the refresh option"));
            System.setOut(initialOut);
            System.out.println("The ability for ChartMapGenerator to retry using the refresh option was successfully tested");
        }
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
        ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.txt"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-d-0.1.0.txt")));
        System.out.println("All versions of each good chart were successfully printed in text format");
        //one and only one (the most recent) version of each of the charts is printed in text format
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg .generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-d-0.2.0.txt")));
        assertFalse(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.txt"))); 
        assertFalse(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.txt")));
        assertFalse(Files.exists(Paths.get(testDirectoryName,"test-app-d-0.1.0.txt")));
        assertFalse(Files.exists(Paths.get(testDirectoryName,"test-app-g-0.1.0.txt"))); 
        System.out.println("One version of each good chart was successfully printed in text format");
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
            ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_TRUE);
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
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_JSON, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
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
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_PUML, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
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
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, null, 1, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        System.out.println("null file format mask was handled correctly");
        // empty string format specifier
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, null, 1, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        System.out.println("empty file format mask was handled correctly");
        // mixed case format specifier
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_ALL, 1, TEST_ENV_SPEC, VERBOSE_FALSE);
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
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        // Test a bad helm command
        ChartMapGenerator cmg1 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg1 = spy(cmg1);
        // Use a command that is the same across all the OS's so it will run
        Process p1 = Runtime.getRuntime().exec(new String[] { "echo", "I am the foo process" });
        Process sp1 = spy(p1);
        doReturn(sp1).when(scmg1).getProcess(any());
        doReturn("helm").when(scmg1).getHelmCommand();
        // Return 1 to mimic a bad helm command forcing a ChartMapGeneratorException
        doReturn(1).when(sp1).exitValue();
        assertThrows(ChartMapGeneratorException.class, () -> scmg1.checkHelmVersion());
        System.out.println("ChartMapGeneratorException thrown as expected");
        // Test not helm version 3
        ChartMapGenerator cmg2 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg2 = spy(cmg2);
        // Use a command that is the same across all the OS's to mimic a helm not v3
        Process p2 = Runtime.getRuntime().exec(new String[] { "echo", "I am not helm version 3" });
        doReturn(p2).when(scmg2).getProcess(any());
        assertThrows(ChartMapGeneratorException.class, () -> scmg2.checkHelmVersion());
        System.out.println("ChartMapGeneratorException thrown as expected");
        // Use a command that will cause the process' BufferedReader to return null and
        // force the ChartMapGeneratorException.
        ChartMapGenerator cmg3 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg3 = spy(cmg3);
        String nullCommand = isWindows() ? "type" : "cat";
        String nullArgument = isWindows() ? "NUL" : "/dev/null";
        Process p3 = Runtime.getRuntime().exec(new String[] { nullCommand, nullArgument });
        doReturn(p3).when(scmg3).getProcess(any());
        assertThrows(ChartMapGeneratorException.class, () -> scmg3.checkHelmVersion());
        System.out.println("ChartMapGeneratorException thrown as expected");
        // Use a command that will cause the process' BufferedReader to just one
        // character and force the ChartMapGeneratorException
        ChartMapGenerator cmg4 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg4 = spy(cmg4);
        Process p4 = Runtime.getRuntime().exec(new String[] { "echo", "1" });
        doReturn(p4).when(scmg4).getProcess(any());
        assertThrows(ChartMapGeneratorException.class, () -> scmg4.checkHelmVersion());
        System.out.println("ChartMapGeneratorException thrown as expected");
        // Cause an IOException -> ChartMapGeneratorException on getProcess()
        ChartMapGenerator cmg5 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg5 = spy(cmg5);
        doThrow(IOException.class).when(scmg5).getProcess(any());
        assertThrows(ChartMapGeneratorException.class, () -> scmg5.checkHelmVersion());
        System.out.println("IOException -> ChartMapGeneratorException thrown as expected");
        // Cause an InterruptedException -> ChartMapGeneratorException on waitFor()
        // Be careful to put InterruptedException case last in the test case since the
        // thread is not usable after that
        ChartMapGenerator cmg6 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, true);
        ChartMapGenerator scmg6 = spy(cmg6);
        Process p6 = Runtime.getRuntime()
                .exec(new String[] { "echo", "I am going to throw an InterruptedException!!" });
        Process sp6 = spy(p6);
        doReturn(sp6).when(scmg6).getProcess(any());
        doThrow(InterruptedException.class).when(sp6).waitFor(ChartMapGenerator.HELM_PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
        assertThrows(ChartMapGeneratorException.class, () -> scmg6.checkHelmVersion());
        System.out.println("InterruptedException -> ChartMapGeneratorException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test helmClientInformationTest
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    void helmClientInformationTest() throws ChartMapGeneratorException, IOException, InterruptedException{
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg1 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, false);
        ChartMapGenerator scmg1 = spy(cmg1);
        // force setExecutable to return false to force ChartMapGeneratorException
        File f1 = new File("foo");
        File sf1 = spy(f1);
        doReturn(sf1).when(scmg1).getTempFile(any(), any());
        doReturn(false).when(sf1).setExecutable(true, true);
        doReturn(true).when(sf1).setWritable(true, true);
        doReturn(true).when(sf1).setReadable(true, true);
        assertThrows(ChartMapGeneratorException.class, () -> scmg1.getHelmClientInformation());
        System.out.println("ChartMapGeneratorException thrown as expected testing setExecutable");
        // force setWritable to return false to force ChartMapGeneratorException
        File f2 = new File("foo");
        File sf2 = spy(f2);
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg2 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, false);
        ChartMapGenerator scmg2 = spy(cmg2);
        doReturn(sf2).when(scmg2).getTempFile(any(), any());
        doReturn(true).when(sf2).setExecutable(true, true);
        doReturn(false).when(sf2).setWritable(true, true);
        doReturn(true).when(sf2).setReadable(true, true);
        assertThrows(ChartMapGeneratorException.class, () -> scmg2.getHelmClientInformation());
        System.out.println("ChartMapGeneratorException thrown as expected testing setWritable");
        // force setReadable to return false to force ChartMapGeneratorException
        File f3 = new File("foo");
        File sf3 = spy(f3);
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg3 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, false);
        ChartMapGenerator scmg3 = spy(cmg3);
        doReturn(sf3).when(scmg3).getTempFile(any(), any());
        doReturn(true).when(sf3).setExecutable(true, true);
        doReturn(true).when(sf3).setWritable(true, true);
        doReturn(false).when(sf3).setReadable(true, true);
        assertThrows(ChartMapGeneratorException.class, () -> scmg3.getHelmClientInformation());
        System.out.println("ChartMapGeneratorException thrown as expected testing setReadable");
        // force a bad exit value 
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg4 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, false);
        ChartMapGenerator scmg4 = spy(cmg4);
        ProcessBuilder pb4 = new ProcessBuilder("foo", "bar");
        ProcessBuilder spb4 = spy(pb4);
        Process p4 = Runtime.getRuntime().exec(new String[] { "echo", "I am going to return a bad exit value ... just watch me!!" });
        Process sp4 = spy(p4);
        doReturn(1).when(sp4).exitValue();
        doReturn(sp4).when(spb4).start();
        doReturn(spb4).when(scmg4).getProcessBuilder(any(), any());
        assertThrows(ChartMapGeneratorException.class, () -> scmg4.getHelmClientInformation());
        System.out.println("ChartMapGeneratorException thrown as expected with simulated bad exit code");
        // force an InterruptedException on waitFor
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg5 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, false);
        ChartMapGenerator scmg5 = spy(cmg5);
        ProcessBuilder pb5 = new ProcessBuilder("foo", "bar");
        ProcessBuilder spb5 = spy(pb5);
        Process p5 = Runtime.getRuntime().exec(new String[] { "echo", "I am going to throw an InterruptedException on waitFor ... just watch me!!" });
        Process sp5 = spy(p5);
        doThrow(InterruptedException.class).when(sp5).waitFor(ChartMapGenerator.HELM_PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
        doReturn(sp5).when(spb5).start();
        doReturn(spb5).when(scmg5).getProcessBuilder(any(), any());
        assertThrows(ChartMapGeneratorException.class, () -> scmg5.getHelmClientInformation());
        Thread.interrupted(); // Since the interrupt was interruped, make sure to set the interrupt flag or the next test will fail
        System.out.println("InterruptedException -> ChartMapGeneratorException thrown as expected");
        // force an IOException on process start
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg6 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, false);
        ChartMapGenerator scmg6 = spy(cmg6);
        ProcessBuilder pb6 = new ProcessBuilder("foo", "bar");
        ProcessBuilder spb6 = spy(pb6);
        doThrow(IOException.class).when(spb6).start(); 
        doReturn(spb6).when(scmg6).getProcessBuilder(any(), any());
        assertThrows(ChartMapGeneratorException.class, () -> scmg6.getHelmClientInformation());
        System.out.println("IOException -> ChartMapGeneratorException thrown as expected");
        System.out.println(m.concat(" completed"));
    }

     /**
     * Forces various IOException conditions using spies for complete
     * code coverage.
     * @throws IOException
     * @throws ChartMapGeneratorException
     * 
     */
    @Test
    void addHeadTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        // test addHead
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg1 = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, 1, null, false);
        ChartMapGenerator scmg1 = spy(cmg1);
        doThrow(IOException.class).when(scmg1).addHead(); 
        assertThrows(ChartMapGeneratorException.class, ()->scmg1.generate());
        System.out.println("IOException -> ChartMapGeneratorException thrown as expected by addHead");
        System.out.println(m.concat(" completed"));
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
        // no arguments
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMapGenerator.main(new String[0]);
            assertTrue(streamContains(o, "Usage:"));
            System.setOut(initialOut);
            System.out.println("Help was returned as expected when no arguments were provided");
        }
        // missing repo name
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMapGenerator.main(new String[]{"-n", "2"});
            assertTrue(streamContains(o, "Usage:"));
            System.setOut(initialOut);
            System.out.println("Help was returned as expected when no repo name was provided");
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
