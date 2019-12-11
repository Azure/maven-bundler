package com.microsoft.azure.plugins.bundler;

import com.microsoft.azure.plugins.bundler.io.AzureStorageTransferManager;
import com.microsoft.azure.plugins.bundler.io.FileTransferManager;
import com.microsoft.azure.plugins.bundler.io.LocalTransferManager;
import com.microsoft.azure.plugins.bundler.io.SmbTransferManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * Bundles all poms and jars with corresponding names.
 */
@Mojo(name = "bundle")
public class Bundler extends AbstractMojo {
    private static final String SERVER_ID = "azuresdkpartnerdrops";

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;
    @Parameter(defaultValue="${settings}", readonly=true, required=true)
    private Settings settings;

    Bundler setSettings(Settings settings) {
        this.settings = settings;
        return this;
    }

    Bundler setProject(MavenProject project) {
        this.project = project;
        return this;
    }

    @Parameter(property = "properties", defaultValue = "${session.executionRootDirectory}/bundler.properties")
    private String propertiesFile;

    Bundler setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
        return this;
    }

    @Parameter(property = "team")
    private String team;

    Bundler setTeam(String team) {
        this.team = team;
        return this;
    }

    @Parameter(property = "product")
    private String product;

    Bundler setProduct(String product) {
        this.product = product;
        return this;
    }

    @Parameter(defaultValue = "${project.version}", required = true)
    private String version;

    Bundler setVersion(String version) {
        this.version = version;
        return this;
    }

    @Parameter(property = "exclude")
    private String excludedFiles;

    Bundler setExcludedFiles(String excludedFiles) {
        this.excludedFiles = excludedFiles;
        return this;
    }

    @Parameter(property = "pomFile")
    private String pomFile;

    Bundler setPomFile(String pomFile) {
        this.pomFile = pomFile;
        return this;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
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
            if (pomFile == null) {
                pomFile = properties.getProperty("pomFile");
            }
        } catch (IOException e) {
            // ignore
        }

        if (team == null || product == null) {
            throw new MojoFailureException("Missing property 'team' and 'product'.");
        }

        List<String> exclude;
        if (excludedFiles != null) {
            exclude = new ArrayList<>(Arrays.asList(excludedFiles.split(",")));
        } else {
            exclude = new ArrayList<>();
        }

        String blobPath = Paths.get(team, product, version).toString();
        String accountName;
        String accountKey;
        if (settings.getServer(SERVER_ID).getUsername() != null && settings.getServer(SERVER_ID).getPassword() != null) {
            accountName = settings.getServer(SERVER_ID).getUsername();
            accountKey = settings.getServer(SERVER_ID).getPassword();
        } else {
            throw new MojoFailureException("Please set the account name and key for azuresdkpartnerdrops in your maven settings.xml");
        }
        FileTransferManager transferManager = new AzureStorageTransferManager(accountName, accountKey, blobPath);

        pomFile = (pomFile == null) ? "pom.xml" : pomFile;
        Path pomLocation = Paths.get(pomFile);
        if (!pomLocation.isAbsolute()) {
            pomLocation = Paths.get(project.getBasedir().getPath(), pomFile);
        }
        if (!Files.exists(pomLocation)) {
            throw new MojoFailureException(String.format("POM file not found at '%s'.", pomLocation));
        }
        File[] artifacts = new File(project.getBasedir(), "target").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") && name.startsWith(project.getArtifactId());
            }
        });
        try {
            getLog().info("============== Bundler =============");
            String pomFileName = String.format("%s-%s.pom", project.getArtifactId(), version);
            if (exclude.stream().noneMatch(s -> pomFileName.equalsIgnoreCase(s) || pomFileName.matches(s))) {
                getLog().info("Copying POM: " + pomLocation);
                transferManager.copy(pomLocation, pomFileName);
            }

            if (artifacts != null) {
                for (File artifact : artifacts) {
                    if (exclude.stream().noneMatch(s -> artifact.getName().equalsIgnoreCase(s) || artifact.getName().matches(s))) {
                        getLog().info("Copying artifacts: " + artifact.getPath());
                        transferManager.copy(Paths.get(artifact.getPath()), artifact.getName());
                    }
                }
            }

            getLog().info("============== Uploaded to blob path " + blobPath.replace("\\", "/") + " =============");
            getLog().info("============== Now you can run pipeline https://dev.azure.com/azure-sdk/internal/_release?definitionId=6 =============");
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
