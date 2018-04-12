package com.microsoft.azure.plugins.bundler;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.HashSet;
import java.util.Set;

@Mojo(name = "auto", aggregator = true)
public class Pipeline extends Preparer {

    @Parameter(property = "dest", defaultValue = "${session.executionRootDirectory}/output")
    private String dest;

    @Parameter(property = "stage", defaultValue = "false")
    private boolean stage;

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

        try {
            // Package
            runner.runCommand("mvn clean source:jar javadoc:jar package -DskipTests");

            // Bundle
            String version = super.project().getVersion().replace("-SNAPSHOT", "");
            Bundler bundler = new Bundler().setProject(super.project()).setDest(dest).setVersion(version);
            bundler.execute();
            groupIds.add(super.project().getGroupId());

            for (MavenProject project : project().getCollectedProjects()) {
                version = project.getVersion().replace("-SNAPSHOT", "");
                bundler = new Bundler().setProject(project).setDest(dest).setVersion(version);
                bundler.execute();
                groupIds.add(project.getGroupId());
            }

        } finally {
            if (isSnapshot) {
                // Checkout HEAD
                runner.runCommand("git checkout -");
            }
        }

        //Stage
        if (stage) {
            for (String groupId : groupIds) {
                Stager stager = new Stager().setGroupId(groupId).setSource(dest + "\\" + groupId).setSettings(super.session().getSettings());
                stager.execute();
            }
        }
    }
}
