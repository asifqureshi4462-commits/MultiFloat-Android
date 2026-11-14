package com.example.model;

public class MiniAppItem {
    public final ToolType toolType;
    public final String title;
    public final String subtitle;
    public boolean isFavorite;
    public long lastOpenedAt;

    public MiniAppItem(ToolType toolType, String title, String subtitle, boolean isFavorite, long lastOpenedAt) {
        this.toolType = toolType;
        this.title = title;
        this.subtitle = subtitle;
        this.isFavorite = isFavorite;
        this.lastOpenedAt = lastOpenedAt;
    }
}
