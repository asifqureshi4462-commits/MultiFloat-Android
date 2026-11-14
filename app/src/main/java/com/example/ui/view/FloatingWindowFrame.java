package com.example.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Root container for floating windows that intercepts initial touch events at the window level
 * so tapping anywhere inside a background window (title bar or mini-app content) promotes it
 * to the front of the z-index stack without consuming child click/scroll events.
 */
public class FloatingWindowFrame extends FrameLayout {

    public interface OnWindowTouchListener {
        void onWindowTouchDown();
        void onWindowTouchFinished();
    }

    @Nullable
    private OnWindowTouchListener windowTouchListener;

    public FloatingWindowFrame(@NonNull Context context) {
        super(context);
    }

    public FloatingWindowFrame(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingWindowFrame(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnWindowTouchListener(@Nullable OnWindowTouchListener listener) {
        this.windowTouchListener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && windowTouchListener != null) {
            windowTouchListener.onWindowTouchDown();
        }
        boolean handled = super.dispatchTouchEvent(ev);
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                && windowTouchListener != null) {
            windowTouchListener.onWindowTouchFinished();
        }
        return handled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
