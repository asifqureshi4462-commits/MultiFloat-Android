package com.example.service;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.model.FloatingWindowState;
import com.example.model.ToolType;
import com.example.util.PermissionHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reactive state bus bridging FloatingWindowService and Activities/ViewModels
 * without leaking Service or Activity references.
 */
public class FloatingEngineBus {

    private static final MutableLiveData<Boolean> engineRunning = new MutableLiveData<>(false);
    private static final MutableLiveData<List<FloatingWindowState>> windowsLive =
            new MutableLiveData<>(Collections.emptyList());

    public static LiveData<Boolean> observeEngineRunning() {
        return engineRunning;
    }

    public static LiveData<List<FloatingWindowState>> observeWindows() {
        return windowsLive;
    }

    public static boolean isRunning() {
        Boolean val = engineRunning.getValue();
        return val != null && val;
    }

    public static void updateRunning(boolean running) {
        engineRunning.postValue(running);
        if (!running) {
            windowsLive.postValue(Collections.emptyList());
        }
    }

    public static void updateWindows(List<FloatingWindowState> list) {
        windowsLive.postValue(new ArrayList<>(list));
    }

    public static boolean startEngine(Context context) {
        if (!PermissionHelper.canDrawOverlays(context)) {
            return false;
        }
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(FloatingWindowService.ACTION_START_ENGINE);
        ContextCompat.startForegroundService(context, intent);
        return true;
    }

    public static void stopEngine(Context context) {
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(FloatingWindowService.ACTION_STOP_ENGINE);
        context.startService(intent);
    }

    public static boolean openFloatingTool(Context context, ToolType toolType) {
        return openFloatingTool(context, toolType, false);
    }

    public static boolean openFloatingTool(Context context, ToolType toolType, boolean forceNewInstance) {
        if (!PermissionHelper.canDrawOverlays(context)) {
            return false;
        }
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(FloatingWindowService.ACTION_OPEN_TOOL);
        intent.putExtra(FloatingWindowService.EXTRA_TOOL_ID, toolType.id);
        intent.putExtra(FloatingWindowService.EXTRA_FORCE_NEW_INSTANCE, forceNewInstance);
        ContextCompat.startForegroundService(context, intent);
        return true;
    }

    public static void sendWindowAction(Context context, String action, String windowId) {
        if (!isRunning()) return;
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(action);
        if (windowId != null) {
            intent.putExtra(FloatingWindowService.EXTRA_WINDOW_ID, windowId);
        }
        context.startService(intent);
    }

    public static void refreshConfig(Context context) {
        if (!isRunning()) return;
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(FloatingWindowService.ACTION_REFRESH_CONFIG);
        context.startService(intent);
    }

    public static boolean openSidebarDrawer(Context context) {
        if (!PermissionHelper.canDrawOverlays(context)) {
            return false;
        }
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.setAction(FloatingWindowService.ACTION_OPEN_SIDEBAR);
        ContextCompat.startForegroundService(context, intent);
        return true;
    }
}
