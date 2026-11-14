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

    private TextView tvOverlayStatus, tvNotifStatus, tvCameraStatus;
    private Button btnOverlay, btnNotif, btnCamera;

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
        tvCameraStatus = findViewById(R.id.tv_status_camera);
        btnOverlay = findViewById(R.id.btn_grant_overlay);
        btnNotif = findViewById(R.id.btn_grant_notifications);
        btnCamera = findViewById(R.id.btn_grant_camera);

        findViewById(R.id.btn_perm_back).setOnClickListener(v -> finish());

        btnOverlay.setOnClickListener(v ->
                startActivity(PermissionHelper.buildOverlayPermissionIntent(this)));

        btnNotif.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        btnCamera.setOnClickListener(v -> {
            if (!PermissionHelper.hasCameraPermission(this)) {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        findViewById(R.id.btn_open_app_settings).setOnClickListener(v ->
                startActivity(PermissionHelper.buildAppSettingsIntent(this)));

        findViewById(R.id.btn_perm_continue).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatuses();
    }

    private void refreshStatuses() {
        boolean overlay = PermissionHelper.canDrawOverlays(this);
        boolean notif = PermissionHelper.hasNotificationPermission(this);
        boolean camera = PermissionHelper.hasCameraPermission(this);

        applyBadge(tvOverlayStatus, btnOverlay, overlay, "Granted");
        applyBadge(tvNotifStatus, btnNotif, notif, "Granted");
        applyBadge(tvCameraStatus, btnCamera, camera, "Granted");
    }

    private void applyBadge(TextView badge, Button actionBtn, boolean granted, String okLabel) {
        if (granted) {
            badge.setText(okLabel);
            badge.setTextColor(getColor(R.color.mf_success));
            actionBtn.setEnabled(false);
            actionBtn.setText("Granted");
        } else {
            badge.setText(R.string.perm_needed);
            badge.setTextColor(getColor(R.color.mf_warning));
            actionBtn.setEnabled(true);
        }
    }
}
