package com.melahn.util.helm;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmChartLocalCache;
import com.melahn.util.helm.model.HelmChartRepoLocal;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.map.MultiKeyMap;

public class ReportGenerator {

    private static MultiKeyMap charts = new MultiKeyMap();;
    private static String localRepoName=null;

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
     * Constructs a helm repo file name from the short name of a helm repo
     *
     * @param   r   A repo name (e.g. stable)
     * @return The fqfn of a local yaml file where helm chart info can be found, null otherwise
     */
    private static String constructRepoFilename(String r) {
        String helm_home = System. getenv("HELM_HOME");
        if (helm_home != null) {
            String repoFileName = localRepoName.concat("/repository/cache/").concat(r).concat("-index.yaml");
            File f = new File(localRepoName);
            if (f.exists()) {
                return repoFileName;
            }
        }
        return null;
    }

    /**
     * Generates a set of reports
     * @throws IOException
     */
    private static void generate() throws IOException {
        String localRepoFilename = constructRepoFilename(localRepoName);
        if (localRepoFilename !=null) {
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
        HelmChartLocalCache cache;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            cache = mapper.readValue(new File(f), HelmChartLocalCache.class);
            Map<String, HelmChart[]> entries = cache.getEntries();
            for (Map.Entry<String, HelmChart[]> entry : entries.entrySet()) {
                for (HelmChart h : entry.getValue()) {
                    charts.put(h.getName(), h.getVersion(), h);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
