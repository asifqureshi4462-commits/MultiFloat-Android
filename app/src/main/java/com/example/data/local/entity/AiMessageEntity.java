package com.example.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ai_messages")
public class AiMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String role; // "user" or "assistant" or "error"
    public String text;
    public long timestamp;

    public AiMessageEntity(String role, String text, long timestamp) {
        this.role = role;
        this.text = text;
        this.timestamp = timestamp;
    }
}
