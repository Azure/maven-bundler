package com.microsoft.azure.plugins.bundler.io;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.CommonRestResponse;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.TransferManager;
import com.microsoft.rest.v2.http.HttpPipeline;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.Locale;

public class AzureStorageTransferManager implements FileTransferManager {
    private ContainerURL containerURL;
    private String path;

    public AzureStorageTransferManager(String accountName, String accountKey, String path) {
        try {
            HttpPipeline pipeline = StorageURL.createPipeline(new SharedKeyCredentials(accountName, accountKey));
            URL url = new URL(String.format(Locale.ROOT, "https://%s.blob.core.windows.net", accountName));
            ServiceURL serviceURL = new ServiceURL(url, pipeline);
            this.containerURL = serviceURL.createContainerURL("drops");
            this.path = path;
        } catch (InvalidKeyException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void copy(Path localFile, String name) throws IOException {
        BlockBlobURL blobURL = containerURL.createBlockBlobURL(Paths.get(path, name).toString());
        CommonRestResponse response = TransferManager.uploadFileToBlockBlob(AsynchronousFileChannel.open(localFile), blobURL, 4096, null, null)
                .blockingGet();

        if (response.statusCode() / 100 != 2) {
            throw new IOException("Upload " + name + " failed.");
        }
    }
}
