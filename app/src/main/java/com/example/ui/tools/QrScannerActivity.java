package com.example.ui.tools;

import com.example.miniapps.QrScannerController;
import com.example.model.ToolType;

public class QrScannerActivity extends BaseToolActivity {
    @Override
    protected ToolType getToolType() { return ToolType.QR_SCANNER; }

    @Override
    protected void onCameraPermissionResult(boolean granted) {
        super.onCameraPermissionResult(granted);
        if (controller instanceof QrScannerController) {
            ((QrScannerController) controller).refreshCameraState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controller instanceof QrScannerController) {
            ((QrScannerController) controller).refreshCameraState();
        }
    }
}
