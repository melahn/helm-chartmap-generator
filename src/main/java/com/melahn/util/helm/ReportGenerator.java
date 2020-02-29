package com.melahn.util.helm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.melahn.util.helm.ChartMap;
import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmChartLocalCache;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ReportGenerator {

    private static String localRepoName = null;
    private static String outputDirname = null;
    private static String envFilename = null;
    private static boolean verbose = false;
    private static String formatString = "";
    private static HashSet<String> extensions = new HashSet<String>();

    /**
     * Parses the command line and generates a set of reports
     *
     * @param arg The command line args
     */
    public static void main(String[] arg) {
        try {
            parseArgs(arg);
            generate();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("ParseException: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    /**
     * Parses the command line
     *
     * @param a The command line args
     */
    private static void parseArgs(String[] a) throws IOException, ParseException {
        Options options = new Options();
        options.addOption("h", false, "Help");
        options.addOption("o", true, "The Output Directory name");
        options.addOption("r", true, "The Local Helm Repo Name");
        options.addOption("f", true, "The Formats to be printed");
        options.addOption("n", true, "The Max Number of Chart Versions to Print");
        options.addOption("e", true, "Environment Specification File");
        options.addOption("v", false, "Verbose");
        options.addOption("z", false, "Debug Mode");
        CommandLineParser parser = new DefaultParser();
        int count = 0;
        try {
            CommandLine cmd = parser.parse(options, a);
            if (cmd.hasOption("v")) {
                verbose = true;
            }
            if (cmd.hasOption("z")) {
                boolean debug = true;
            }
            if (cmd.hasOption("o")) {
                outputDirname = cmd.getOptionValue("o");
                log("Files will be written to directory \"".concat(outputDirname).concat("\""));
            }
            if (cmd.hasOption("r")) {
                localRepoName = cmd.getOptionValue("r");
                log("local helm chart repo \"".concat(localRepoName).concat("\" will be used"));
            }
            if (cmd.hasOption("f")) {
                formatString = cmd.getOptionValue("f");
                log("format string".concat(formatString).concat("\" will be used"));
            }
            if (cmd.hasOption("n")) {
                count = Integer.parseInt(cmd.getOptionValue("n"));
                log("A maximum of ".concat(cmd.getOptionValue("n")).concat(" versions will be printed"));
            }
            if (cmd.hasOption("e")) {
                envFilename = cmd.getOptionValue("e");
                log("Environment file \"".concat(envFilename).concat("\" will be used"));
            }
            if (a.length == 0
                    || cmd.hasOption("h")
                    || localRepoName == null
                    || outputDirname == null) {
                System.out.println(getHelp());
                System.exit(0);
            }
            parseFormatString();
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            throw (e);
        }
    }

    /**
     * Constructs a helm repo file name from the short name of a helm repo
     *
     * @param r A repo name (e.g. stable)
     * @return The fqfn of a local yaml file where helm chart info can be found, null otherwise
     */
    private static String constructRepoFilename(String r) {
        String helmHome = System.getenv("HELM_HOME");
        if (helmHome != null) {
            String repoFileName = helmHome.concat("/").concat("/repository/cache/").concat(r).concat("-index.yaml");
            File f = new File(repoFileName);
            if (f.exists()) {
                return repoFileName;
            }
        }
        return null;
    }

    /**
     * Generates a set of reports
     *
     */
    private static void generate() {
        String localRepoFilename = constructRepoFilename(localRepoName);
        if (localRepoFilename != null) {
            loadChartsFromCache(localRepoFilename);
        }
    }

    /**
     * Takes the name of a file containing helm charts in yaml form and constructs a HelmChart object from
     * each and adds that HelmChart object to the charts map
     *
     * @param f a file containing helm charts in yaml form
     */
    private static void loadChartsFromCache(String f) {
        log("loading charts from ".concat(f));
        HelmChartLocalCache cache;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            cache = mapper.readValue(new File(f), HelmChartLocalCache.class);
            Map<String, HelmChart[]> entries = cache.getEntries();
            log(String.valueOf(entries.size()).concat(" charts found"));
            for (Map.Entry<String, HelmChart[]> entry : entries.entrySet()) {
                int i = 1;
                for (HelmChart h : entry.getValue()) {
                    printChart(h);
                    int count = 1;
                    if (++i > count) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Prints some help
     *
     * @return a string containing some help
     */
    private static String getHelp() {
        String help = "\nUsage:\n";
        help += "java -jar ---<filename>----  -r <chart repo name>----  -o <directoryname>----+------------+---+------------+\n";
        help += "                                                                             |            |   |            |\n";
        help += "                                                                             +---  -v  ---+   +---  -h  ---+\n";
        help += "\nSee https://github.com/melahn/helm-chartmap-generator for more information\n";
        return help;
    }

    /**
     * prints a chart
     *
     * @param h helm chart
     */
    private static void printChart(HelmChart h) {

        try {
            log("printing chart: " + h.getNameFull());
            for (String e : extensions) {
                printChart(h, e);
            }
        } catch (Exception e) {
            System.out.println("Exception printing Chart " + h.getNameFull() + " : " + e.getMessage());
        }
    }

    /**
     * prints a chart of a specific format using the ChartMap API
     *
     * @param h helm chart
     * @param f the file extension to use
     */
    private static void printChart(HelmChart h, String f) {
        String filename = h.getName().concat("-").concat(h.getVersion()).concat(f);
        try {
            ChartMap testMap = new ChartMap(
                    ChartOption.CHARTNAME,
                    h.getNameFull(),
                    outputDirname.concat("/").concat(filename),
                    System.getenv("HELM_HOME"),
                    envFilename,
                    false,
                    false);
            testMap.print();
            log(filename.concat(filename.concat(" created")));
        } catch (Exception e) {
            System.out.println("Exception creating file ".concat(filename).concat(" : ").concat(e.getMessage()));
        }
    }

    /**
     * Parses the format string passed in by the caller and
     * adds a set of corresponding extensions to
     * the extensions set which is later used for file writing
     */
    private static void parseFormatString() {
        if (formatString.indexOf('j') >= 0) {
            extensions.add(".json");
        }
        if (formatString.indexOf('p') >= 0) {
            extensions.add(".puml");
        }
        if (formatString.indexOf('t') >= 0 || extensions.isEmpty()) {
            extensions.add(".txt");
        }
    }

    /**
     * Logs a string if verbose is on
     */
    private static void log(String s) {
        if (verbose) {
            System.out.println(s);
        }
    }
}
