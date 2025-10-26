package com.syncdir.android.utils;

import android.util.Log;

import com.syncdir.android.data.Directory;
import com.syncdir.android.data.DirectoryRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigImporter {
    
    private static final String TAG = "ConfigImporter";
    
    public static void importFromJson(InputStream inputStream, DirectoryRepository directoryRepository) throws Exception {
        importFromJson(inputStream, directoryRepository, null);
    }
    
    public static void importFromJson(InputStream inputStream, DirectoryRepository directoryRepository, String password) throws Exception {
        // Lire le contenu
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        reader.close();
        
        String jsonContent = jsonBuilder.toString();
        Log.d(TAG, "Contenu lu: " + jsonContent.length() + " caractères");
        
        // Détecter si c'est du Base64 (fichier chiffré)
        JSONObject root;
        if (jsonContent.startsWith("{")) {
            // JSON en clair
            root = new JSONObject(jsonContent);
        } else {
            // Fichier chiffré en Base64 - demander le mot de passe
            if (password == null) {
                throw new Exception("PASSWORD_REQUIRED");
            }
            
            try {
                byte[] encrypted = android.util.Base64.decode(jsonContent, android.util.Base64.NO_WRAP);
                com.syncdir.android.crypto.OpenSSLCryptoManager crypto = new com.syncdir.android.crypto.OpenSSLCryptoManager(password);
                byte[] decrypted = crypto.decryptFile(encrypted);
                jsonContent = new String(decrypted, "UTF-8");
                root = new JSONObject(jsonContent);
            } catch (Exception e) {
                throw new Exception("Mot de passe incorrect");
            }
        }
        
        // Détecter le type (config complète ou partage)
        String shareType = root.optString("share_type", "");
        
        if ("single_directory".equals(shareType)) {
            // Fichier de partage
            importShare(root, directoryRepository);
            return;
        }
        
        // Fichier de configuration avec multiples répertoires (legacy)
        importLegacyConfig(root, directoryRepository);
    }
    
    private static void importShare(JSONObject root, DirectoryRepository directoryRepository) throws Exception {
        Log.d(TAG, "Import de fichier de partage");
        
        // Extraire les informations
        String hostname = root.getString("hostname");
        int port = root.getInt("port");
        String sshKey = root.getString("ssh_key");
        sshKey = sshKey.replace("\\n", "\n");
        
        String shareName = root.getString("share_name");
        String username = root.getString("username");
        String remoteDirectory = root.getString("remote_directory");
        String password = root.getString("password");
        
        // Chercher si le répertoire existe déjà
        Directory existing = directoryRepository.getDirectoryByUsernameAndRemoteDir(username, remoteDirectory);
        
        if (existing != null) {
            Log.d(TAG, "Répertoire existe déjà: " + existing.getName());
            // Mettre à jour
            existing.setName(shareName);
            existing.setHostname(hostname);
            existing.setPort(port);
            existing.setSshPrivateKey(sshKey);
            existing.setPassword(password);
            directoryRepository.updateDirectory(existing);
        } else {
            // Créer nouveau répertoire
            Directory directory = new Directory();
            directory.setName(shareName);
            directory.setHostname(hostname);
            directory.setPort(port);
            directory.setSshPrivateKey(sshKey);
            directory.setUsername(username);
            directory.setRemoteDirectory(remoteDirectory);
            directory.setPassword(password);
            
            directoryRepository.saveDirectory(directory);
            Log.d(TAG, "Nouveau répertoire créé: " + directory.getName());
        }
    }
    
    private static void importLegacyConfig(JSONObject root, DirectoryRepository directoryRepository) throws Exception {
        JSONObject serverJson = root.getJSONObject("server");
        String hostname = serverJson.getString("hostname");
        int port = serverJson.optInt("port", 22);
        String sshKey = serverJson.getString("ssh_key");
        sshKey = sshKey.replace("\\n", "\n");
        
        JSONArray usersJson = root.getJSONArray("users");
        
        for (int i = 0; i < usersJson.length(); i++) {
            JSONObject userJson = usersJson.getJSONObject(i);
            String username = userJson.getString("username");
            String remoteDir = userJson.getString("remote_directory");
            String name = userJson.getString("name");
            String password = userJson.getString("password");
            
            Directory existing = directoryRepository.getDirectoryByUsernameAndRemoteDir(username, remoteDir);
            
            if (existing != null) {
                existing.setName(name);
                existing.setPassword(password);
                directoryRepository.updateDirectory(existing);
            } else {
                Directory directory = new Directory();
                directory.setName(name);
                directory.setHostname(hostname);
                directory.setPort(port);
                directory.setSshPrivateKey(sshKey);
                directory.setUsername(username);
                directory.setRemoteDirectory(remoteDir);
                directory.setPassword(password);
                
                directoryRepository.saveDirectory(directory);
            }
        }
    }
}
