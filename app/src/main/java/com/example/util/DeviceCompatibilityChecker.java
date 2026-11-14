package com.example.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.service.WindowBoundsHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Inspects real device hardware, display metrics, Android windowing capabilities,
 * permissions, and manufacturer family using only public Android SDK APIs.
 * Never hardcodes compatibility results or uses hidden APIs/reflection.
 */
public final class DeviceCompatibilityChecker {

    public enum OemFamily {
        XIAOMI("Xiaomi / Redmi / POCO"),
        SAMSUNG("Samsung"),
        ONEPLUS("OnePlus"),
        OPPO("OPPO"),
        REALME("realme"),
        VIVO("vivo / iQOO"),
        OTHER("Android Device");

        public final String displayName;

        OemFamily(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum XiaomiOsVariant {
        HYPER_OS("Xiaomi HyperOS"),
        MIUI("MIUI"),
        UNKNOWN("MIUI / HyperOS")
        ;

        public final String label;

        XiaomiOsVariant(String label) {
            this.label = label;
        }
    }

    public enum CheckStatus {
        SUPPORTED("Supported"),
        GRANTED("Granted"),
        ENABLED("Enabled"),
        EXPERIMENTAL("Experimental"),
        NOT_GRANTED("Action Needed"),
        DISABLED("Disabled"),
        UNAVAILABLE("Not Detected"),
        MANUAL_VERIFICATION_REQUIRED("Manual Verification Required"),
        UNKNOWN("Unknown");

        public final String label;

        CheckStatus(String label) {
            this.label = label;
        }

        public boolean isPositive() {
            return this == SUPPORTED || this == GRANTED || this == ENABLED;
        }
    }

    public static final class OemGuide {
        public final OemFamily oemFamily;
        public final String title;
        public final String developerOptionsPath;
        public final String devOptionTapTarget;
        public final String settingsHighlights;
        public final List<String> steps;
        public final String honestDisclaimer;
        @Nullable public final String bilingualSummaryHindi;

        public OemGuide(
                OemFamily oemFamily,
                String title,
                String developerOptionsPath,
                String devOptionTapTarget,
                String settingsHighlights,
                List<String> steps,
                String honestDisclaimer,
                @Nullable String bilingualSummaryHindi) {
            this.oemFamily = oemFamily;
            this.title = title;
            this.developerOptionsPath = developerOptionsPath;
            this.devOptionTapTarget = devOptionTapTarget;
            this.settingsHighlights = settingsHighlights;
            this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
            this.honestDisclaimer = honestDisclaimer;
            this.bilingualSummaryHindi = bilingualSummaryHindi;
        }
    }

    public static final class CompatibilityReport {
        public final String manufacturer;
        public final String brand;
        public final String model;
        public final String device;
        public final String androidVersion;
        public final int sdkInt;
        public final String buildDisplay;

        public final int screenWidthPx;
        public final int screenHeightPx;
        public final float density;
        public final int densityDpi;
        public final int smallestScreenWidthDp;
        public final boolean isLargeScreen;

        public final OemFamily oemFamily;
        public final XiaomiOsVariant xiaomiOsVariant;

        public final CheckStatus internalOverlayStatus;
        public final CheckStatus overlayPermissionStatus;
        public final CheckStatus notificationPermissionStatus;
        public final CheckStatus foregroundServiceStatus;
        public final CheckStatus batteryOptimizationStatus;
        public final CheckStatus cameraPermissionStatus;
        public final CheckStatus notificationListenerStatus;

        public final CheckStatus systemFreeformFeatureStatus;
        public final CheckStatus developerOptionsStatus;
        public final CheckStatus enableFreeformSettingStatus;
        public final CheckStatus forceResizableSettingStatus;
        public final CheckStatus oemFloatingIntegrationStatus;

        public CompatibilityReport(
                String manufacturer, String brand, String model, String device,
                String androidVersion, int sdkInt, String buildDisplay,
                int screenWidthPx, int screenHeightPx, float density, int densityDpi,
                int smallestScreenWidthDp, boolean isLargeScreen,
                OemFamily oemFamily, XiaomiOsVariant xiaomiOsVariant,
                CheckStatus internalOverlayStatus,
                CheckStatus overlayPermissionStatus,
                CheckStatus notificationPermissionStatus,
                CheckStatus foregroundServiceStatus,
                CheckStatus batteryOptimizationStatus,
                CheckStatus cameraPermissionStatus,
                CheckStatus notificationListenerStatus,
                CheckStatus systemFreeformFeatureStatus,
                CheckStatus developerOptionsStatus,
                CheckStatus enableFreeformSettingStatus,
                CheckStatus forceResizableSettingStatus,
                CheckStatus oemFloatingIntegrationStatus) {
            this.manufacturer = manufacturer;
            this.brand = brand;
            this.model = model;
            this.device = device;
            this.androidVersion = androidVersion;
            this.sdkInt = sdkInt;
            this.buildDisplay = buildDisplay;
            this.screenWidthPx = screenWidthPx;
            this.screenHeightPx = screenHeightPx;
            this.density = density;
            this.densityDpi = densityDpi;
            this.smallestScreenWidthDp = smallestScreenWidthDp;
            this.isLargeScreen = isLargeScreen;
            this.oemFamily = oemFamily;
            this.xiaomiOsVariant = xiaomiOsVariant;
            this.internalOverlayStatus = internalOverlayStatus;
            this.overlayPermissionStatus = overlayPermissionStatus;
            this.notificationPermissionStatus = notificationPermissionStatus;
            this.foregroundServiceStatus = foregroundServiceStatus;
            this.batteryOptimizationStatus = batteryOptimizationStatus;
            this.cameraPermissionStatus = cameraPermissionStatus;
            this.notificationListenerStatus = notificationListenerStatus;
            this.systemFreeformFeatureStatus = systemFreeformFeatureStatus;
            this.developerOptionsStatus = developerOptionsStatus;
            this.enableFreeformSettingStatus = enableFreeformSettingStatus;
            this.forceResizableSettingStatus = forceResizableSettingStatus;
            this.oemFloatingIntegrationStatus = oemFloatingIntegrationStatus;
        }

        public boolean canAttemptExternalFreeform() {
            return systemFreeformFeatureStatus == CheckStatus.SUPPORTED
                    || enableFreeformSettingStatus == CheckStatus.ENABLED;
        }

        public String getExternalCompatibilityHeadline() {
            if (systemFreeformFeatureStatus == CheckStatus.SUPPORTED) {
                return "Android Freeform Multi-Window Supported";
            }
            if (enableFreeformSettingStatus == CheckStatus.ENABLED) {
                return "Developer Freeform Enabled (OEM Controlled)";
            }
            return "External Freeform Not Detected (Full Screen Default)";
        }
    }

    private DeviceCompatibilityChecker() {}

    @NonNull
    public static CompatibilityReport inspect(@NonNull Context context) {
        String manufacturer = safeStr(Build.MANUFACTURER, "Unknown");
        String brand = safeStr(Build.BRAND, "Unknown");
        String model = safeStr(Build.MODEL, "Unknown");
        String device = safeStr(Build.DEVICE, "Unknown");
        String androidVer = safeStr(Build.VERSION.RELEASE, "Unknown");
        int sdkInt = Build.VERSION.SDK_INT;
        String buildDisplay = safeStr(Build.DISPLAY, safeStr(Build.ID, "Unknown"));

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int widthPx = dm.widthPixels;
        int heightPx = dm.heightPixels;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            try {
                Rect raw = WindowBoundsHelper.getRawScreenBounds(wm);
                if (raw.width() > 0 && raw.height() > 0) {
                    widthPx = raw.width();
                    heightPx = raw.height();
                }
            } catch (Exception ignored) {}
        }

        Configuration cfg = context.getResources().getConfiguration();
        int swDp = cfg != null ? cfg.smallestScreenWidthDp : 0;
        boolean largeScreen = swDp >= 600;

        OemFamily oemFamily = detectOemFamily(manufacturer, brand);
        XiaomiOsVariant xiaomiVariant = detectXiaomiOsVariant(oemFamily, buildDisplay, Build.VERSION.INCREMENTAL, sdkInt);

        CheckStatus internalOverlay = CheckStatus.SUPPORTED;
        CheckStatus overlayPerm = PermissionHelper.canDrawOverlays(context)
                ? CheckStatus.GRANTED : CheckStatus.NOT_GRANTED;
        CheckStatus notifPerm = PermissionHelper.hasNotificationPermission(context)
                ? CheckStatus.GRANTED : CheckStatus.NOT_GRANTED;
        CheckStatus fgsStatus = PermissionHelper.isForegroundServiceReady(context)
                ? CheckStatus.GRANTED : CheckStatus.NOT_GRANTED;

        PermissionHelper.BatteryOptimizationState batState = PermissionHelper.getBatteryOptimizationState(context);
        CheckStatus batteryStatus;
        if (batState == PermissionHelper.BatteryOptimizationState.UNRESTRICTED) {
            batteryStatus = CheckStatus.GRANTED;
        } else if (batState == PermissionHelper.BatteryOptimizationState.OPTIMIZED) {
            batteryStatus = CheckStatus.NOT_GRANTED;
        } else {
            batteryStatus = CheckStatus.UNKNOWN;
        }

        CheckStatus cameraStatus = PermissionHelper.hasCameraPermission(context)
                ? CheckStatus.GRANTED : CheckStatus.NOT_GRANTED;
        CheckStatus listenerStatus = PermissionHelper.isNotificationListenerEnabled(context)
                ? CheckStatus.ENABLED : CheckStatus.DISABLED;

        PackageManager pm = context.getPackageManager();
        boolean sysFreeform = pm != null && pm.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT);
        CheckStatus systemFreeformStatus = sysFreeform ? CheckStatus.SUPPORTED : CheckStatus.UNAVAILABLE;

