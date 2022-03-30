package com.melahn.util.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;

import java.io.*;

import com.melahn.util.helm.ChartMapGenerator;
class ChartMapGeneratorTest {
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
     * Tests the Help
     * 
     * @throws IOException
     */
    @Test
    void helpTest() throws IOException {
        String helpTextExpected = "\nUsage:\n\n"
                .concat("java -jar helm-chartmap-generator-1.0.0.jar\n")
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
}
