package com.syncdir.android.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "directories")
public class Directory {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "name")
    private String name;
    
    @ColumnInfo(name = "hostname")
    private String hostname;
    
    @ColumnInfo(name = "port")
    private int port;
    
    @ColumnInfo(name = "ssh_private_key")
    private String sshPrivateKey;
    
    @ColumnInfo(name = "username")
    private String username;
    
    @ColumnInfo(name = "remote_directory")
    private String remoteDirectory;
    
    @ColumnInfo(name = "password")
    private String password;
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getSshPrivateKey() {
        return sshPrivateKey;
    }
    
    public void setSshPrivateKey(String sshPrivateKey) {
        this.sshPrivateKey = sshPrivateKey;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRemoteDirectory() {
        return remoteDirectory;
    }
    
    public void setRemoteDirectory(String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}
