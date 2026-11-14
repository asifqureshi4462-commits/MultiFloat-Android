package com.example.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clipboard_entries")
public class ClipboardEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String text;
    public long copiedAt;

    public ClipboardEntity(String text, long copiedAt) {
        this.text = text;
        this.copiedAt = copiedAt;
    }
}