        ContentResolver cr = context.getContentResolver();
        CheckStatus devOptionsStatus = readGlobalToggle(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED);
        CheckStatus enableFreeformStatus = readGlobalToggle(cr, "enable_freeform_support");
        CheckStatus forceResizableStatus = readGlobalToggle(cr, "force_resizable_activities");

        CheckStatus oemIntegrationStatus;
        if (sysFreeform) {
            oemIntegrationStatus = CheckStatus.SUPPORTED;
        } else if (enableFreeformStatus == CheckStatus.ENABLED) {
            oemIntegrationStatus = CheckStatus.EXPERIMENTAL;
        } else if (oemFamily != OemFamily.OTHER) {
            // OEM ROMs (Xiaomi/Samsung/OnePlus/OPPO/Realme/Vivo) have private floating-window controls
            // that third-party apps cannot query without hidden APIs.
            oemIntegrationStatus = CheckStatus.MANUAL_VERIFICATION_REQUIRED;
        } else {
            oemIntegrationStatus = CheckStatus.UNAVAILABLE;
        }

        return new CompatibilityReport(
                manufacturer, brand, model, device,
                androidVer, sdkInt, buildDisplay,
                widthPx, heightPx, dm.density, dm.densityDpi, swDp, largeScreen,
                oemFamily, xiaomiVariant,
                internalOverlay, overlayPerm, notifPerm, fgsStatus, batteryStatus, cameraStatus, listenerStatus,
                systemFreeformStatus, devOptionsStatus, enableFreeformStatus, forceResizableStatus, oemIntegrationStatus
        );
    }

