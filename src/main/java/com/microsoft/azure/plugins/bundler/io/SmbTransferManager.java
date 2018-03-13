package com.microsoft.azure.plugins.bundler.io;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.utils.SmbFiles;

import java.io.IOException;
import java.nio.file.Path;

public class SmbTransferManager implements FileTransferManager {
    private DiskShare diskShare;
    private String server;
    private String share;
    private String relPath;
    private String domain;
    private String user;
    private char[] password;

    public SmbTransferManager(String dest, String groupId, String domain, String user, char[] password) {
        if (dest.endsWith("/") || dest.endsWith("\\")) {
            dest = dest.substring(0, dest.length() - 1);
        }
        String fullPath = (dest + "\\" + groupId)
                .replace("smb://", "")
                .replace("/", "\\");
        server = fullPath.split("\\\\")[0];
        share = fullPath.split("\\\\")[1];
        relPath = fullPath.replace(server + "\\" + share + "\\", "");
        this.domain = domain;
        this.user = user;
        this.password = password;
    }

    public void copy(Path localFile, String name) throws IOException {
        if (diskShare == null) {
            Connection connection = new SMBClient().connect(server);
            AuthenticationContext ac = new AuthenticationContext(user, password, domain);
            Session session = connection.authenticate(ac);

            diskShare = (DiskShare) session.connectShare(share);

            String[] parts = relPath.split("\\\\");
            String soFar = "";
            for (String part : parts) {
                if (soFar.isEmpty()) {
                    soFar = part;
                } else {
                    soFar = soFar + "\\" + part;
                }
                if (!diskShare.folderExists(soFar)) {
                    diskShare.mkdir(soFar);
                }
            }
        }

        SmbFiles.copy(localFile.toFile(), diskShare, relPath + "\\" + name, true);
    }
}
