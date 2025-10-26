package com.syncdir.android.network;

import android.util.Log;
import com.syncdir.android.crypto.OpenSSLCryptoManager;
import com.syncdir.android.data.Directory;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Factory;
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

public class SshManager_age {
    
    private static final String TAG = "SshManager_age";
    private SSHClient ssh;
    private SFTPClient sftp;
    private final Directory directory;
    private String lastError = "";
    private OpenSSLCryptoManager cryptoManager;
    
    public SshManager_age(Directory directory) {
        this.directory = directory;
        this.cryptoManager = new OpenSSLCryptoManager(directory.getPassword());
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
            
            // Retirer .enc pour l'affichage
            String displayName = filename;
            if (filename.endsWith(".enc")) {
                displayName = filename.substring(0, filename.length() - 4);
            }
            
            file.setName(filename);
            file.setDecryptedName(displayName);
            file.setDirectory(attrs.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY);
            file.setSize(attrs.getSize());
            file.setModifiedTime(attrs.getMtime() * 1000L);
            file.setPath(remotePath.isEmpty() ? filename : remotePath + "/" + filename);
            files.add(file);
        }
        
        return files;
    }
    
    /**
     * Télécharge fichier .enc et déchiffre localement avec OpenSSL
     */
    public byte[] downloadFile(String remotePath) throws IOException {
        if (!isConnected()) throw new IllegalStateException("Non connecté");
        
        String basePath = "/home/" + directory.getUsername() + "/" + directory.getRemoteDirectory();
        String fullPath = basePath + "/" + remotePath;
        
        Log.d(TAG, "Téléchargement: " + fullPath);
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = sftp.open(fullPath).new ReadAheadRemoteFileInputStream(16);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            
            byte[] encryptedData = outputStream.toByteArray();
            Log.d(TAG, "Fichier téléchargé (chiffré): " + encryptedData.length + " bytes");
            
            // Déchiffrer localement avec OpenSSL
            byte[] decryptedData = cryptoManager.decryptFile(encryptedData);
            Log.d(TAG, "Fichier déchiffré: " + decryptedData.length + " bytes");
            
            return decryptedData;
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur téléchargement/déchiffrement", e);
            throw new IOException("Erreur: " + e.getMessage(), e);
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
