package com.syncdir.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.syncdir.android.R;
import com.syncdir.android.data.Directory;

import java.util.ArrayList;
import java.util.List;

public class DirectoryAdapter extends RecyclerView.Adapter<DirectoryAdapter.ViewHolder> {
    
    private List<Directory> directories = new ArrayList<>();
    private final OnDirectoryClickListener clickListener;
    private final OnDirectoryLongClickListener longClickListener;
    private final OnDirectoryShareListener shareListener;
    
    public interface OnDirectoryClickListener {
        void onDirectoryClick(Directory directory);
    }
    
    public interface OnDirectoryLongClickListener {
        void onDirectoryLongClick(Directory directory);
    }
    
    public interface OnDirectoryShareListener {
        void onDirectoryShare(Directory directory);
    }
    
    public DirectoryAdapter(OnDirectoryClickListener clickListener, 
                           OnDirectoryLongClickListener longClickListener,
                           OnDirectoryShareListener shareListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.shareListener = shareListener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_directory, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Directory directory = directories.get(position);
        holder.bind(directory);
    }
    
    @Override
    public int getItemCount() {
        return directories.size();
    }
    
    public void setDirectories(List<Directory> directories) {
        this.directories = directories;
        notifyDataSetChanged();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textInfo;
        private final ImageButton buttonShare;
        
        ViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textDirectoryName);
            textInfo = itemView.findViewById(R.id.textDirectoryInfo);
            buttonShare = itemView.findViewById(R.id.buttonShare);
        }
        
        void bind(Directory directory) {
            textName.setText(directory.getName());
            textInfo.setText(directory.getRemoteDirectory() + " @ " + directory.getHostname());
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onDirectoryClick(directory);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onDirectoryLongClick(directory);
                }
                return true;
            });
            
            buttonShare.setOnClickListener(v -> {
                if (shareListener != null) {
                    shareListener.onDirectoryShare(directory);
                }
            });
        }
    }
}
