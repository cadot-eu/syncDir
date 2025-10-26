package com.syncdir.android.network;

/**
 * Représente un fichier ou dossier distant
 */
public class RemoteFile {
    
    private String name;           // Nom chiffré
    private String decryptedName;  // Nom décrypté
    private boolean isDirectory;
    private long size;
    private long modifiedTime;
    private String path;           // Chemin relatif depuis encrypted/
    
    public RemoteFile() {
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDecryptedName() {
        return decryptedName != null ? decryptedName : name;
    }
    
    public void setDecryptedName(String decryptedName) {
        this.decryptedName = decryptedName;
    }
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public long getModifiedTime() {
        return modifiedTime;
    }
    
    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Retourne une taille lisible (Ko, Mo, Go)
     */
    public String getFormattedSize() {
        if (isDirectory) {
            return "--";
        }
        
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f Ko", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f Mo", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f Go", size / (1024.0 * 1024 * 1024));
        }
    }
}
