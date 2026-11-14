package com.example.ui.tools;

import com.example.miniapps.TranslatorController;
import com.example.model.ToolType;

public class TranslatorActivity extends BaseToolActivity {
    @Override
    protected ToolType getToolType() { return ToolType.TRANSLATOR; }

    @Override
    protected void onResume() {
        super.onResume();
        if (controller instanceof TranslatorController) {
            ((TranslatorController) controller).refreshConfigBanner();
        }
    }
}