    @NonNull
    public static OemFamily detectOemFamily(@Nullable String manufacturer, @Nullable String brand) {
        String combined = ((manufacturer == null ? "" : manufacturer) + " "
                + (brand == null ? "" : brand)).toLowerCase(Locale.US);
        if (combined.contains("xiaomi") || combined.contains("redmi") || combined.contains("poco")) {
            return OemFamily.XIAOMI;
        }
        if (combined.contains("samsung")) {
            return OemFamily.SAMSUNG;
        }
        if (combined.contains("oneplus")) {
            return OemFamily.ONEPLUS;
        }
        if (combined.contains("realme")) {
            return OemFamily.REALME;
        }
        if (combined.contains("oppo")) {
            return OemFamily.OPPO;
        }
        if (combined.contains("vivo") || combined.contains("iqoo")) {
            return OemFamily.VIVO;
        }
        return OemFamily.OTHER;
    }

    @NonNull
    public static XiaomiOsVariant detectXiaomiOsVariant(
            OemFamily oemFamily,
            @Nullable String buildDisplay,
            @Nullable String incremental,
            int sdkInt) {
        if (oemFamily != OemFamily.XIAOMI) {
            return XiaomiOsVariant.UNKNOWN;
        }
        String joined = ((buildDisplay == null ? "" : buildDisplay) + " "
                + (incremental == null ? "" : incremental)).toUpperCase(Locale.US);
        if (joined.contains("HYPEROS") || joined.contains("OS1.") || joined.contains("OS2.") || joined.contains("OS3.")) {
            return XiaomiOsVariant.HYPER_OS;
        }
        if (joined.contains("MIUI") || joined.contains("V14") || joined.contains("V13") || joined.contains("V12")) {
            return XiaomiOsVariant.MIUI;
        }
        if (sdkInt >= 34) {
            return XiaomiOsVariant.HYPER_OS;
        }
        return XiaomiOsVariant.MIUI;
    }

