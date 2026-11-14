package com.example.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tool_usage")
public class ToolUsageEntity {
    @PrimaryKey
    @NonNull
    public String toolId;
    public long lastOpenedAt;
    public int openCount;

    public ToolUsageEntity(@NonNull String toolId, long lastOpenedAt, int openCount) {
        this.toolId = toolId;
        this.lastOpenedAt = lastOpenedAt;
        this.openCount = openCount;
    }
}
