package com.example.ui.tools;

import android.content.Context;
import android.content.Intent;
import com.example.model.ToolType;

public class ToolNavigationHelper {

    public static void openFullScreen(Context context, ToolType type) {
        Class<?> target;
        switch (type) {
            case CALCULATOR: target = CalculatorActivity.class; break;
            case NOTES: target = NotesActivity.class; break;
            case BROWSER: target = BrowserActivity.class; break;
            case STOPWATCH_TIMER: target = StopwatchTimerActivity.class; break;
            case CLOCK: target = ClockActivity.class; break;
            case TEXT_EDITOR: target = TextEditorActivity.class; break;
            case CLIPBOARD: target = ClipboardActivity.class; break;
            case TODO: target = TodoActivity.class; break;
            case QR_SCANNER: target = QrScannerActivity.class; break;
            case TRANSLATOR: target = TranslatorActivity.class; break;
            case AI_ASSISTANT: target = AiAssistantActivity.class; break;
            case MUSIC_CONTROLS: target = MusicControlsActivity.class; break;
            default: target = CalculatorActivity.class; break;
        }
        Intent intent = new Intent(context, target);
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }
}