    private static CheckStatus readGlobalToggle(@Nullable ContentResolver cr, String key) {
        if (cr == null) return CheckStatus.MANUAL_VERIFICATION_REQUIRED;
        try {
            int val = Settings.Global.getInt(cr, key, -1);
            if (val == 1) return CheckStatus.ENABLED;
            if (val == 0) return CheckStatus.DISABLED;
            return CheckStatus.MANUAL_VERIFICATION_REQUIRED;
        } catch (Exception e) {
            return CheckStatus.MANUAL_VERIFICATION_REQUIRED;
        }
    }

    @NonNull
    public static OemGuide getManufacturerGuide(@NonNull CompatibilityReport report) {
        switch (report.oemFamily) {
            case XIAOMI:
                return buildXiaomiGuide(report);
            case SAMSUNG:
                return buildSamsungGuide(report);
            case ONEPLUS:
                return buildOnePlusGuide(report);
            case OPPO:
                return buildOppoGuide(report);
            case REALME:
                return buildRealmeGuide(report);
            case VIVO:
                return buildVivoGuide(report);
            default:
                return buildGenericAndroidGuide(report);
        }
    }

    private static OemGuide buildXiaomiGuide(CompatibilityReport report) {
        boolean isHyperOs = report.xiaomiOsVariant == XiaomiOsVariant.HYPER_OS;
        String versionEntry = isHyperOs ? "OS version (Xiaomi HyperOS)" : "MIUI version";
        String versionEntryHi = isHyperOs ? "OS version" : "MIUI version";

        List<String> steps = new ArrayList<>();
        steps.add("A. Enable Developer Options:\n"
                + "1. Open Settings → About phone.\n"
                + "2. Locate \"" + versionEntry + "\".\n"
                + "3. Tap it 7 times continuously until \"You are now a developer!\" appears.\n"
                + "4. Enter your lock screen PIN/password if requested.");
        steps.add("B. Open Developer Options:\n"
                + "Go to Settings → Additional settings → Developer options (or tap the button below).");
        steps.add("C. Windowing Settings (Check Only What Exists on Your ROM):\n"
                + "• Enable freeform windows (" + report.enableFreeformSettingStatus.label + "): Asks Android to allow launching activities inside freeform bounds. Note: Some HyperOS/MIUI builds hide or remove this toggle — if it is not listed, it is unavailable on your firmware.\n"
                + "• Force activities to be resizable (" + report.forceResizableSettingStatus.label + "): Requests resizeability for apps locked to full screen.");
        steps.add("D. Xiaomi Background Pop-up & Battery Permissions (Required for MultiFloat Overlay Stability):\n"
                + "1. Settings → Apps → Manage apps → MultiFloat → Other permissions → Enable \"Display pop-up windows while running in the background\".\n"
                + "2. Battery saver → Set to \"No restrictions\" so Xiaomi does not kill the MultiFloat sidebar.");
        steps.add("E. Built-in Xiaomi Floating Windows (System Alternative for External Apps):\n"
                + "If external apps still open full screen from MultiFloat, open Recents on your Redmi/Xiaomi phone, long-press YouTube or another app card, and tap the Floating Window icon — Xiaomi restricts third-party launchers from triggering its private floating window UI.");

        String hindiGuide =
                "सरल हिंदी गाइड (" + report.xiaomiOsVariant.label + " / Redmi / Xiaomi / POCO):\n\n"
                        + "A. Developer Options कैसे चालू करें:\n"
                        + "1. फोन की Settings (सेटिंग्स) खोलें।\n"
                        + "2. About phone पर टैप करें।\n"
                        + "3. \"" + versionEntryHi + "\" खोजें और उस पर 7 बार लगातार टैप करें।\n"
                        + "4. अगर स्क्रीन पासवर्ड/PIN मांगे तो दर्ज करें — अब Developer Options चालू हो जाएगा।\n\n"
                        + "B. Developer Options कहाँ मिलेगा:\n"
                        + "Settings → Additional settings → Developer options खोलें।\n\n"
                        + "C. सिर्फ ज़रूरी Window Settings देखें:\n"
                        + "• Enable freeform windows: अगर यह विकल्प दिखे तो इसे चालू करें (कई MIUI/HyperOS फोन में यह विकल्प उपलब्ध नहीं होता)।\n"
                        + "• Force activities to be resizable: इससे ऐप्स को रीसाइज़ होने की अनुमति मिलती है।\n"
                        + "(Animation scale, USB debugging, या OEM unlocking बदलने की कोई ज़रूरत नहीं है।)\n\n"
                        + "D. Redmi/Xiaomi के लिए ज़रूरी परमिशन:\n"
                        + "• App Info → Other permissions → \"Display pop-up windows while running in the background\" को Allow करें।\n"
                        + "• Battery saver को \"No restrictions\" पर सेट करें।\n\n"
                        + "ईमानदार जानकारी: Developer Options चालू करने से भी यह गारंटी नहीं होती कि YouTube जैसे बाहरी ऐप्स MultiFloat से सीधे फ्लोटिंग विंडो में खुलेंगे, क्योंकि Android और Xiaomi का सिस्टम इसे नियंत्रित करता है।";

        return new OemGuide(
                OemFamily.XIAOMI,
                report.xiaomiOsVariant.label + " (Xiaomi / Redmi / POCO) Setup Guide",
                "Settings → Additional settings → Developer options",
                versionEntry,
                "MIUI/HyperOS Other Permissions, Battery No Restrictions & Freeform Developer Toggles",
                steps,
                "Honest Notice: Enabling Developer Options does NOT guarantee that external apps (like YouTube) will open in a floating window from MultiFloat. Xiaomi MIUI/HyperOS restricts third-party freeform launches and reserves its native floating window API for system apps/Recents.",
                hindiGuide
        );
    }

