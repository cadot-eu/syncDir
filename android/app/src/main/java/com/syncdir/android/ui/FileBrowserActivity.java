package com.syncdir.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.syncdir.android.R;
import com.syncdir.android.data.Directory;
import com.syncdir.android.data.DirectoryRepository;
import com.syncdir.android.network.RemoteFile;
import com.syncdir.android.network.SshManager_age;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileBrowserActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textViewStatus;
    private SwipeRefreshLayout swipeRefresh;
    private EditText editTextSearch;
    private Button buttonSort;
    
    private FileAdapter fileAdapter;
    private Directory directory;
    private SshManager_age sshManager;
    private DirectoryRepository directoryRepository;
    
    private Stack<String> pathStack = new Stack<>();
    private String currentPath = "";
    private List<RemoteFile> allFiles = new ArrayList<>();
    private int currentSortMode = 0; // 0=alpha, 1=extension, 2=date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerViewFiles);
        progressBar = findViewById(R.id.progressBar);
        textViewStatus = findViewById(R.id.textViewStatus);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonSort = findViewById(R.id.buttonSort);

        directoryRepository = new DirectoryRepository(this);
        
        long directoryId = getIntent().getLongExtra("directory_id", -1);
        
        directory = directoryRepository.getDirectoryById(directoryId);

        if (directory == null) {
            Toast.makeText(this, "Erreur: répertoire non trouvé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setTitle(directory.getName());

        // Initialiser le manager SSH
        sshManager = new SshManager_age(directory);

        // Adapter
        fileAdapter = new FileAdapter(new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(RemoteFile file) {
                if (file.isDirectory()) {
                    // Ouvrir le dossier
                    navigateToDirectory(file.getPath());
                } else {
                    // Télécharger et ouvrir le fichier
                    downloadAndOpenFile(file);
                }
            }
            
            @Override
            public void onFileLongClick(RemoteFile file) {
                // Partager le fichier ou dossier
                shareFileOrDirectory(file);
            }
        });
        recyclerView.setAdapter(fileAdapter);

        swipeRefresh.setOnRefreshListener(this::loadFiles);

        // Recherche en temps réel
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFiles(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Menu de tri
        buttonSort.setOnClickListener(v -> showSortMenu());

        // Connexion et chargement initial
        connectAndLoad();
    }

    private void connectAndLoad() {
        showStatus(getString(R.string.connecting));
        
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    sshManager.connect();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    hideStatus();
                    loadFiles();
                } else {
                    showStatus(getString(R.string.connection_error));
                    Toast.makeText(FileBrowserActivity.this, 
                        sshManager.getLastError(), Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void loadFiles() {
        showStatus(getString(R.string.loading_files));
        swipeRefresh.setRefreshing(true);

        new AsyncTask<Void, Void, List<RemoteFile>>() {
            @Override
            protected List<RemoteFile> doInBackground(Void... voids) {
                try {
                    // SshManager_age retire déjà l'extension .age
                    List<RemoteFile> files = sshManager.listFiles(currentPath);
                    
                    
                    return files;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<RemoteFile> files) {
                swipeRefresh.setRefreshing(false);
                hideStatus();
                
                if (files != null) {
                    allFiles = files;
                    filterFiles(editTextSearch.getText().toString());
                } else {
                    Toast.makeText(FileBrowserActivity.this, 
                        "Erreur de chargement des fichiers", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void navigateToDirectory(String path) {
        pathStack.push(currentPath);
        currentPath = path;
        loadFiles();
    }
    
    private void downloadAndOpenFile(RemoteFile file) {
        showStatus("Téléchargement de " + file.getDecryptedName());
        
        new AsyncTask<Void, Void, File>() {
            private String errorMessage = "";
            
            @Override
            protected File doInBackground(Void... voids) {
                try {
                    Log.d("FileBrowser", "Téléchargement: " + file.getPath());
                    
                    // Télécharger et déchiffrer
                    byte[] data = sshManager.downloadFile(file.getPath());
                    
                    if (data == null || data.length == 0) {
                        errorMessage = "Fichier vide après déchiffrement";
                        return null;
                    }
                    
                    Log.d("FileBrowser", "Données déchiffrées: " + data.length + " bytes");
                    
                    // Sauvegarder dans cache
                    File cacheDir = new File(getCacheDir(), "downloads");
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    
                    File localFile = new File(cacheDir, file.getDecryptedName());
                    try (FileOutputStream fos = new FileOutputStream(localFile)) {
                        fos.write(data);
                        fos.flush();
                    }
                    
                    Log.d("FileBrowser", "Fichier sauvegardé: " + localFile.getAbsolutePath() + " (" + localFile.length() + " bytes)");
                    return localFile;
                    
                } catch (Exception e) {
                    Log.e("FileBrowser", "Erreur téléchargement", e);
                    errorMessage = e.getMessage();
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(File localFile) {
                hideStatus();
                
                if (localFile != null && localFile.exists()) {
                    openFile(localFile);
                } else {
                    String msg = "Erreur de téléchargement";
                    if (!errorMessage.isEmpty()) {
                        msg += ": " + errorMessage;
                    }
                    Toast.makeText(FileBrowserActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
    
    private void openFile(File file) {
        try {
            Log.d("FileBrowser", "Ouverture fichier: " + file.getAbsolutePath());
            Log.d("FileBrowser", "Taille fichier: " + file.length() + " bytes");
            
            Uri uri = FileProvider.getUriForFile(this, 
                getPackageName() + ".fileprovider", file);
            
            Log.d("FileBrowser", "URI: " + uri.toString());
            
            String mimeType = getMimeType(file.getName());
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "*/*";
            }
            
            Log.d("FileBrowser", "MIME type: " + mimeType);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                // Essayer d'ouvrir avec l'app par défaut
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                // Aucune app par défaut, afficher le chooser
                try {
                    startActivity(Intent.createChooser(intent, "Ouvrir avec"));
                } catch (Exception e2) {
                    Log.e("FileBrowser", "Aucune app trouvée", e2);
                    Toast.makeText(this, "Aucune application pour ouvrir ce type de fichier", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e("FileBrowser", "Erreur ouverture fichier", e);
            Toast.makeText(this, "Impossible d'ouvrir le fichier: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private String getMimeType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "*/*";
        }
        
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "*/*";
        }
        
        String extension = filename.substring(lastDot + 1).toLowerCase();
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        
        return mimeType != null ? mimeType : "*/*";
    }

    @Override
    public void onBackPressed() {
        if (!pathStack.isEmpty()) {
            currentPath = pathStack.pop();
            loadFiles();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showStatus(String message) {
        textViewStatus.setText(message);
        textViewStatus.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        textViewStatus.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void filterFiles(String query) {
        List<RemoteFile> filtered = new ArrayList<>();
        
        for (RemoteFile file : allFiles) {
            String name = file.getDecryptedName() != null ? file.getDecryptedName() : file.getName();
            if (query.isEmpty() || name.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(file);
            }
        }
        
        sortFiles(filtered);
        fileAdapter.setFiles(filtered);
    }
    
    private void showSortMenu() {
        String[] options = {"Ordre alphabétique", "Par extension", "Plus récents"};
        
        new AlertDialog.Builder(this)
            .setTitle("Trier par")
            .setSingleChoiceItems(options, currentSortMode, (dialog, which) -> {
                currentSortMode = which;
                filterFiles(editTextSearch.getText().toString());
                dialog.dismiss();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }
    
    private void sortFiles(List<RemoteFile> files) {
        switch (currentSortMode) {
            case 0: // Alphabétique
                Collections.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    
                    String name1 = f1.getDecryptedName() != null ? f1.getDecryptedName() : f1.getName();
                    String name2 = f2.getDecryptedName() != null ? f2.getDecryptedName() : f2.getName();
                    return name1.compareToIgnoreCase(name2);
                });
                break;
            case 1: // Par extension
                Collections.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    
                    String ext1 = getExtension(f1.getDecryptedName());
                    String ext2 = getExtension(f2.getDecryptedName());
                    int extCompare = ext1.compareToIgnoreCase(ext2);
                    if (extCompare != 0) return extCompare;
                    
                    String name1 = f1.getDecryptedName() != null ? f1.getDecryptedName() : f1.getName();
                    String name2 = f2.getDecryptedName() != null ? f2.getDecryptedName() : f2.getName();
                    return name1.compareToIgnoreCase(name2);
                });
                break;
            case 2: // Plus récents
                Collections.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return Long.compare(f2.getModifiedTime(), f1.getModifiedTime());
                });
                break;
        }
    }
    
    private String getExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return "";
        return filename.substring(lastDot + 1).toLowerCase();
    }
    
    private void shareFileOrDirectory(RemoteFile file) {
        if (file.isDirectory()) {
            shareDirectory(file);
        } else {
            shareFile(file);
        }
    }
    
    private void shareFile(RemoteFile file) {
        showStatus("Préparation du partage...");
        
        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... voids) {
                try {
                    // Télécharger le fichier
                    byte[] data = sshManager.downloadFile(file.getPath());
                    if (data == null) return null;
                    
                    // Sauvegarder dans cache
                    File cacheDir = new File(getCacheDir(), "share");
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    
                    File localFile = new File(cacheDir, file.getDecryptedName());
                    try (FileOutputStream fos = new FileOutputStream(localFile)) {
                        fos.write(data);
                    }
                    
                    return localFile;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(File localFile) {
                hideStatus();
                
                if (localFile != null && localFile.exists()) {
                    shareLocalFile(localFile);
                } else {
                    Toast.makeText(FileBrowserActivity.this, 
                        "Erreur de téléchargement", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }
    
    private void shareDirectory(RemoteFile directory) {
        showStatus("Compression du dossier...");
        
        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... voids) {
                try {
                    // Lister tous les fichiers du dossier
                    List<RemoteFile> files = sshManager.listFiles(directory.getPath());
                    if (files == null || files.isEmpty()) return null;
                    
                    // Créer un fichier ZIP
                    File cacheDir = new File(getCacheDir(), "share");
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    
                    File zipFile = new File(cacheDir, directory.getDecryptedName() + ".zip");
                    
                    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                        for (RemoteFile file : files) {
                            if (!file.isDirectory()) {
                                // Télécharger et ajouter au ZIP
                                byte[] data = sshManager.downloadFile(file.getPath());
                                if (data != null) {
                                    ZipEntry entry = new ZipEntry(file.getDecryptedName());
                                    zos.putNextEntry(entry);
                                    zos.write(data);
                                    zos.closeEntry();
                                }
                            }
                        }
                    }
                    
                    return zipFile;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(File zipFile) {
                hideStatus();
                
                if (zipFile != null && zipFile.exists()) {
                    shareLocalFile(zipFile);
                } else {
                    Toast.makeText(FileBrowserActivity.this, 
                        "Erreur de compression", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }
    
    private void shareLocalFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, 
                getPackageName() + ".fileprovider", file);
            
            String mimeType = getMimeType(file.getName());
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "*/*";
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Partager via"));
        } catch (Exception e) {
            Toast.makeText(this, "Erreur de partage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sshManager != null) {
            // Déconnexion en arrière-plan pour éviter NetworkOnMainThreadException
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        sshManager.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
        }
    }
}
