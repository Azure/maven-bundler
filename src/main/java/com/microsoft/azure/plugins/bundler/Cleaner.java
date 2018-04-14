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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleans any work done by this plugin.
 */
@Mojo(name = "clean" , aggregator = true)
public class Cleaner extends AbstractMojo {
    private static final String RELEASE_PLUGIN_PREFIX = "[maven-release-plugin]";

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        CommandRunner commandRunner = new CommandRunner(this, session);

        String commitMessage = getCommitMessage(commandRunner);
        if (commitMessage.contains(RELEASE_PLUGIN_PREFIX)) {
            if (commitMessage.contains("prepare for next development iteration")) {
                rollbackOneCommit(commandRunner);
                commitMessage = getCommitMessage(commandRunner);
            }

            Pattern pattern = Pattern.compile("prepare release ([^ $]+)");
            Matcher matcher = pattern.matcher(commitMessage);
            if (matcher.find()) {
                deleteGitTag(matcher.group(1), commandRunner);
            }

            rollbackOneCommit(commandRunner);
        }

        cleanUpUntrackeFiles(commandRunner);
    }

    public String getCommitMessage(CommandRunner commandRunner) throws MojoFailureException {
       return commandRunner.runCommandSilent("git log -1 --pretty=%B");
    }

    public void rollbackOneCommit(CommandRunner commandRunner) throws MojoFailureException {
        commandRunner.runCommand("git reset --hard HEAD~1");
    }

    public void cleanUpUntrackeFiles(CommandRunner commandRunner) throws MojoFailureException {
        commandRunner.runCommand("git clean -df");
    }

    public void deleteGitTag(String tag, CommandRunner commandRunner) throws MojoFailureException {
        commandRunner.runCommand("git tag -d " + tag);
    }
}
