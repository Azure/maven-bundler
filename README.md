
# Maven Bundler Plugin

The project structure released to Maven Central should be 

```
myproject-0.0.1.pom
myproject-0.0.1.jar
myproject-0.0.1-sources.jar
myproject-0.0.1-javadoc.jar
```

This plugin aims to provide integration with Maven release plugin and to bundle these artifacts in `azuresdkpartnerdrops` storage account.

## How to use

### One time set ups

1. Request access to the [Azure SDK Partners](https://idweb/identitymanagement/aspx/groups/MyGroups.aspx?popupFromClipboard=https%3A%2F%2Fidweb%2Fidentitymanagement%2Faspx%2FGroups%2FEditGroup.aspx%3Fid%3D319a0a39-4c37-4791-ada5-2b390fedac52) security group. 
2. Follow [this documentation](https://dev.azure.com/azure-sdk/internal/_wiki/wikis/internal.wiki?wikiVersion=GBwikiMaster&pagePath=%2FPartner%20Release%20Pipelines&pageId=1) to find the access key to storage account `azuresdkpartnerdrops` in the key vault.
3. Add this snippet to your maven settings.xml:

```xml
<server>
  <id>azuresdkpartnerdrops</id>
  <username>azuresdkpartnerdrops</username>
  <password>${access key from the key vault secret}</password>
</server>
```

4. Add `bundler.properties` to your project:
```bash
team=storage
product=asyncv10
exclude=.*-tests.jar,azure-samples.*
```

where `exclude` is a comma separated string containing exact matches or regex matches of built files to exclude from publishing to Maven. You can also provide these on the commandline arguments documented below.

5. Commit your changes and make sure your local Git repo is clean. Then run

```bash
mvn com.microsoft.azure:bundler-maven-plugin:auto
```

if you want to release the version as is defined in `pom.xml`, or

```bash
mvn com.microsoft.azure:bundler-maven-plugin:auto -Dversion={release-version} -DdevVersion={next-dev-version}
```

if your version has `-SNAPSHOT` and you want the bundler to run maven release plugin for you.

6. The previous job will print the `blobPath` in the console. Paste that into the [azuresdkpartners to maven](https://dev.azure.com/azure-sdk/internal/_release?definitionId=6) pipeline and start the job.

The individual goals are listed below.

### Goal: prepare

To prepare the release for a SNAPSHOT project with 2 maven release plugin commits, run
```bash
mvn com.microsoft.azure:bundler-maven-plugin:prepare
```
Argument properties must be appended in `-Dargument=value` format.

| Property | Description |
|----------|-------------|
| `version` | Required if `versionConfig` is not provided. All modules in the project will have this release version. |
| `devVersion` | Required if `versionConfig` is not provided. All modules in the project will have this as the next development version. Usually ends with `-SNAPSHOT`. |
| `versionConfig` | Required if different child modules in this project have different versions. Must be pointing to a comma separated file in the line format of `groupId,artifactId,releaseVersion,nextDevelopmentVersion`. |
 
Examples:
```bash
mvn com.microsoft.azure:bundler-maven-plugin:prepare -DversionConfig=`pwd`/versionConfig.csv
```
Will prepare the release with versions defined in `versionConfig.csv` and release tag `v1.7.1`. 2 Commits with messages "[maven-release-plugin] prepare release v1.7.1" and "[maven-release-plugin] prepare for next development iteration" will be generated.

```bash
mvn com.microsoft.azure:bundler-maven-plugin:prepare -Dversion=1.3.1 -DdevVersion=1.3.2-SNAPSHOT
```
Will prepare the release with versions 1.3.1 and next development version 1.3.2-SNAPSHOT. Release tag will be `v1.3.1`. 2 Commits with messages "[maven-release-plugin] prepare release v1.3.1" and "[maven-release-plugin] prepare for next development iteration" will be generated.

### Goal: bundle

Whether you are doing a snapshot release or an official Maven Central release, you can run

```bash
mvn clean source:jar javadoc:jar package -DskipTests // or your own build command
mvn com.microsoft.azure:bundler-maven-plugin:bundle -Dteam=fluent -Dproduct=network-2018-12-01
```

Bundler will collect all the pom files and jar files and upload to `azuresdkpartnerdrops` storage account, excluding the files specified.

Argument properties may be appended in `-Dargument=value` format.

| Property | Description |
|----------|-------------|
| `team` | **Required.** The name of the team for indexing purpose. Must be provided on the commandline or in `bundler.properties`. Commandline argument overrides value defined in `bundler.properties`.|
| `product` | **Required.** The name of the product for indexing purpose. Must be provided on the commandline or in `bundler.properties`. Commandline argument overrides value defined in `bundler.properties`. |
| `exclude` | *Optional.* A comma separated string containing exact matches or regex matches of built files to exclude from publishing to Maven. Can be provided on the commandline or in `bundler.properties`. Commandline argument overrides value defined in `bundler.properties`. |

### Goal: auto

Whether you are doing a snapshot release or an official Maven Central release, you can run

```bash
mvn com.microsoft.azure:bundler-maven-plugin:auto -Dteam=fluent -Dproduct=network-2018-12-01
```

Bundler will run `prepare`, a customizable `buildCmd`, and then `bundle`.

Argument properties may be appended in `-Dargument=value` format.

| Property | Description |
|----------|-------------|
| `team` | **Required.** The name of the team for indexing purpose. Must be provided on the commandline or in `bundler.properties`. Commandline argument overrides value defined in `bundler.properties`.|
| `product` | **Required.** The name of the product for indexing purpose. Must be provided on the commandline or in `bundler.properties`. Commandline argument overrides value defined in `bundler.properties`. |
| `exclude` | *Optional.* A comma separated string containing exact matches or regex matches of built files to exclude from publishing to Maven. Can be provided on the commandline or in `bundler.properties`. Commandline argument overrides value defined in `bundler.properties`. |
| `buildCmd` | *Optional.* A build command for Maven to build all the artifacts. Default value is `clean source:jar javadoc:jar package -DskipTests`. Can be provided on the commandline or in `bundler.properties`. Commandline argument overrides value defined in `bundler.properties`. |

## Legacy

### Goal: legacyBundle

Whether you are doing a snapshot release or an official Maven Central release, you can run

```bash
mvn clean source:jar javadoc:jar package -DskipTests
mvn com.microsoft.azure:bundler-maven-plugin:legacyBundle
```

Argument properties may be appended in `-Dargument=value` format.

| Property | Description |
|----------|-------------|
| `dest` | *Optional.* The output folder for bundled artifacts. Default to `output` directory in the command execution directory. |

### Goal : legacyAuto

To start an official release with Maven release plugin and bundle them together in one step, run `legacyAuto` goal. All arguments to both `prepare` and `bundle` goals are accepted. Between `prepare` and `bundle`, this command will checkout the release commit (through `git checkout HEAD~1`) and reset to head (through `git checkout -`) after bundling. Goal `prepare` will only be run for SNAPSHOT projects.

### (Deleted) Goal : stage

For developers publishing to group IDs `com.microsoft.azure` or `com.microsoft.rest`, this goal is connected to the Jenkins server https://azuresdkci.cloudapp.net/ to easily run staging jobs. You will be prompted to enter your Jenkins user ID and API token, which can be fetched here: http://azuresdkci.cloudapp.net/me/configure.

```bash
mvn com.microsoft.azure:bundler-maven-plugin:stage
```

Argument properties must be appended in `-Dargument=value` format.

| Property | Description |
|----------|-------------|
| `source` | **Required.** The source network location where artifacts are. |
| `groupId` | **Required.** The group ID, starting with either `com.microsoft.azure` or `com.microsoft.rest`. |

Examples:
```bash
mvn com.microsoft.azure:bundler-maven-plugin:stage -Dsource=\\\\scratch2\\scratch\\jianghlu\\release-100 -DgroupId=com.microsoft.azure
```
Will run the Jenkins job with location parameter set to `\\scratch2\scratch\jianghlu\release-100\`.

## Next steps
Ideally we should be able to run `stage` goal without the dependency on Jenkins.

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
