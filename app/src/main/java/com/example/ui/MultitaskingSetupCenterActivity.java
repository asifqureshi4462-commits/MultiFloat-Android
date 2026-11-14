package com.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.model.ToolType;
import com.example.service.FloatingEngineBus;
import com.example.util.DeviceCompatibilityChecker;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;

/**
 * Multitasking Setup Center — appears automatically after onboarding on first launch
 * and remains permanently accessible from Settings and the Dashboard.
 */
public class MultitaskingSetupCenterActivity extends AppCompatActivity {

    public static final String EXTRA_FROM_ONBOARDING = "extra_from_onboarding";

    private TextView tvOemBadge, tvDeviceInfo;
    private TextView tvCompatBadge, tvCompatDetails;
    private TextView tvOverlayBadge, tvBatteryBadge, tvBatteryDesc, tvPopupBadge, tvPopupDesc;
    private TextView tvOemGuideTitle, tvOemIntegrationBadge, tvOemGuideBody, tvOemHindiGuide, tvOemDisclaimer;
    private TextView tvDevBadge, tvDevGuideDesc, tvFreeformBadge, tvFreeformDetails;
    private Button btnContinueDashboard;

    private boolean fromOnboarding = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multitasking_setup_center);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.setup_center_root));

        fromOnboarding = getIntent().getBooleanExtra(EXTRA_FROM_ONBOARDING, false);

        tvOemBadge = findViewById(R.id.tv_sc_oem_badge);
        tvDeviceInfo = findViewById(R.id.tv_sc_device_info);
        tvCompatBadge = findViewById(R.id.tv_sc_compat_badge);
        tvCompatDetails = findViewById(R.id.tv_sc_compat_details);
        tvOverlayBadge = findViewById(R.id.tv_sc_overlay_badge);
        tvBatteryBadge = findViewById(R.id.tv_sc_battery_badge);
        tvBatteryDesc = findViewById(R.id.tv_sc_battery_desc);
        tvPopupBadge = findViewById(R.id.tv_sc_popup_badge);
        tvPopupDesc = findViewById(R.id.tv_sc_popup_desc);
        tvOemGuideTitle = findViewById(R.id.tv_sc_oem_guide_title);
        tvOemIntegrationBadge = findViewById(R.id.tv_sc_oem_integration_badge);
        tvOemGuideBody = findViewById(R.id.tv_sc_oem_guide_body);
        tvOemHindiGuide = findViewById(R.id.tv_sc_oem_hindi_guide);
        tvOemDisclaimer = findViewById(R.id.tv_sc_oem_disclaimer);
        tvDevBadge = findViewById(R.id.tv_sc_dev_badge);
        tvDevGuideDesc = findViewById(R.id.tv_sc_dev_guide_desc);
        tvFreeformBadge = findViewById(R.id.tv_sc_freeform_badge);
        tvFreeformDetails = findViewById(R.id.tv_sc_freeform_details);
        btnContinueDashboard = findViewById(R.id.btn_sc_continue_dashboard);

        findViewById(R.id.btn_sc_back).setOnClickListener(v -> {
            if (fromOnboarding) {
                continueToNextScreen();
            } else {
                finish();
            }
        });

        View.OnClickListener recheckAction = v -> refreshReport(true);
        findViewById(R.id.btn_sc_recheck_top).setOnClickListener(recheckAction);
        findViewById(R.id.btn_sc_recheck_main).setOnClickListener(recheckAction);
        findViewById(R.id.btn_sc_recheck_compat_card).setOnClickListener(recheckAction);
        findViewById(R.id.btn_sc_recheck_freeform).setOnClickListener(recheckAction);

        findViewById(R.id.btn_sc_open_about_phone).setOnClickListener(v -> {
            if (!PermissionHelper.launchSettingsSafely(this,
                    PermissionHelper.buildDeviceInfoSettingsIntent(),
                    PermissionHelper.buildGeneralSettingsIntent())) {
                Toast.makeText(this, "Open Android Settings → About phone.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_sc_open_overlay).setOnClickListener(v -> {
            if (!PermissionHelper.launchSettingsSafely(this,
                    PermissionHelper.buildOverlayPermissionIntent(this),
                    PermissionHelper.buildAppSettingsIntent(this))) {
                Toast.makeText(this, "Open Android Settings → Apps → MultiFloat → Display over other apps.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_sc_open_battery).setOnClickListener(v -> {
            if (!PermissionHelper.launchSettingsSafely(this,
                    PermissionHelper.buildBatteryOptimizationSettingsIntent(),
                    PermissionHelper.buildAppSettingsIntent(this))) {
                Toast.makeText(this, "Open Android Settings → Apps → MultiFloat → Battery.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_sc_open_app_info).setOnClickListener(v -> {
            if (!PermissionHelper.launchSettingsSafely(this,
                    PermissionHelper.buildAppSettingsIntent(this),
                    PermissionHelper.buildGeneralSettingsIntent())) {
                Toast.makeText(this, "Open Android Settings → Apps → MultiFloat.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_sc_open_perm_wizard).setOnClickListener(v ->
                startActivity(new Intent(this, PermissionSetupActivity.class)));

        findViewById(R.id.btn_sc_open_dev_options).setOnClickListener(v -> {
            if (!PermissionHelper.launchSettingsSafely(this,
                    PermissionHelper.buildDeveloperOptionsIntent(),
                    PermissionHelper.buildDeviceInfoSettingsIntent(),
                    PermissionHelper.buildGeneralSettingsIntent())) {
                Toast.makeText(this, "Enable Developer Options in Settings → About phone first.", Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btn_sc_open_troubleshooting).setOnClickListener(v ->
                startActivity(new Intent(this, TroubleshootingActivity.class)));

        findViewById(R.id.btn_sc_open_compat_test).setOnClickListener(v ->
                startActivity(new Intent(this, CompatibilityTestActivity.class)));

        findViewById(R.id.btn_sc_quick_test_calc).setOnClickListener(v -> {
            if (!PermissionHelper.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant Display Over Other Apps permission first.", Toast.LENGTH_LONG).show();
                PermissionHelper.launchSettingsSafely(this,
                        PermissionHelper.buildOverlayPermissionIntent(this),
                        PermissionHelper.buildAppSettingsIntent(this));
                return;
            }
            FloatingEngineBus.openFloatingTool(this, ToolType.CALCULATOR);
            Toast.makeText(this, "Launched Floating Calculator", Toast.LENGTH_SHORT).show();
        });

        if (fromOnboarding) {
            btnContinueDashboard.setVisibility(View.VISIBLE);
            btnContinueDashboard.setOnClickListener(v -> continueToNextScreen());
        } else {
            btnContinueDashboard.setVisibility(View.GONE);
        }

        MultiFloatApp.get().getPreferences().setSetupCenterCompleted(true);
        refreshReport(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshReport(false);
    }

    private void continueToNextScreen() {
        MultiFloatApp.get().getPreferences().setSetupCenterCompleted(true);
        Intent next = PermissionHelper.canDrawOverlays(this)
                ? new Intent(this, MainActivity.class)
                : new Intent(this, PermissionSetupActivity.class);
        startActivity(next);
        finish();
    }

    private void refreshReport(boolean showToast) {
        DeviceCompatibilityChecker.CompatibilityReport r = DeviceCompatibilityChecker.inspect(this);
        DeviceCompatibilityChecker.OemGuide guide = DeviceCompatibilityChecker.getManufacturerGuide(r);

        // 1. Device Information
        tvOemBadge.setText(r.oemFamily.displayName);
        String osLabel = r.oemFamily == DeviceCompatibilityChecker.OemFamily.XIAOMI
                ? " (" + r.xiaomiOsVariant.label + ")" : "";
        tvDeviceInfo.setText(
                "Manufacturer : " + r.manufacturer + osLabel + "\n"
                        + "Brand / Model: " + r.brand + " / " + r.model + " (" + r.device + ")\n"
                        + "Android      : " + r.androidVersion + " (SDK " + r.sdkInt + ")\n"
                        + "Build        : " + r.buildDisplay + "\n"
                        + "Display      : " + r.screenWidthPx + " × " + r.screenHeightPx + " px\n"
                        + "Density      : " + r.densityDpi + " dpi (" + r.density + "x) • sw" + r.smallestScreenWidthDp + "dp"
        );

        // 2. Device Compatibility Status
        boolean extReady = r.canAttemptExternalFreeform();
        tvCompatBadge.setText(extReady ? "Freeform Path Found" : "Internal Ready • Ext. Restricted");
        tvCompatBadge.setTextColor(getColor(extReady ? R.color.mf_success : R.color.mf_warning));
        tvCompatDetails.setText(
                "• Internal MultiFloat Tools (12 apps): " + r.internalOverlayStatus.label + "\n"
                        + "• Overlay Permission: " + r.overlayPermissionStatus.label + "\n"
                        + "• External Apps Mode: " + r.getExternalCompatibilityHeadline() + "\n"
                        + "• OEM Integration: " + r.oemFloatingIntegrationStatus.label
        );

        // 3. Overlay Permission
        applyStatusBadge(tvOverlayBadge, r.overlayPermissionStatus);

        // 4. Battery Optimization
        if (r.batteryOptimizationStatus == DeviceCompatibilityChecker.CheckStatus.GRANTED) {
            tvBatteryBadge.setText("Unrestricted");
            tvBatteryBadge.setTextColor(getColor(R.color.mf_success));
        } else if (r.batteryOptimizationStatus == DeviceCompatibilityChecker.CheckStatus.NOT_GRANTED) {
            tvBatteryBadge.setText("Optimized (May Stop Service)");
            tvBatteryBadge.setTextColor(getColor(R.color.mf_warning));
        } else {
            tvBatteryBadge.setText("Manual Verification Required");
            tvBatteryBadge.setTextColor(getColor(R.color.mf_warning));
        }
        tvBatteryDesc.setText(
                "Aggressive OEM battery savers stop background floating window services when the screen locks.\n"
                        + "Steps: Tap Open Battery Settings (or App Info → Battery) and choose \"Unrestricted\" / \"No restrictions\"."
        );

        // 5. Display Over Other Apps & Background Pop-ups
        if (r.oemFamily == DeviceCompatibilityChecker.OemFamily.XIAOMI) {
            tvPopupBadge.setText("Manual Verification Required");
            tvPopupBadge.setTextColor(getColor(R.color.mf_warning));
            tvPopupDesc.setText(
                    "In addition to Display Over Other Apps, Xiaomi MIUI/HyperOS requires:\n"
                            + "1. Tap Open App Info → Other permissions.\n"
                            + "2. Allow \"Display pop-up windows while running in the background\".\n"
                            + "3. Allow \"Display pop-up window\" if shown."
            );
        } else {
            applyStatusBadge(tvPopupBadge, r.overlayPermissionStatus);
            tvPopupDesc.setText(
                    "Android requires \"Display over other apps\" so MultiFloat can render its sidebar and mini-apps above other screens, and exempt foreground services from background launch blocks."
            );
        }

        // 6. Manufacturer-Specific Settings (Strictly matched OEM only)
        tvOemGuideTitle.setText("6. " + guide.title);
        applyStatusBadge(tvOemIntegrationBadge, r.oemFloatingIntegrationStatus);
        StringBuilder stepsSb = new StringBuilder();
        stepsSb.append("Path: ").append(guide.developerOptionsPath).append("\n\n");
        for (int i = 0; i < guide.steps.size(); i++) {
            stepsSb.append(guide.steps.get(i));
            if (i < guide.steps.size() - 1) stepsSb.append("\n\n");
        }
        tvOemGuideBody.setText(stepsSb.toString());

        if (guide.bilingualSummaryHindi != null && !guide.bilingualSummaryHindi.isEmpty()) {
            tvOemHindiGuide.setVisibility(View.VISIBLE);
            tvOemHindiGuide.setText(guide.bilingualSummaryHindi);
        } else {
            tvOemHindiGuide.setVisibility(View.GONE);
        }
        tvOemDisclaimer.setText(guide.honestDisclaimer);

        // 7. Developer Options Guide
        applyStatusBadge(tvDevBadge, r.developerOptionsStatus);
        tvDevGuideDesc.setText(
                "How to unlock on " + r.oemFamily.displayName + ":\n"
                        + "1. Open Settings → About phone.\n"
                        + "2. Find \"" + guide.devOptionTapTarget + "\" and tap it 7 times.\n"
                        + "3. Enter your PIN/password if asked.\n"
                        + "4. Open " + guide.developerOptionsPath + ".\n\n"
                        + "Only inspect windowing settings relevant to multitasking (Enable freeform windows, Force activities to be resizable). Do NOT change animation scales, USB debugging, OEM unlocking, or Force Desktop Mode."
        );

        // 8. Freeform Window Compatibility
        if (r.systemFreeformFeatureStatus == DeviceCompatibilityChecker.CheckStatus.SUPPORTED) {
            tvFreeformBadge.setText("System Supported");
            tvFreeformBadge.setTextColor(getColor(R.color.mf_success));
        } else if (r.enableFreeformSettingStatus == DeviceCompatibilityChecker.CheckStatus.ENABLED) {
            tvFreeformBadge.setText("Dev Toggle Enabled");
            tvFreeformBadge.setTextColor(getColor(R.color.mf_cyan_text));
        } else {
            tvFreeformBadge.setText(r.enableFreeformSettingStatus.label);
            tvFreeformBadge.setTextColor(getColor(R.color.mf_warning));
        }

        tvFreeformDetails.setText(
                "• System Freeform Feature: " + r.systemFreeformFeatureStatus.label + "\n"
                        + "• Enable Freeform Windows: " + r.enableFreeformSettingStatus.label
                        + " (asks Android to open external apps in freeform bounds; unavailable on some ROMs)\n"
                        + "• Force Activities to Be Resizable: " + r.forceResizableSettingStatus.label
                        + " (asks Android to allow resizing full-screen-locked apps)\n\n"
                        + "Important: Enabling Developer Options does NOT guarantee external applications like YouTube will launch as floating windows."
        );

        if (showToast) {
            Toast.makeText(this, "Compatibility report updated", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyStatusBadge(TextView badge, DeviceCompatibilityChecker.CheckStatus status) {
        badge.setText(status.label);
        if (status.isPositive()) {
            badge.setTextColor(getColor(R.color.mf_success));
        } else if (status == DeviceCompatibilityChecker.CheckStatus.EXPERIMENTAL) {
            badge.setTextColor(getColor(R.color.mf_cyan_text));
        } else {
            badge.setTextColor(getColor(R.color.mf_warning));
        }
    }
}
