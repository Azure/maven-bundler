package com.microsoft.azure.plugins.bundler;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Mojo(name = "auto", aggregator = true)
public class Pipeline extends Preparer {
    @Parameter(property = "properties", defaultValue = "${session.executionRootDirectory}/bundler.properties")
    private String propertiesFile;

    @Parameter(property = "team")
    private String team;

    @Parameter(property = "product")
    private String product;

    @Parameter(property = "exclude")
    private String excludedFiles;

    @Parameter(property = "buildCmd")
    private String buildCmd;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        boolean isSnapshot = project().getVersion().endsWith("-SNAPSHOT");

        CommandRunner runner = new CommandRunner(this, super.session());
        if (isSnapshot) {
            // Prepare
            super.execute();

            // Checkout HEAD~1
            runner.runCommand("git checkout HEAD~1");
        }

        Set<String> groupIds = new HashSet<>();

        System.out.println("Property file is " + propertiesFile);

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
            if (team == null) {
                team = properties.getProperty("team");
            }
            if (product == null) {
                product = properties.getProperty("product");
            }
            if (excludedFiles == null) {
                excludedFiles = properties.getProperty("exclude");
            }
            if (buildCmd == null) {
                buildCmd = properties.getProperty("buildCmd");
            }
        } catch (IOException e) {
            // ignore
        }

        if (team == null || product == null) {
            throw new MojoFailureException("Missing property 'team' and 'product'.");
        }
        if (buildCmd == null) {
            buildCmd = "clean source:jar javadoc:jar package -DskipTests";
        }

        Bundler bundler = new Bundler()
                .setProject(super.project())
                .setSettings(super.session().getSettings())
                .setPropertiesFile(propertiesFile)
                .setVersion(isSnapshot ? getVersion(project().getArtifactId()) : super.project().getVersion())
                .setTeam(team)
                .setProduct(product)
                .setExcludedFiles(excludedFiles);

        try {
            // Package
            runner.runCommand("mvn " + buildCmd);

            // Bundle
            bundler.execute();
            groupIds.add(super.project().getGroupId());

            for (MavenProject project : project().getCollectedProjects()) {
                String version = isSnapshot ? getVersion(project.getArtifactId()) : project.getVersion();
                if (version != null) {
                    bundler = new Bundler()
                            .setProject(project)
                            .setSettings(super.session().getSettings())
                            .setPropertiesFile(propertiesFile)
                            .setTeam(team)
                            .setProduct(product)
                            .setVersion(version)
                            .setExcludedFiles(excludedFiles);
                    bundler.execute();
                    groupIds.add(project.getGroupId());
                }
            }

        } finally {
            if (isSnapshot) {
                // Checkout HEAD
                runner.runCommand("git checkout -");
            }
        }
    }
}
