package com.melahn.util.helm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmChartLocalCache;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class ChartMapGenerator {

    private static String localRepoName = null;
    private static String outputDirname = System.getProperty("user.dir");
    private static String envFilename = null;
    private static boolean verbose = false;
    private static String formatString = "";
    private static HashSet<String> extensions = new HashSet<String>();
    private static String indexFilename = null;
    private static String styleFilename = null;
    private static int maxVersions = 0;

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
                log("format string \"".concat(formatString).concat("\" will be used"));
            }
            if (cmd.hasOption("n")) {
                maxVersions = Integer.parseInt(cmd.getOptionValue("n"));
                if (maxVersions == 1) {
                    log("Only one version of each chart will be printed");
                }
                else {
                    log("A maximum of ".concat(cmd.getOptionValue("n")).concat(" versions of each chart will be printed"));
                }
            }
            if (cmd.hasOption("e")) {
                envFilename = cmd.getOptionValue("e");
                log("Environment file \"".concat(envFilename).concat("\" will be used"));
            }
            if (a.length == 0
                    || cmd.hasOption("h")
                    || localRepoName == null) {
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
     * each and prints it, keeping a count aling the way
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
            createIndex();
            for (Map.Entry<String, HelmChart[]> entry : entries.entrySet()) {
                int i = 1;
                for (HelmChart h : entry.getValue()) {
                    printChart(h);
                    if (++i > maxVersions) {
                        break;
                    }
                }
            }
            endIndex();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Prints some help
     *
     * @return a string containing some help
     */
    public static String getHelp() {
        String help = "\nUsage:\n\n"
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

        return help;
    }

    /**
     * Prints a chart
     *
     * @param h helm chart
     */
    private static void printChart(HelmChart h) {

        try {
            log("printing chart: " + h.getNameFull());
            startStanzaInIndex(h.getNameFull());
            for (String e : extensions) {
                printChart(h, e);
            }
            closeStanzaInIndex();
        } catch (Exception e) {
            System.out.println("Exception printing Chart " + h.getNameFull() + " : " + e.getMessage());
        }
    }

    /**
     * Prints a chart of a specific format using the ChartMap API
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
                    true,
                    false,
                    false);
            testMap.print();
            addChartToIndex(filename);
            log(filename.concat(filename.concat(" created")));
        } catch (Exception e) {
            System.out.println("Exception creating file ".concat(filename).concat(" : ").concat(e.getMessage()));
        }
    }

    /**
     * Creates the index file and adds a head element
     *
     */
    private static void createIndex() throws Exception {
        indexFilename = outputDirname.concat("/index.html");
        String h = "<!DOCTYPE HTML>\n<html>\n";
        try {
            Files.write(Paths.get(indexFilename),
                    h.getBytes(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            addHead();
        } catch (IOException e) {
            throw (new Exception("Error creating index file ".concat(indexFilename).concat(".  Possibly it already exists")));
        }
    }

    /**
     * Adds head elements to the index file and adds a H1 tag, then creates css file
     *
     */
    private static void addHead () throws IOException {
        String h = "<head>"
                .concat("\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n")
                .concat("\t<link rel=\"stylesheet\" href=\"./style.css\">\n")
                .concat("\t<title>Helm Dependency Charts</title>\n")
                .concat("</head>\n<body>\n")
                .concat("\t<h1 class=\"header\">Helm Dependency Charts</h1>\n");
        Files.write(Paths.get(indexFilename),
                h.getBytes(),
                StandardOpenOption.APPEND);
        createStyleFile();
    }

    /**
     * Creates a css style file.  If the file already exists, the file is left as-is
     *
     */
    private static void createStyleFile () throws IOException {
        styleFilename = outputDirname.concat("/style.css");
        String s = "* {\n\tfont-family: \"Arial\", Helvetica, san-serif;\n\tcolor: lightgray;\n\tbackground-color: black;\n}\n"
                .concat("a:visited {\n\tcolor: grey;\n}\n")
                .concat(".header {").concat("\n\tfont: 30px sans-serif;\n}\n")
                .concat(".chartname {").concat("\n\tfont-weight: bold;\n\tfont: 16px sans-serif;\n}\n")
                .concat(".chartlink {").concat("\n\tfont: 16px sans-serif;\n}\n");
        Files.write(Paths.get(styleFilename),
                s.getBytes(),
                StandardOpenOption.CREATE);
    }

    /**
     * Starts one chart stanza with the chartname to the index file
     *
     */
    private static void startStanzaInIndex(String s) throws IOException {
        String l = "\t<p class=\"chartname\">".concat(s).concat("</p>\n\t<ul>\n");
        Files.write(Paths.get(indexFilename),
                l.getBytes(),
                StandardOpenOption.APPEND);
    }

    /**
     * Closes a stanza in the index file
     *
     */
    private static void closeStanzaInIndex() throws IOException {
        String l = "\t</ul>\n";
        Files.write(Paths.get(indexFilename),
                l.getBytes(),
                StandardOpenOption.APPEND);
    }

    /**
     * Adds a single chart filename to the index file.  If the file is a PlantUML file, also adds the png file to the index.
     *
     * @param f helm chart filename
     */
    private static void addChartToIndex(String f) throws IOException {
        String l = "\t\t<li class=\"chartlink\">".concat("<a href=\"./").concat(f).concat("\">").concat(f).concat("</a>").concat("</li>\n");
        Files.write(Paths.get(indexFilename),
                l.getBytes(),
                StandardOpenOption.APPEND);
        if (f.endsWith(".puml")) {
            String p = f.replace("puml", "png");
            l = "\t\t<li class=\"chartlink\">".concat("<a href=\"./").concat(p).concat("\">").concat(p).concat("</a>").concat("</li>\n");
            Files.write(Paths.get(indexFilename),
                    l.getBytes(),
                    StandardOpenOption.APPEND);
        }
    }

    /**
     * @return A format for date and time
     *
     */
    private static String getCurrentDateTime() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return (f.format(LocalDateTime.now()));
    }
    /**
     * Adds end elements to the index file
     *
     */
    private static void addFooterToIndex() throws IOException {
        String f = "<hr/>Generated on "
                .concat(getCurrentDateTime())
                .concat(" by ")
                .concat(ChartMapGenerator.class.getCanonicalName())
                .concat(" ")
                .concat("<a href=\"https://github.com/melahn/helm-chartmap-generator\">https://github.com/melahn/helm-chartmap-generator</a>");
        Files.write(Paths.get(indexFilename),
                f.getBytes(),
                StandardOpenOption.APPEND);
    }

    /**
     * Adds closing elements to the index file
     *
     */
    private static void endIndex () throws IOException {
        addFooterToIndex();
        String e = "</body>\n</html>";
        Files.write(Paths.get(indexFilename),
                e.getBytes(),
                StandardOpenOption.APPEND);
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
