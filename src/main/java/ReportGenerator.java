package com.melahn.util.helm;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ReportGenerator {

    /**
     * Parses the command line and generates a set of reports
     *
     * @param arg The command line args
     * @throws IOException
     */
    public static void main(String[] arg) {
        try {
            parseArgs(arg);
            generate();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
    /**
     * Parses the command line
     *
     * @param a The command line args
     * @throws IOException
     */
    private static void parseArgs(String[] a) throws IOException {
        Options options = new Options();
    }

    /**
     * Generates a set of reports
     *
     * @throws IOException
     */
    private static void generate() throws IOException {

    }
}
