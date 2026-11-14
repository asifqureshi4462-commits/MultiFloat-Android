package com.example.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    public String content;
    public long updatedAt;
    public boolean pinned;

    public NoteEntity(String title, String content, long updatedAt, boolean pinned) {
        this.title = title;
        this.content = content;
        this.updatedAt = updatedAt;
        this.pinned = pinned;
    }
}
