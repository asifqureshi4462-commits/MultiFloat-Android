package com.example.miniapps;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.R;
import com.example.model.ToolType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Modular registry mapping each {@link ToolType} to a {@link MiniAppDescriptor}
 * that handles view inflation metadata and {@link MiniApp} instantiation.
 */
public final class MiniAppRegistry {

    private static volatile MiniAppRegistry defaultInstance;

    private final Map<ToolType, MiniAppDescriptor> descriptors = new EnumMap<>(ToolType.class);

    public MiniAppRegistry() {}

    @NonNull
    public static MiniAppRegistry getInstance() {
        MiniAppRegistry inst = defaultInstance;
        if (inst == null) {
            synchronized (MiniAppRegistry.class) {
                inst = defaultInstance;
                if (inst == null) {
                    inst = new MiniAppRegistry();
                    inst.registerBuiltInMiniApps();
                    defaultInstance = inst;
                }
            }
        }
        return inst;
    }

    public synchronized void registerBuiltInMiniApps() {
        register(new MiniAppDescriptor(
                ToolType.CALCULATOR, R.layout.miniapp_calculator,
                true, true, false, false,
                CalculatorController::new));
        register(new MiniAppDescriptor(
                ToolType.NOTES, R.layout.miniapp_notes,
                true, true, false, false,
                NotesController::new));
        register(new MiniAppDescriptor(
                ToolType.BROWSER, R.layout.miniapp_browser,
                true, true, false, true,
                BrowserController::new));
        register(new MiniAppDescriptor(
                ToolType.STOPWATCH_TIMER, R.layout.miniapp_stopwatch_timer,
                true, true, false, false,
                StopwatchTimerController::new));
        register(new MiniAppDescriptor(
                ToolType.CLOCK, R.layout.miniapp_clock,
                true, false, false, false,
                ClockController::new));
        register(new MiniAppDescriptor(
                ToolType.TEXT_EDITOR, R.layout.miniapp_text_editor,
                true, true, false, false,
                TextEditorController::new));
        register(new MiniAppDescriptor(
                ToolType.CLIPBOARD, R.layout.miniapp_clipboard,
                true, false, false, false,
                ClipboardController::new));
        register(new MiniAppDescriptor(
                ToolType.TODO, R.layout.miniapp_todo,
                true, true, false, false,
                TodoController::new));
        register(new MiniAppDescriptor(
                ToolType.QR_SCANNER, R.layout.miniapp_qr_scanner,
                false, false, true, false,
                QrScannerController::new));
        register(new MiniAppDescriptor(
                ToolType.TRANSLATOR, R.layout.miniapp_translator,
                true, true, false, true,
                TranslatorController::new));
        register(new MiniAppDescriptor(
                ToolType.AI_ASSISTANT, R.layout.miniapp_ai_assistant,
                true, true, false, true,
                AiAssistantController::new));
        register(new MiniAppDescriptor(
                ToolType.MUSIC_CONTROLS, R.layout.miniapp_music_controls,
                false, false, false, false,
                MusicControlsController::new));
    }

    public synchronized void register(@NonNull MiniAppDescriptor descriptor) {
        descriptors.put(descriptor.toolType, descriptor);
    }

    @Nullable
    public synchronized MiniAppDescriptor unregister(@NonNull ToolType toolType) {
        return descriptors.remove(toolType);
    }

    public synchronized boolean isRegistered(@NonNull ToolType toolType) {
        return descriptors.containsKey(toolType);
    }

    @Nullable
    public synchronized MiniAppDescriptor getDescriptor(@NonNull ToolType toolType) {
        return descriptors.get(toolType);
    }

    @NonNull
    public synchronized List<MiniAppDescriptor> getAllDescriptors() {
        return Collections.unmodifiableList(new ArrayList<>(descriptors.values()));
    }

    public synchronized int size() {
        return descriptors.size();
    }

    @NonNull
    public MiniApp create(@NonNull ToolType toolType,
                          @NonNull Context context,
                          @NonNull MiniApp.HostCallback hostCallback) {
        MiniAppDescriptor descriptor;
        synchronized (this) {
            descriptor = descriptors.get(toolType);
            if (descriptor == null) {
                descriptor = descriptors.get(ToolType.CALCULATOR);
            }
        }
        if (descriptor == null) {
            throw new IllegalStateException("No MiniApp registered for " + toolType);
        }
        return descriptor.create(context, hostCallback);
    }
}
