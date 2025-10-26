package com.syncdir.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.syncdir.android.R;
import com.syncdir.android.data.Directory;
import com.syncdir.android.data.DirectoryRepository;
import com.syncdir.android.utils.ConfigImporter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyView;
    private DirectoryAdapter adapter;
    private DirectoryRepository directoryRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Mes Répertoires");

        directoryRepository = new DirectoryRepository(this);

        recyclerView = findViewById(R.id.recyclerViewUsers);
        emptyView = findViewById(R.id.emptyView);
        
        // Vérifier si l'app a été lancée pour ouvrir un fichier .syn
        handleIncomingIntent(getIntent());

        adapter = new DirectoryAdapter(
            directory -> {
                // Click sur un répertoire -> ouvrir le navigateur
                Intent intent = new Intent(MainActivity.this, FileBrowserActivity.class);
                intent.putExtra("directory_id", directory.getId());
                startActivity(intent);
            },
            directory -> {
                // Long click -> supprimer
                deleteDirectory(directory);
            },
            directory -> {
                // Partager
                shareDirectory(directory);
            }
        );

        recyclerView.setAdapter(adapter);

        loadDirectories();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDirectories();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }
    
    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        Uri uri = null;
        
        // Gérer ACTION_VIEW (click sur fichier)
        if (Intent.ACTION_VIEW.equals(action)) {
            uri = intent.getData();
        }
        // Gérer ACTION_SEND (partage depuis une app)
        else if (Intent.ACTION_SEND.equals(action)) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        
        if (uri != null) {
            String path = uri.getPath();
            // Accepter les fichiers .syn ou tout fichier venant de content://
            if (path != null && (path.endsWith(".syn") || path.endsWith(".SYN") || path.endsWith(".syncdir-share"))) {
                importConfigFromUri(uri);
            } else if ("content".equals(uri.getScheme())) {
                // Pour les fichiers venant de WhatsApp/Email, essayer de détecter le nom
                String fileName = getFileNameFromUri(uri);
                if (fileName != null && (fileName.endsWith(".syn") || fileName.endsWith(".SYN"))) {
                    importConfigFromUri(uri);
                }
            }
        }
    }
    
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if ("content".equals(uri.getScheme())) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return fileName;
    }

    private void loadDirectories() {
        List<Directory> directories = directoryRepository.getAllDirectories();
        adapter.setDirectories(directories);

        if (directories.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }


    private void importConfigFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                try {
                    ConfigImporter.importFromJson(inputStream, directoryRepository);
                    inputStream.close();
                    Toast.makeText(this, "Répertoire ajouté avec succès", Toast.LENGTH_SHORT).show();
                    loadDirectories();
                } catch (Exception e) {
                    inputStream.close();
                    if (e.getMessage() != null && e.getMessage().equals("PASSWORD_REQUIRED")) {
                        // Fichier chiffré - demander le mot de passe
                        showPasswordDialog(uri);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Erreur import", e);
            String errorMsg = "Erreur lors de l'import";
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMsg += ": " + e.getMessage();
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }
    
    private void showPasswordDialog(Uri uri) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Mot de passe");
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Fichier protégé")
            .setMessage("Ce fichier est protégé par mot de passe")
            .setView(input)
            .setPositiveButton("OK", (dialog, which) -> {
                String password = input.getText().toString().trim();
                if (password.isEmpty()) {
                    Toast.makeText(this, "Mot de passe requis", Toast.LENGTH_SHORT).show();
                    return;
                }
                tryImportWithPassword(uri, password);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void tryImportWithPassword(Uri uri, String password) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                ConfigImporter.importFromJson(inputStream, directoryRepository, password);
                inputStream.close();
                Toast.makeText(this, "Répertoire ajouté avec succès", Toast.LENGTH_SHORT).show();
                loadDirectories();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Erreur import avec mot de passe", e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void deleteDirectory(Directory directory) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Supprimer " + directory.getName() + " ?")
            .setPositiveButton("Supprimer", (dialog, which) -> {
                directoryRepository.deleteDirectory(directory.getId());
                loadDirectories();
                Toast.makeText(this, "Supprimé", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void shareDirectory(Directory directory) {
        // Demander le mot de passe de partage
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Mot de passe de partage");
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mot de passe de partage")
            .setMessage("Choisissez un mot de passe pour protéger ce partage")
            .setView(input)
            .setPositiveButton("Partager", (dialog, which) -> {
                String sharePassword = input.getText().toString().trim();
                if (sharePassword.isEmpty()) {
                    Toast.makeText(this, "Mot de passe requis", Toast.LENGTH_SHORT).show();
                    return;
                }
                performShare(directory, sharePassword);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void performShare(Directory directory, String sharePassword) {
        try {
            // Créer fichier JSON de partage
            JSONObject json = new JSONObject();
            json.put("version", "3.0-share-encrypted");
            json.put("share_type", "single_directory");
            json.put("created_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new java.util.Date()));
            json.put("shared_by", android.os.Build.MODEL);
            json.put("share_name", directory.getName());
            json.put("hostname", directory.getHostname());
            json.put("port", directory.getPort());
            json.put("ssh_key", directory.getSshPrivateKey());
            json.put("username", directory.getUsername());
            json.put("remote_directory", directory.getRemoteDirectory());
            json.put("password", directory.getPassword());
            
            String jsonString = json.toString();
            
            // Chiffrer avec OpenSSL AES-256-CBC (compatible avec le script)
            com.syncdir.android.crypto.OpenSSLCryptoManager crypto = new com.syncdir.android.crypto.OpenSSLCryptoManager(sharePassword);
            byte[] encrypted = crypto.encryptFile(jsonString.getBytes("UTF-8"));
            
            // Encoder en Base64 pour stockage
            String encryptedBase64 = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP);
            
            // Créer fichier avec données chiffrées
            File cacheDir = new File(getCacheDir(), "shares");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            
            String fileName = directory.getRemoteDirectory().replaceAll("[^a-zA-Z0-9]", "_") + ".syn";
            File shareFile = new File(cacheDir, fileName);
            
            FileWriter writer = new FileWriter(shareFile);
            writer.write(encryptedBase64);
            writer.close();
            
            // Partager via Intent
            Uri uriFile = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", shareFile);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/octet-stream");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uriFile);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Partage SyncDir - " + directory.getRemoteDirectory());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Fichier de configuration pour accéder à " + directory.getRemoteDirectory() + "\n\nMot de passe requis pour ouvrir ce fichier.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Partager via"));
            
        } catch (Exception e) {
            Log.e("MainActivity", "Erreur partage", e);
            Toast.makeText(this, "Erreur lors du partage: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
