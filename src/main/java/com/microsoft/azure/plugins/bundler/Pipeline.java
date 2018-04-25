package com.microsoft.azure.plugins.bundler;

import com.google.common.base.Joiner;
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
        String specify = "Please specify %s for file share " + dest + ": ";
        String domain = System.getenv("USERDOMAIN");
        String user = System.getProperty("user.name");
        char[] password;
        if (domain == null) {
            if (System.getProperty("domain") != null) {
                domain = System.getProperty("domain");
            } else {
                System.out.print(String.format(specify, "domain"));
                domain = System.console().readLine();
                System.setProperty("domain", domain);
            }
        }
        if (user == null) {
            if (System.getProperty("user") != null) {
                user = System.getProperty("user");
            } else {
                System.out.print(String.format(specify, "user"));
                user = System.console().readLine();
                System.setProperty("user", user);
            }
        }
        if (System.getProperty("password") != null) {
            password = System.getProperty("password").toCharArray();
        } else {
            System.out.print(String.format(specify, domain + "\\" + user + "'s password"));
            password = System.console().readPassword();
            System.setProperty("password", new String(password));
        }
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
            Bundler bundler = new Bundler().setProject(super.project()).setDest(dest).setVersion(getVersion(project().getArtifactId()));
            bundler.execute();
            groupIds.add(super.project().getGroupId());

            for (MavenProject project : project().getCollectedProjects()) {
                String version = getVersion(project.getArtifactId());
                if (version != null) {
                    bundler = new Bundler().setProject(project).setDest(dest).setVersion(getVersion(project.getArtifactId()));
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

        //Stage
        if (stage) {
            Stager stager = new Stager().setGroupIds(Joiner.on(',').join(groupIds)).setSource(dest).setSettings(super.session().getSettings());
            stager.execute();
        }
    }
}
