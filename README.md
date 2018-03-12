
# Maven Bundler Plugin

The project structure released to Maven Central should be 

```
myproject-0.0.1.pom
myproject-0.0.1.jar
myproject-0.0.1-sources.jar
myproject-0.0.1-javadoc.jar
```

This tools aims to provide integration with Maven release plugin and to bundle these artifacts in a user designated output directory.

## Usage
Add plugin repository:

```xml
<pluginRepositories>
  <pluginRepository>
    <id>ossrh</id>
    <name>Sonatype Snapshots</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    <layout>default</layout>
    <snapshots>
      <enabled>true</enabled>
      <updatePolicy>always</updatePolicy>
    </snapshots>
  </pluginRepository>
</pluginRepositories>
```

Add plugin:
```xml
<plugins>
  <plugin>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>bundler-maven-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </plugin>
<plugins>
```

### Goal: prepare

To prepare the release with 2 maven release plugin commits, run
```bash
mvn com.microsoft.azure:bundler-maven-plugin:prepare
```
Argument properties must be appended in `-Dargument=value` format.
| Property | Description |
|----------|-------------|
| `tag` | **Required.** The release tag on GitHub. |
| `versionConfig` | Required if different child modules in this project have different versions. Must be pointing to a comma separated file in the line format of `groupId,artifactId,releaseVersion,nextDevelopmentVersion`. |
| `version` | Required if `versionConfig` is not provided. All modules in the project will have this release version. |
| `devVersion` | Required if `versionConfig` is not provided. All modules in the project will have this as the next development version. Usually ends with `-SNAPSHOT`. |
 
Examples:
```bash
mvn com.microsoft.azure:bundler-maven-plugin:prepare -DversionConfig=`pwd`/versionConfig.csv -Dtag=v1.7.1
```
Will prepare the release with versions defined in `versionConfig.csv` and release tag `v1.7.1`. 2 Commits with messages "[maven-release-plugin] prepare release v1.7.1" and "[maven-release-plugin] prepare for next development iteration" will be generated.

```bash
mvn com.microsoft.azure:bundler-maven-plugin:prepare -Dversion=1.3.1 -DdevVersion=1.3.2-SNAPSHOT -Dtag=v1.3.1
```
Will prepare the release with versions 1.3.1 and next development version 1.3.2-SNAPSHOT. Release tag will be `v1.3.1`. 2 Commits with messages "[maven-release-plugin] prepare release v1.3.1" and "[maven-release-plugin] prepare for next development iteration" will be generated.

### Goal: bundle

Whether you are doing a snapshot release or an official Maven Central release, you can run

```bash
mvn clean source:jar javadoc:jar package -DskipTests
mvn com.microsoft.azure:bundler-maven-plugin:bundle
```

Argument properties may be appended in `-Dargument=value` format.
| Property | Description |
|----------|-------------|
| `dest` | *Optional.* The output folder for bundled artifacts. Default to `output` directory in the command execution directory. |

### Goal : auto

To start an official release with Maven release plugin and bundle them together in one step, run `auto` goal. All arguments to both `prepare` and `bundle` goals are accepted. Between `prepare` and `bundle`, this command will checkout the release commit (through `git checkout HEAD~1`) and reset to head (through `git checkout -`) after bundling.

##  Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
