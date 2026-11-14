package com.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.R;
import com.example.util.DeviceCompatibilityChecker;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Beginner-friendly Troubleshooting Center with manufacturer-specific guidance
 * covering external app full-screen launches, floating overlays, battery killing,
 * Developer Options, and freeform compatibility.
 */
public class TroubleshootingActivity extends AppCompatActivity {

    public static final class Article {
        public final String question;
        public final String answer;
        public final String oemSpecificTip;
        @Nullable public final String actionLabel;
        @Nullable public final Runnable action;

        public Article(String question, String answer, String oemSpecificTip,
                       @Nullable String actionLabel, @Nullable Runnable action) {
            this.question = question;
            this.answer = answer;
            this.oemSpecificTip = oemSpecificTip;
            this.actionLabel = actionLabel;
            this.action = action;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_troubleshooting);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.troubleshooting_root));

        findViewById(R.id.btn_ts_back).setOnClickListener(v -> finish());

        DeviceCompatibilityChecker.CompatibilityReport report = DeviceCompatibilityChecker.inspect(this);
        TextView tvBanner = findViewById(R.id.tv_ts_oem_banner);
        tvBanner.setText("Tailored for: " + report.manufacturer + " " + report.model
                + " (Android " + report.androidVersion + ")");

        LinearLayout container = findViewById(R.id.container_ts_articles);
        LayoutInflater inflater = LayoutInflater.from(this);

        List<Article> articles = buildArticles(report);
        for (int i = 0; i < articles.size(); i++) {
            Article art = articles.get(i);
            View card = inflater.inflate(R.layout.item_troubleshooting_article, container, false);
            TextView tvQ = card.findViewById(R.id.tv_ts_question);
            TextView tvA = card.findViewById(R.id.tv_ts_answer);
            TextView tvTip = card.findViewById(R.id.tv_ts_oem_tip);
            ImageView chevron = card.findViewById(R.id.iv_ts_chevron);
            LinearLayout body = card.findViewById(R.id.layout_ts_body);
            Button btnAction = card.findViewById(R.id.btn_ts_action);

            tvQ.setText((i + 1) + ". " + art.question);
            tvA.setText(art.answer);
            tvTip.setText(art.oemSpecificTip);

            // Expand first two items by default for quick discovery
            if (i < 2) {
                body.setVisibility(View.VISIBLE);
                chevron.setRotation(180f);
            }

            card.findViewById(R.id.row_ts_header).setOnClickListener(v -> {
                boolean expanded = body.getVisibility() == View.VISIBLE;
                body.setVisibility(expanded ? View.GONE : View.VISIBLE);
                chevron.animate().rotation(expanded ? 0f : 180f).setDuration(150).start();
            });

            if (art.actionLabel != null && art.action != null) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText(art.actionLabel);
                btnAction.setOnClickListener(v -> art.action.run());
            } else {
                btnAction.setVisibility(View.GONE);
            }
            container.addView(card);
        }
    }

    private List<Article> buildArticles(DeviceCompatibilityChecker.CompatibilityReport report) {
        List<Article> list = new ArrayList<>();
        String oemHint = getOemGuidanceSnippet(report);

        list.add(new Article(
                "Why does YouTube open full screen?",
                "YouTube is an external Android app installed separately from MultiFloat. On Android smartphones, third-party apps cannot draw another app inside their own overlay window—only Android's Window Manager can place YouTube into a floating freeform box. If your phone's firmware does not declare Freeform Window Management (or ignores freeform launch bounds), Android launches YouTube in normal full screen.",
                oemHint,
                "Open Compatibility Test",
                () -> startActivity(new Intent(this, CompatibilityTestActivity.class))
        ));

        list.add(new Article(
                "Why is an external application not floating?",
                "There are three common reasons:\n"
                        + "1. Your Android build does not enable Freeform Window Management.\n"
                        + "2. The target app itself restricts multi-window/resizing in its manifest.\n"
                        + "3. Your phone's manufacturer (OEM) uses its own private floating window system (like Xiaomi Floating Windows or Samsung Pop-up View) instead of standard Android freeform bounds.",
                getExternalOemSpecificTip(report),
                "Open Multitasking Setup Center",
                () -> startActivity(new Intent(this, MultitaskingSetupCenterActivity.class))
        ));

        list.add(new Article(
                "Why does a floating window disappear?",
                "Aggressive battery optimization on Android phones often stops background foreground services when screen locks or memory is low. Also check if \"Display over other apps\" was revoked or if the window was minimized into the bottom taskbar dock.",
                getBatteryOemSpecificTip(report),
                "Open Battery Settings",
                () -> {
                    if (!PermissionHelper.launchSettingsSafely(this,
                            PermissionHelper.buildBatteryOptimizationSettingsIntent(),
                            PermissionHelper.buildAppSettingsIntent(this))) {
                        Toast.makeText(this, "Open Android Settings → Apps → MultiFloat → Battery.", Toast.LENGTH_SHORT).show();
                    }
                }
        ));

        list.add(new Article(
                "Why is the sidebar not responding?",
                "If the edge sidebar handle stops responding or disappears:\n"
                        + "1. Verify \"Display over other apps\" is granted.\n"
                        + "2. Start the Floating Engine from the MultiFloat Dashboard.\n"
                        + "3. Check if Edge Sidebar is turned ON in Quick Controls.\n"
                        + "4. On some phones, full-screen games or secure screens (banking/lock screen) temporarily block touch overlays.",
                "Tip for " + report.oemFamily.displayName + ": Keep MultiFloat exempted from battery restrictions and ensure the foreground notification is not blocked.",
                "Open Permission Wizard",
                () -> startActivity(new Intent(this, PermissionSetupActivity.class))
        ));

        list.add(new Article(
                "Why does Android block background launching?",
                "Starting in Android 10 (API 29) and strengthened in Android 14+, Android blocks apps in the background from suddenly opening Activities on screen to prevent ad spam. MultiFloat uses the \"Display over other apps\" exemption plus foreground service mode, while Xiaomi MIUI/HyperOS adds a second toggle: \"Display pop-up windows while running in the background\".",
                report.oemFamily == DeviceCompatibilityChecker.OemFamily.XIAOMI
                        ? "Xiaomi/Redmi Fix: App Info → MultiFloat → Other permissions → Enable \"Display pop-up windows while running in the background\"."
                        : "Ensure \"Display over other apps\" is allowed for MultiFloat in App Info.",
                "Open MultiFloat App Info",
                () -> PermissionHelper.launchSettingsSafely(this, PermissionHelper.buildAppSettingsIntent(this))
        ));

        list.add(new Article(
                "Why is Developer Options missing?",
                "Android hides Developer Options by default on every new phone until you manually unlock it by tapping the OS/Build version entry 7 times in Settings → About phone.",
                getDevOptionsOemTip(report),
                "Open About Phone Settings",
                () -> PermissionHelper.launchSettingsSafely(this,
                        PermissionHelper.buildDeviceInfoSettingsIntent(),
                        PermissionHelper.buildGeneralSettingsIntent())
        ));

        list.add(new Article(
                "Why does the device not support freeform windows?",
                "Android originally designed Freeform Window Management for tablets, foldables, ChromeOS, and Android Desktop Mode. Most smartphone makers disable standard AOSP freeform on small phone screens to avoid cramped layouts, shipping their own custom Sidebar or Recents floating window instead. MultiFloat's 12 built-in mini-apps always float cleanly regardless of OEM freeform limits.",
                "Note: Enabling Developer Options does not guarantee external apps will float if the OEM ROM disables freeform.",
                "Test Internal Floating Tools",
                () -> startActivity(new Intent(this, CompatibilityTestActivity.class))
        ));

        return list;
    }

    private String getOemGuidanceSnippet(DeviceCompatibilityChecker.CompatibilityReport report) {
        switch (report.oemFamily) {
            case XIAOMI:
                return "Redmi / Xiaomi Tip (" + report.xiaomiOsVariant.label + "): You can float YouTube natively using Xiaomi's Recents screen (long-press YouTube → Floating window icon) or Xiaomi's Side Bar, because MIUI/HyperOS restricts third-party apps from triggering its private floating window.";
            case SAMSUNG:
                return "Samsung One UI Tip: In Recents, tap the YouTube icon → \"Open in pop-up view\", or enable Labs → \"Multi window for all apps\".";
            case ONEPLUS:
            case OPPO:
            case REALME:
                return report.oemFamily.displayName + " Tip: Use Smart Sidebar or Recents → \"Floating window\" (Flexible Windows) if standard Android freeform is restricted.";
            case VIVO:
                return "vivo / iQOO Tip: Use Recents → \"Small window\" or Settings → Shortcuts & accessibility → Smart multi-window.";
            default:
                return "Android Tip: MultiFloat's own Mini Browser & 11 other built-in tools float anywhere via overlay permission.";
        }
    }

    private String getExternalOemSpecificTip(DeviceCompatibilityChecker.CompatibilityReport report) {
        if (report.oemFamily == DeviceCompatibilityChecker.OemFamily.XIAOMI) {
            return "Redmi / Xiaomi (हिंदी): Xiaomi के MIUI/HyperOS में बाहरी ऐप्स को फ्लोट करने का सिस्टम Xiaomi अपने Recents/Sidebar के लिए सुरक्षित रखता है। Developer Options में \"Enable freeform windows\" उपलब्ध हो तो टेस्ट करें, वरना नॉर्मल लॉन्च या Recents Floating Window का उपयोग करें।";
        }
        return "Detected OEM: " + report.oemFamily.displayName + " • System Freeform: "
                + report.systemFreeformFeatureStatus.label;
    }

    private String getBatteryOemSpecificTip(DeviceCompatibilityChecker.CompatibilityReport report) {
        if (report.oemFamily == DeviceCompatibilityChecker.OemFamily.XIAOMI) {
            return "Redmi / Xiaomi Fix: App Info → Battery saver → Choose \"No restrictions\". Also enable Autostart if shown.";
        }
        if (report.oemFamily == DeviceCompatibilityChecker.OemFamily.SAMSUNG) {
            return "Samsung Fix: Settings → Apps → MultiFloat → Battery → Choose \"Unrestricted\".";
        }
        return "Set MultiFloat battery usage to Unrestricted / Don't optimize in Android Settings.";
    }

    private String getDevOptionsOemTip(DeviceCompatibilityChecker.CompatibilityReport report) {
        if (report.oemFamily == DeviceCompatibilityChecker.OemFamily.XIAOMI) {
            String target = report.xiaomiOsVariant == DeviceCompatibilityChecker.XiaomiOsVariant.HYPER_OS
                    ? "OS version" : "MIUI version";
            return "Redmi / Xiaomi: Settings → About phone → Tap \"" + target + "\" 7 times, then go to Additional settings → Developer options.";
        }
        return "Settings → About phone → Tap \"Build number\" 7 times.";
    }
}
