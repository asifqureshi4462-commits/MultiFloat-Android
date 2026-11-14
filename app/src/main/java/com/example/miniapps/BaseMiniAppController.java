package com.example.miniapps;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseMiniAppController implements MiniApp {

    public interface HostCallback extends MiniApp.HostCallback {}

    protected final Context context;
    protected final MiniApp.HostCallback hostCallback;
    protected View rootView;
    @Nullable private Bundle pendingSavedState;
    private LifecycleState lifecycleState = LifecycleState.CREATED;

    private static class BoundObserver<T> {
        final LiveData<T> liveData;
        final Observer<T> observer;

        BoundObserver(LiveData<T> ld, Observer<T> obs) {
            this.liveData = ld;
            this.observer = obs;
        }

        void detach() {
            liveData.removeObserver(observer);
        }
    }

    private final List<BoundObserver<?>> boundObservers = new ArrayList<>();

    protected BaseMiniAppController(@NonNull Context context, @NonNull MiniApp.HostCallback hostCallback) {
        this.context = context;
        this.hostCallback = hostCallback;
    }

    @NonNull
    @Override
    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

    @NonNull
    public View createView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return onCreateView(inflater, parent, null);
    }

    @NonNull
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater,
                                   @NonNull ViewGroup parent,
                                   @Nullable Bundle savedState) {
        this.pendingSavedState = savedState;
        rootView = onCreateView(inflater, parent);
        attachInputFocusListeners(rootView);
        lifecycleState = LifecycleState.VIEW_CREATED;
        if (savedState != null) {
            onRestoreState(savedState);
        }
        return rootView;
    }

    @NonNull
    protected abstract View onCreateView(LayoutInflater inflater, ViewGroup parent);

    protected void onRestoreState(@NonNull Bundle savedState) {}

    @Nullable
    protected Bundle getPendingSavedState() {
        return pendingSavedState;
    }

    @Override
    public void onStart() {
        if (lifecycleState == LifecycleState.VIEW_CREATED) {
            lifecycleState = LifecycleState.STARTED;
        }
    }

    @Override
    public void onResume() {
        if (lifecycleState == LifecycleState.STARTED || lifecycleState == LifecycleState.PAUSED) {
            lifecycleState = LifecycleState.RESUMED;
        }
    }

    @Override
    public void onPause() {
        if (lifecycleState == LifecycleState.RESUMED || lifecycleState == LifecycleState.STARTED) {
            lifecycleState = LifecycleState.PAUSED;
        }
    }

    @Override
    public void onFocusChanged(boolean focused) {
        if (!focused && rootView != null) {
            View focusedChild = rootView.findFocus();
            if (focusedChild != null) {
                focusedChild.clearFocus();
            }
        }
    }

    @Override
    public void onSaveState(@NonNull Bundle outState) {}

    @Override
    public void onDestroy() {
        if (lifecycleState == LifecycleState.DESTROYED) {
            return;
        }
        lifecycleState = LifecycleState.DESTROYED;
        for (BoundObserver<?> bo : boundObservers) {
            bo.detach();
        }
        boundObservers.clear();
        rootView = null;
        pendingSavedState = null;
    }

    public void onDetached() {
        onDestroy();
    }

    protected <T> void observe(LiveData<T> liveData, Observer<T> observer) {
        liveData.observeForever(observer);
        boundObservers.add(new BoundObserver<>(liveData, observer));
    }

    protected void attachInputFocusListeners(View view) {
        if (view instanceof EditText) {
            view.setOnTouchListener((v, event) -> {
                if (hostCallback != null) {
                    hostCallback.onInputFocusRequested(true);
                }
                return false;
            });
            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (hostCallback != null) {
                    hostCallback.onInputFocusRequested(hasFocus);
                }
            });
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                attachInputFocusListeners(vg.getChildAt(i));
            }
        }
    }
}
