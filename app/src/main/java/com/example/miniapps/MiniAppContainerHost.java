package com.example.miniapps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Coordinates view inflation, state restoration, and deterministic lifecycle transitions
 * for a {@link MiniApp} hosted inside a floating window or activity container.
 */
public final class MiniAppContainerHost {

    @Nullable private MiniApp miniApp;
    @Nullable private ViewGroup container;
    @Nullable private View contentView;
    private boolean focused = false;

    @NonNull
    public View attach(@NonNull MiniApp app,
                       @NonNull LayoutInflater inflater,
                       @NonNull ViewGroup hostContainer,
                       @Nullable Bundle savedState,
                       boolean startMinimized) {
        if (this.miniApp != null) {
            destroy();
        }
        this.miniApp = app;
        this.container = hostContainer;

        View view = app.onCreateView(inflater, hostContainer, savedState);
        this.contentView = view;
        hostContainer.removeAllViews();
        hostContainer.addView(view);

        app.onStart();
        if (startMinimized) {
            app.onPause();
        } else {
            app.onResume();
        }
        return view;
    }

    public void dispatchStart() {
        if (miniApp == null) return;
        MiniApp.LifecycleState s = miniApp.getLifecycleState();
        if (s == MiniApp.LifecycleState.VIEW_CREATED) {
            miniApp.onStart();
        }
    }

    public void dispatchResume() {
        if (miniApp == null) return;
        MiniApp.LifecycleState s = miniApp.getLifecycleState();
        if (s == MiniApp.LifecycleState.DESTROYED) return;
        if (s == MiniApp.LifecycleState.VIEW_CREATED) {
            miniApp.onStart();
            s = miniApp.getLifecycleState();
        }
        if (s == MiniApp.LifecycleState.STARTED || s == MiniApp.LifecycleState.PAUSED) {
            miniApp.onResume();
        }
    }

    public void dispatchPause() {
        if (miniApp == null) return;
        if (miniApp.getLifecycleState() == MiniApp.LifecycleState.RESUMED
                || miniApp.getLifecycleState() == MiniApp.LifecycleState.STARTED) {
            miniApp.onPause();
        }
    }

    public void setMinimized(boolean minimized) {
        if (minimized) {
            setFocused(false);
            dispatchPause();
        } else {
            dispatchResume();
        }
    }

    public void setFocused(boolean nowFocused) {
        if (miniApp == null || miniApp.getLifecycleState() == MiniApp.LifecycleState.DESTROYED) {
            return;
        }
        if (this.focused != nowFocused) {
            this.focused = nowFocused;
            miniApp.onFocusChanged(nowFocused);
        }
    }

    @NonNull
    public Bundle captureState() {
        Bundle out = new Bundle();
        if (miniApp != null && miniApp.getLifecycleState() != MiniApp.LifecycleState.DESTROYED) {
            miniApp.onSaveState(out);
        }
        return out;
    }

    public void destroy() {
        if (miniApp != null) {
            if (focused) {
                focused = false;
                miniApp.onFocusChanged(false);
            }
            MiniApp.LifecycleState s = miniApp.getLifecycleState();
            if (s == MiniApp.LifecycleState.RESUMED || s == MiniApp.LifecycleState.STARTED) {
                miniApp.onPause();
            }
            if (miniApp.getLifecycleState() != MiniApp.LifecycleState.DESTROYED) {
                miniApp.onDestroy();
            }
            miniApp = null;
        }
        if (container != null) {
            container.removeAllViews();
            container = null;
        }
        contentView = null;
    }

    @Nullable
    public MiniApp getMiniApp() {
        return miniApp;
    }

    @Nullable
    public View getContentView() {
        return contentView;
    }

    public boolean isFocused() {
        return focused;
    }
}
