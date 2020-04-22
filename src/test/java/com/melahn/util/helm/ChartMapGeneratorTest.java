package com.melahn.util.helm;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.fail;

/*
 Tests the integrity of the jar by executing it to get the command line help
 */
public class ChartMapGeneratorTest {
    @Test
    public void testHelp() {
        final String helpTextExpected = ChartMapGenerator.getHelp();
        try {
            Process process = Runtime.getRuntime().exec("java -jar helm-chartmap-generator-1.0.0.jar", null, new File("./resource/jar"));
            InputStream reader = process.getInputStream();
            byte[] b = new byte[helpTextExpected.length()];
            int i = 0;
            while ((i < b.length) && ((reader.read(b, i++, 1)) != -1)) {};
            String helpText = new String(b);
            assert (helpText.equals(helpTextExpected));
        } catch (IOException e) {
            fail("testHelp failed:" + e.getMessage());
        }
    }
}
