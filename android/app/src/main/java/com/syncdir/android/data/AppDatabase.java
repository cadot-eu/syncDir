package com.syncdir.android.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Directory.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "syncdir_db";
    private static volatile AppDatabase INSTANCE;
    
    public abstract DirectoryDao directoryDao();
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration() // Efface DB si version change
                    .allowMainThreadQueries() // Pour simplicité, à éviter en production
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
