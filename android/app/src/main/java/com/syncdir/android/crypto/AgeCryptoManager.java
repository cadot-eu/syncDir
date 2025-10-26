package com.syncdir.android.crypto;

import android.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Gestionnaire simplifié pour déchiffrer les fichiers .age
 * Version simplifiée compatible Android
 */
public class AgeCryptoManager {
    
    private static final String TAG = "AgeCryptoManager";
    private final byte[] key;
    
    public AgeCryptoManager(String password) {
        // Dériver une clé de 256 bits du mot de passe
        this.key = deriveKey(password);
        Log.d(TAG, "AgeCryptoManager initialisé");
    }
    
    /**
     * Déchiffre un fichier .age
     * ATTENTION: Version très simplifiée
     * Fonctionne uniquement pour fichiers chiffrés par notre CLI
     */
    public byte[] decryptFile(byte[] encryptedData) throws Exception {
        Log.d(TAG, "Déchiffrement fichier: " + encryptedData.length + " bytes");
        
        // Notre CLI utilise un format simplifié
        // On utilise juste AES pour la compatibilité
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            
            // IV = premiers 16 bytes
            byte[] iv = new byte[16];
            System.arraycopy(encryptedData, 0, iv, 0, 16);
            
            // Reste = données chiffrées
            byte[] ciphertext = new byte[encryptedData.length - 16];
            System.arraycopy(encryptedData, 16, ciphertext, 0, ciphertext.length);
            
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(ciphertext);
            
            Log.d(TAG, "Déchiffrement réussi: " + decrypted.length + " bytes");
            return decrypted;
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur déchiffrement", e);
            throw new IOException("Erreur déchiffrement: " + e.getMessage());
        }
    }
    
    /**
     * Dérive une clé de 256 bits depuis le mot de passe
     */
    private byte[] deriveKey(String password) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Erreur dérivation clé", e);
        }
    }
}
