package com.example.util;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.provider.Settings;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import com.example.MultiFloatApp;
import com.example.data.preferences.AppPreferences;
import com.example.service.WindowBoundsHelper;
import java.util.HashSet;
import java.util.Set;

/**
 * Genuine external Android app launcher with Android Freeform Multi-Window capability detection.
 * Uses only public Android SDK APIs (PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT and
 * ActivityOptions.setLaunchBounds) and never fakes external apps via WebViews or screenshots.
 */
public final class ExternalAppLauncher {

    public enum LaunchStatus {
        LAUNCHED_FREEFORM,
        LAUNCHED_NORMAL,
        FREEFORM_UNSUPPORTED,
        APP_UNINSTALLED,
        FAILED
    }

    private ExternalAppLauncher() {}

    private static final String TAG = "ExternalAppLauncher";

    /** Hidden-API value of WindowConfiguration.WINDOWING_MODE_FREEFORM. */
    private static final int WINDOWING_MODE_FREEFORM = 5;

    public static boolean isFreeformSupported(Context context) {
        if (context == null) return false;
        PackageManager pm = context.getPackageManager();
        if (pm == null) return false;
        return pm.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT);
    }

    /** True when the "Enable freeform windows" developer option is switched on. */
    public static boolean isDeveloperFreeformEnabled(Context context) {
        if (context == null) return false;
        try {
            return Settings.Global.getInt(context.getContentResolver(), "enable_freeform_support", 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /** Freeform launch is worth attempting if the system feature or the developer toggle is present. */
    public static boolean canAttemptFreeform(Context context) {
        return isFreeformSupported(context) || isDeveloperFreeformEnabled(context);
    }

    /**
     * Asks the system to open the activity in freeform mode. Without an explicit windowing
     * mode, setLaunchBounds() is ignored on fullscreen displays and the app opens full screen.
     * The method is a hidden API, so it is called through reflection and failures are ignored.
     */
    private static void requestFreeformWindowingMode(ActivityOptions options) {
        try {
            java.lang.reflect.Method m = ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class);
            m.invoke(options, WINDOWING_MODE_FREEFORM);
        } catch (Throwable t) {
            Log.w(TAG, "setLaunchWindowingMode not available: " + t);
        }
    }

    public static Rect computeDefaultFreeformBounds(Context context, @Nullable Point dropCenter) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Rect safe = wm != null
                ? WindowBoundsHelper.getSafeScreenBounds(wm, context)
                : new Rect(24, 96, 1056, 2000);
        int winW = Math.min(safe.width() - EdgeToEdgeHelper.dpToPx(context, 16), EdgeToEdgeHelper.dpToPx(context, 330));
        int winH = Math.min(safe.height() - EdgeToEdgeHelper.dpToPx(context, 32), EdgeToEdgeHelper.dpToPx(context, 480));
        int x;
        int y;
        if (dropCenter != null) {
            x = dropCenter.x - winW / 2;
            y = dropCenter.y - winH / 2;
        } else {
            x = safe.centerX() - winW / 2;
            y = safe.centerY() - winH / 2;
        }
        WindowBoundsHelper.ClampedPoint clamped = WindowBoundsHelper.clampWindowPosition(x, y, winW, winH, safe);
        return new Rect(clamped.x, clamped.y, clamped.x + winW, clamped.y + winH);
    }

    public static LaunchStatus launchExternalApp(
            Context context,
            String packageName,
            @Nullable Rect preferredBounds,
            boolean allowNormalFallback) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return LaunchStatus.APP_UNINSTALLED;
        }

        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            pruneMissingPackage(context, packageName);
            return LaunchStatus.APP_UNINSTALLED;
        }

        boolean freeformSupported = canAttemptFreeform(context);
        if (!freeformSupported && !allowNormalFallback) {
            return LaunchStatus.FREEFORM_UNSUPPORTED;
        }

        recordAppUsed(context, packageName);

        if (freeformSupported) {
            try {
                Intent freeformIntent = new Intent(launchIntent);
                freeformIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                ActivityOptions options = ActivityOptions.makeBasic();
                Rect bounds = preferredBounds != null
                        ? preferredBounds
                        : computeDefaultFreeformBounds(context, null);
                options.setLaunchBounds(bounds);
                requestFreeformWindowingMode(options);
                context.startActivity(freeformIntent, options.toBundle());
                return LaunchStatus.LAUNCHED_FREEFORM;
            } catch (Exception e) {
                if (!allowNormalFallback) {
                    return LaunchStatus.FREEFORM_UNSUPPORTED;
                }
            }
        }

        try {
            Intent normalIntent = new Intent(launchIntent);
            normalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(normalIntent);
            return LaunchStatus.LAUNCHED_NORMAL;
        } catch (Exception e) {
            return LaunchStatus.FAILED;
        }
    }

    public static Intent buildDeveloperOptionsIntent() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private static void recordAppUsed(Context context, String packageName) {
        AppPreferences prefs = MultiFloatApp.get() != null
                ? MultiFloatApp.get().getPreferences()
                : new AppPreferences(context);
        prefs.recordRecentApp(packageName, System.currentTimeMillis());
    }

    private static void pruneMissingPackage(Context context, String missingPackage) {
        AppPreferences prefs = MultiFloatApp.get() != null
                ? MultiFloatApp.get().getPreferences()
                : new AppPreferences(context);
        PackageManager pm = context.getPackageManager();
        Set<String> valid = new HashSet<>();
        for (String pkg : prefs.getFavoriteApps()) {
            if (!pkg.equals(missingPackage) && pm.getLaunchIntentForPackage(pkg) != null) valid.add(pkg);
        }
        for (String pkg : prefs.getSelectedSidebarApps()) {
            if (!pkg.equals(missingPackage) && pm.getLaunchIntentForPackage(pkg) != null) valid.add(pkg);
        }
        for (String pkg : prefs.getRecentAppTimestamps().keySet()) {
            if (!pkg.equals(missingPackage) && pm.getLaunchIntentForPackage(pkg) != null) valid.add(pkg);
        }
        prefs.cleanupUninstalledApps(valid);
    }
}
