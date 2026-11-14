package com.example.data.repository;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.example.data.preferences.AppPreferences;
import com.example.model.InstalledAppInfo;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Queries real launchable Android applications installed on the device using PackageManager,
 * sorts alphabetically, tracks recents/favorites/sidebar selection, and prunes uninstalled apps.
 */
public class InstalledAppsRepository {

    public enum FilterMode {
        ALL,
        RECENT,
        FAVORITES,
        SIDEBAR
    }

    private final AppPreferences preferences;

    public InstalledAppsRepository(AppPreferences preferences) {
        this.preferences = preferences;
    }

    public List<InstalledAppInfo> loadLaunchableApps(Context context) {
        PackageManager pm = context.getPackageManager();
        String selfPkg = context.getPackageName();

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved = pm.queryIntentActivities(launcherIntent, 0);
        if (resolved == null) {
            resolved = Collections.emptyList();
        }

        Set<String> installedPackages = new HashSet<>();
        Map<String, ResolveInfo> uniqueByPkg = new HashMap<>();

        for (ResolveInfo ri : resolved) {
            if (ri.activityInfo == null || ri.activityInfo.packageName == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(selfPkg)) continue;
            installedPackages.add(pkg);
            if (!uniqueByPkg.containsKey(pkg)) {
                uniqueByPkg.put(pkg, ri);
            }
        }

        // Remove any previously saved packages that are no longer installed
        preferences.cleanupUninstalledApps(installedPackages);

        Set<String> favorites = preferences.getFavoriteApps();
        Set<String> sidebarPinned = new HashSet<>(preferences.getSelectedSidebarApps());
        Map<String, Long> recents = preferences.getRecentAppTimestamps();

        List<InstalledAppInfo> result = new ArrayList<>(uniqueByPkg.size());
        for (ResolveInfo ri : uniqueByPkg.values()) {
            String pkg = ri.activityInfo.packageName;
            CharSequence rawLabel = ri.loadLabel(pm);
            String label = (rawLabel != null && rawLabel.length() > 0) ? rawLabel.toString() : pkg;
            Drawable icon = null;
            try {
                icon = ri.loadIcon(pm);
            } catch (Exception ignored) {}

            long lastUsed = recents.containsKey(pkg) ? recents.get(pkg) : 0L;
            result.add(new InstalledAppInfo(
                    pkg,
                    ri.activityInfo.name != null ? ri.activityInfo.name : "",
                    label,
                    icon,
                    lastUsed,
                    favorites.contains(pkg),
                    sidebarPinned.contains(pkg)
            ));
        }

        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(result, (a, b) -> {
            int cmp = collator.compare(a.appName, b.appName);
            return cmp != 0 ? cmp : a.packageName.compareToIgnoreCase(b.packageName);
        });
        return result;
    }

    @Nullable
    public InstalledAppInfo loadAppByPackage(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return null;
        PackageManager pm = context.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        query.setPackage(packageName);

        List<ResolveInfo> resolved = pm.queryIntentActivities(query, 0);
        if (resolved == null || resolved.isEmpty()) {
            return null;
        }
        ResolveInfo ri = resolved.get(0);
        CharSequence rawLabel = ri.loadLabel(pm);
        String label = (rawLabel != null && rawLabel.length() > 0) ? rawLabel.toString() : packageName;
        Drawable icon = null;
        try {
            icon = ri.loadIcon(pm);
        } catch (Exception ignored) {}
        long lastUsed = preferences.getRecentAppTimestamps().getOrDefault(packageName, 0L);
        return new InstalledAppInfo(
                packageName,
                ri.activityInfo != null && ri.activityInfo.name != null ? ri.activityInfo.name : "",
                label,
                icon,
                lastUsed,
                preferences.isFavoriteApp(packageName),
                preferences.isSidebarAppSelected(packageName)
        );
    }

    public boolean isAppInstalled(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        PackageManager pm = context.getPackageManager();
        return pm.getLaunchIntentForPackage(packageName) != null;
    }

    public static List<InstalledAppInfo> filterAndSort(
            List<InstalledAppInfo> allApps,
            @Nullable String query,
            FilterMode mode) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        List<InstalledAppInfo> filtered = new ArrayList<>();
        for (InstalledAppInfo app : allApps) {
            if (mode == FilterMode.FAVORITES && !app.isFavorite) continue;
            if (mode == FilterMode.SIDEBAR && !app.isSidebarPinned) continue;
            if (mode == FilterMode.RECENT && app.lastUsedTimeMs <= 0L) continue;

            if (!q.isEmpty()) {
                boolean matchName = app.appName.toLowerCase(Locale.US).contains(q);
                boolean matchPkg = app.packageName.toLowerCase(Locale.US).contains(q);
                if (!matchName && !matchPkg) continue;
            }
            filtered.add(app);
        }

        if (mode == FilterMode.RECENT) {
            Collections.sort(filtered, (a, b) -> Long.compare(b.lastUsedTimeMs, a.lastUsedTimeMs));
        } else {
            Collator collator = Collator.getInstance(Locale.getDefault());
            collator.setStrength(Collator.PRIMARY);
            Collections.sort(filtered, Comparator.comparing((InstalledAppInfo a) -> a.appName, collator));
        }
        return filtered;
    }
}