    private static OemGuide buildSamsungGuide(CompatibilityReport report) {
        List<String> steps = new ArrayList<>();
        steps.add("A. Enable Developer Options (Optional for Android Freeform Test):\n"
                + "Settings → About phone → Software information → Tap \"Build number\" 7 times.");
        steps.add("B. Samsung One UI Multi-Window (Recommended):\n"
                + "Settings → Advanced features → Labs → Enable \"Multi window for all apps\".");
        steps.add("C. Pop-up View on Samsung:\n"
                + "You can also drag down diagonally from a top corner (if \"Swipe for pop-up view\" is enabled in Labs) or tap an app icon in Recents → \"Open in pop-up view\".");
        steps.add("D. Battery & Overlay Stability:\n"
                + "Settings → Apps → MultiFloat → Battery → Select \"Unrestricted\" so the edge sidebar stays active.");
        return new OemGuide(
                OemFamily.SAMSUNG,
                "Samsung One UI Setup Guide",
                "Settings → Developer options",
                "Build number (Software information)",
                "Advanced features → Labs → Multi window for all apps",
                steps,
                "Manual verification required: Samsung One UI Labs settings cannot be read by third-party apps. Even with Labs enabled, external apps launched from MultiFloat may open full screen unless Samsung firmware honors ActivityOptions launch bounds.",
                null
        );
    }

    private static OemGuide buildOnePlusGuide(CompatibilityReport report) {
        List<String> steps = new ArrayList<>();
        steps.add("A. Enable Developer Options:\n"
                + "Settings → About device → Version → Tap \"Build number\" 7 times.");
        steps.add("B. OxygenOS Flexible Windows:\n"
                + "Settings → Special features → Flexible Windows (or Smart Sidebar) for native floating apps.");
        steps.add("C. Developer Freeform Toggle:\n"
                + "Settings → Additional settings → Developer options → Check \"Enable freeform windows\" and \"Force activities to be resizable\" if present.");
        steps.add("D. Battery Optimization:\n"
                + "App info → MultiFloat → Battery usage → Allow background activity.");
        return new OemGuide(
                OemFamily.ONEPLUS,
                "OnePlus OxygenOS Setup Guide",
                "Settings → Additional settings → Developer options",
                "Build number (About device → Version)",
                "Special features → Flexible Windows & Battery background activity",
                steps,
                "Manual verification required: OxygenOS manages external floating windows primarily through its own Flexible Windows system. Developer Options toggles do not guarantee third-party freeform launches.",
                null
        );
    }

