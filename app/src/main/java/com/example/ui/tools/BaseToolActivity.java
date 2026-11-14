package com.example.ui.tools;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.miniapps.BaseMiniAppController;
import com.example.miniapps.MiniAppFactory;
import com.example.model.ToolType;
import com.example.service.FloatingEngineBus;
import com.example.ui.PermissionSetupActivity;
import com.example.ui.settings.GeneralSettingsActivity;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;

public abstract class BaseToolActivity extends AppCompatActivity implements BaseMiniAppController.HostCallback {

    protected BaseMiniAppController controller;

    private final ActivityResultLauncher<String> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                onCameraPermissionResult(granted);
            });

    protected abstract ToolType getToolType();

    protected void onCameraPermissionResult(boolean granted) {
        if (!granted) {
            Toast.makeText(this, "Camera permission denied. Enable it in Settings to scan QR codes.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_host);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.tool_host_root));

        ToolType type = getToolType();
        MultiFloatApp.get().getToolsRepository().recordToolOpened(type);

        TextView tvTitle = findViewById(R.id.tv_host_title);
        tvTitle.setText(type.titleRes);

        findViewById(R.id.btn_host_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_host_float).setOnClickListener(v -> {
            if (PermissionHelper.canDrawOverlays(this)) {
                FloatingEngineBus.openFloatingTool(this, type);
                Toast.makeText(this, "Launched floating window", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, PermissionSetupActivity.class));
            }
        });

        FrameLayout container = findViewById(R.id.tool_host_container);
        controller = MiniAppFactory.create(type, this, this);
        container.addView(controller.createView(getLayoutInflater(), container));
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.onDetached();
        }
        super.onDestroy();
    }

    @Override
    public void onInputFocusRequested(boolean focusable) {
        // Not needed in full-screen Activity
    }

    @Override
    public void onOpenSettingsRequested() {
        startActivity(new Intent(this, GeneralSettingsActivity.class));
    }

    @Override
    public void onRequestCameraPermission() {
        cameraLauncher.launch(Manifest.permission.CAMERA);
    }

    @Override
    public void onOpenFullScreenTool(ToolType toolType) {
        ToolNavigationHelper.openFullScreen(this, toolType);
    }
}
