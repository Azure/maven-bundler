package com.microsoft.azure.plugins.bundler;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Bundles all poms and jars with corresponding names.
 */
@Mojo(name = "bundle")
public class Bundler extends AbstractMojo {

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

    Bundler setProject(MavenProject project) {
        this.project = project;
        return this;
    }

    @Parameter(defaultValue = "${session.executionRootDirectory}/output", required = true)
    private String dest;

    Bundler setDest(String dest) {
        this.dest = dest;
        return this;
    }

    @Parameter(defaultValue = "${project.version}", required = true)
    private String version;

    Bundler setVersion(String version) {
        this.version = version;
        return this;
    }

    private boolean isWindows = System.getProperty("os.name")
            .toLowerCase().startsWith("windows");

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File output = getDirForGroupId(project.getGroupId());
        Path pomLocation = Paths.get(project.getBasedir().getPath(), "pom.xml");
        File[] artifacts = new File(project.getBasedir(), "target").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") && name.startsWith(project.getArtifactId());
            }
        });
        try {
            getLog().info("============== Bundler =============");
            getLog().info("Copying POM: " + pomLocation);
            String pomFileName = String.format("%s-%s.pom", project.getArtifactId(), version);
            Files.copy(pomLocation, Paths.get(output.getPath(), pomFileName), StandardCopyOption.REPLACE_EXISTING);

            if (artifacts != null) {
                for (File artifact : artifacts) {
                    getLog().info("Copying artifacts: " + artifact.getPath());
                    Files.copy(Paths.get(artifact.getPath()), Paths.get(output.getPath(), artifact.getName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Creates or fetches a directory for a group ID.
     * @param groupId the Maven group ID
     * @return the directory to put artifacts
     */
    private File getDirForGroupId(String groupId) {
        File outputDir = new File(dest);
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        File groupDir = new File(outputDir, groupId);
        if (!groupDir.exists()) {
            groupDir.mkdir();
        }
        return groupDir;
    }
}
