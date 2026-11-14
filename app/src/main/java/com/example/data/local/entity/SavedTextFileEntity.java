package com.example.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_text_files")
public class SavedTextFileEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String fileName;
    public String content;
    public long updatedAt;

    public SavedTextFileEntity(String fileName, String content, long updatedAt) {
        this.fileName = fileName;
        this.content = content;
        this.updatedAt = updatedAt;
    }
}
