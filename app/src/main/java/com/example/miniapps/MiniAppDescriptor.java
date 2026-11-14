package com.example.miniapps;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import com.example.model.ToolType;

/**
 * Immutable registration descriptor binding a {@link ToolType} to its layout metadata,
 * capabilities, and factory provider inside {@link MiniAppRegistry}.
 */
public final class MiniAppDescriptor {

    @FunctionalInterface
    public interface Provider {
        @NonNull
        MiniApp create(@NonNull Context context, @NonNull MiniApp.HostCallback hostCallback);
    }

    @NonNull public final ToolType toolType;
    @LayoutRes public final int layoutRes;
    public final boolean supportsMultipleInstances;
    public final boolean supportsStatePersistence;
    public final boolean requiresCamera;
    public final boolean requiresNetwork;
    @NonNull public final Provider provider;

    public MiniAppDescriptor(
            @NonNull ToolType toolType,
            @LayoutRes int layoutRes,
            boolean supportsMultipleInstances,
            boolean supportsStatePersistence,
            boolean requiresCamera,
            boolean requiresNetwork,
            @NonNull Provider provider) {
        this.toolType = toolType;
        this.layoutRes = layoutRes;
        this.supportsMultipleInstances = supportsMultipleInstances;
        this.supportsStatePersistence = supportsStatePersistence;
        this.requiresCamera = requiresCamera;
        this.requiresNetwork = requiresNetwork;
        this.provider = provider;
    }

    @NonNull
    public MiniApp create(@NonNull Context context, @NonNull MiniApp.HostCallback hostCallback) {
        return provider.create(context, hostCallback);
    }
}
