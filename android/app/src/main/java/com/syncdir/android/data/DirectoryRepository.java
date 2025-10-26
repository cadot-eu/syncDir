package com.syncdir.android.data;

import android.content.Context;

import java.util.List;

public class DirectoryRepository {
    
    private final DirectoryDao directoryDao;
    
    public DirectoryRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.directoryDao = database.directoryDao();
    }
    
    public long saveDirectory(Directory directory) {
        if (directory.getId() > 0) {
            directoryDao.update(directory);
            return directory.getId();
        } else {
            return directoryDao.insert(directory);
        }
    }
    
    public List<Directory> getAllDirectories() {
        return directoryDao.getAllDirectories();
    }
    
    public Directory getDirectoryById(long id) {
        return directoryDao.getDirectoryById(id);
    }
    
    public Directory getDirectoryByUsernameAndRemoteDir(String username, String remoteDirectory) {
        return directoryDao.getDirectoryByUsernameAndRemoteDir(username, remoteDirectory);
    }
    
    public void updateDirectory(Directory directory) {
        directoryDao.update(directory);
    }
    
    public void deleteDirectory(long id) {
        directoryDao.deleteById(id);
    }
}
