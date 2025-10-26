package com.syncdir.android;

import android.app.Application;
import android.util.Log;

import net.i2p.crypto.eddsa.EdDSASecurityProvider;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SyncDirApplication extends Application {
    
    private static final String TAG = "SyncDirApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Initialisation des providers de sécurité...");
        
        // Enregistrer BouncyCastle
        Security.removeProvider("BC");
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Log.d(TAG, "BouncyCastle enregistré");
        
        // Enregistrer EdDSA pour support ED25519
        Security.addProvider(new EdDSASecurityProvider());
        Log.d(TAG, "EdDSA provider enregistré");
        
        Log.d(TAG, "Providers actifs: " + Security.getProviders().length);
    }
}
