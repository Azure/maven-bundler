package com.microsoft.azure.plugins.bundler;

import com.google.common.base.Joiner;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Bundles all poms and jars with corresponding names.
 */
@Mojo(name = "prepare" , aggregator = true)
public class Preparer extends AbstractMojo {
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    MavenSession session() {
        return session;
    }

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

    MavenProject project() {
        return project;
    }

    @Parameter(property = "tag")
    private String tag;

    @Parameter(property = "version")
    private String version;

    @Parameter(property = "devVersion")
    private String devVersion;

    @Parameter(property = "versionConfig")
    private String versionConfig;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.getVersion().endsWith("-SNAPSHOT")) {
            throw new MojoFailureException("Prepare goal can only be run for SNAPSHOT projects");
        } else if (tag == null) {
            throw new MojoFailureException("Argument `tag` must be provided");
        }
        CommandRunner runner = new CommandRunner(this, session);
        String prepareCmd = "mvn -B release:prepare -DpushChanges=false -Darguments=\"-DskipTests=true\" -Dresume=false";
        prepareCmd = prepareCmd + " -Dtag=" + tag;
        if (versionConfig != null) {
            prepareCmd = prepareCmd + " " + Joiner.on(" ").join(loadVersions(versionConfig));
        } else if (version != null && devVersion != null) {
            prepareCmd = prepareCmd + String.format(" -DreleaseVersion=%s -DdevelopmentVersion=%s", version, devVersion);
        } else {
            throw new MojoFailureException("Either 'versionConfig' or the combination of 'version' and 'devVersion' must be provided");
        }

        runner.runCommand(prepareCmd);
    }

    private List<String> loadVersions(String versionConfig) throws MojoFailureException{
        List<String> args = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(versionConfig)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",");
                assert info.length == 4;
                String groupId = info[0];
                String artifactId = info[1];
                String version = info[2];
                String devVersion = info[3];
                args.add(String.format("-Dproject.rel.%s:%s=%s -Dproject.dev.%s:%s=%s",
                        groupId, artifactId, version, groupId, artifactId, devVersion));
            }
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        return args;
    }
}
