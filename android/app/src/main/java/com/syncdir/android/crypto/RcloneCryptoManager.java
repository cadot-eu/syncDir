package com.syncdir.android.crypto;

import android.util.Base64;
import android.util.Log;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Manager pour le décryptage compatible rclone crypt
 * Rclone utilise AES-256-CTR pour les noms de fichiers et AES-256-CBC pour le contenu
 */
public class RcloneCryptoManager {
    
    private static final String TAG = "RcloneCryptoManager";
    private final byte[] key;
    private final byte[] salt;
    
    public RcloneCryptoManager(String password, String passwordSalt) {
        Log.d(TAG, "Init crypto avec password length: " + (password != null ? password.length() : 0));
        
        // Le mot de passe est déjà en clair (pas obscurci)
        // Générer les clés de chiffrement
        this.key = deriveKey(password);
        this.salt = deriveKey(passwordSalt);
        
        Log.d(TAG, "Clé générée: " + key.length + " bytes");
    }
    
    /**
     * Dérive une clé de 256 bits à partir d'un mot de passe
     */
    private byte[] deriveKey(String password) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }
    
    /**
     * Décrypte un nom de fichier chiffré par rclone
     * Rclone utilise base64 pour encoder les noms chiffrés
     */
    public String decryptFilename(String encryptedName) {
        if (encryptedName == null || encryptedName.isEmpty()) {
            return encryptedName;
        }
        
        Log.d(TAG, "Décryptage filename: " + encryptedName);
        
        try {
            // Les noms de fichiers rclone sont en base64
            byte[] encrypted = Base64.decode(encryptedName.replace('-', '+'). replace('_', '/'), Base64.NO_WRAP);
            byte[] decrypted = decryptAES(encrypted, key);
            String result = new String(decrypted, StandardCharsets.UTF_8);
            Log.d(TAG, "Décrypté: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Erreur décryptage filename: " + e.getMessage(), e);
            // Si le décryptage échoue, retourner le nom original (peut-être pas chiffré)
            return encryptedName;
        }
    }
    
    /**
     * Décrypte le contenu d'un fichier
     */
    public byte[] decryptFileContent(byte[] encryptedContent) throws Exception {
        return decryptAES(encryptedContent, key);
    }
    
    /**
     * Décryptage AES-256-CBC
     */
    private byte[] decryptAES(byte[] encrypted, byte[] key) throws InvalidCipherTextException {
        // Extraire l'IV (16 premiers octets)
        byte[] iv = new byte[16];
        System.arraycopy(encrypted, 0, iv, 0, 16);
        
        // Le reste est le contenu chiffré
        byte[] ciphertext = new byte[encrypted.length - 16];
        System.arraycopy(encrypted, 16, ciphertext, 0, ciphertext.length);
        
        // Configurer le déchiffreur
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new CBCBlockCipher(new AESEngine())
        );
        
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(key), iv);
        cipher.init(false, params);
        
        // Décrypter
        byte[] output = new byte[cipher.getOutputSize(ciphertext.length)];
        int outputLen = cipher.processBytes(ciphertext, 0, ciphertext.length, output, 0);
        outputLen += cipher.doFinal(output, outputLen);
        
        // Retourner uniquement les octets valides
        byte[] result = new byte[outputLen];
        System.arraycopy(output, 0, result, 0, outputLen);
        
        return result;
    }
    
    /**
     * Vérifie si un nom de fichier semble chiffré
     */
    public boolean isEncrypted(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        // Les noms chiffrés rclone sont généralement en base64 et assez longs
        return filename.matches("[A-Za-z0-9+/_-]+") && filename.length() > 20;
    }
}
