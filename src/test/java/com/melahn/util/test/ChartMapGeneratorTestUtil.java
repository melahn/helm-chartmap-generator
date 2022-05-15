package com.melahn.util.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.melahn.util.helm.ChartMapGenerator;
import com.melahn.util.helm.ChartMapGeneratorException;

public class ChartMapGeneratorTestUtil {

    private static final int WAIT_TIME = 240; // wait for a long time because a fair number of charts need to be generated

    /**
     * Answers true if the log contains a particular entry.
     * 
     * @param l the file
     * @param s entry being looked for
     * @return true if the log contains s, false otherwise.
     * @throws IOException if an error occurs reading the file
     */
    public static boolean fileContains(Path l, String s) throws IOException {
        List<String> list = new ArrayList<>();
        BufferedReader br = Files.newBufferedReader(l);
        list = br.lines().collect(Collectors.toList());
        boolean found = false;
        ListIterator<String> listIterator = list.listIterator();
        while (listIterator.hasNext() && !found) {
            found = listIterator.next().contains(s) ? true : false;
        }
        return found;
    }

    /**
     * Answers true if the stream contains a particular entry.
     * 
     * @param baos the log stream
     * @param s    entry being looked for
     * @return true if the log contains s, false otherwise
     */
    public static boolean streamContains(ByteArrayOutputStream baos, String s) {
        return baos.toString().contains(s);
    }

    /**
     * Cleans a directory to a depth of 3
     * 
     * @param d Path of the directory
     */
    public static void cleanDirectory(Path d) throws IOException {
        final int depth = 7; // go five deep

        System.out.println("Deleting any previously created files");
        if (Files.exists(d)) {
            try (Stream<Path> s = Files.walk(d, depth)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                System.out.println("Error walking directory: " + d);
            }
        }
    }

    /**
     * Creates a ChartMapGenerator.
     * 
     * @param repoName the name of the repo
     * @param outputDirName the name of the output directory
     * @param fileFormatMask the file format mask
     * @param maxVersions how many chart versions to print, at most
     * @param envFilename the name of the env file
     * @param verbose whether verbose output is desired
     * @return a new ChartMapGenerator
     * @throws ChartMapGeneratorException if an error occurs creating the ChartMapGenerator
     */
    public static ChartMapGenerator createTestMapGenerator(String repoName, String outputDirName, String fileFormatMask,
            int maxVersions, String envFilename, boolean verbose) throws ChartMapGeneratorException {
        return new ChartMapGenerator(repoName, outputDirName, fileFormatMask, maxVersions, envFilename,
                verbose);
    }

    /**
     *
     * Start a sub-process, setting and removing some environment variables and
     * system properties beforehand.
     *
     * @param a arguments
     * @param e Environment variables to add and remove
     * @param o value of os.type to set
     * @param j Jacoco agent string
     * @param c name of class to run
     * @param p directory in which to run the process
     * @param l log file
     * @return exit value of process
     * @throws IOException
     * @throws InterruptedException
     */
    public int createProcess(List<String> a, Object[] e, String o, String j, String c, Path p, Path l)
            throws IOException, InterruptedException {
        ProcessBuilder pb = null;
        String shadedJarName = this.getShadedJarName();
        pb = new ProcessBuilder();
        pb.command().add("java");
        pb.command().add("-javaagent:".concat(j));
        pb.command().add("-cp");
        pb.command().add("../../".concat(shadedJarName)); // expect the jar two levels up
        if (o != null) {
            pb.command().add("-Dos.name=".concat(o));
        }
        pb.command().add(c);
        for (int i = 0; i < a.size(); i++) {
            pb.command().add(a.get(i));
        }
        // modify the environment
        if (e != null) {
            Map<String, String> env = pb.environment();
            String[] addEnv = (String[]) e[0];
            for (int i = 0; i < addEnv.length; i = i + 2) {
                if (addEnv[i] != null && addEnv[i + 1] != null) {
                    env.put(addEnv[i], addEnv[i + 1]);
                }
            }
            String[] removeEnv = (String[]) e[1];
            for (int i = 0; i < removeEnv.length; i++) {
                env.remove(removeEnv[i]);
            }
        }
        // Capture fhe output in case its interesting for debugging
        pb.directory(p.toAbsolutePath().toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.appendTo(l.toFile()));
        Process process = pb.start();
        process.waitFor(WAIT_TIME, TimeUnit.SECONDS);
        return process.exitValue();
    }

    /**
     * Get the name of the shaded jar based on the version in the pom.
     *
     * @return the name of the shaded jar
     */
    public String getShadedJarName() throws IOException {
        final Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream("resources.properties"));
        return "helm-chartmap-generator-".concat(properties.getProperty("shaded.jar.version")).concat(".jar");
    }

    /**
     * Answer true if this a Windows OS
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
