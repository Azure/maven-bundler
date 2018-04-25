package com.microsoft.azure.plugins.bundler;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

@Mojo(name = "stage", aggregator = true)
public class Stager extends AbstractMojo {
    @Parameter(defaultValue="${settings}", readonly=true, required=true)
    private Settings settings;

    Stager setSettings(Settings settings) {
        this.settings = settings;
        return this;
    }

    @Parameter(property = "dest")
    private String dest;

    Stager setSource(String source) {
        this.dest = source;
        return this;
    }

    @Parameter(property = "groupId")
    private String groupIds;

    Stager setGroupIds(String groupIds) {
        this.groupIds = groupIds;
        return this;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        dest = dest.replace("smb://", "\\\\")
                .replace("/", "\\");
        if (!dest.startsWith("\\\\")) {
            throw new MojoFailureException("Must specify dest as a network share or SMB path");
        }

        String username;
        String password;
        if (settings.getServer("azuresdkci").getUsername() != null && settings.getServer("azuresdkci").getPassword() != null) {
            username = settings.getServer("azuresdkci").getUsername();
            password = settings.getServer("azuresdkci").getPassword();
        }
        else if (System.getProperty("jenkinsUser") != null) {
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

        for (final String groupId : groupIds.split(",")) {
            try {
                String jobName = getJobName(groupId);
                JenkinsServer jenkins = new JenkinsServer(new URI("http://azuresdkci.cloudapp.net"), username, password);
                Job job = jenkins.getJob(jobName);
                job.build(new HashMap<String, String>() {{
                    put("location", dest + "\\" + groupId);
                }});
                getLog().info(String.format("Job %s queue at %s.", jobName, job.getUrl()));
            } catch (URISyntaxException | IOException e) {
                throw new MojoFailureException(e.getMessage(), e);
            }
        }
    }

    private String getJobName(String groupId) throws MojoFailureException {
        if (groupId.startsWith("com.microsoft.rest")) {
            return "publish-to-maven-com.microsoft.rest";
        } else if (groupId.startsWith("com.microsoft.azure")) {
            return "publish-to-maven";
        } else {
            throw new MojoFailureException(String.format("Group ID '%s' is currently not supported", groupId));
        }
    }
}
