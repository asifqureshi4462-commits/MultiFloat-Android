package com.example.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "todos")
public class TodoEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    public boolean completed;
    public String priority;
    public long createdAt;

    public TodoEntity(String title, boolean completed, String priority, long createdAt) {
        this.title = title;
        this.completed = completed;
        this.priority = priority;
        this.createdAt = createdAt;
    }
}
