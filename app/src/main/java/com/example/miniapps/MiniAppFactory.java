package com.example.miniapps;

import android.content.Context;
import com.example.model.ToolType;

public class MiniAppFactory {

    public static BaseMiniAppController create(ToolType toolType, Context context,
                                               BaseMiniAppController.HostCallback callback) {
        switch (toolType) {
            case CALCULATOR:
                return new CalculatorController(context, callback);
            case NOTES:
                return new NotesController(context, callback);
            case BROWSER:
                return new BrowserController(context, callback);
            case STOPWATCH_TIMER:
                return new StopwatchTimerController(context, callback);
            case CLOCK:
                return new ClockController(context, callback);
            case TEXT_EDITOR:
                return new TextEditorController(context, callback);
            case CLIPBOARD:
                return new ClipboardController(context, callback);
            case TODO:
                return new TodoController(context, callback);
            case QR_SCANNER:
                return new QrScannerController(context, callback);
            case TRANSLATOR:
                return new TranslatorController(context, callback);
            case AI_ASSISTANT:
                return new AiAssistantController(context, callback);
            case MUSIC_CONTROLS:
                return new MusicControlsController(context, callback);
            default:
                return new CalculatorController(context, callback);
        }
    }
}
