package com.example.model;

import androidx.annotation.Nullable;

/**
 * Unified shortcut descriptor for custom ordering in the MultiFloat sidebar drawer.
 * Keys use "tool:<id>" for internal mini-apps and "app:<packageName>" for installed Android apps.
 */
public final class SidebarShortcut {

    public static final String PREFIX_TOOL = "tool:";
    public static final String PREFIX_APP = "app:";

    public final String key;
    public final boolean isExternalApp;
    @Nullable public final ToolType toolType;
    @Nullable public final String packageName;

    private SidebarShortcut(String key, boolean isExternalApp,
                            @Nullable ToolType toolType, @Nullable String packageName) {
        this.key = key;
        this.isExternalApp = isExternalApp;
        this.toolType = toolType;
        this.packageName = packageName;
    }

    public static String toolKey(ToolType type) {
        return PREFIX_TOOL + type.id;
    }

    public static String appKey(String packageName) {
        return PREFIX_APP + packageName;
    }

    public static boolean isToolKey(@Nullable String key) {
        return key != null && key.startsWith(PREFIX_TOOL);
    }

    public static boolean isAppKey(@Nullable String key) {
        return key != null && key.startsWith(PREFIX_APP);
    }

    public static SidebarShortcut forTool(ToolType type) {
        return new SidebarShortcut(toolKey(type), false, type, null);
    }

    public static SidebarShortcut forApp(String packageName) {
        return new SidebarShortcut(appKey(packageName), true, null, packageName);
    }

    @Nullable
    public static SidebarShortcut fromKey(@Nullable String key) {
        if (key == null) return null;
        if (key.startsWith(PREFIX_TOOL)) {
            String id = key.substring(PREFIX_TOOL.length());
            for (ToolType t : ToolType.values()) {
                if (t.id.equals(id)) {
                    return forTool(t);
                }
            }
            return null;
        }
        if (key.startsWith(PREFIX_APP)) {
            String pkg = key.substring(PREFIX_APP.length());
            if (!pkg.isEmpty()) {
                return forApp(pkg);
            }
        }
        return null;
    }
}
