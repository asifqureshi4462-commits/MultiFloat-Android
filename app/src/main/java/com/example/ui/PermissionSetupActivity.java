package com.example.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.R;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;

public class PermissionSetupActivity extends AppCompatActivity {

    private TextView tvOverlayStatus, tvNotifStatus, tvFgsStatus, tvBatteryStatus, tvCameraStatus, tvMediaListenerStatus;
    private Button btnOverlay, btnNotif, btnBattery, btnCamera, btnMediaListener;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                refreshStatuses();
                if (!granted) {
                    Toast.makeText(this, "Permission declined. You can enable it anytime in App Settings.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.perm_root));

        tvOverlayStatus = findViewById(R.id.tv_status_overlay);
        tvNotifStatus = findViewById(R.id.tv_status_notifications);
        tvFgsStatus = findViewById(R.id.tv_status_fgs);
        tvBatteryStatus = findViewById(R.id.tv_status_battery);
        tvCameraStatus = findViewById(R.id.tv_status_camera);
        tvMediaListenerStatus = findViewById(R.id.tv_status_media_listener);

        btnOverlay = findViewById(R.id.btn_grant_overlay);
        btnNotif = findViewById(R.id.btn_grant_notifications);
        btnBattery = findViewById(R.id.btn_open_battery_settings);
        btnCamera = findViewById(R.id.btn_grant_camera);
        btnMediaListener = findViewById(R.id.btn_grant_media_listener);

        findViewById(R.id.btn_perm_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_perm_recheck).setOnClickListener(v -> {
            refreshStatuses();
            Toast.makeText(this, "Permissions rechecked", Toast.LENGTH_SHORT).show();
        });

        btnOverlay.setOnClickListener(v ->
                PermissionHelper.launchSettingsSafely(this,
                        PermissionHelper.buildOverlayPermissionIntent(this),
                        PermissionHelper.buildAppSettingsIntent(this)));

        btnNotif.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                PermissionHelper.launchSettingsSafely(this,
                        PermissionHelper.buildAppNotificationSettingsIntent(this),
                        PermissionHelper.buildAppSettingsIntent(this));
            }
        });

        findViewById(R.id.btn_open_notif_settings).setOnClickListener(v ->
                PermissionHelper.launchSettingsSafely(this,
                        PermissionHelper.buildAppNotificationSettingsIntent(this),
                        PermissionHelper.buildAppSettingsIntent(this)));

        btnBattery.setOnClickListener(v ->
                PermissionHelper.launchSettingsSafely(this,
                        PermissionHelper.buildBatteryOptimizationSettingsIntent(),
                        PermissionHelper.buildAppSettingsIntent(this)));

        btnCamera.setOnClickListener(v -> {
            if (!PermissionHelper.hasCameraPermission(this)) {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnMediaListener.setOnClickListener(v ->
                PermissionHelper.launchSettingsSafely(this,
                        PermissionHelper.buildNotificationListenerSettingsIntent(),
                        PermissionHelper.buildAppSettingsIntent(this)));

        findViewById(R.id.btn_open_app_settings).setOnClickListener(v ->
                PermissionHelper.launchSettingsSafely(this, PermissionHelper.buildAppSettingsIntent(this)));

        findViewById(R.id.btn_perm_setup_center).setOnClickListener(v ->
                startActivity(new Intent(this, MultitaskingSetupCenterActivity.class)));

        findViewById(R.id.btn_perm_continue).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        refreshStatuses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatuses();
    }

    private void refreshStatuses() {
        boolean overlay = PermissionHelper.canDrawOverlays(this);
        boolean notif = PermissionHelper.hasNotificationPermission(this);
        boolean fgs = PermissionHelper.isForegroundServiceReady(this);
        PermissionHelper.BatteryOptimizationState battery = PermissionHelper.getBatteryOptimizationState(this);
        boolean camera = PermissionHelper.hasCameraPermission(this);
        boolean mediaListener = PermissionHelper.isNotificationListenerEnabled(this);

        applyBadge(tvOverlayStatus, btnOverlay, overlay, "Granted", "Allow Display Over Apps");
        applyBadge(tvNotifStatus, btnNotif, notif, "Granted", "Allow Notifications");

        tvFgsStatus.setText(fgs ? "Ready" : "Needs Overlay & Notif");
        tvFgsStatus.setTextColor(getColor(fgs ? R.color.mf_success : R.color.mf_warning));

        if (battery == PermissionHelper.BatteryOptimizationState.UNRESTRICTED) {
            tvBatteryStatus.setText("Unrestricted");
            tvBatteryStatus.setTextColor(getColor(R.color.mf_success));
            btnBattery.setText("Unrestricted ✓");
        } else if (battery == PermissionHelper.BatteryOptimizationState.OPTIMIZED) {
            tvBatteryStatus.setText("Optimized (May Kill Service)");
            tvBatteryStatus.setTextColor(getColor(R.color.mf_warning));
            btnBattery.setText("Configure Battery Settings");
        } else {
            tvBatteryStatus.setText("Manual Check");
            tvBatteryStatus.setTextColor(getColor(R.color.mf_warning));
            btnBattery.setText("Configure Battery Settings");
        }

        applyBadge(tvCameraStatus, btnCamera, camera, "Granted", "Allow Camera");

        tvMediaListenerStatus.setText(mediaListener ? "Enabled" : "Optional");
        tvMediaListenerStatus.setTextColor(getColor(mediaListener ? R.color.mf_success : R.color.mf_text_secondary));
    }

    private void applyBadge(TextView badge, Button actionBtn, boolean granted, String okLabel, String actionLabel) {
        if (granted) {
            badge.setText(okLabel);
            badge.setTextColor(getColor(R.color.mf_success));
            actionBtn.setEnabled(false);
            actionBtn.setText(okLabel);
        } else {
            badge.setText(R.string.perm_needed);
            badge.setTextColor(getColor(R.color.mf_warning));
            actionBtn.setEnabled(true);
            actionBtn.setText(actionLabel);
        }
    }
}
