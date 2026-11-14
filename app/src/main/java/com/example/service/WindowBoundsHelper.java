package com.example.service;

import android.content.Context;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import com.example.util.EdgeToEdgeHelper;

/**
 * Computes real system-inset-aware screen boundaries and enforces visible bounds clamping
 * so floating windows never disappear off-screen or slide underneath status/navigation bars.
 */
public final class WindowBoundsHelper {

    private WindowBoundsHelper() {}

    public static final class ClampedPoint {
        public final int x;
        public final int y;

        public ClampedPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static Rect getRawScreenBounds(WindowManager wm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = wm.getCurrentWindowMetrics();
            return new Rect(metrics.getBounds());
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            return new Rect(0, 0, dm.widthPixels, dm.heightPixels);
        }
    }

    public static Rect getSafeScreenBounds(WindowManager wm, Context context) {
        int marginH = EdgeToEdgeHelper.dpToPx(context, 6);
        int marginTopExtra = EdgeToEdgeHelper.dpToPx(context, 6);
        int marginBottomExtra = EdgeToEdgeHelper.dpToPx(context, 8);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = wm.getCurrentWindowMetrics();
            Rect bounds = metrics.getBounds();
            Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int left = bounds.left + insets.left + marginH;
            int top = bounds.top + Math.max(insets.top, EdgeToEdgeHelper.dpToPx(context, 24)) + marginTopExtra;
            int right = bounds.right - insets.right - marginH;
            int bottom = bounds.bottom - Math.max(insets.bottom, EdgeToEdgeHelper.dpToPx(context, 16)) - marginBottomExtra;
            return new Rect(left, top, Math.max(left + 100, right), Math.max(top + 100, bottom));
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            int statusH = getStatusBarHeightPx(context);
            int navH = getNavBarHeightPx(context);
            int left = marginH;
            int top = statusH + marginTopExtra;
            int right = dm.widthPixels - marginH;
            int bottom = dm.heightPixels - navH - marginBottomExtra;
            return new Rect(left, top, Math.max(left + 100, right), Math.max(top + 100, bottom));
        }
    }

    private static int getStatusBarHeightPx(Context context) {
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return EdgeToEdgeHelper.dpToPx(context, 28);
    }

    private static int getNavBarHeightPx(Context context) {
        int resId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return EdgeToEdgeHelper.dpToPx(context, 24);
    }

    /**
     * Pure math clamping keeping a window of (width, height) strictly inside [safeLeft..safeRight, safeTop..safeBottom].
     */
    public static ClampedPoint clampWindowPosition(
            int x, int y, int width, int height,
            int safeLeft, int safeTop, int safeRight, int safeBottom) {
        int maxX = Math.max(safeLeft, safeRight - width);
        int maxY = Math.max(safeTop, safeBottom - height);
        int cx = Math.max(safeLeft, Math.min(maxX, x));
        int cy = Math.max(safeTop, Math.min(maxY, y));
        return new ClampedPoint(cx, cy);
    }

    public static ClampedPoint clampWindowPosition(int x, int y, int width, int height, Rect safeBounds) {
        return clampWindowPosition(x, y, width, height,
                safeBounds.left, safeBounds.top, safeBounds.right, safeBounds.bottom);
    }
}
