package com.example.miniapps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseMiniAppController {

    public interface HostCallback {
        void onInputFocusRequested(boolean focusable);
        void onOpenSettingsRequested();
        void onRequestCameraPermission();
        void onOpenFullScreenTool(com.example.model.ToolType toolType);
    }

    protected final Context context;
    protected final HostCallback hostCallback;
    protected View rootView;

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

    protected BaseMiniAppController(Context context, HostCallback hostCallback) {
        this.context = context;
        this.hostCallback = hostCallback;
    }

    public View createView(LayoutInflater inflater, ViewGroup parent) {
        rootView = onCreateView(inflater, parent);
        attachInputFocusListeners(rootView);
        return rootView;
    }

    protected abstract View onCreateView(LayoutInflater inflater, ViewGroup parent);

    public void onDetached() {
        for (BoundObserver<?> bo : boundObservers) {
            bo.detach();
        }
        boundObservers.clear();
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
