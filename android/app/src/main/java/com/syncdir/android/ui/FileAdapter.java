package com.syncdir.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.syncdir.android.R;
import com.syncdir.android.network.RemoteFile;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<RemoteFile> files = new ArrayList<>();
    private final OnFileClickListener clickListener;

    public interface OnFileClickListener {
        void onFileClick(RemoteFile file);
        void onFileLongClick(RemoteFile file);
    }

    public FileAdapter(OnFileClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setFiles(List<RemoteFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        RemoteFile file = files.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewFilename;
        private final TextView textViewFileSize;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewFilename = itemView.findViewById(R.id.textViewFilename);
            textViewFileSize = itemView.findViewById(R.id.textViewFileSize);
        }

        public void bind(RemoteFile file) {
            // Afficher le nom décrypté ou le nom crypté si échec
            String displayName = file.getDecryptedName() != null && !file.getDecryptedName().isEmpty() 
                ? file.getDecryptedName() 
                : file.getName();
            
            if (file.isDirectory()) {
                textViewFilename.setText("📂 " + displayName);  // Dossier avec emoji
                textViewFileSize.setVisibility(View.GONE);
                textViewFilename.setTextColor(0xFF1976D2);  // Bleu pour dossiers
            } else {
                textViewFilename.setText(displayName);
                textViewFileSize.setVisibility(View.VISIBLE);
                textViewFileSize.setText(file.getFormattedSize());
                textViewFilename.setTextColor(0xFF212121);  // Gris foncé pour fichiers
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onFileClick(file);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onFileLongClick(file);
                    return true;
                }
                return false;
            });
        }
    }
}
