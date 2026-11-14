package com.example.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import com.example.MultiFloatApp;

public class ClipboardHelper {

    public static void copyText(Context context, String label, String text, boolean showToast) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText(label, text));
        }
        MultiFloatApp app = MultiFloatApp.get();
        if (app != null) {
            app.getClipboardRepository().saveEntryIfNew(text);
        }
        if (showToast) {
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    public static String readCurrentClipboard(Context context) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) return "";
        ClipData data = manager.getPrimaryClip();
        if (data != null && data.getItemCount() > 0) {
            CharSequence seq = data.getItemAt(0).coerceToText(context);
            return seq != null ? seq.toString() : "";
        }
        return "";
    }
}