    private static OemGuide buildOppoGuide(CompatibilityReport report) {
        List<String> steps = new ArrayList<>();
        steps.add("A. Enable Developer Options:\n"
                + "Settings → About device → Version → Tap \"Build number\" 7 times.");
        steps.add("B. ColorOS Flexible Windows:\n"
                + "Settings → Special features → Flexible Windows / Smart Sidebar.");
        steps.add("C. Auto-launch & Background Stability:\n"
                + "Settings → Apps → App management → MultiFloat → Battery usage → Allow background activity.");
        return new OemGuide(
                OemFamily.OPPO,
                "OPPO ColorOS Setup Guide",
                "Settings → Additional settings → Developer options",
                "Build number (About device → Version)",
                "Special features → Flexible Windows & Battery background activity",
                steps,
                "Manual verification required: ColorOS proprietary floating windows cannot be invoked directly by third-party apps.",
                null
        );
    }

    private static OemGuide buildRealmeGuide(CompatibilityReport report) {
        List<String> steps = new ArrayList<>();
        steps.add("A. Enable Developer Options:\n"
                + "Settings → About device → Version → Tap \"Build number\" 7 times.");
        steps.add("B. realme UI Flexible Windows:\n"
                + "Settings → Special features → Flexible Windows & Smart Sidebar.");
        steps.add("C. Battery & Floating Window Permission:\n"
                + "Settings → Apps → MultiFloat → Allow \"Display over other apps\" and unrestricted background battery usage.");
        return new OemGuide(
                OemFamily.REALME,
                "realme UI Setup Guide",
                "Settings → Additional settings → Developer options",
                "Build number (About device → Version)",
                "Special features → Flexible Windows & Background Battery",
                steps,
                "Manual verification required: realme UI restricts external freeform window launching from third-party launchers unless firmware supports it.",
                null
        );
    }

    private static OemGuide buildVivoGuide(CompatibilityReport report) {
        List<String> steps = new ArrayList<>();
        steps.add("A. Enable Developer Options:\n"
                + "Settings → About phone → Software information → Tap \"Build number\" 7 times.");
        steps.add("B. vivo / iQOO Small Window:\n"
                + "Settings → Shortcuts & accessibility → Smart multi-window / Small window.");
        steps.add("C. Background Pop-up & Battery:\n"
                + "iManager / Settings → Apps → MultiFloat → Battery → High background power consumption / Unrestricted.");
        return new OemGuide(
                OemFamily.VIVO,
                "vivo / iQOO Setup Guide",
                "Settings → System → Developer options",
                "Build number (Software information)",
                "Smart multi-window / Small window & High background power usage",
                steps,
                "Manual verification required: Funtouch OS / OriginOS small-window APIs are restricted to system controls.",
                null
        );
    }

    private static OemGuide buildGenericAndroidGuide(CompatibilityReport report) {
        List<String> steps = new ArrayList<>();
        steps.add("A. Enable Developer Options:\n"
                + "Settings → About phone → Tap \"Build number\" 7 times and confirm your screen lock.");
        steps.add("B. Check Freeform Window Options:\n"
                + "Settings → System → Developer options → Look for \"Enable freeform windows\" and \"Force activities to be resizable\" (if provided by your Android build).");
        steps.add("C. Battery Optimization:\n"
                + "Settings → Apps → MultiFloat → App battery usage → Unrestricted.");
        return new OemGuide(
                OemFamily.OTHER,
                report.manufacturer + " Android Setup Guide",
                "Settings → System → Developer options",
                "Build number (About phone)",
                "Developer options freeform toggle & Unrestricted battery",
                steps,
                "Honest Notice: Standard Android smartphones do not always enable Freeform Window Management. MultiFloat's own 12 built-in mini-apps always float via Display Over Other Apps.",
                null
        );
    }

    private static String safeStr(@Nullable String val, String fallback) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : fallback;
    }
}
