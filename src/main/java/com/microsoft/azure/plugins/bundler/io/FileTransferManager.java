package com.microsoft.azure.plugins.bundler.io;

import java.io.IOException;
import java.nio.file.Path;

public interface FileTransferManager {
    void copy(Path localFile, String name) throws IOException;
}
