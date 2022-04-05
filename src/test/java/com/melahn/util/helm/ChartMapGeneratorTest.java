package com.melahn.util.helm;

import static com.melahn.util.test.ChartMapGeneratorTestUtil.isWindows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Null;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import com.melahn.util.helm.ChartMapGenerator;
import com.melahn.util.helm.ChartMapGeneratorException;
import com.melahn.util.test.ChartMapGeneratorTestUtil;
class ChartMapGeneratorTest {

    private static final String DIVIDER = "-------------------------------------";
    private static final int PRINT_ONE_VERSION = 1;
    private static final int PRINT_ALL_VERSIONS = 99;
    private static final String FORMAT_ALL = "-jpt";
    private static final String FORMAT_JSON = "-j";
    private static final String FORMAT_PUML = "-p";
    private static final String FORMAT_TEXT = "-t";
    private static final String INDEX_FILENAME = "index.html";
    private static final String TARGET_TEST_DIR_NAME = "target/test";
    private static final Path   TARGET_TEST_DIR_PATH = Paths.get(TARGET_TEST_DIR_NAME);
    private static final String TEST_REPO_NAME = "melahn";
    private static final String TEST_ENV_SPEC = "./resource/example/example-env-spec.yaml";
    private static final String TEST_ENV_SPEC_NOT_EXIST = "./resource/example/no-spec-here.yaml";
    private static final boolean VERBOSE_FALSE = false;
    private static final boolean VERBOSE_TRUE = true;


    private static Path indexFilePath = Paths.get(INDEX_FILENAME);
    private static String targetTest = "target/test";
    private static String targetTestDirectory = Paths.get(targetTest).toString();

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
     * Test variations of the number of versions.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     */
    @Test
    void versionsParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        // validate that all versions of each of the charts are printed in text format
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.txt"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.txt")));
        System.out.println("All versions of each chart were successfully printed in text format");
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_TEXT, PRINT_ONE_VERSION, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg .generate();
        // validate that one and only one (the most recent) version of each of the charts is printed in text format
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
     * Test variations on the format parameter.
     * 
     * @throws ChartMapGeneratorException
     * @throws IOException
     * 
     */
    @Test
    void formatParameterTest() throws ChartMapGeneratorException, IOException {
        String m = new Throwable().getStackTrace()[0].getMethodName();
        testVariation = 0;
        testDirectoryName = createTestDir(m, getTestVariation());
        ChartMapGenerator cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName, FORMAT_TEXT, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        // validate that all versions of each of the charts are printed in text format
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.txt"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.txt")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.txt")));
        System.out.println("All versions of each chart were successfully printed in text format");
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_JSON, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        // validate that all versions of each of the charts are printed in json format 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.json"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.json")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.json")));
        System.out.println("All versions of each chart were successfully printed in json format");
        testDirectoryName = createTestDir(m, getTestVariation());
        cmg = createTestMapGenerator(TEST_REPO_NAME, testDirectoryName.toString(), FORMAT_PUML, PRINT_ALL_VERSIONS, TEST_ENV_SPEC, VERBOSE_FALSE);
        cmg.generate();
        // validate that all versions of each of the charts are printed in puml format with an accompanying image file
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.puml")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.puml"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.puml")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.puml")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.puml")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.puml")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-a-0.1.0.png")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.1.0.png"))); 
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-b-0.2.0.png")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.1.0.png")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.2.0.png")));
        assertTrue(Files.exists(Paths.get(testDirectoryName,"test-app-c-0.3.0.png")));
        System.out.println("All versions of each chart were successfully printed in puml and png format");
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
    void helpTest() throws IOException {
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
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Helper Function to create a ChartMapGenerator
     * @param repoName the name of the repo
     * @param outputDirName the name of the output directory
     * @param fileFormatMask the file format mask
     * @param maxVersions how many chart versions to print, at most
     * @param envFilename the name of the env file
     * @param verbose whether verbose output is desired
     * @return
     * @throws ChartMapGeneratorException if an error occurs creating the ChartMapGenerator
     */
    private ChartMapGenerator createTestMapGenerator(String repoName, String outputDirName, String fileFormatMask,
            int maxVersions, String envFilename, boolean verbose) throws ChartMapGeneratorException {
        ChartMapGenerator cmg = new ChartMapGenerator(repoName, outputDirName, fileFormatMask, maxVersions, envFilename,
                verbose);
        return cmg;
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
        ChartMapGeneratorTestUtil.cleanDirectory(p);
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
