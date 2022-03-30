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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChartMapGenerator {

    protected Logger logger;
    private String chartMapGeneratorVerbose = "CHARTMAP_GENERATOR_VERBOSE";
    private Level logLevelVerbose;

    private String localRepoName = null;
    private String outputDirname = System.getProperty("user.dir");
    private String envFilename = null;
    private boolean verbose = false;
    private String formatString = "";
    private HashSet<String> extensions = new HashSet<>();
    private String indexFilename = null;
    private int maxVersions = 0;

    private static final String DEFAULT_LOCATION_MESSAGE = File.pathSeparator.concat(" will be used");
    private static final boolean GENERATE_IMAGE_SWITCH = true;
    private static final boolean REFRESH_REPOS_SWITCH = false;
    private static final boolean VERBOSE_SWITCH = false;

    /**
     * Parses the command line and generates a set of reports
     *
     * @param arg The command line args
     */
    public static void main(String[] arg) {
        ChartMapGenerator generator = new ChartMapGenerator();
        try {
            generator.parseArgs(arg);
            generator.generate();
        } catch (ChartMapGeneratorException e) {
            generator.logger.error("ChartMapGeneratorException: {} ", e.getMessage());
        }
    }

    /**
     * Parses the command line
     *
     * @param a The command line args
     * @return false if a parse error occurs or help is reqquested, true otherwise
     */
    private boolean parseArgs(String[] a) throws ChartMapGeneratorException {
        Options options = new Options();
        options.addOption("h", false, "Help");
        options.addOption("o", true, "The Output Directory name");
        options.addOption("r", true, "The Local Helm Repo Name");
        options.addOption("f", true, "The Formats to be printed");
        options.addOption("n", true, "The Max Number of Chart Versions to Print");
        options.addOption("e", true, "Environment Specification File");
        options.addOption("v", false, "Verbose");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, a);
            if (cmd.hasOption("v")) {
                verbose = true;
            }
            if (cmd.hasOption("o")) {
                outputDirname = cmd.getOptionValue("o");
                logger.log(logLevelVerbose, "Files will be written to directory \"{}\".", outputDirname);
            }
            if (cmd.hasOption("r")) {
                localRepoName = cmd.getOptionValue("r");
                logger.log(logLevelVerbose, "Local helm chart repo \"{}{}\"", localRepoName, DEFAULT_LOCATION_MESSAGE);
            }
            if (cmd.hasOption("f")) {
                formatString = cmd.getOptionValue("f");
                logger.log(logLevelVerbose, "Format string \"{}{}\"", formatString, DEFAULT_LOCATION_MESSAGE);
            }
            if (cmd.hasOption("n")) {
                maxVersions = Integer.parseInt(cmd.getOptionValue("n"));
                if (maxVersions == 1) {
                    logger.log(logLevelVerbose, "Only one version of each chart will be printed.");
                }
                else {
                    logger.log(logLevelVerbose, "A maximum of {} versions of each chart will be printed.", cmd.getOptionValue("n"));
                }
            }
            if (cmd.hasOption("e")) {
                envFilename = cmd.getOptionValue("e");
                logger.log(logLevelVerbose, "Environment file \"{}{}\"", envFilename, DEFAULT_LOCATION_MESSAGE);
            }
            if (a.length == 0
                    || cmd.hasOption("h")
                    || localRepoName == null) {
                logger.info(ChartMap.getHelp());
                return false;
            }
            parseFormatString();
            return true;
        } catch (ParseException e) {
            throw (new ChartMapGeneratorException(e.getMessage()));
        }
    }

    /**
     * Constructs a helm repo file name from the short name of a helm repo
     *
     * @param r A repo name (e.g. stable)
     * @return The fqfn of a local yaml file where helm chart info can be found, null otherwise
     */
    private String constructRepoFilename(String r) {
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
    private void generate() {
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
    private void loadChartsFromCache(String f) {
        logger.info("Loading charts from {}", f);
        HelmChartLocalCache cache;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            cache = mapper.readValue(new File(f), HelmChartLocalCache.class);
            Map<String, HelmChart[]> entries = cache.getEntries();
            logger.info("{} charts were found", entries.size());
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
            logger.error("Exception: {}",e.getMessage());
        }
    }

    /**
     * Prints some help
     *
     * @return a string containing some help
     */
    public String getHelp() {
        return "\nUsage:\n\n"
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
    }

    /**
     * Prints a chart
     *
     * @param h helm chart
     */
    private void printChart(HelmChart h) {

        try {
            logger.info("Printing chart: {}", h.getNameFull());
            startStanzaInIndex(h.getNameFull());
            for (String e : extensions) {
                printChart(h, e);
            }
            closeStanzaInIndex();
        } catch (Exception e) {
            logger.error("Exception printing Chart \"{}\" : {}", h.getNameFull(), e.getMessage());
        }
    }

    /**
     * Prints a chart of a specific format using the ChartMap API
     *
     * @param h helm chart
     * @param f the file extension to use
     */
    private void printChart(HelmChart h, String f) {
        String filename = h.getName().concat("-").concat(h.getVersion()).concat(f);
        try {
            ChartMap testMap = new ChartMap(
                    ChartOption.CHARTNAME,
                    h.getNameFull(),
                    outputDirname.concat("/").concat(filename),
                    envFilename,
                    new boolean[]{ GENERATE_IMAGE_SWITCH, REFRESH_REPOS_SWITCH, VERBOSE_SWITCH});
            testMap.print();
            addChartToIndex(filename);
            logger.info("File {} created.", filename);
        } catch (Exception e) {
            logger.error("Exception creating file \"{}\" : {}", filename, e.getMessage());
        }
    }

    /**
     * Creates the index file and adds a head element
     *
     */
    private void createIndex() throws ChartMapGeneratorException {
        indexFilename = outputDirname.concat("/index.html");
        String h = "<!DOCTYPE HTML>\n<html>\n";
        try {
            Files.write(Paths.get(indexFilename),
                    h.getBytes(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            addHead();
        } catch (IOException e) {
            throw (new ChartMapGeneratorException("Error creating index file ".concat(indexFilename).concat(".  Possibly it already exists")));
        }
    }

    /**
     * Adds head elements to the index file and adds a H1 tag, then creates css file
     *
     */
    private void addHead () throws IOException {
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
    private void createStyleFile () throws IOException {
        String styleFilename = outputDirname.concat("/style.css");
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
    private void startStanzaInIndex(String s) throws IOException {
        String l = "\t<p class=\"chartname\">".concat(s).concat("</p>\n\t<ul>\n");
        Files.write(Paths.get(indexFilename),
                l.getBytes(),
                StandardOpenOption.APPEND);
    }

    /**
     * Closes a stanza in the index file
     *
     */
    private void closeStanzaInIndex() throws IOException {
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
    private void addChartToIndex(String f) throws IOException {
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
    private void addFooterToIndex() throws IOException {
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
    private void endIndex () throws IOException {
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
    private void parseFormatString() {
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
     * If the user has specified the verbose flag, set the log level so it has a
     * higher priority (ie. a lower level value) than the logger configured in
     * log4j2.xml, which is INFO (level 400). Otherwise set it to a higher level
     * number so verbose log entries will be ignored.
     * 
     * Note that log4j will ignore the integer values if the level already exists so
     * there is no point in calling this method twice or initializing the Levels
     * when they are declared.
     * 
     * The reason for the timestamped value for the levels is that they are static
     * in Log4j2 and this can cause issues across multiple usages of ChartMap if a
     * common name is used with some pretty hard to debug problems. This can be seen
     * for example in Junit tests when different debug and verbose levels are used
     * in different tests.
     */
    protected void setVerboseLogLevel() {
        String t = String.valueOf(System.currentTimeMillis());
        chartMapGeneratorVerbose = chartMapGeneratorVerbose.concat(t);
        logger = LogManager.getLogger(t);
        if (isVerbose()) {
            logLevelVerbose = Level.forName(chartMapGeneratorVerbose, 350); // higher priority than INFO
        } else {
            logLevelVerbose = Level.forName(chartMapGeneratorVerbose, 450); // lower priority than INFO
        }
    }

    public boolean isVerbose() {
        return verbose;
    }

    protected void setVerbose(boolean v) {
        this.verbose = v;
    }
}
