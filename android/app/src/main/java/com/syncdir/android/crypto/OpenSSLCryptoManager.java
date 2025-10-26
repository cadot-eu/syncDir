package com.syncdir.android.crypto;

import android.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Déchiffrement compatible OpenSSL AES-256-CBC avec PBKDF2
 * Format identique à: openssl enc -aes-256-cbc -salt -pbkdf2
 */
public class OpenSSLCryptoManager {
    
    private static final String TAG = "OpenSSLCrypto";
    private static final byte[] SALTED_MAGIC = "Salted__".getBytes(StandardCharsets.UTF_8);
    private static final int PBKDF2_ITERATIONS = 10000; // OpenSSL default
    private final String password;
    
    public OpenSSLCryptoManager(String password) {
        this.password = password;
    }
    
    /**
     * Déchiffre un fichier .enc (format OpenSSL avec -pbkdf2)
     */
    public byte[] decryptFile(byte[] encryptedData) throws Exception {
        Log.d(TAG, "Déchiffrement fichier: " + encryptedData.length + " bytes");
        
        // Vérifier magic "Salted__"
        if (encryptedData.length < 16 || 
            !Arrays.equals(Arrays.copyOfRange(encryptedData, 0, 8), SALTED_MAGIC)) {
            throw new IOException("Format OpenSSL invalide");
        }
        
        // Extraire le salt (8 bytes après "Salted__")
        byte[] salt = Arrays.copyOfRange(encryptedData, 8, 16);
        
        // Dériver clé + IV avec PBKDF2 (compatible openssl -pbkdf2)
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            (32 + 16) * 8  // 32 bytes clé + 16 bytes IV
        );
        byte[] keyAndIV = factory.generateSecret(spec).getEncoded();
        
        byte[] key = Arrays.copyOfRange(keyAndIV, 0, 32);
        byte[] iv = Arrays.copyOfRange(keyAndIV, 32, 48);
        
        // Données chiffrées (après "Salted__" + salt)
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, 16, encryptedData.length);
        
        // Déchiffrer
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(ciphertext);
        
        Log.d(TAG, "Déchiffrement réussi: " + decrypted.length + " bytes");
        return decrypted;
    }
    
    /**
     * Chiffre des données (format OpenSSL avec -pbkdf2)
     */
    public byte[] encryptFile(byte[] data) throws Exception {
        Log.d(TAG, "Chiffrement: " + data.length + " bytes");
        
        // Générer salt aléatoire
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        
        // Dériver clé + IV avec PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            (32 + 16) * 8
        );
        byte[] keyAndIV = factory.generateSecret(spec).getEncoded();
        
        byte[] key = Arrays.copyOfRange(keyAndIV, 0, 32);
        byte[] iv = Arrays.copyOfRange(keyAndIV, 32, 48);
        
        // Chiffrer
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(data);
        
        // Format OpenSSL: "Salted__" + salt + ciphertext
        byte[] result = new byte[SALTED_MAGIC.length + salt.length + ciphertext.length];
        System.arraycopy(SALTED_MAGIC, 0, result, 0, SALTED_MAGIC.length);
        System.arraycopy(salt, 0, result, SALTED_MAGIC.length, salt.length);
        System.arraycopy(ciphertext, 0, result, SALTED_MAGIC.length + salt.length, ciphertext.length);
        
        Log.d(TAG, "Chiffrement réussi: " + result.length + " bytes");
        return result;
    }
}
