package com.syncdir.android.network;

import android.util.Log;

import com.syncdir.android.data.Directory;

import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SshManager {
    
    private static final String TAG = "SshManager";
    private SSHClient ssh;
    private SFTPClient sftp;
    private final Directory directory;
    private String lastError = "";
    
    public SshManager(Directory directory) {
        this.directory = directory;
    }
    
    public void connect() throws IOException {
        DefaultConfig config = new DefaultConfig();
        
        config.setKeyExchangeFactories(Arrays.asList(
            new net.schmizz.sshj.transport.kex.DHGexSHA256.Factory(),
            new net.schmizz.sshj.transport.kex.DHGexSHA1.Factory(),
            new net.schmizz.sshj.transport.kex.DHG14.Factory(),
            new net.schmizz.sshj.transport.kex.DHG1.Factory()
        ));
        
        ssh = new SSHClient(config);
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        
        try {
            ssh.connect(directory.getHostname(), directory.getPort());
            
            String keyContent = directory.getSshPrivateKey();
            FileKeyProvider keyProvider = null;
            
            for (Factory.Named<FileKeyProvider> factory : config.getFileKeyProviderFactories()) {
                try {
                    keyProvider = factory.create();
                    keyProvider.init(new StringReader(keyContent), null);
                    break;
                } catch (Exception e) {
                    keyProvider = null;
                }
            }
            
            if (keyProvider == null) {
                throw new IOException("Format de clé non supporté");
            }
            
            ssh.authPublickey(directory.getUsername(), keyProvider);
            sftp = ssh.newSFTPClient();
            
        } catch (Exception e) {
            lastError = "Erreur: " + e.getMessage();
            disconnect();
            throw new IOException(lastError, e);
        }
    }
    
    public String decryptFilename(String encryptedName) {
        try {
            Session session = ssh.startSession();
            try {
                String baseUsername = directory.getUsername().replace("Sync", "");
                String cmd = "rclone cryptdecode remote_crypt: " + encryptedName + " --reverse --config /home/" + baseUsername + "/.config/rclone/rclone.conf 2>&1 || echo '" + encryptedName + "'";
                
                Session.Command command = session.exec(cmd);
                String output = IOUtils.readFully(command.getInputStream()).toString().trim();
                command.join(5, TimeUnit.SECONDS);
                
                return output.isEmpty() ? encryptedName : output;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            return encryptedName;
        }
    }
    
    public String getLastError() {
        return lastError.isEmpty() ? "Erreur de connexion" : lastError;
    }
    
    public boolean isConnected() {
        return ssh != null && ssh.isConnected() && sftp != null;
    }
    
    public void disconnect() {
        try {
            if (sftp != null) sftp.close();
            if (ssh != null && ssh.isConnected()) ssh.disconnect();
        } catch (IOException e) {
            // ignore
        }
    }
    
    public List<RemoteFile> listFiles(String remotePath) throws IOException {
        if (!isConnected()) throw new IllegalStateException("Non connecté");
        
        String basePath = "/home/" + directory.getUsername() + "/" + directory.getRemoteDirectory();
        String fullPath = remotePath.isEmpty() ? basePath : basePath + "/" + remotePath;
        
        List<RemoteFile> files = new ArrayList<>();
        for (RemoteResourceInfo resource : sftp.ls(fullPath)) {
            String filename = resource.getName();
            if (".".equals(filename) || "..".equals(filename)) continue;
            
            FileAttributes attrs = resource.getAttributes();
            RemoteFile file = new RemoteFile();
            file.setName(filename);
            file.setDirectory(attrs.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY);
            file.setSize(attrs.getSize());
            file.setModifiedTime(attrs.getMtime() * 1000L);
            file.setPath(remotePath.isEmpty() ? filename : remotePath + "/" + filename);
            files.add(file);
        }
        
        return files;
    }
    
    public byte[] downloadFile(String remotePath) throws IOException {
        if (!isConnected()) throw new IllegalStateException("Non connecté");
        
        try {
            Session session = ssh.startSession();
            try {
                String baseUsername = directory.getUsername().replace("Sync", "");
                // Utiliser rclone cat pour décrypter automatiquement
                String rclonePath = "remote_crypt:" + directory.getRemoteDirectory() + "/" + remotePath;
                String cmd = "rclone cat \"" + rclonePath + "\" --config /home/" + baseUsername + "/.config/rclone/rclone.conf";
                
                Session.Command command = session.exec(cmd);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                
                // Lire la sortie
                InputStream inputStream = command.getInputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                command.join(30, TimeUnit.SECONDS);
                Integer exitCode = command.getExitStatus();
                
                if (exitCode != null && exitCode != 0) {
                    throw new IOException("Erreur rclone (code " + exitCode + ")");
                }
                
                return outputStream.toByteArray();
            } finally {
                session.close();
            }
        } catch (Exception e) {
            throw new IOException("Erreur de téléchargement: " + e.getMessage(), e);
        }
    }
    
    public boolean exists(String remotePath) {
        try {
            String basePath = "/home/" + directory.getUsername() + "/" + directory.getRemoteDirectory();
            String fullPath = remotePath.isEmpty() ? basePath : basePath + "/" + remotePath;
            sftp.stat(fullPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
