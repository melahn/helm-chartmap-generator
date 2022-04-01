# helm-chartmap-generator

[![GitHub last commit](https://img.shields.io/github/last-commit/melahn/helm-chartmap-generator)](https://github.com/melahn/helm-chartmap-generator/commit/master)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/melahn/helm-chartmap-generator)](https://github.com/melahn/helm-chartmap-generator/releases/tag/v1.1.0)
[![GitHub Release Date](https://img.shields.io/github/release-date/melahn/helm-chartmap-generator)](https://github.com/melahn/helm-chartmap-generator/releases/tag/v1.1.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.melahn/helm-chartmap-generator)](https://search.maven.org/artifact/com.melahn/helm-chartmap-generator/1.1.0/jar)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/
[![Sonar Cloud Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=helm-chartmap-generator&metric=alert_status)](https://sonarcloud.io/dashboard?id=helm-chartmap-generator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=helm-chartmap-generator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=helm-chartmap-generator)

## Overview

This project generates a set of helm dependency reports in various formats for a given helm chart repository.  It uses
the [ChartMap project](https://github.com/melahn/helm-chartmap) to generate the reports.  

The generated reports can be in various formats including

* JSON which can be viewed using [helm-inspector](https://github.com/melahn/helm-inspector)  
* PlantUML which are then turned into images using [PlantUML](https://plantuml.com/)
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

2. Run the command line.  See Syntax and Examples below.

### Command Line Syntax

``` java
java -jar helm-chartmap-generator-1.1.0-SNAPSHOT.jar

Flags:
   -r <repo name>  the name of the local helm repo to use (required)
   -o <directory name>  the output directory to use (default <pwd>) (optional)
   -f <file format mask> a string containing any combination of 'j','p' and 't' (default 't') (optional)
   -n <integer>  the maximum number of chart versions to print (default 1) (optional)
   -e <file name>  environment file (optional)
   -v verbose   print verbose output (optional)
   -h help   provide help (optional)
```

#### Flags

* **Required**
  * **-r** \<repo name\>
    * The name of the local helm repo to use.  Run 'helm repo list' to see the names of the local helm repos and use any one of them here.  A set of one or more reports will be generated for all the charts in that repo.  
* **Optional**
  * **-o** \<directory name\>
    * The name of directory to which the files will be written.  The default is the current working directory.
  * **-f** \<file format mask\>
    * A string that is formed from the characters 'j','p' and 't' in any order or frequency.  If the mask contains the character 'j' a JSON file will be generated.  If the mask contains the character 'p' a PlantUML file will be generated. If the mask contains the character 't' a text file will be generated.  For example. if the file format mask contains 'jt', then a JSON file report and a text file report will be generated for each chart found in the repo.  The default is 't'.
  * **-n** \<integer\>
    * The maximum number of versions of a chart to print.  For example if there are 8 versions of a given chart in the chart repo and you specify a value of 2, then reports for only the 2 most recent versions will be printed.  The default is 1.
  * **-e** \<filename\>
    * The location of an Environment Specification which is a yaml file containing a list of environment variables to set before rendering helm templates. See the example environment specification provided in resource/example-env-spec.yaml to understand the format.   The default is that no environment specification will be used.
  * **-v**
    * Verbose.  If specified, some extra command line output is shown.
  * **-h**
    * Help.  Whenever specified, any other parameters are ignored.  When no parameters are specified, **-h** is assumed.

#### Example Commands

##### Generating reports from the incubator repo using all the detaults

``` java
    java -helm-chartmap-generator-1.1.0-SNAPSHOT.jar -r incubator 
```

##### Generating JSON and PlantUML reports from the stable repo, with up to 3 versions printed with verbose output

``` java
    java -helm-chartmap-generator-1.1.0-SNAPSHOT.jar -r stable -f pt -n 3 -v 
```

### Notes

When using PlantUML files, the images that are then produced from those files will look best when the following
system environment variable variable is set

``` java
PLANTUML_LIMIT_SIZE 8192
```

### Issues

If you find any problems please open an [issue](https://github.com/melahn/helm-chartmap-generator/issues).

### License

MIT
