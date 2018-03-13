package com.microsoft.azure.plugins.bundler;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "auto", aggregator = true)
public class Pipeline extends Preparer {

    @Parameter(property = "dest", defaultValue = "${session.executionRootDirectory}/output")
    private String dest;

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

        try {
            // Package
            runner.runCommand("mvn clean source:jar javadoc:jar package -DskipTests");

            // Bundle
            String version = super.project().getVersion().replace("-SNAPSHOT", "");
            Bundler bundler = new Bundler().setProject(super.project()).setDest(dest).setVersion(version);
            bundler.execute();

            for (MavenProject project : project().getCollectedProjects()) {
                version = project.getVersion().replace("-SNAPSHOT", "");
                bundler = new Bundler().setProject(project).setDest(dest).setVersion(version);
                bundler.execute();
            }

        } finally {
            if (isSnapshot) {
                // Checkout HEAD
                runner.runCommand("git checkout -");
            }
        }
    }
}
