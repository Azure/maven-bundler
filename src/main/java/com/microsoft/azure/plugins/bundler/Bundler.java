package com.microsoft.azure.plugins.bundler;

import com.microsoft.azure.plugins.bundler.io.FileTransferManager;
import com.microsoft.azure.plugins.bundler.io.LocalTransferManager;
import com.microsoft.azure.plugins.bundler.io.SmbTransferManager;
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

    @Parameter(property = "dest", defaultValue = "${session.executionRootDirectory}/output", required = true)
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

    private FileTransferManager transferManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (isSmbDest(dest)) {
            String domain;
            String user;
            char[] password;
            if (System.getProperty("domain") != null) {
                domain = System.getProperty("domain");
                user = System.getProperty("user");
                password = System.getProperty("password").toCharArray();
            } else {
                String specify = "Please specify `%s` for file share " + dest + ": ";
                System.out.print(String.format(specify, "domain"));
                domain = System.console().readLine();
                System.out.print(String.format(specify, "user"));
                user = System.console().readLine();
                System.out.print(String.format(specify, "password"));
                password = System.console().readPassword();
                System.setProperty("domain", domain);
                System.setProperty("user", user);
                System.setProperty("password", new String(password));
            }
            transferManager = new SmbTransferManager(dest, project.getGroupId(), domain, user, password);
        } else {
            transferManager = new LocalTransferManager(dest, project.getGroupId());
        }

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
            transferManager.copy(pomLocation, pomFileName);

            if (artifacts != null) {
                for (File artifact : artifacts) {
                    getLog().info("Copying artifacts: " + artifact.getPath());
                    transferManager.copy(Paths.get(artifact.getPath()), artifact.getName());
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

    private void copyFile(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean isSmbDest(String destPath) {
        return destPath.startsWith("\\\\") || destPath.startsWith("smb://");
    }
}
