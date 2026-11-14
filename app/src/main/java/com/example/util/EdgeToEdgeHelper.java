package com.example.util;

import android.app.Activity;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EdgeToEdgeHelper {

    public static void setup(AppCompatActivity activity, View rootView) {
        EdgeToEdge.enable(activity);
        if (rootView != null) {
            final int origLeft = rootView.getPaddingLeft();
            final int origTop = rootView.getPaddingTop();
            final int origRight = rootView.getPaddingRight();
            final int origBottom = rootView.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        origLeft + bars.left,
                        origTop + bars.top,
                        origRight + bars.right,
                        origBottom + bars.bottom
                );
                return windowInsets;
            });
        }
    }

    public static int dpToPx(Activity activity, int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    public static int dpToPx(android.content.Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
