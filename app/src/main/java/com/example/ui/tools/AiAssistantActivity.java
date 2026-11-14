package com.example.ui.tools;

import com.example.miniapps.AiAssistantController;
import com.example.model.ToolType;

public class AiAssistantActivity extends BaseToolActivity {
    @Override
    protected ToolType getToolType() { return ToolType.AI_ASSISTANT; }

    @Override
    protected void onResume() {
        super.onResume();
        if (controller instanceof AiAssistantController) {
            ((AiAssistantController) controller).refreshKeyStatus();
        }
    }
}
