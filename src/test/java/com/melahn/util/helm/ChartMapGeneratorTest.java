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

import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Null;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import com.melahn.util.helm.ChartMapGenerator;
import com.melahn.util.helm.ChartMapGeneratorException;
class ChartMapGeneratorTest {

    private static String targetTest = "target/test";
    private static String targetTestDirectory = Paths.get(targetTest).toString();
    private final static String FORMAT_JSON = "-j";
    private final static String FORMAT_PUML = "-p";
    private final static String FORMAT_TEXT = "-t";
    
    private final static String TEST_REPO_NAME = "melahn";
    private final static String DIVIDER = "-------------------------------------";

    @BeforeAll
    static void setUp() {
        System.out.println(DIVIDER.concat(" UNIT TESTS START ").concat(DIVIDER));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

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
     * Tests the ChartMap.checkHelmVersion method.
     * 
     * @throws ChartMapException
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
     * Tests the Help
     * 
     * @throws IOException
     */
    @Test
    void helpTest() throws IOException {
        String helpTextExpected = "\nUsage:\n\n"
                .concat("java -jar helm-chartmap-generator-1.0.0-SNAPSHOT.jar\n")
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

    private ChartMapGenerator createTestMapGenerator(String repoName, String outputDirName, String fileFormatMask,
            int maxVersions, String envFilename, boolean verbose) throws ChartMapGeneratorException {
        ChartMapGenerator cmg = new ChartMapGenerator(repoName, outputDirName, fileFormatMask, maxVersions, envFilename,
                verbose);
        // cmg. // set this explictly so that test cases can test helm dependent methods
        // without
        // necessarily calling print
        return cmg;
    }
}
