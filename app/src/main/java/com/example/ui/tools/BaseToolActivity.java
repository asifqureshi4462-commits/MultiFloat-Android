package com.example.ui.tools;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.miniapps.MiniApp;
import com.example.miniapps.MiniAppContainerHost;
import com.example.miniapps.MiniAppRegistry;
import com.example.model.ToolType;
import com.example.service.FloatingEngineBus;
import com.example.ui.PermissionSetupActivity;
import com.example.ui.settings.GeneralSettingsActivity;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;

public abstract class BaseToolActivity extends AppCompatActivity implements MiniApp.HostCallback {

    private static final String KEY_MINIAPP_STATE = "miniapp_saved_state";

    protected MiniApp controller;
    private final MiniAppContainerHost miniAppHost = new MiniAppContainerHost();

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
        Bundle miniAppState = savedInstanceState != null ? savedInstanceState.getBundle(KEY_MINIAPP_STATE) : null;
        controller = MiniAppRegistry.getInstance().create(type, this, this);
        miniAppHost.attach(controller, getLayoutInflater(), container, miniAppState, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        miniAppHost.dispatchStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        miniAppHost.dispatchResume();
    }

    @Override
    protected void onPause() {
        miniAppHost.dispatchPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_MINIAPP_STATE, miniAppHost.captureState());
    }

    @Override
    protected void onDestroy() {
        miniAppHost.destroy();
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
    public void onOpenFullScreenTool(@NonNull ToolType toolType) {
        ToolNavigationHelper.openFullScreen(this, toolType);
    }
}
