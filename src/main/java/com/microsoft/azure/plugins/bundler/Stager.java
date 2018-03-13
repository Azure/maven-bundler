package com.microsoft.azure.plugins.bundler;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

@Mojo(name = "stage", aggregator = true)
public class Stager extends AbstractMojo {

    @Parameter(property = "source")
    private String source;

    Stager setSource(String source) {
        this.source = source;
        return this;
    }

    @Parameter(property = "groupId")
    private String groupId;

    Stager setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        source = source.replace("smb://", "\\\\")
                .replace("/", "\\");
        if (!source.startsWith("\\\\")) {
            throw new MojoFailureException("Must specify source as a network share or SMB path");
        }

        String jobName;
        if (groupId.startsWith("com.microsoft.rest")) {
            jobName = "publish-to-maven-com.microsoft.rest";
        } else if (groupId.startsWith("com.microsoft.azure")) {
            jobName = "publish-to-maven";
        } else {
            throw new MojoFailureException(String.format("Group ID '%s' is currently not supported", groupId));
        }

        String username;
        String password;
        if (System.getProperty("jenkinsUser") != null) {
            username = System.getProperty("jenkinsUser");
            password = System.getProperty("jenkinsPassword");
        } else {
            String specify = "Please specify `%s` for Jenkins server: ";
            System.out.print(String.format(specify, "user ID"));
            username = System.console().readLine();
            System.setProperty("jenkinsUser", username);
            System.out.print(String.format(specify, "API token"));
            password = new String(System.console().readPassword());
            System.setProperty("jenkinsPassword", password);
        }

        try {
            JenkinsServer jenkins = new JenkinsServer(new URI("http://azuresdkci.cloudapp.net"), username, password);
            Job job = jenkins.getJob(jobName);
            job.build(new HashMap<String, String>() {{
                put("location", source);
            }});
            getLog().info(String.format("Job %s queue at %s.", jobName, job.getUrl()));
        } catch (URISyntaxException | IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
