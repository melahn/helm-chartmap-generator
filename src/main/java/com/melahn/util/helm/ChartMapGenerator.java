package com.melahn.util.helm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private int chartCount = 0;
    private int chartCountBad = 0;
    private int chartCountGood = 0;
    private String chartMapGeneratorVerbose = "VERBOSE";
    private ArrayList<String> chartsWithErrors = new ArrayList<>();
    private String envFilename = null;
    private HashSet<String> extensions = new HashSet<>();
    private String formatString = "";
    private String helmCommand;
    private String helmRepositoryCachePath;
    private String indexFilename = null;
    private String localRepoName = null;
    private String logFileName = CHARTMAP_LOG_NAME;
    private Level logLevelVerbose;
    private Logger logger = null;

    private int maxVersions = 1;
    private String outputDirName = System.getProperty("user.dir");
    private boolean verbose = false;
    private String writeBuffer = "";

    protected static final String INTERRUPTED_EXCEPTION = "InterruptedException {} running command %s : %s";
    protected static final int PROCESS_TIMEOUT = 100000;
       
    private static final String CHARTMAP_GENERATOR_LOGGER_NAME = "helm-chartmap-generator-logger";
    private static final String CHARTMAP_LOG_NAME = "helm-chartmap.log";
    private static final boolean CHARTMAP_GENERATE_IMAGE_SWITCH_TRUE = true;
    private static final boolean CHARTMAP_VERBOSE_SWITCH_FALSE = false;
    private static final String LI_END = "</li>\n";

    /**
     * Parses the command line and generates a set of reports
     *
     * @param arg The command line args
     * 
     * @throws ChartMapGeneratorException should any error occur generating the chart reports.
     */
    public static void main(String[] arg) throws ChartMapGeneratorException {
        ChartMapGenerator generator = new ChartMapGenerator();
        try {
            if (generator.prepare(arg)) {
                generator.generate();
            }
        } catch (ChartMapGeneratorException e) {
            if (generator.logger == null) {
                generator.prepareLogger();
            }
            generator.logger.info("ChartMapGeneratorException: {} ", e.getMessage());
        }
    }

    /**
     * Constructor
     *
     * @param repoName       The name of the helm repo from which to generate charts
     * @param outputDirName  The name of the directopry to which the cnart maps will be generated
     * @param fileFormatMask The file format mask controlling which file types get generated
     * @param maxVersions    The maximum number of file versions to generate
     * @param envFilename    The name of a yaml file that contains a set of
     *                       environment variables which may influence the way the
     *                       charts are rendered by helm
     * @param verbose        A flag controlling what output is displayed.

     * @throws ChartMapGeneratorException when an error occurs generating the chart maps
     **/

    public ChartMapGenerator(String repoName, String outputDirName, String fileFormatMask, int maxVersions, String envFilename, boolean verbose)
            throws ChartMapGeneratorException {
        this();
        ArrayList<String> args = new ArrayList<>();
        if (repoName == null) {
            throw new ChartMapGeneratorException("No repository name was provided");
        }
        args.add("-r");
        args.add(repoName);
        if (outputDirName != null) {
            args.add("-o");
            args.add(outputDirName);
        }
        if (fileFormatMask != null) {
            args.add("-f");
            args.add(fileFormatMask);
        }
        args.add("-n");
        args.add(Integer.toString(maxVersions));
        if (envFilename != null) {
            args.add("-e");
            args.add(envFilename);
        }
        if (verbose) {
            args.add("-v");
        }        
        prepare(args.toArray(new String[args.size()]));
    }

     /**
     * Default constructor.
     * 
     */
    private ChartMapGenerator() {
        // do nothing
    }

    /**
     * Parses the command line, prepares the logger, sets the log level, sets
     * the helm environment and prints some introductory details.
     *
     * @param a The command line args
     * @return false if a parse error occurs or help is requested, true otherwise
     */
    private boolean prepare(String[] a) throws ChartMapGeneratorException {
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
            CommandLine cmd = parser.parse(options, a, false);
            if (cmd.hasOption("v")) {
                verbose = true;
            }
            if (cmd.hasOption("o")) {
                outputDirName = cmd.getOptionValue("o");
            }
            if (cmd.hasOption("r")) {
                localRepoName = cmd.getOptionValue("r");
            }
            if (cmd.hasOption("f")) {
                formatString = cmd.getOptionValue("f");
            }
            if (cmd.hasOption("n")) {
                maxVersions = Integer.parseInt(cmd.getOptionValue("n"));
            }
            if (cmd.hasOption("e")) {
                envFilename = cmd.getOptionValue("e");
            } 
            parseFormatString();
            prepareLogger();
            if (a.length == 0
            || cmd.hasOption("h")
            || localRepoName == null) {
                logger.info(ChartMapGenerator.getHelp());
                return false;
            }
            setVerboseLogLevel();
            setHelmEnvironment();
            printIntroduction();
            return true;
        } catch (ParseException e) {
            throw (new ChartMapGeneratorException(e.getMessage()));
        }
    }

    /**
     * Prepares the logger by setting the output log filename.
     * 
     * The logger that is used is different from the one used by ChartMap because by default
     * I want to log all the messages from ChartMap to a file so they can be viewed, if needed,
     * by the user from a link in the generated web page, whereas the ChartMapGenerator messages
     * are written, by default, to the console only. This is all configurable of course using 
     * the log4j2 configuration.
     * 
     */

     private void prepareLogger() {
        String u = System.getProperty("user.dir");
        logFileName = CHARTMAP_LOG_NAME;
        if (!outputDirName.equals(u)) {
            logFileName = outputDirName.concat("/").concat(CHARTMAP_LOG_NAME);
        }
        System.setProperty("chartmap-generator-log-filename", logFileName);
        logger = LogManager.getLogger(CHARTMAP_GENERATOR_LOGGER_NAME);
     }

    /**
     * Prints some details about the helm charts that will be printed.
     * 
     */
    private void printIntroduction () {
        logger.log(logLevelVerbose, "Local helm chart repo \"{}\"", localRepoName);
        logger.log(logLevelVerbose, "Format string \"{}\"", formatString);
        logger.log(logLevelVerbose, "Files will be written to directory \"{}\".", outputDirName);
        logger.log(logLevelVerbose, "Errors reported from ChartMap will be logged in the file \"{}\".", logFileName);
        if (maxVersions == 1) {
            logger.log(logLevelVerbose, "Only one version of each chart will be printed.");
        }
        else {
            logger.log(logLevelVerbose, "A maximum of {} versions of each chart will be printed.", maxVersions);
        }
        if (envFilename != null) {
            logger.log(logLevelVerbose, "Environment file \"{}\"", envFilename);
        }
    }

    /**
     * Using a local chart repo, prints each chart, keeping a count along the way.
     * 
     * @throws ChartMapGeneratorException should an IOException occur generating the chart.
     *
     */
    public void generate() throws ChartMapGeneratorException {
        String localRepoFilename = getHelmRepositoryCachePath().concat(File.separator).concat(localRepoName).concat("-index.yaml");
        logger.log(logLevelVerbose,"Loading charts from {}", localRepoFilename);
        HelmChartLocalCache cache;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            cache = mapper.readValue(new File(localRepoFilename), HelmChartLocalCache.class);
            Map<String, HelmChart[]> entries = cache.getEntries();
            logger.log(logLevelVerbose,"Number of charts found in local helm repo {}: {}", localRepoName, entries.size());
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
        } catch (IOException e) {
            logger.error("Exception: {}",e.getMessage());
            throw new ChartMapGeneratorException(String.format("Error generating charts from repo %s", localRepoFilename));
        }
    }

    /**
     * Prints some help.
     *
     * @return a string containing some help
     */
    public static String getHelp() {
        return "\nUsage:\n\n"
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
    }

    /**
     * Prints a stanza that shows a chart name and links to all the ChartMaps produced from the chart using the ChartMap class.
     *
     * Note that the printChart(h, e, r) method will first attempt to print the chart with refresh set to false.  If
     * that fails, it tries again with the refresh fhag set to true. If that fails no further attempts are made to print
     * that chart. But also note that if the refresh option was set to true and the print succeeded then all the rest of the
     * attempts for that chart for the other extensions will be attempted with the refresh flag set to true. This avoids us 
     * needing to rediscover for each extension that refresh was needed.
     * 
     * @param h helm chart
     * 
     */
    protected void printChart(HelmChart h) {
        try {
            startStanzaInIndex(h.getNameFull());
            boolean r = false;
            for (String e : extensions) {
                r = printChart(h, e, r);
            }
            closeStanzaInIndex();
            Files.write(Paths.get(indexFilename),
               writeBuffer.getBytes(),
               StandardOpenOption.APPEND);
            chartCountGood++;
            String s = r?" with the refresh option":"";
            logger.info("Printed chart: {} {}", h.getNameFull(), s);
        }
        catch (ChartMapGeneratorException | IOException e) {
            chartCountBad++;
            chartsWithErrors.add(h.getNameFull());
            logger.info("{} could not be printed. See helm-chartmap.log for details", h.getNameFull());
        }
        writeBuffer = "";
        chartCount++;
    }    

    /**
     * Prints a chart of a specific format using the ChartMap API.
     * 
     * First, an attempt is made to print the chart without the refresh flag. 
     * 
     * If that fails, then an attempt is made to print the chart with the 
     * refresh flag set.  
     * 
     * @param h helm chart
     * @param e the file extension to use
     * @param r refresh option 
     * @return the refresh option used to print the chart
     * @throws ChartMapGeneratorException if ChartMap throws a ChartMapException generating the chart
     */
    protected Boolean printChart(HelmChart h, String e, Boolean r) throws ChartMapGeneratorException {
        String filename = h.getName().concat("-").concat(h.getVersion()).concat(e);
        boolean refresh = r;
        try {
            printChartMap(h.getNameFull(), filename, refresh);
        } catch (ChartMapException e1) {
            try {
                if (!refresh) {
                    refresh = true;
                    printChartMap(h.getNameFull(), filename, refresh);
                }
            } catch (ChartMapException x) {
                LogManager.getLogger().error("Chart {} failed to print even with the refresh option", h.getNameFull());
                throw new ChartMapGeneratorException(x.getMessage());
            }
        }
        addChartToIndex(filename);
        return refresh;
    }

    /**
     * Prints a Chart Map.
     * 
     * @param c the name of the chart from which the Chart Map will be created
     * @param f the file name to whichh the Chart Map should be printed
     * @param r the refresh option to use
     * @throws ChartMapException if the Chart Map could not be printed
     */
    void printChartMap(String c, String f, boolean r) throws ChartMapException {
        ChartMap cm = getChartMap(
                ChartOption.CHARTNAME,
                c,
                outputDirName.concat(File.separator).concat(f),
                envFilename,
                new boolean[] { CHARTMAP_GENERATE_IMAGE_SWITCH_TRUE, r,
                        CHARTMAP_VERBOSE_SWITCH_FALSE });
        cm.print();
    }

    /**
     * Returns an instance of a ChartMap. The reason for this method is to allow injection of 
     * a spy for complete test coverage.
     * 
     * @param o chart option
     * @param h helm chart name
     * @param f file name to write the chartmap
     * @param e env spec filename
     * @param s switches
     * @return a ChartMap
     * @throws ChartMapException when the ChartMap could not be created
     */
    protected ChartMap getChartMap(ChartOption o, String h, String f, String e, boolean[] s) throws ChartMapException  {
        return new ChartMap(o, h, f, e, s);
    }

    /**
     * Creates the index file and adds a head element.
     * 
     * @throws ChartMapGeneratorException when an IOException occurs creating the index file.
     */
    protected void createIndex() throws ChartMapGeneratorException {
        indexFilename = outputDirName.concat("/index.html");
        String h = "<!DOCTYPE HTML>\n<html lang=\"en\">\n";
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
     * Adds head elements to the index file and adds a H1 tag, then creates css file.
     * 
     * @throws IOException if an IO error occurs adding the head
     */
    protected void addHead () throws IOException {
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
     * Creates a css style file.  If the file already exists, the file is left as-is.
     *
     * @throws IOException if an IO error occurs creating the css style file
     */
    protected void createStyleFile () throws IOException {
        String s = "* {\n\tfont-family: \"Andale Mono\";\n\tcolor: #66ff00;\n\tbackground-color: #000000;\n}\n"
                .concat("a:visited {\n\tcolor: #00FFFF;\n}\n")
                .concat(".header {").concat("\n\tfont: 30px;\n}\n")
                .concat(".charterror {").concat("\n\tfont: 16px;\n\tcolor: #FF0000;\n}\n")
                .concat(".chartname  {").concat("\n\tfont-weight: bold;\n\tfont: 16px;\n}\n")
                .concat(".chartlink  {").concat("\n\tfont: 16px;\n}\n");
        Files.write(Paths.get(outputDirName.concat("/style.css")),
                s.getBytes(),
                StandardOpenOption.CREATE);
    }

    /**
     * Adds an opening stanza to the write buffer.
     * 
     * @param s the name of the chart
     */
    protected void startStanzaInIndex(String s) {
        writeBuffer = writeBuffer.concat("\t<p class=\"chartname\">".concat(s).concat("</p>\n\t<ul>\n"));
    }

    /**
     * Adds a closing stanza to the write buffer.
     */
    protected void closeStanzaInIndex() {
        writeBuffer = writeBuffer.concat("\t</ul>\n");
    }

    /**
     * Adds a single chart filename to the write buffer.  If the file is a PlantUML file, also adds the png file to the buffer.
     *
     * @param f helm chart filename
     */
    protected void addChartToIndex(String f) {
        writeBuffer = writeBuffer.concat("\t\t<li class=\"chartlink\">").concat("<a href=\"./").concat(f).concat("\">").concat(f).concat("</a>").concat(LI_END);
        if (f.endsWith(".puml")) {
            String p = f.replace("puml", "png");
            writeBuffer = writeBuffer.concat("\t\t<li class=\"chartlink\">").concat("<a href=\"./").concat(p).concat("\">").concat(p).concat("</a>").concat(LI_END);
        }
    }

    /**
     * @return a format for date and time
     */
    private static String getCurrentDateTime() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return (f.format(LocalDateTime.now()));
    }
    /**
     * Adds end elements to the index file.
     *
     * @throws IOException if an IO error occurs adding the footer
     */
    protected void addFooterToIndex() throws IOException {
        String s = getDetails();
        s = s.concat("<hr>Generated on ")
                .concat(getCurrentDateTime())
                .concat(" by ")
                .concat(ChartMapGenerator.class.getCanonicalName())
                .concat(" ")
                .concat("<a href=\"https://github.com/melahn/helm-chartmap-generator\">https://github.com/melahn/helm-chartmap-generator</a>");
        Files.write(Paths.get(indexFilename),
                s.getBytes(),
                StandardOpenOption.APPEND);
    }

    /**
     * Adds more details about the charts that were processed.
     * 
     * @return the string with details if verbose is set, otherwise an empty string
     */
    protected String getDetails() {
        String s = "";
        if (verbose) {
            s = s.concat(String.format("<hr><p>Charts successfully processed: %s</p>%n", chartCountGood))
                    .concat(String.format("<p>Charts with errors: %s</p>%n", chartCountBad))
                    .concat(String.format("<p>Total charts processed: %s</p>%n%n", chartCount));
            if (chartCountBad > 0) {
                s = s.concat(
                        "<p>Here is a list of the charts with errors. Consult the <a href=\"./helm-chartmap.log\">output log</a> to see the error messages logged by ChartMap.</p>\n\t<ul>\n");
                for (String c : chartsWithErrors) {
                    s = s.concat("\t\t<li class=\"charterror\">");
                    s = s.concat(c).concat(LI_END);
                }
                s = s.concat("\t</ul>\n");
            }
        }
        return s;
    }

    /**
     * Adds closing elements to the index file.
     * 
     * @throws IOException if an IO error occurs ending the index.
     */
    protected void endIndex () throws IOException {
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
        String f = formatString.toLowerCase();
        if (f.indexOf('j') >= 0) {
            extensions.add(".json");
        }
        if (f.indexOf('p') >= 0) {
            extensions.add(".puml");
        }
        if (f.indexOf('t') >= 0 || extensions.isEmpty()) {
            extensions.add(".txt");
        }
    }

    /**
     * Gets the environment variable. Using my own function for this allows testing
     * of different environment variable values using mocks and spies.  
     * 
     * @param e The name of the variable to fetch
     * @return The value of the variable or null
     */
    protected String getEnv(String e) {
       return System.getenv(e);
    }

    /**
     * Sets the helm information, include the helm command, version and paths.
     * 
     * @throws ChartMapGeneratorException if any of the helm information cannot be set
     */
    void setHelmEnvironment() throws ChartMapGeneratorException {
        helmCommand = getHelmCommand();
        checkHelmVersion();
        getHelmClientInformation();
    }

    /**
     * Return the helm command to use, giving priority to the value of HELM_BIN if
     * set explictly. Setting HELM_BIN explicitly is arguably more secure since the
     * user does not need to worry then about some evil helm command in the PATH,
     * though it does then rely on the value of HELM_BIN itself being secure.
     * 
     * @return the helm command
     */
    protected String getHelmCommand() {
        String helmBin = getEnv("HELM_BIN");
        String helmCommandResolved = getEnv("HELM_BIN") == null ? "helm" : helmBin;
        logger.info("The helm command \'{}\' will be used to discover helm path information.", helmCommandResolved);
        return helmCommandResolved;
    }

    /**
     * Gets the major version of the helm client and sets helmMajorVersionUsed.
     * 
     * The helm version command offers templated output using go template syntax but
     * the values were not designed to be forward or backward compatible (!) hence
     * the tortured logic here
     * 
     * @throws ChartMapGeneratorException if a version other than V3 is found
     */
    protected void checkHelmVersion() throws ChartMapGeneratorException {
        String[] c = { helmCommand,  "version", "--template", "{{ .Version }}" };
        try {
            Process p = getProcess(c);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor(PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
            int exitValue = p.exitValue();
            if (exitValue == 0) {
                String o = br.readLine();
                if (o != null && o.length() > 1 && o.charAt(1) == '3') {
                    return;
                }
                throw new ChartMapGeneratorException(
                        "Unsupported Helm Version. Please upgrade to helm V3 or use a previous version of ChartMapGenerator.");
            } else { // we could not even execute the helm command
                throw new ChartMapGeneratorException("Error Code: " + exitValue + " executing command " + c[0]
                        + c[1] + c[2] + c[3]);
            }
        } catch (IOException e) {
            // we could not get the output of the helm command
            throw new ChartMapGeneratorException(
                    String.format("Exception trying to discover Helm Version: %s ", e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChartMapGeneratorException(String.format(INTERRUPTED_EXCEPTION, helmCommand, e.getMessage()));
        }
    }

    /**
     * Finds the helm client environment information.
     * 
     * @throws ChartMapGeneratorException if an error occurs finding the client information                   
     */
    protected void getHelmClientInformation() throws ChartMapGeneratorException {
        // run the helm env command to get the client information
        try {
            File tempEnvFile = getTempFile("helm-chartmap-env", ".txt");  
            // Guard against a vulnerability in the temp file by restricting to owner permissions
            // Note: Not using Posix permissions here for reasons of Windows portability
            boolean br = tempEnvFile.setReadable(true, true);
            boolean bw = tempEnvFile.setWritable(true, true);
            boolean be = tempEnvFile.setExecutable(true, true);
            if (!br || !bw || !be) {
                throw new ChartMapGeneratorException(String.format("Failure to set permissions on %s: r=%b, w=%b, e=%b ", tempEnvFile.toString(), br, bw, be));
            }
            ProcessBuilder pb = getProcessBuilder(helmCommand, "env");
            pb.redirectOutput(tempEnvFile);
            Process p = pb.start();
            p.waitFor(PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
            int exitValue = p.exitValue();
            if (exitValue == 0) {
                FileReader fileReader = new FileReader(tempEnvFile);
                // read the file and extract the useful information
                String line = null;
                try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                    line = bufferedReader.readLine();
                    while (line != null) {
                        extractHelmClientInformation(line);
                        line = bufferedReader.readLine();
                    }
                }
            } else {
                throw new ChartMapGeneratorException(
                        String.format("Error Code: %c executing helm env command ", exitValue));
            }
        } catch (IOException e) {
            throw (new ChartMapGeneratorException("IOException executing helm env command"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw (new ChartMapGeneratorException("IOException executing helm env command"));
        }
    }

    /**
     * Creates a temporary file and returns that as a File solely for
     * the purpose of testing exception conditions in the automated tests. 
     * 
     * @param p prefix to use
     * @param s suffix to use
     * @return a new temp File
     * @throws IOException when an error occurs creating the temp file
     */
    protected File getTempFile(String p, String s) throws IOException {
        File f = File.createTempFile(p, s);
        f.deleteOnExit();
        return f;
    }

    /**
     * Extracts the relevant helm path from the string.  Note that
     * the values returned in a line returned by the helm env command 
     * are enclosed in double quotes so these double quotes
     * must be stripped out so as not to cause problems later 
     * when the values are referenced.
     * 
     * @param l a line containing some helm client information
     *  in format name=\"value\"
     */
    private void extractHelmClientInformation(String l) {
        String[] a = l.split("=");
        if (a[0].equals("HELM_REPOSITORY_CACHE")) {
            setHelmRepositoryCachePath(a[1].substring(1,a[1].length()-1));
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
        if (isVerbose()) {
            logLevelVerbose = Level.forName(chartMapGeneratorVerbose, 350); // higher priority than INFO
        } else {
            logLevelVerbose = Level.forName(chartMapGeneratorVerbose, 450); // lower priority than INFO
        }
    }

    /**
     * 
     * This method was introduced to allow providing a test version of a Process so
     * as to return non zero exit codes for testing.
     * 
     * @param c the command and its parameters
     * @return a Process
     * @throws IOException if an error occurs getting the process
     */
    public Process getProcess(String[] c) throws IOException {
        return Runtime.getRuntime().exec(c, null);
    }

    /**
     * 
     * This method was introduced to allow providing a test version of a ProcessBuilder so
     * as to return non zero exit codes for testing.
     * 
     * @param c the command 
     * @param a the arg 
     * @return a ProcessBuilder
     * @throws IOException if an error occurs getting the process
     */
    public ProcessBuilder getProcessBuilder(String c, String a) throws IOException {
        return new ProcessBuilder(c, a);
    }

    public String getHelmRepositoryCachePath() {
      return helmRepositoryCachePath;
    }

    protected void setHelmRepositoryCachePath(String s) {
        helmRepositoryCachePath = s;
    }

    public boolean isVerbose() {
        return verbose;
    }

    int getChartCount() {
        return chartCount;
    }

    int getChartCountBad() {
        return chartCountBad;
    }

    int getChartCountGood() {
        return chartCountGood;
    }
}
