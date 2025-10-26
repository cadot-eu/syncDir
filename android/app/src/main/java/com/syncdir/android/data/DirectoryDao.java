package com.syncdir.android.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DirectoryDao {
    
    @Query("SELECT * FROM directories ORDER BY name ASC")
    List<Directory> getAllDirectories();
    
    @Query("SELECT * FROM directories WHERE id = :id")
    Directory getDirectoryById(long id);
    
    @Query("SELECT * FROM directories WHERE username = :username AND remote_directory = :remoteDirectory LIMIT 1")
    Directory getDirectoryByUsernameAndRemoteDir(String username, String remoteDirectory);
    
    @Insert
    long insert(Directory directory);
    
    @Update
    void update(Directory directory);
    
    @Delete
    void delete(Directory directory);
    
    @Query("DELETE FROM directories WHERE id = :id")
    void deleteById(long id);
}
