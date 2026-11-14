package com.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.model.InstalledAppInfo;
import com.example.model.ToolType;
import com.example.service.FloatingEngineBus;
import com.example.util.DeviceCompatibilityChecker;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.ExternalAppLauncher;
import com.example.util.PermissionHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Floating Window Compatibility Test screen.
 * Tests MultiFloat's own internal floating windows first, then tests external app
 * freeform compatibility separately without ever marking unsupported features as supported.
 */
public class CompatibilityTestActivity extends AppCompatActivity {

    private LinearLayout resultsContainer;
    private TextView tvDeviceLabel, tvInternalResult, tvSelectedApp, tvExternalResult;
    private final List<InstalledAppInfo> installedApps = new ArrayList<>();
    @Nullable private InstalledAppInfo selectedExternalApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compatibility_test);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.compat_test_root));

        resultsContainer = findViewById(R.id.container_ct_results);
        tvDeviceLabel = findViewById(R.id.tv_ct_device_label);
        tvInternalResult = findViewById(R.id.tv_ct_internal_result);
        tvSelectedApp = findViewById(R.id.tv_ct_selected_app);
        tvExternalResult = findViewById(R.id.tv_ct_external_result);

        findViewById(R.id.btn_ct_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_ct_recheck_top).setOnClickListener(v -> refreshDiagnostics(true));
        findViewById(R.id.btn_ct_recheck).setOnClickListener(v -> refreshDiagnostics(true));

        findViewById(R.id.btn_ct_test_calc).setOnClickListener(v -> testInternalTool(ToolType.CALCULATOR));
        findViewById(R.id.btn_ct_test_notes).setOnClickListener(v -> testInternalTool(ToolType.NOTES));
        findViewById(R.id.btn_ct_test_browser).setOnClickListener(v -> testInternalTool(ToolType.BROWSER));

        findViewById(R.id.btn_ct_pick_app).setOnClickListener(v -> showAppPickerDialog());
        findViewById(R.id.btn_ct_run_external).setOnClickListener(v -> runExternalAppTest());
        findViewById(R.id.btn_ct_open_setup).setOnClickListener(v ->
                startActivity(new Intent(this, MultitaskingSetupCenterActivity.class)));

        refreshDiagnostics(false);
        loadInstalledAppsAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDiagnostics(false);
    }

    private void refreshDiagnostics(boolean showToast) {
        DeviceCompatibilityChecker.CompatibilityReport report = DeviceCompatibilityChecker.inspect(this);
        tvDeviceLabel.setText(report.manufacturer + " " + report.model
                + " • Android " + report.androidVersion + " (SDK " + report.sdkInt + ")");

        resultsContainer.removeAllViews();
        addStatusRow("Internal Overlay", report.internalOverlayStatus.label, R.color.mf_success);
        addStatusRow("Overlay Permission",
                report.overlayPermissionStatus == DeviceCompatibilityChecker.CheckStatus.GRANTED
                        ? "Granted" : "Not Granted",
                report.overlayPermissionStatus.isPositive() ? R.color.mf_success : R.color.mf_warning);

        String freeformText;
        int freeformColor;
        if (report.systemFreeformFeatureStatus == DeviceCompatibilityChecker.CheckStatus.SUPPORTED) {
            freeformText = "Detected (System Feature)";
            freeformColor = R.color.mf_success;
        } else if (report.enableFreeformSettingStatus == DeviceCompatibilityChecker.CheckStatus.ENABLED) {
            freeformText = "Detected (Developer Option)";
            freeformColor = R.color.mf_cyan_text;
        } else if (report.enableFreeformSettingStatus == DeviceCompatibilityChecker.CheckStatus.DISABLED) {
            freeformText = "Not Detected";
            freeformColor = R.color.mf_warning;
        } else {
            freeformText = "Not Detected / Unknown";
            freeformColor = R.color.mf_warning;
        }
        addStatusRow("External Freeform Support", freeformText, freeformColor);

        String oemText;
        int oemColor;
        if (report.oemFloatingIntegrationStatus == DeviceCompatibilityChecker.CheckStatus.SUPPORTED) {
            oemText = "Available";
            oemColor = R.color.mf_success;
        } else if (report.oemFloatingIntegrationStatus == DeviceCompatibilityChecker.CheckStatus.EXPERIMENTAL) {
            oemText = "Experimental (OEM Controlled)";
            oemColor = R.color.mf_cyan_text;
        } else if (report.oemFloatingIntegrationStatus == DeviceCompatibilityChecker.CheckStatus.MANUAL_VERIFICATION_REQUIRED) {
            oemText = "Manual Verification Required";
            oemColor = R.color.mf_warning;
        } else {
            oemText = "Unavailable";
            oemColor = R.color.mf_warning;
        }
        addStatusRow("OEM Floating Window Integration", oemText, oemColor);

        int devColor = report.developerOptionsStatus.isPositive() ? R.color.mf_success : R.color.mf_warning;
        addStatusRow("Developer Settings", report.developerOptionsStatus.label, devColor);

        if (showToast) {
            Toast.makeText(this, "Device compatibility rechecked", Toast.LENGTH_SHORT).show();
        }
    }

    private void addStatusRow(String key, String value, int colorRes) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, EdgeToEdgeHelper.dpToPx(this, 4), 0, EdgeToEdgeHelper.dpToPx(this, 4));

        TextView tvKey = new TextView(this);
        tvKey.setText(key);
        tvKey.setTextColor(getColor(R.color.mf_text_secondary));
        tvKey.setTextSize(12.5f);
        tvKey.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextColor(getColor(colorRes));
        tvVal.setTextSize(12.5f);
        tvVal.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(tvKey);
        row.addView(tvVal);
        resultsContainer.addView(row);
    }

    private void testInternalTool(ToolType type) {
        if (!PermissionHelper.canDrawOverlays(this)) {
            tvInternalResult.setText("Result: Overlay permission is NOT granted yet. Grant Display Over Other Apps first.");
            tvInternalResult.setTextColor(getColor(R.color.mf_warning));
            new AlertDialog.Builder(this)
                    .setTitle("Overlay Permission Needed")
                    .setMessage("MultiFloat needs \"Display over other apps\" to render its internal floating " + getString(type.titleRes) + " window.")
                    .setPositiveButton("Open Permission Setup", (d, w) ->
                            startActivity(new Intent(this, PermissionSetupActivity.class)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        boolean started = FloatingEngineBus.openFloatingTool(this, type);
        if (started) {
            tvInternalResult.setText("Result: Internal Floating " + getString(type.titleRes) + " launched via MultiFloat overlay engine.");
            tvInternalResult.setTextColor(getColor(R.color.mf_success));
        } else {
            tvInternalResult.setText("Result: Unable to start floating tool.");
            tvInternalResult.setTextColor(getColor(R.color.mf_error));
        }
    }

    private void loadInstalledAppsAsync() {
        MultiFloatApp.get().getIoExecutor().execute(() -> {
            List<InstalledAppInfo> loaded = MultiFloatApp.get().getInstalledAppsRepository()
                    .loadLaunchableApps(getApplicationContext());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                installedApps.clear();
                installedApps.addAll(loaded);
                if (selectedExternalApp == null && !installedApps.isEmpty()) {
                    // Prefer YouTube if installed, otherwise first app
                    selectedExternalApp = installedApps.get(0);
                    for (InstalledAppInfo app : installedApps) {
                        if ("com.google.android.youtube".equals(app.packageName)) {
                            selectedExternalApp = app;
                            break;
                        }
                    }
                }
                updateSelectedAppLabel();
            });
        });
    }

    private void updateSelectedAppLabel() {
        if (selectedExternalApp != null) {
            tvSelectedApp.setText("Selected App: " + selectedExternalApp.appName
                    + " (" + selectedExternalApp.packageName + ")");
        } else {
            tvSelectedApp.setText("Selected App: No third-party launchable apps detected");
        }
    }

    private void showAppPickerDialog() {
        if (installedApps.isEmpty()) {
            Toast.makeText(this, "No external launchable apps found on this device.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[installedApps.size()];
        for (int i = 0; i < installedApps.size(); i++) {
            labels[i] = installedApps.get(i).appName + " (" + installedApps.get(i).packageName + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose External App to Test")
                .setItems(labels, (d, which) -> {
                    selectedExternalApp = installedApps.get(which);
                    updateSelectedAppLabel();
                })
                .show();
    }

    private void runExternalAppTest() {
        if (selectedExternalApp == null) {
            Toast.makeText(this, "Please select an installed app first.", Toast.LENGTH_SHORT).show();
            return;
        }

        ExternalAppLauncher.LaunchStatus status = ExternalAppLauncher.launchExternalApp(
                this,
                selectedExternalApp.packageName,
                ExternalAppLauncher.computeDefaultFreeformBounds(this, null),
                false
        );

        switch (status) {
            case LAUNCHED_FREEFORM:
                tvExternalResult.setText("Result: System Freeform feature detected. Requested freeform launch bounds for "
                        + selectedExternalApp.appName + ".");
                tvExternalResult.setTextColor(getColor(R.color.mf_success));
                break;
            case FREEFORM_UNSUPPORTED:
                tvExternalResult.setText("Result: External Freeform Not Detected. MultiFloat blocked silent full-screen fallback so you see the true device capability. You can open "
                        + selectedExternalApp.appName + " normally or inspect the Setup Center.");
                tvExternalResult.setTextColor(getColor(R.color.mf_warning));
                new AlertDialog.Builder(this)
                        .setTitle("External Freeform Not Detected")
                        .setMessage("This device does not expose Android Freeform Window Management or Developer freeform support.\n\nWould you like to open "
                                + selectedExternalApp.appName + " in Normal Full Screen instead?")
                        .setPositiveButton(R.string.action_launch_normally, (d, w) ->
                                ExternalAppLauncher.launchExternalApp(this, selectedExternalApp.packageName, null, true))
                        .setNeutralButton(R.string.btn_setup_center, (d, w) ->
                                startActivity(new Intent(this, MultitaskingSetupCenterActivity.class)))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
            case APP_UNINSTALLED:
                tvExternalResult.setText("Result: App was uninstalled. Refreshing list.");
                tvExternalResult.setTextColor(getColor(R.color.mf_error));
                selectedExternalApp = null;
                loadInstalledAppsAsync();
                break;
            default:
                tvExternalResult.setText("Result: External app test failed.");
                tvExternalResult.setTextColor(getColor(R.color.mf_error));
                break;
        }
    }
}
