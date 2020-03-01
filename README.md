# helm-chartmap-generator

## Overview
This project generates a set of helm dependency reports in various formats for a given helm chart repository.  It uses
the [ChartMap project](https://github.com/melahn/helm-chartmap) to generate the reports.  

The generated reports can be in various formats including
* JSON which can be viewed using [helm-inspector](https://github.com/melahn/helm-inspector)  
* PlantUML which can be turned into images using [PlantUML](https://plantuml.com/)
* Text which can of course be viewed using any text editor

## Prerequisites

* Java 8 or later.

## Usage

1. Download the executable jar from the [resource directory](./resource/jar), or build it yourself from source (see below).

2. Run the command line.  See Syntax and Examples below.

### Command Line Syntax
```
java -jar helm-chartmap-generator-1.0-SNAPSHOT.jar

Flags:
	-r	<repo name>		the name of the local helm repo to use (required)
	-o	<directory name>	the output directory to use (default <pwd>) (optional)
	-f	<file format mask>	a string containing any combination of 'j','p' and 't' (default 't') (optional)
	-n	<integer>		the maximum number of chart versions to print (default 1) (optional)
	-e	<file name>		environment file (optional)
	-v	verbose			print verbose output (optional)
	-h	help			provide help (optional)
```
#### Parameters
* **Required**
   * **-r** \<repo name\>
     * The name of the local helm repo to use.  Run 'helm repo list' to see the names of the local helm repos and use any one
     of them here.  A set of one or more reports will be generated for all the charts in that repo.  
* **Optional**
   * **-f** \<file format mask\>
      *  A string that is formed from the characters 'j','p' and 't' in any order or frequency.  If the mask contains the character 'j' a JSON
      file will be generated.  If the mask contains the character 'p' a PlantUML file will be generated. If the mask contains the character 't' a text file will be generated.  For example. if the file format mask contains 'jt', then
      a JSON file report and a text file report will be generated for each chart found in the repo.  The default is 't'.
   * **-n** \<integer\>
      *  The maximum number of versions of a chart to print.  For example if there are 8 versions of a given chart in the chart repo and you
      specify a value of 2, then reports for only the 2 most recent versions will be printed.  The default is 1.
   * **-e** \<filename\>
      *  The location of an Environment Specification which is a yaml file containing a list of environment variables to set before rendering helm templates.
      See the example environment specification provided in resource/example-env-spec.yaml to understand the format.   The default is
      that no environment specification will be used.
   * **-v**
      * Verbose.  If specified, some extra command line output is shown.
   * **-h**
      * Help.  Whenever specified, any other parameters are ignored.  When no parameters are specified, **-h** is assumed.

#### Example Commands
    
##### Generating reports from the incubator repo using all the detaults
```
    java -helm-chartmap-generator-1.0-SNAPSHOT.jar -r incubator 
``` 
     
##### Generating JSON and PlantUML reports from the stable repo, with up to 3 versions printed with verbose output
```
    java -helm-chartmap-generator-1.0-SNAPSHOT.jar -r stable -f pt -n 3 -v 
``` 
### Issues
If you find any problems please open an [issue](https://github.com/melahn/helm-chartmap-generator/issues).

### License
MIT

     

