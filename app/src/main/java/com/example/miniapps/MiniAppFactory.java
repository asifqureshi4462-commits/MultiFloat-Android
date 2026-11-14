package com.example.miniapps;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.model.ToolType;

/**
 * Compatibility facade delegating MiniApp creation to {@link MiniAppRegistry}.
 */
public final class MiniAppFactory {

    private MiniAppFactory() {}

    @NonNull
    public static MiniApp create(@NonNull ToolType toolType,
                                 @NonNull Context context,
                                 @NonNull MiniApp.HostCallback callback) {
        return MiniAppRegistry.getInstance().create(toolType, context, callback);
    }
}
