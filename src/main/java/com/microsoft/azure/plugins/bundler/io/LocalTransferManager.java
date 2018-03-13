package com.microsoft.azure.plugins.bundler.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalTransferManager implements FileTransferManager {
    private String outputDir;

    public LocalTransferManager(String dest, String groupId) {
        File outputDir = new File(dest);
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        File groupDir = new File(outputDir, groupId);
        if (!groupDir.exists()) {
            groupDir.mkdir();
        }
        this.outputDir = groupDir.getPath();
    }

    public void copy(Path localFile, String name) throws IOException {
        Files.copy(localFile, Paths.get(outputDir, name), StandardCopyOption.REPLACE_EXISTING);
    }
}
