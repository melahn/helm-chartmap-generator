# helm-chartmap-generator

[![GitHub last commit](https://img.shields.io/github/last-commit/melahn/helm-chartmap-generator)](https://github.com/melahn/helm-chartmap-generator/commit/master)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/melahn/helm-chartmap-generator)](https://github.com/melahn/helm-chartmap-generator/releases/tag/v1.1.0)
[![GitHub Release Date](https://img.shields.io/github/release-date/melahn/helm-chartmap-generator)](https://github.com/melahn/helm-chartmap-generator/releases/tag/v1.1.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.melahn/helm-chartmap-generator)](https://search.maven.org/artifact/com.melahn/helm-chartmap-generator/1.1.0/jar)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/)
[![Sonar Cloud Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=helm-chartmap-generator&metric=alert_status)](https://sonarcloud.io/dashboard?id=helm-chartmap-generator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=helm-chartmap-generator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=helm-chartmap-generator)

## Overview

This project generates a set of helm dependency reports in various formats for a given helm chart repository. It uses
the [ChartMap project](https://github.com/melahn/helm-chartmap) to generate the reports.

The generated reports can be in various formats including

* JSON which can be viewed using [helm-inspector](https://github.com/melahn/helm-inspector)  
* PlantUML which are also then turned into images using [PlantUML](https://plantuml.com/)
* Text which can of course be viewed using any text editor

In addition to producing the dependency report files, an 'index.html' file is produced that lists the files
and provided links to view them.

You can see lots of examples here -> (<https://melahn.github.io/helm-chartmap-generator/>)

## Maven Central

The jar file, along with source and javadoc, is available from **Maven Central**.

``` xml
<dependency>
  <groupId>com.melahn</groupId>
  <artifactId>helm-chartmap-generator</artifactId>
  <version>1.0.2</version>
</dependency>
```

## Prerequisites

* Java 8 or later.

## Usage

1. Download the stable release from [Maven Central](https://oss.sonatype.org/service/local/repositories/releases/content/com/melahn/helm-chartmap-generator/1.0.2/helm-chartmap-generator-1.0.2.jar)
or build the latest yourself from source (see below).

2. Run the command line. See Syntax and Examples below.

### Command Line Syntax

``` java
java -jar helm-chartmap-generator-1.1.0-SNAPSHOT.jar

Flags:
   -r <repo name>  
   -o <directory name>  
   -f <file format mask> 
   -n <integer>  
   -e <file name> 
   -v verbose   
   -h help 
 ```

#### Flags

* **Required**
  * **-r** \<repo name\>
    * The name of the local helm repo to use. Run *helm repo list* to see the names of the local helm repos and use any one of them here. A set of one or more reports will be generated for all the charts in that repo.
* **Optional**
  * **-o** \<directory name\>
    * The name of directory to which the files will be written. The default is the current working directory.
  * **-f** \<file format mask\>
    * A string that is formed from the characters 'j','p' and 't' in any case, order or frequency. If the mask contains the character 'j' a JSON file will be generated. If the mask contains the character 'p' a PlantUML file will be generated. If the mask contains the character 't' a text file will be generated. For example. if the file format mask contains 'jt', then a JSON file report and a text file report will be generated for each chart found in the repo. If 'p' is specified a png image file will also be generated from the PlantUML file. If not specifed or an empty string is provided, then 't' is assumed.
  * **-n** \<integer\>
    * The maximum number of versions of a chart to print. For example if there are 8 versions of a given chart in the chart repo and you specify a value of 2, then reports for only the 2 most recent versions will be printed. The default is 1.
  * **-e** \<filename\>
    * The location of an Environment Specification which is a yaml file containing a list of environment variables to set before rendering helm templates. See the example environment specification provided in resource/example-env-spec.yaml to understand the format. The default is that no environment specification will be used.
  * **-v**
    * Verbose. If specified, some extra output is shown on the command line and in the index file.
  * **-h**
    * Help. Whenever specified, any other parameters are ignored. When no parameters are specified, *-h* is assumed.

#### Example Commands

##### Generating reports from the incubator repo using all the detaults

``` java
    java -helm-chartmap-generator-1.1.0-SNAPSHOT.jar -r incubator 
```

##### Generating JSON and PlantUML reports from the stable repo, with up to 3 versions printed with verbose output

``` java
    java -helm-chartmap-generator-1.1.0-SNAPSHOT.jar -r stable -f pj -n 3 -v 
```

Note: Since PlantUML reports are generated in this example, corresonding image files will also be produced.

### Java Methods

In addition to the command line interface, a Java API is provided.

#### Constructor

``` java
    public ChartMapGenerator(String repoName,
                    String outputDirName,
                    String fileFormatMask,
                    int maxVersions,
                    String envSpecFilename,
                    boolean verbose)     
```

##### Description of ChartMap constructor

Constructs a new instance of the *com.melahn.util.helm.ChartMapGenerator* class

##### Parameters

* *repoName*
  * The name of the local helm repo to use. Run *helm repo list* to see the names of the local helm repos and use any one of them here. A set of one or more reports will be generated for all the charts in that repo.
* *outputDirName*
  * The location in the file system to which the Chart Maps will be written. If null, then the
  * value of the System property *user.dir* will be used.
* *fileFormatMask*
  * A string that is formed from the characters 'j','p' and 't' in any case, order or frequency. If the mask contains the character 'j' a JSON file will be generated. If the mask contains the character 'p' a PlantUML file will be generated. If the mask contains the character 't' a text file will be generated. For example. if the file format mask contains 'jt', then a JSON file report and a text file report will be generated for each chart found in the repo. If 'p' is specified a png image file will also be generated from the PlantUML file. If the *fileFormatmask* is null or empty, then 't' is assumed.
* *maxVersions*
  * The maximum number of versions of a chart to print. For example if there are 8 versions of a given chart in the chart repo and you specify a value of 2, then reports for only the 2 most recent versions will be generated.
* *envSpecFilename*
  * The location of an Environment Specification which is a yaml file containing a list of environment variables to set before rendering helm templates. See the example environment specification provided in resource/example-env-spec.yaml to understand the format. If null, then no environment specification will be used.
* *verbose*
  * If true, some extra messages are shown.

##### Throws

* *com.melahn.util.helm.ChartMapGeneratorException*

#### generate

##### Description of print command

Generates a set of *ChartMaps* for the helm repo.

``` java

    public void generate ()
                    
```

##### Throws in generate command

* *com.melahn.util.helm.ChartMapGeneratorException*

#### Java Example

``` java
import com.melahn.util.helm.ChartMapGenerator;
import com.melahn.util.helm.ChartMapGeneratorException;

public class ChartMapGeneratorExample {
    public static void generateExampleChartMap(String[] args) {
        try {
            ChartMap testMapGenerator = new ChartMapGenerator(
                    "bitnami",
                    "target/test",
                    "pt",
                    "resource/example/example-env-spec.yaml",
                    2,
                    false);
            testMap.generate();
        } catch (ChartMapGeneratorException e) {
            System.out.println("ChartMapGeneratorException generating chart maps: ".concat(e.getMessage()));
        }
    }
}
```

More examples illustrating the use of the Java interface can be found in [ChartMapGeneratorTest.java](./src/test/java/com/melahn/util/helm/ChartMapGeneratorTest.java).

### PlantUML Notes

When generating PlantUML files, the images that are then produced from those files will have the best layout when the following system environment variable is set before running *helm-chart-generator*.

``` java
PLANTUML_LIMIT_SIZE 8192
```

## Build Notes

### Maven Commands

#### Building the jar from source and running tests

1. git clone this repository
2. run maven

``` maven
mvn clean install 
```

Note: The jar targets Java 8 for the widest compatibility. You can target a different
version of Java by modifying the configuration in the maven-compiler-plugin to use a different target like in the example below.

``` xml
<target>11</target>
```

### Build Warnings

When the shaded jar is built by the maven-shade-plugin there is a warning produced like this...

```text
     target/classes (Is a directory)
```

This warning is due to a long-standing issue where the shade plugin checks if a classpath element is a jar, and if it is not, swallows useful error information.  It instead prints out this meaningless warning.  See <https://issues.apache.org/jira/browse/MSHADE-376> for more details. See <https://github.com/melahn/maven-shade-plugin> if you want to install your own version of the plugin, with a fix that eliminates this warning.

### Issues

If you find any problems please open an [issue](https://github.com/melahn/helm-chartmap-generator/issues).

### License

MIT
