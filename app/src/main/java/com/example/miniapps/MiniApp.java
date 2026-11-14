package com.example.miniapps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.model.ToolType;

/**
 * Modular contract for all MultiFloat productivity mini-apps hosted inside floating windows
 * or full-screen tool containers.
 */
public interface MiniApp {

    enum LifecycleState {
        CREATED,
        VIEW_CREATED,
        STARTED,
        RESUMED,
        PAUSED,
        DESTROYED
    }

    interface HostCallback {
        void onInputFocusRequested(boolean focusable);
        void onOpenSettingsRequested();
        void onRequestCameraPermission();
        void onOpenFullScreenTool(@NonNull ToolType toolType);
    }

    @NonNull
    ToolType getToolType();

    @NonNull
    LifecycleState getLifecycleState();

    @NonNull
    View onCreateView(@NonNull LayoutInflater inflater,
                      @NonNull ViewGroup parent,
                      @Nullable Bundle savedState);

    void onStart();

    void onResume();

    void onPause();

    void onFocusChanged(boolean focused);

    void onSaveState(@NonNull Bundle outState);

    void onDestroy();
}
