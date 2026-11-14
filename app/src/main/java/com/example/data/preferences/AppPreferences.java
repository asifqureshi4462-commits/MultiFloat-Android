package com.example.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.model.ToolType;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class AppPreferences {

    private static final String PREFS_NAME = "multifloat_prefs";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final String ACCENT_ELECTRIC = "electric";
    public static final String ACCENT_CYAN = "cyan";
    public static final String ACCENT_EMERALD = "emerald";
    public static final String ACCENT_VIOLET = "violet";
    public static final String ACCENT_AMBER = "amber";

    public static final String SIDEBAR_LEFT = "left";
    public static final String SIDEBAR_RIGHT = "right";

    public static final String AI_PROVIDER_GEMINI = "gemini";
    public static final String AI_PROVIDER_OPENAI = "openai";

    public static final String LAUNCH_MODE_FLOATING = "floating";
    public static final String LAUNCH_MODE_NORMAL = "normal";
    public static final String LAUNCH_MODE_ASK = "ask";

    public static final String KEY_AI_API = "ai_api_key";
    public static final String KEY_TRANSLATE_API = "translate_api_key";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean("onboarding_completed", false);
    }

    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply();
    }

    public boolean isSetupCenterCompleted() {
        return prefs.getBoolean("setup_center_completed", false);
    }

    public void setSetupCenterCompleted(boolean completed) {
        prefs.edit().putBoolean("setup_center_completed", completed).apply();
    }

    public String getExternalLaunchMode() {
        return prefs.getString("external_launch_mode", LAUNCH_MODE_FLOATING);
    }

    public void setExternalLaunchMode(String mode) {
        if (LAUNCH_MODE_NORMAL.equals(mode) || LAUNCH_MODE_ASK.equals(mode)) {
            prefs.edit().putString("external_launch_mode", mode).apply();
        } else {
            prefs.edit().putString("external_launch_mode", LAUNCH_MODE_FLOATING).apply();
        }
    }

    public int getThemeMode() {
        return prefs.getInt("theme_mode", THEME_DARK);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt("theme_mode", mode).apply();
        applyThemeMode(mode);
    }

    public static void applyThemeMode(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public String getAccentColorKey() {
        return prefs.getString("accent_color", ACCENT_ELECTRIC);
    }

    public void setAccentColorKey(String accentKey) {
        prefs.edit().putString("accent_color", accentKey).apply();
    }

    public int getAccentColorInt() {
        switch (getAccentColorKey()) {
            case ACCENT_CYAN: return 0xFF00E5FF;
            case ACCENT_EMERALD: return 0xFF10B981;
            case ACCENT_VIOLET: return 0xFF8B5CF6;
            case ACCENT_AMBER: return 0xFFF59E0B;
            default: return 0xFF2979FF;
        }
    }

    // Floating Bubble
    public boolean isBubbleEnabled() {
        return prefs.getBoolean("bubble_enabled", true);
    }

    public void setBubbleEnabled(boolean enabled) {
        prefs.edit().putBoolean("bubble_enabled", enabled).apply();
    }

    public int getBubbleSizeDp() {
        return prefs.getInt("bubble_size_dp", 56);
    }

    public void setBubbleSizeDp(int sizeDp) {
        prefs.edit().putInt("bubble_size_dp", Math.max(40, Math.min(84, sizeDp))).apply();
    }

    public int getBubbleOpacityPercent() {
        return prefs.getInt("bubble_opacity_pct", 95);
    }

    public void setBubbleOpacityPercent(int pct) {
        prefs.edit().putInt("bubble_opacity_pct", Math.max(30, Math.min(100, pct))).apply();
    }

    public int getBubbleX(int defaultX) {
        return prefs.getInt("bubble_x", defaultX);
    }

    public int getBubbleY(int defaultY) {
        return prefs.getInt("bubble_y", defaultY);
    }

    public void saveBubblePosition(int x, int y) {
        prefs.edit().putInt("bubble_x", x).putInt("bubble_y", y).apply();
    }

    // Floating Windows
    public int getWindowOpacityPercent() {
        return prefs.getInt("window_opacity_pct", 94);
    }

    public void setWindowOpacityPercent(int pct) {
        prefs.edit().putInt("window_opacity_pct", Math.max(40, Math.min(100, pct))).apply();
    }

    public int getWindowCornerRadiusDp() {
        return prefs.getInt("window_radius_dp", 20);
    }

    public void setWindowCornerRadiusDp(int dp) {
        prefs.edit().putInt("window_radius_dp", Math.max(0, Math.min(32, dp))).apply();
    }

    public int getDefaultWindowScalePercent() {
        return prefs.getInt("default_window_scale_pct", 100);
    }

    public void setDefaultWindowScalePercent(int pct) {
        prefs.edit().putInt("default_window_scale_pct", Math.max(75, Math.min(130, pct))).apply();
    }

    public boolean isWindowAnimationEnabled() {
        return prefs.getBoolean("window_animations", true);
    }

    public void setWindowAnimationEnabled(boolean enabled) {
        prefs.edit().putBoolean("window_animations", enabled).apply();
    }

    public boolean isEdgeSnapEnabled() {
        return prefs.getBoolean("edge_snap_enabled", true);
    }

    public void setEdgeSnapEnabled(boolean enabled) {
        prefs.edit().putBoolean("edge_snap_enabled", enabled).apply();
    }

    public boolean isRememberWindowPositions() {
        return prefs.getBoolean("remember_positions", true);
    }

    public void setRememberWindowPositions(boolean remember) {
        prefs.edit().putBoolean("remember_positions", remember).apply();
    }

    public boolean isRestoreSessionEnabled() {
        return prefs.getBoolean("restore_session", true);
    }

    public void setRestoreSessionEnabled(boolean restore) {
        prefs.edit().putBoolean("restore_session", restore).apply();
    }

    public void saveToolPosition(String toolId, int x, int y, int w, int h) {
        prefs.edit()
                .putInt("pos_x_" + toolId, x)
                .putInt("pos_y_" + toolId, y)
                .putInt("pos_w_" + toolId, w)
                .putInt("pos_h_" + toolId, h)
                .apply();
    }

    public int[] getSavedToolGeometry(String toolId) {
        if (!prefs.contains("pos_x_" + toolId)) return null;
        return new int[]{
                prefs.getInt("pos_x_" + toolId, 40),
                prefs.getInt("pos_y_" + toolId, 160),
                prefs.getInt("pos_w_" + toolId, 0),
                prefs.getInt("pos_h_" + toolId, 0)
        };
    }

    public String getSavedSessionJson() {
        return prefs.getString("saved_window_session", "[]");
    }

    public void saveSessionJson(String json) {
        prefs.edit().putString("saved_window_session", json).apply();
    }

    // Sidebar
    public boolean isSidebarEnabled() {
        return prefs.getBoolean("sidebar_enabled", true);
    }

    public void setSidebarEnabled(boolean enabled) {
        prefs.edit().putBoolean("sidebar_enabled", enabled).apply();
    }

    public String getSidebarPosition() {
        return prefs.getString("sidebar_side", SIDEBAR_RIGHT);
    }

    public void setSidebarPosition(String side) {
        prefs.edit().putString("sidebar_side", side).apply();
    }

    public int getSidebarOpacityPercent() {
        return prefs.getInt("sidebar_opacity_pct", 92);
    }

    public void setSidebarOpacityPercent(int pct) {
        prefs.edit().putInt("sidebar_opacity_pct", Math.max(40, Math.min(100, pct))).apply();
    }

    public int getSidebarSizeLevel() {
        return prefs.getInt("sidebar_size_level", 1); // 0=Compact, 1=Standard, 2=Large
    }

    public void setSidebarSizeLevel(int level) {
        prefs.edit().putInt("sidebar_size_level", Math.max(0, Math.min(2, level))).apply();
    }

    public String getCustomShortcutPackage() {
        return prefs.getString("custom_shortcut_label", "System Settings");
    }

    public void setCustomShortcutLabel(String label) {
        prefs.edit().putString("custom_shortcut_label", label).apply();
    }

    public int getSidebarIconStyle() {
        return prefs.getInt("sidebar_icon_style", 0); // 0=Rounded, 1=Minimal, 2=Accent Ring
    }

    public void setSidebarIconStyle(int style) {
        prefs.edit().putInt("sidebar_icon_style", Math.max(0, Math.min(2, style))).apply();
    }

    public boolean isAllowDuplicateWindows() {
        return prefs.getBoolean("allow_duplicate_windows", false);
    }

    public void setAllowDuplicateWindows(boolean allow) {
        prefs.edit().putBoolean("allow_duplicate_windows", allow).apply();
    }

    // Favorites (Internal Mini-Apps)
    public Set<String> getFavoriteTools() {
        Set<String> stored = prefs.getStringSet("favorite_tools", null);
        if (stored == null) {
            return new LinkedHashSet<>(Arrays.asList(
                    ToolType.CALCULATOR.id,
                    ToolType.NOTES.id,
                    ToolType.BROWSER.id,
                    ToolType.STOPWATCH_TIMER.id
            ));
        }
        return new LinkedHashSet<>(stored);
    }

    public boolean isFavorite(String toolId) {
        return getFavoriteTools().contains(toolId);
    }

    public void toggleFavorite(String toolId) {
        Set<String> current = getFavoriteTools();
        if (current.contains(toolId)) {
            current.remove(toolId);
        } else {
            current.add(toolId);
        }
        prefs.edit().putStringSet("favorite_tools", current).apply();
    }

    // Installed Android Apps: Favorites, Sidebar Selection, Recents, & Custom Order
    public Set<String> getFavoriteApps() {
        Set<String> stored = prefs.getStringSet("favorite_installed_apps", null);
        return stored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stored);
    }

    public boolean isFavoriteApp(String packageName) {
        return packageName != null && getFavoriteApps().contains(packageName);
    }

    public void toggleFavoriteApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        Set<String> current = getFavoriteApps();
        if (current.contains(packageName)) {
            current.remove(packageName);
        } else {
            current.add(packageName);
        }
        prefs.edit().putStringSet("favorite_installed_apps", current).apply();
    }

    public java.util.List<String> getSelectedSidebarApps() {
        return readJsonStringList("selected_sidebar_apps");
    }

    public boolean isSidebarAppSelected(String packageName) {
        return packageName != null && getSelectedSidebarApps().contains(packageName);
    }

    public void setSelectedSidebarApps(java.util.List<String> packages) {
        writeJsonStringList("selected_sidebar_apps", packages);
    }

    public boolean toggleSidebarApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        java.util.List<String> selected = getSelectedSidebarApps();
        java.util.List<String> order = getSidebarCustomOrder();
        String appKey = com.example.model.SidebarShortcut.appKey(packageName);
        boolean nowSelected;
        if (selected.contains(packageName)) {
            selected.remove(packageName);
            order.remove(appKey);
            nowSelected = false;
        } else {
            selected.add(packageName);
            if (!order.contains(appKey)) {
                order.add(0, appKey);
            }
            nowSelected = true;
        }
        setSelectedSidebarApps(selected);
        setSidebarCustomOrder(order);
        return nowSelected;
    }

    public java.util.List<String> getSidebarCustomOrder() {
        java.util.List<String> stored = readJsonStringList("sidebar_custom_order");
        if (stored.isEmpty()) {
            java.util.List<String> defaults = new java.util.ArrayList<>();
            for (String pkg : getSelectedSidebarApps()) {
                defaults.add(com.example.model.SidebarShortcut.appKey(pkg));
            }
            for (ToolType type : ToolType.getOrderedDrawerTools(ToolType.DrawerFilter.ALL, getFavoriteTools())) {
                defaults.add(com.example.model.SidebarShortcut.toolKey(type));
            }
            return defaults;
        }
        // Append any newly added selected apps or tools not yet in stored list
        java.util.List<String> merged = new java.util.ArrayList<>(stored);
        for (String pkg : getSelectedSidebarApps()) {
            String key = com.example.model.SidebarShortcut.appKey(pkg);
            if (!merged.contains(key)) {
                merged.add(0, key);
            }
        }
        for (ToolType type : ToolType.values()) {
            String key = com.example.model.SidebarShortcut.toolKey(type);
            if (!merged.contains(key)) {
                merged.add(key);
            }
        }
        return merged;
    }

    public void setSidebarCustomOrder(java.util.List<String> keys) {
        writeJsonStringList("sidebar_custom_order", keys);
    }

    public void recordRecentApp(String packageName, long timestampMs) {
        if (packageName == null || packageName.isEmpty()) return;
        java.util.Map<String, Long> map = getRecentAppTimestamps();
        java.util.LinkedHashMap<String, Long> updated = new java.util.LinkedHashMap<>();
        updated.put(packageName, timestampMs);
        int count = 1;
        for (java.util.Map.Entry<String, Long> entry : map.entrySet()) {
            if (!packageName.equals(entry.getKey()) && count < 30) {
                updated.put(entry.getKey(), entry.getValue());
                count++;
            }
        }
        org.json.JSONArray arr = new org.json.JSONArray();
        for (java.util.Map.Entry<String, Long> entry : updated.entrySet()) {
            try {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("pkg", entry.getKey());
                obj.put("ts", entry.getValue());
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString("recent_installed_apps", arr.toString()).apply();
    }

    public java.util.Map<String, Long> getRecentAppTimestamps() {
        java.util.LinkedHashMap<String, Long> result = new java.util.LinkedHashMap<>();
        String raw = prefs.getString("recent_installed_apps", "[]");
        try {
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                String pkg = obj.optString("pkg", "");
                long ts = obj.optLong("ts", 0L);
                if (!pkg.isEmpty()) {
                    result.put(pkg, ts);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    public boolean cleanupUninstalledApps(Set<String> installedPackages) {
        if (installedPackages == null) return false;
        boolean changed = false;

        Set<String> favs = getFavoriteApps();
        if (favs.removeIf(pkg -> !installedPackages.contains(pkg))) {
            prefs.edit().putStringSet("favorite_installed_apps", favs).apply();
            changed = true;
        }

        java.util.List<String> selected = getSelectedSidebarApps();
        if (selected.removeIf(pkg -> !installedPackages.contains(pkg))) {
            setSelectedSidebarApps(selected);
            changed = true;
        }

        java.util.List<String> order = readJsonStringList("sidebar_custom_order");
        if (!order.isEmpty()) {
            boolean orderChanged = order.removeIf(key -> {
                if (!com.example.model.SidebarShortcut.isAppKey(key)) return false;
                String pkg = key.substring(com.example.model.SidebarShortcut.PREFIX_APP.length());
                return !installedPackages.contains(pkg);
            });
            if (orderChanged) {
                setSidebarCustomOrder(order);
                changed = true;
            }
        }

        java.util.Map<String, Long> recents = getRecentAppTimestamps();
        if (recents.keySet().removeIf(pkg -> !installedPackages.contains(pkg))) {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (java.util.Map.Entry<String, Long> entry : recents.entrySet()) {
                try {
                    org.json.JSONObject obj = new org.json.JSONObject();
                    obj.put("pkg", entry.getKey());
                    obj.put("ts", entry.getValue());
                    arr.put(obj);
                } catch (Exception ignored) {}
            }
            prefs.edit().putString("recent_installed_apps", arr.toString()).apply();
            changed = true;
        }

        return changed;
    }

    private java.util.List<String> readJsonStringList(String key) {
        java.util.List<String> list = new java.util.ArrayList<>();
        String raw = prefs.getString(key, "[]");
        try {
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String val = arr.optString(i, "");
                if (!val.isEmpty() && !list.contains(val)) {
                    list.add(val);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void writeJsonStringList(String key, java.util.List<String> items) {
        org.json.JSONArray arr = new org.json.JSONArray();
        if (items != null) {
            for (String item : items) {
                if (item != null && !item.isEmpty()) {
                    arr.put(item);
                }
            }
        }
        prefs.edit().putString(key, arr.toString()).apply();
    }

    // AI & Translator API Configuration
    public String getAiProvider() {
        return prefs.getString("ai_provider", AI_PROVIDER_GEMINI);
    }

    public void setAiProvider(String provider) {
        prefs.edit().putString("ai_provider", provider).apply();
    }

    public String getAiModel() {
        return prefs.getString("ai_model", "gemini-2.5-flash");
    }

    public void setAiModel(String model) {
        prefs.edit().putString("ai_model", model).apply();
    }

    public String getAiEndpoint() {
        return prefs.getString("ai_endpoint", "https://api.openai.com/v1/chat/completions");
    }

    public void setAiEndpoint(String endpoint) {
        prefs.edit().putString("ai_endpoint", endpoint).apply();
    }

    public String getTranslateEndpoint() {
        return prefs.getString("translate_endpoint", "https://translation.googleapis.com/language/translate/v2");
    }

    public void setTranslateEndpoint(String endpoint) {
        prefs.edit().putString("translate_endpoint", endpoint).apply();
    }

    public String getBrowserHomeUrl() {
        return prefs.getString("browser_home_url", "https://www.wikipedia.org");
    }

    public void setBrowserHomeUrl(String url) {
        prefs.edit().putString("browser_home_url", url).apply();
    }

    public boolean isClock24Hour() {
        return prefs.getBoolean("clock_24h", false);
    }

    public void setClock24Hour(boolean is24h) {
        prefs.edit().putBoolean("clock_24h", is24h).apply();
    }
}
