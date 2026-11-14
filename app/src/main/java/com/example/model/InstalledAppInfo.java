package com.example.model;

import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;

public class InstalledAppInfo {
    public final String packageName;
    public final String activityName;
    public final String appName;
    @Nullable
    public final Drawable icon;
    public long lastUsedTimeMs;
    public boolean isFavorite;
    public boolean isSidebarPinned;

    public InstalledAppInfo(
            String packageName,
            String activityName,
            String appName,
            @Nullable Drawable icon,
            long lastUsedTimeMs,
            boolean isFavorite,
            boolean isSidebarPinned) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.appName = appName;
        this.icon = icon;
        this.lastUsedTimeMs = lastUsedTimeMs;
        this.isFavorite = isFavorite;
        this.isSidebarPinned = isSidebarPinned;
    }
}
