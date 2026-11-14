package com.example.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.MultiFloatApp;
import com.example.service.MediaSessionMonitorService;

public class PermissionHelper {

    public enum BatteryOptimizationState {
        UNRESTRICTED,
        OPTIMIZED,
        UNKNOWN
    }

    public static boolean canDrawOverlays(Context context) {
        return context != null && Settings.canDrawOverlays(context);
    }

    public static boolean hasCameraPermission(Context context) {
        return context != null && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasNotificationPermission(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static boolean isForegroundServiceReady(Context context) {
        if (context == null) return false;
        if (!canDrawOverlays(context) || !hasNotificationPermission(context)) {
            return false;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = nm.getNotificationChannel(MultiFloatApp.CHANNEL_ENGINE);
            if (ch != null && ch.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                return false;
            }
        }
        return true;
    }

    public static BatteryOptimizationState getBatteryOptimizationState(Context context) {
        if (context == null) return BatteryOptimizationState.UNKNOWN;
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm == null) return BatteryOptimizationState.UNKNOWN;
            boolean ignoring = pm.isIgnoringBatteryOptimizations(context.getPackageName());
            return ignoring ? BatteryOptimizationState.UNRESTRICTED : BatteryOptimizationState.OPTIMIZED;
        } catch (Exception e) {
            return BatteryOptimizationState.UNKNOWN;
        }
    }

    public static boolean isNotificationListenerEnabled(Context context) {
        if (context == null) return false;
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (flat == null || flat.isEmpty()) return false;
        ComponentName cn = new ComponentName(context, MediaSessionMonitorService.class);
        return flat.contains(cn.flattenToString());
    }

    public static Intent buildOverlayPermissionIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent buildAppSettingsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent buildAppNotificationSettingsIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }
        return buildAppSettingsIntent(context);
    }

    public static Intent buildBatteryOptimizationSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent buildDeviceInfoSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent buildDeveloperOptionsIntent() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent buildGeneralSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent buildNotificationListenerSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static boolean launchSettingsSafely(Context context, Intent primary, Intent... fallbacks) {
        if (context == null) return false;
        try {
            if (primary != null && primary.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(primary);
                return true;
            }
        } catch (Exception ignored) {}
        if (fallbacks != null) {
            for (Intent fb : fallbacks) {
                if (fb == null) continue;
                try {
                    if (fb.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(fb);
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }
}
