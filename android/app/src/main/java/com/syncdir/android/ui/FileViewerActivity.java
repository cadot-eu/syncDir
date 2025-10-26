package com.syncdir.android.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.syncdir.android.R;

/**
 * Activity pour visualiser les fichiers décryptés
 * À implémenter selon les types de fichiers supportés
 */
public class FileViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.file_viewer);

        Toast.makeText(this, "Visionneuse à implémenter", Toast.LENGTH_SHORT).show();
        
        // TODO: Implémenter la visualisation selon le type de fichier
        // - Images: ImageView
        // - PDF: PDFView library
        // - Texte: TextView
        // - Vidéo: VideoView
    }
}
