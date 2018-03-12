package com.microsoft.azure.plugins.bundler;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoFailureException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandRunner {
    private Mojo mojo;
    private MavenSession session;

    public CommandRunner(Mojo mojo, MavenSession session) {
        this.mojo = mojo;
        this.session = session;
    }

    public void runCommand(String command) throws MojoFailureException {
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
        ProcessBuilder pBuilder = new ProcessBuilder();
        if (isWindows) {
            pBuilder.command("cmd.exe", "-c", command);
        } else {
            pBuilder.command("/bin/bash", "-c", command);
        }
        pBuilder.environment().put("PATH", System.getenv("PATH"));
        pBuilder.directory(new File(session.getExecutionRootDirectory()));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Process p = pBuilder.start();

            // Consume the output
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    InputStream is = p.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            mojo.getLog().info(line);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            p.waitFor();

        } catch (IOException | InterruptedException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }
}
