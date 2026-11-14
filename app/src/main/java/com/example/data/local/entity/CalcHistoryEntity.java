package com.example.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "calc_history")
public class CalcHistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String expression;
    public String result;
    public long createdAt;

    public CalcHistoryEntity(String expression, String result, long createdAt) {
        this.expression = expression;
        this.result = result;
        this.createdAt = createdAt;
    }
}
