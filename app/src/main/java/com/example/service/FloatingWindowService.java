package com.example.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;
import com.example.data.repository.InstalledAppsRepository;
import com.example.miniapps.MiniApp;
import com.example.miniapps.MiniAppContainerHost;
import com.example.miniapps.MiniAppRegistry;
import com.example.model.FloatingWindowState;
import com.example.model.InstalledAppInfo;
import com.example.model.SidebarShortcut;
import com.example.model.ToolType;
import com.example.ui.InstalledAppsActivity;
import com.example.ui.MainActivity;
import com.example.ui.PermissionSetupActivity;
import com.example.ui.settings.GeneralSettingsActivity;
import com.example.ui.settings.SidebarSettingsActivity;
import com.example.ui.tools.QrScannerActivity;
import com.example.ui.view.FloatingWindowFrame;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.ExternalAppLauncher;
import com.example.util.PermissionHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;

public class FloatingWindowService extends Service {

    public static final String ACTION_START_ENGINE = "com.example.action.START_ENGINE";
    public static final String ACTION_STOP_ENGINE = "com.example.action.STOP_ENGINE";
    public static final String ACTION_OPEN_TOOL = "com.example.action.OPEN_TOOL";
    public static final String ACTION_FOCUS_WINDOW = "com.example.action.FOCUS_WINDOW";
    public static final String ACTION_MINIMIZE_WINDOW = "com.example.action.MINIMIZE_WINDOW";
    public static final String ACTION_RESTORE_WINDOW = "com.example.action.RESTORE_WINDOW";
    public static final String ACTION_CLOSE_WINDOW = "com.example.action.CLOSE_WINDOW";
    public static final String ACTION_MINIMIZE_ALL = "com.example.action.MINIMIZE_ALL";
    public static final String ACTION_CLOSE_ALL = "com.example.action.CLOSE_ALL";
    public static final String ACTION_RESTORE_SESSION = "com.example.action.RESTORE_SESSION";
    public static final String ACTION_TOGGLE_BUBBLE = "com.example.action.TOGGLE_BUBBLE";
    public static final String ACTION_REFRESH_CONFIG = "com.example.action.REFRESH_CONFIG";
    public static final String ACTION_OPEN_SIDEBAR = "com.example.action.OPEN_SIDEBAR";

    public static final String EXTRA_TOOL_ID = "extra_tool_id";
    public static final String EXTRA_WINDOW_ID = "extra_window_id";
    public static final String EXTRA_FORCE_NEW_INSTANCE = "extra_force_new_instance";

    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private LayoutInflater themedInflater;
    private Context themedContext;
    private int appliedThemeMode = -1;
    private AppPreferences preferences;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable reassertOverlaysRunnable = this::reassertSystemOverlaysOnTop;

    private View bubbleView;
    private WindowManager.LayoutParams bubbleParams;
    private TextView tvBubbleBadge;

    private View bubbleMenuView;
    private WindowManager.LayoutParams bubbleMenuParams;

    private View sidebarView;
    private WindowManager.LayoutParams sidebarParams;
    private boolean sidebarExpanded = false;
    private boolean torchOn = false;
    private ToolType.DrawerFilter currentDrawerFilter = ToolType.DrawerFilter.ALL;
    private String sidebarSearchQuery = "";

    private View dragGhostView;
    private WindowManager.LayoutParams dragGhostParams;
    private TextView tvDragGhostHint;

    private View taskbarDockView;
    private WindowManager.LayoutParams taskbarDockParams;

    private final Map<String, FloatingWindowInstance> windows = new LinkedHashMap<>();
    private final WindowZOrderManager zOrderManager = new WindowZOrderManager();
    @Nullable
    private String pendingBringToFrontWindowId = null;

    private static class FloatingWindowInstance {
        final FloatingWindowState state;
        final View rootView;
        final WindowManager.LayoutParams params;
        final MiniAppContainerHost miniAppHost;
        final LinearLayout cardInner;
        final View focusDot;
        final View opacityPanel;
        @Nullable ValueAnimator transitionAnimator;

        FloatingWindowInstance(FloatingWindowState state, View rootView,
                               WindowManager.LayoutParams params, MiniAppContainerHost miniAppHost) {
            this.state = state;
            this.rootView = rootView;
            this.params = params;
            this.miniAppHost = miniAppHost;
            this.cardInner = rootView.findViewById(R.id.window_card_inner);
            this.focusDot = rootView.findViewById(R.id.view_focus_dot);
            this.opacityPanel = rootView.findViewById(R.id.panel_win_opacity_bar);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        preferences = MultiFloatApp.get().getPreferences();
        rebuildThemedContext();
    }

    private void rebuildThemedContext() {
        int mode = preferences.getThemeMode();
        appliedThemeMode = mode;
        Configuration config = new Configuration(getResources().getConfiguration());
        if (mode == AppPreferences.THEME_LIGHT) {
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                    | Configuration.UI_MODE_NIGHT_NO;
        } else if (mode == AppPreferences.THEME_DARK) {
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                    | Configuration.UI_MODE_NIGHT_YES;
        }
        Context cfgContext = createConfigurationContext(config);
        themedContext = new ContextThemeWrapper(cfgContext, R.style.Theme_MultiFloat);
        themedInflater = LayoutInflater.from(themedContext);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // startForegroundService() requires startForeground() to be called quickly, otherwise
        // Android 12+ kills the app. So always promote first, then stop if overlay is missing.
        startAsForeground();
        if (!PermissionHelper.canDrawOverlays(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        FloatingEngineBus.updateRunning(true);

        if (bubbleView == null && preferences.isBubbleEnabled()) {
            createBubble();
        }
        if (sidebarView == null && preferences.isSidebarEnabled()) {
            createSidebar();
        }

        String action = intent != null ? intent.getAction() : ACTION_START_ENGINE;
        if (action == null) action = ACTION_START_ENGINE;

        switch (action) {
            case ACTION_START_ENGINE:
                if (windows.isEmpty() && preferences.isRestoreSessionEnabled()) {
                    restorePreviousSession();
                }
                break;
            case ACTION_STOP_ENGINE:
                closeAllWindows();
                removeBubble();
                removeBubbleMenu();
                removeDragGhost();
                removeTaskbarDock();
                removeSidebar();
                FloatingEngineBus.updateRunning(false);
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return START_NOT_STICKY;
            case ACTION_OPEN_TOOL:
                String toolId = intent.getStringExtra(EXTRA_TOOL_ID);
                boolean forceNew = intent.getBooleanExtra(EXTRA_FORCE_NEW_INSTANCE, false);
                openToolWindow(ToolType.fromId(toolId), null, null, forceNew);
                break;
            case ACTION_FOCUS_WINDOW:
            case ACTION_RESTORE_WINDOW:
                restoreOrFocusWindow(intent.getStringExtra(EXTRA_WINDOW_ID));
                break;
            case ACTION_MINIMIZE_WINDOW:
                minimizeWindow(intent.getStringExtra(EXTRA_WINDOW_ID));
                break;
            case ACTION_CLOSE_WINDOW:
                closeWindow(intent.getStringExtra(EXTRA_WINDOW_ID));
                break;
            case ACTION_MINIMIZE_ALL:
                minimizeAllWindows();
                break;
            case ACTION_CLOSE_ALL:
                closeAllWindows();
                break;
            case ACTION_RESTORE_SESSION:
                restorePreviousSession();
                break;
            case ACTION_TOGGLE_BUBBLE:
                if (bubbleView == null) {
                    preferences.setBubbleEnabled(true);
                    createBubble();
                } else {
                    preferences.setBubbleEnabled(false);
                    removeBubble();
                }
                break;
            case ACTION_REFRESH_CONFIG:
                applyPreferencesRefresh();
                break;
            case ACTION_OPEN_SIDEBAR:
                if (sidebarView == null) {
                    createSidebar();
                }
                expandSidebar();
                break;
        }

        updateNotification();
        broadcastWindowStates();
        return START_STICKY;
    }

    private void startAsForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification() {
        Intent dashIntent = new Intent(this, MainActivity.class);
        PendingIntent dashPi = PendingIntent.getActivity(
                this, 0, dashIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent minAllPi = PendingIntent.getService(
                this, 1, new Intent(this, FloatingWindowService.class).setAction(ACTION_MINIMIZE_ALL),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent closeAllPi = PendingIntent.getService(
                this, 2, new Intent(this, FloatingWindowService.class).setAction(ACTION_CLOSE_ALL),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent stopPi = PendingIntent.getService(
                this, 3, new Intent(this, FloatingWindowService.class).setAction(ACTION_STOP_ENGINE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, MultiFloatApp.CHANNEL_ENGINE)
                .setSmallIcon(R.drawable.ic_windows)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text, windows.size()))
                .setContentIntent(dashPi)
                .setOngoing(true)
                .setSilent(true)
                .addAction(0, getString(R.string.notif_action_minimize_all), minAllPi)
                .addAction(0, getString(R.string.notif_action_close_all), closeAllPi)
                .addAction(0, getString(R.string.notif_action_stop), stopPi)
                .build();
    }

    private void updateNotification() {
        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private int overlayWindowType() {
        return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    }

    private DisplayMetrics screenMetrics() {
        Rect raw = WindowBoundsHelper.getRawScreenBounds(windowManager);
        DisplayMetrics dm = new DisplayMetrics();
        dm.setTo(getResources().getDisplayMetrics());
        dm.widthPixels = raw.width();
        dm.heightPixels = raw.height();
        return dm;
    }

    // ==================== FLOATING BUBBLE ====================

    @SuppressLint("ClickableViewAccessibility")
    private void createBubble() {
        if (bubbleView != null) return;
        bubbleView = themedInflater.inflate(R.layout.overlay_bubble, null);
        tvBubbleBadge = bubbleView.findViewById(R.id.tv_bubble_badge);
        FrameLayout orb = bubbleView.findViewById(R.id.bubble_orb);

        int sizePx = EdgeToEdgeHelper.dpToPx(this, preferences.getBubbleSizeDp());
        ViewGroup.LayoutParams orbLp = orb.getLayoutParams();
        orbLp.width = sizePx;
        orbLp.height = sizePx;
        orb.setLayoutParams(orbLp);
        bubbleView.setAlpha(preferences.getBubbleOpacityPercent() / 100f);

        Rect safe = WindowBoundsHelper.getSafeScreenBounds(windowManager, this);
        bubbleParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        int defX = safe.right - sizePx - EdgeToEdgeHelper.dpToPx(this, 8);
        int defY = safe.top + safe.height() / 3;
        WindowBoundsHelper.ClampedPoint clamped = WindowBoundsHelper.clampWindowPosition(
                preferences.getBubbleX(defX),
                preferences.getBubbleY(defY),
                sizePx, sizePx, safe);
        bubbleParams.x = clamped.x;
        bubbleParams.y = clamped.y;

        final int touchSlop = ViewConfiguration.get(themedContext).getScaledTouchSlop();
        orb.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY;
            float downX, downY;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = bubbleParams.x;
                        initY = bubbleParams.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (Math.hypot(dx, dy) > touchSlop) {
                            moved = true;
                        }
                        if (moved) {
                            Rect curSafe = WindowBoundsHelper.getSafeScreenBounds(windowManager, FloatingWindowService.this);
                            WindowBoundsHelper.ClampedPoint p = WindowBoundsHelper.clampWindowPosition(
                                    initX + Math.round(dx), initY + Math.round(dy), sizePx, sizePx, curSafe);
                            bubbleParams.x = p.x;
                            bubbleParams.y = p.y;
                            windowManager.updateViewLayout(bubbleView, bubbleParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            toggleBubbleMenu();
                        } else {
                            Rect curSafe = WindowBoundsHelper.getSafeScreenBounds(windowManager, FloatingWindowService.this);
                            int edgePad = EdgeToEdgeHelper.dpToPx(FloatingWindowService.this, 6);
                            int targetX = (bubbleParams.x + sizePx / 2 < curSafe.centerX())
                                    ? curSafe.left + edgePad : curSafe.right - sizePx - edgePad;
                            animateBubbleSnapX(bubbleParams.x, targetX);
                            preferences.saveBubblePosition(targetX, bubbleParams.y);
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(bubbleView, bubbleParams);
        updateBubbleBadge();
    }

    private void animateBubbleSnapX(int fromX, int toX) {
        if (bubbleView == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(fromX, toX);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            if (bubbleView != null && bubbleView.isAttachedToWindow()) {
                bubbleParams.x = (int) anim.getAnimatedValue();
                windowManager.updateViewLayout(bubbleView, bubbleParams);
            }
        });
        animator.start();
    }

    private void updateBubbleBadge() {
        if (tvBubbleBadge == null) return;
        int count = windows.size();
        if (count > 0) {
            tvBubbleBadge.setVisibility(View.VISIBLE);
            tvBubbleBadge.setText(String.valueOf(count));
        } else {
            tvBubbleBadge.setVisibility(View.GONE);
        }
    }

    private void removeBubble() {
        if (bubbleView != null) {
            try { windowManager.removeView(bubbleView); } catch (Exception ignored) {}
            bubbleView = null;
            tvBubbleBadge = null;
        }
    }

    private void toggleBubbleMenu() {
        if (bubbleMenuView != null) {
            removeBubbleMenu();
            return;
        }
        bubbleMenuView = themedInflater.inflate(R.layout.overlay_bubble_menu, null);
        bubbleMenuParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        bubbleMenuParams.gravity = Gravity.CENTER;

        bubbleMenuView.findViewById(R.id.btn_bubble_menu_close).setOnClickListener(v -> removeBubbleMenu());

        // Populate Active/Minimized Windows
        TextView tvWinHeader = bubbleMenuView.findViewById(R.id.tv_bm_windows_header);
        ScrollView scrollWindows = bubbleMenuView.findViewById(R.id.scroll_bm_windows);
        LinearLayout winContainer = bubbleMenuView.findViewById(R.id.container_bm_windows);
        if (!windows.isEmpty()) {
            tvWinHeader.setVisibility(View.VISIBLE);
            if (windows.size() > 3 && scrollWindows != null) {
                ViewGroup.LayoutParams slp = scrollWindows.getLayoutParams();
                slp.height = EdgeToEdgeHelper.dpToPx(this, 130);
                scrollWindows.setLayoutParams(slp);
            }
            for (FloatingWindowInstance inst : windows.values()) {
                LinearLayout pill = new LinearLayout(themedContext);
                pill.setOrientation(LinearLayout.HORIZONTAL);
                pill.setGravity(Gravity.CENTER_VERTICAL);
                pill.setBackgroundResource(R.drawable.bg_pill_chip);
                pill.setPadding(20, 12, 20, 12);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = 8;
                pill.setLayoutParams(lp);

                TextView label = new TextView(themedContext);
                label.setText(inst.state.title + (inst.state.minimized ? " (Minimized)" : " (Open)"));
                label.setTextSize(12f);
                label.setTextColor(themedContext.getColor(R.color.mf_text_primary));
                label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                pill.addView(label);

                TextView action = new TextView(themedContext);
                action.setText(inst.state.minimized ? "Restore" : "Focus");
                action.setTextSize(11f);
                action.setTextColor(themedContext.getColor(R.color.mf_cyan_text));
                pill.addView(action);

                pill.setOnClickListener(v -> {
                    restoreOrFocusWindow(inst.state.windowId);
                    removeBubbleMenu();
                });
                winContainer.addView(pill);
            }
        }

        // Populate Quick Launch Mini-Apps Grid
        GridLayout grid = bubbleMenuView.findViewById(R.id.grid_bm_tools);
        for (ToolType type : ToolType.values()) {
            LinearLayout cell = (LinearLayout) themedInflater.inflate(R.layout.item_sidebar_tool, grid, false);
            ImageView icon = cell.findViewById(R.id.iv_sb_tool_icon);
            TextView name = cell.findViewById(R.id.tv_sb_tool_name);
            icon.setImageResource(type.iconRes);
            icon.setImageTintList(ColorStateList.valueOf(themedContext.getColor(R.color.mf_cyan_text)));
            name.setText(type.titleRes);
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = EdgeToEdgeHelper.dpToPx(this, 88);
            gp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            cell.setLayoutParams(gp);
            cell.setOnClickListener(v -> {
                removeBubbleMenu();
                openToolWindow(type, null, null, false);
            });
            grid.addView(cell);
        }

        bubbleMenuView.findViewById(R.id.btn_bm_minimize_all).setOnClickListener(v -> {
            minimizeAllWindows();
            removeBubbleMenu();
        });
        bubbleMenuView.findViewById(R.id.btn_bm_close_all).setOnClickListener(v -> {
            closeAllWindows();
            removeBubbleMenu();
        });
        bubbleMenuView.findViewById(R.id.btn_bm_dashboard).setOnClickListener(v -> {
            removeBubbleMenu();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });
        bubbleMenuView.findViewById(R.id.btn_bm_close_bubble).setOnClickListener(v -> {
            removeBubbleMenu();
            preferences.setBubbleEnabled(false);
            removeBubble();
            Toast.makeText(this, "Floating bubble closed. Re-enable from MultiFloat Dashboard.", Toast.LENGTH_SHORT).show();
        });

        bubbleMenuView.setAlpha(0f);
        bubbleMenuView.setScaleX(0.92f);
        bubbleMenuView.setScaleY(0.92f);
        windowManager.addView(bubbleMenuView, bubbleMenuParams);
        bubbleMenuView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
    }

    private void removeBubbleMenu() {
        if (bubbleMenuView != null) {
            try { windowManager.removeView(bubbleMenuView); } catch (Exception ignored) {}
            bubbleMenuView = null;
        }
    }

    // ==================== SLIDE-OUT DRAWER SIDEBAR ====================

    @SuppressLint("ClickableViewAccessibility")
    private void createSidebar() {
        if (sidebarView != null) return;
        sidebarView = themedInflater.inflate(R.layout.overlay_sidebar, null);
        sidebarExpanded = false;
        sidebarSearchQuery = "";

        boolean isLeft = AppPreferences.SIDEBAR_LEFT.equals(preferences.getSidebarPosition());
        LinearLayout root = sidebarView.findViewById(R.id.sidebar_root);
        View handle = sidebarView.findViewById(R.id.sidebar_handle);
        View panel = sidebarView.findViewById(R.id.sidebar_panel);
        ImageView ivHandleArrow = sidebarView.findViewById(R.id.iv_sb_handle_arrow);
        ScrollView scrollTools = sidebarView.findViewById(R.id.scroll_sb_tools);

        if (ivHandleArrow != null) {
            ivHandleArrow.setImageResource(isLeft ? R.drawable.ic_chevron_right : R.drawable.ic_chevron_left);
            ivHandleArrow.setRotation(0f);
        }

        // Order handle vs panel according to edge side
        root.removeAllViews();
        if (isLeft) {
            root.addView(panel);
            root.addView(handle);
        } else {
            root.addView(handle);
            root.addView(panel);
        }

        int sizeLevel = preferences.getSidebarSizeLevel();
        int widthDp = sizeLevel == 0 ? 196 : (sizeLevel == 2 ? 256 : 226);
        ViewGroup.LayoutParams panelLp = panel.getLayoutParams();
        panelLp.width = EdgeToEdgeHelper.dpToPx(this, widthDp);
        panel.setLayoutParams(panelLp);

        if (scrollTools != null) {
            DisplayMetrics dm = screenMetrics();
            ViewGroup.LayoutParams scrollLp = scrollTools.getLayoutParams();
            scrollLp.height = Math.min(EdgeToEdgeHelper.dpToPx(this, 290), Math.round(dm.heightPixels * 0.44f));
            scrollTools.setLayoutParams(scrollLp);
        }

        sidebarView.setAlpha(preferences.getSidebarOpacityPercent() / 100f);

        sidebarParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        sidebarParams.gravity = (isLeft ? Gravity.START : Gravity.END) | Gravity.CENTER_VERTICAL;

        int swipeThresholdPx = EdgeToEdgeHelper.dpToPx(this, 24);
        int touchSlop = ViewConfiguration.get(themedContext).getScaledTouchSlop();
        handle.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            boolean swipedOrMoved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        swipedOrMoved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                            swipedOrMoved = true;
                        }
                        if (!sidebarExpanded && ((isLeft && dx > swipeThresholdPx) || (!isLeft && dx < -swipeThresholdPx))) {
                            expandSidebar();
                            swipedOrMoved = true;
                        } else if (sidebarExpanded && ((isLeft && dx < -swipeThresholdPx) || (!isLeft && dx > swipeThresholdPx))) {
                            collapseSidebar(true);
                            swipedOrMoved = true;
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!swipedOrMoved) {
                            if (sidebarExpanded) {
                                collapseSidebar(true);
                            } else {
                                expandSidebar();
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        EditText etSearch = sidebarView.findViewById(R.id.et_sb_search);
        if (etSearch != null) {
            etSearch.setOnTouchListener((v, event) -> {
                setSidebarInputFocusable(true);
                return false;
            });
            etSearch.setOnFocusChangeListener((v, hasFocus) -> setSidebarInputFocusable(hasFocus));
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    sidebarSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.US) : "";
                    populateSidebarTools();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        sidebarView.findViewById(R.id.tab_sb_all).setOnClickListener(v -> {
            currentDrawerFilter = ToolType.DrawerFilter.ALL;
            updateDrawerFilterTabs();
            populateSidebarTools();
        });
        sidebarView.findViewById(R.id.tab_sb_apps).setOnClickListener(v -> {
            currentDrawerFilter = ToolType.DrawerFilter.APPS;
            updateDrawerFilterTabs();
            populateSidebarTools();
        });
        sidebarView.findViewById(R.id.tab_sb_productivity).setOnClickListener(v -> {
            currentDrawerFilter = ToolType.DrawerFilter.PRODUCTIVITY;
            updateDrawerFilterTabs();
            populateSidebarTools();
        });
        sidebarView.findViewById(R.id.tab_sb_favorites).setOnClickListener(v -> {
            currentDrawerFilter = ToolType.DrawerFilter.FAVORITES;
            updateDrawerFilterTabs();
            populateSidebarTools();
        });

        sidebarView.findViewById(R.id.btn_sb_collapse).setOnClickListener(v -> collapseSidebar(true));
        sidebarView.findViewById(R.id.btn_sb_flashlight).setOnClickListener(v -> toggleFlashlight());
        sidebarView.findViewById(R.id.btn_sb_apps).setOnClickListener(v -> {
            collapseSidebar(true);
            Intent i = new Intent(this, InstalledAppsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
        sidebarView.findViewById(R.id.btn_sb_dashboard).setOnClickListener(v -> {
            collapseSidebar(true);
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });
        sidebarView.findViewById(R.id.btn_sb_settings).setOnClickListener(v -> {
            collapseSidebar(true);
            Intent i = new Intent(this, SidebarSettingsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        windowManager.addView(sidebarView, sidebarParams);
    }

    private void setSidebarInputFocusable(boolean focusable) {
        if (sidebarView == null || sidebarParams == null || !sidebarView.isAttachedToWindow()) return;
        int prevFlags = sidebarParams.flags;
        if (focusable) {
            sidebarParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            sidebarParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        if (prevFlags != sidebarParams.flags) {
            windowManager.updateViewLayout(sidebarView, sidebarParams);
        }
    }

    private void expandSidebar() {
        if (sidebarView == null) return;
        View panel = sidebarView.findViewById(R.id.sidebar_panel);
        View arrow = sidebarView.findViewById(R.id.iv_sb_handle_arrow);
        boolean isLeft = AppPreferences.SIDEBAR_LEFT.equals(preferences.getSidebarPosition());

        sidebarExpanded = true;
        updateDrawerFilterTabs();
        populateSidebarTools();

        panel.animate().cancel();
        panel.setVisibility(View.VISIBLE);
        int widthPx = panel.getLayoutParams().width;
        if (widthPx <= 0) widthPx = EdgeToEdgeHelper.dpToPx(this, 226);
        panel.setTranslationX(isLeft ? -widthPx * 0.85f : widthPx * 0.85f);
        panel.setAlpha(0f);
        panel.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (arrow != null) {
            arrow.animate().rotation(180f).setDuration(200).start();
        }
    }

    private void collapseSidebar(boolean animate) {
        if (sidebarView == null) return;
        setSidebarInputFocusable(false);
        View panel = sidebarView.findViewById(R.id.sidebar_panel);
        View arrow = sidebarView.findViewById(R.id.iv_sb_handle_arrow);
        boolean isLeft = AppPreferences.SIDEBAR_LEFT.equals(preferences.getSidebarPosition());

        sidebarExpanded = false;
        if (arrow != null) {
            arrow.animate().rotation(0f).setDuration(180).start();
        }

        panel.animate().cancel();
        if (!animate || panel.getVisibility() != View.VISIBLE) {
            panel.setVisibility(View.GONE);
            panel.setTranslationX(0f);
            panel.setAlpha(1f);
            return;
        }

        int widthPx = panel.getLayoutParams().width;
        if (widthPx <= 0) widthPx = EdgeToEdgeHelper.dpToPx(this, 226);
        panel.animate()
                .translationX(isLeft ? -widthPx * 0.85f : widthPx * 0.85f)
                .alpha(0f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    if (!sidebarExpanded) {
                        panel.setVisibility(View.GONE);
                        panel.setTranslationX(0f);
                        panel.setAlpha(1f);
                    }
                })
                .start();
    }

    private void updateDrawerFilterTabs() {
        if (sidebarView == null) return;
        TextView tabAll = sidebarView.findViewById(R.id.tab_sb_all);
        TextView tabApps = sidebarView.findViewById(R.id.tab_sb_apps);
        TextView tabProd = sidebarView.findViewById(R.id.tab_sb_productivity);
        TextView tabFav = sidebarView.findViewById(R.id.tab_sb_favorites);

        int activeColor = themedContext.getColor(R.color.mf_cyan_text);
        int inactiveColor = themedContext.getColor(R.color.mf_text_secondary);

        if (tabAll != null) {
            tabAll.setTextColor(currentDrawerFilter == ToolType.DrawerFilter.ALL ? activeColor : inactiveColor);
        }
        if (tabApps != null) {
            tabApps.setTextColor(currentDrawerFilter == ToolType.DrawerFilter.APPS ? activeColor : inactiveColor);
        }
        if (tabProd != null) {
            tabProd.setTextColor(currentDrawerFilter == ToolType.DrawerFilter.PRODUCTIVITY ? activeColor : inactiveColor);
        }
        if (tabFav != null) {
            tabFav.setTextColor(currentDrawerFilter == ToolType.DrawerFilter.FAVORITES ? activeColor : inactiveColor);
        }
    }

    private void populateSidebarTools() {
        if (sidebarView == null) return;
        LinearLayout container = sidebarView.findViewById(R.id.sidebar_tools_container);
        ScrollView scrollView = sidebarView.findViewById(R.id.scroll_sb_tools);
        View panel = sidebarView.findViewById(R.id.sidebar_panel);
        container.removeAllViews();

        Set<String> favTools = preferences.getFavoriteTools();
        Set<String> favApps = preferences.getFavoriteApps();
        List<String> selectedApps = preferences.getSelectedSidebarApps();
        InstalledAppsRepository appsRepo = MultiFloatApp.get().getInstalledAppsRepository();
        Map<String, InstalledAppInfo> appCache = new HashMap<>();
        for (String pkg : selectedApps) {
            InstalledAppInfo info = appsRepo.loadAppByPackage(this, pkg);
            if (info != null) {
                appCache.put(pkg, info);
            }
        }

        List<SidebarShortcut> shortcuts = new ArrayList<>();
        if (currentDrawerFilter == ToolType.DrawerFilter.ALL) {
            for (String key : preferences.getSidebarCustomOrder()) {
                SidebarShortcut sc = SidebarShortcut.fromKey(key);
                if (sc == null) continue;
                if (sc.isExternalApp) {
                    if (appCache.containsKey(sc.packageName)) {
                        shortcuts.add(sc);
                    }
                } else {
                    shortcuts.add(sc);
                }
            }
        } else if (currentDrawerFilter == ToolType.DrawerFilter.APPS) {
            for (String pkg : selectedApps) {
                if (appCache.containsKey(pkg)) {
                    shortcuts.add(SidebarShortcut.forApp(pkg));
                }
            }
        } else if (currentDrawerFilter == ToolType.DrawerFilter.FAVORITES) {
            for (String pkg : selectedApps) {
                if (favApps.contains(pkg) && appCache.containsKey(pkg)) {
                    shortcuts.add(SidebarShortcut.forApp(pkg));
                }
            }
            for (ToolType t : ToolType.getOrderedDrawerTools(ToolType.DrawerFilter.FAVORITES, favTools)) {
                shortcuts.add(SidebarShortcut.forTool(t));
            }
        } else {
            for (ToolType t : ToolType.getOrderedDrawerTools(currentDrawerFilter, favTools)) {
                shortcuts.add(SidebarShortcut.forTool(t));
            }
        }

        // Apply live search filter
        List<SidebarShortcut> filtered = new ArrayList<>();
        for (SidebarShortcut sc : shortcuts) {
            if (sidebarSearchQuery.isEmpty()) {
                filtered.add(sc);
            } else if (sc.isExternalApp && sc.packageName != null) {
                InstalledAppInfo info = appCache.get(sc.packageName);
                if (info != null && (info.appName.toLowerCase(Locale.US).contains(sidebarSearchQuery)
                        || info.packageName.toLowerCase(Locale.US).contains(sidebarSearchQuery))) {
                    filtered.add(sc);
                }
            } else if (sc.toolType != null) {
                String title = getString(sc.toolType.titleRes).toLowerCase(Locale.US);
                if (title.contains(sidebarSearchQuery)
                        || sc.toolType.category.toLowerCase(Locale.US).contains(sidebarSearchQuery)) {
                    filtered.add(sc);
                }
            }
        }

        if (filtered.isEmpty()) {
            TextView empty = new TextView(themedContext);
            if (currentDrawerFilter == ToolType.DrawerFilter.APPS && selectedApps.isEmpty()) {
                empty.setText("No installed apps pinned yet.\nTap the grid icon below to add apps.");
            } else if (currentDrawerFilter == ToolType.DrawerFilter.FAVORITES) {
                empty.setText("No favorite tools or apps yet.");
            } else {
                empty.setText("No matching tools or apps.");
            }
            empty.setTextColor(themedContext.getColor(R.color.mf_text_secondary));
            empty.setTextSize(11f);
            empty.setGravity(Gravity.CENTER);
            int pad = EdgeToEdgeHelper.dpToPx(this, 16);
            empty.setPadding(pad, pad, pad, pad);
            container.addView(empty);
            return;
        }

        int iconStyle = preferences.getSidebarIconStyle();
        int accent = preferences.getAccentColorInt();

        for (SidebarShortcut sc : filtered) {
            View item = themedInflater.inflate(R.layout.item_sidebar_drawer_tool, container, false);
            FrameLayout iconFrame = item.findViewById(R.id.frame_drawer_icon_bg);
            ImageView icon = item.findViewById(R.id.iv_drawer_tool_icon);
            TextView label = item.findViewById(R.id.tv_drawer_tool_name);
            TextView cat = item.findViewById(R.id.tv_drawer_tool_cat);

            applySidebarIconStyle(iconFrame, iconStyle, accent);

            if (sc.isExternalApp && sc.packageName != null) {
                InstalledAppInfo info = appCache.get(sc.packageName);
                if (info == null) continue;
                icon.setImageTintList(null);
                if (info.icon != null) {
                    icon.setImageDrawable(info.icon);
                } else {
                    icon.setImageResource(R.drawable.ic_apps);
                }
                label.setText(info.appName);
                String prefix = info.isFavorite ? "★ App" : "App";
                cat.setText(prefix + (ExternalAppLauncher.canAttemptFreeform(this) ? " • Freeform" : " • Shortcut"));
                attachDrawerShortcutGesture(item, sc, info, scrollView, panel);
            } else if (sc.toolType != null) {
                ToolType t = sc.toolType;
                icon.setImageResource(t.iconRes);
                icon.setImageTintList(ColorStateList.valueOf(themedContext.getColor(R.color.mf_cyan_text)));
                label.setText(t.titleRes);
                boolean isFav = favTools.contains(t.id);
                cat.setText((isFav ? "★ " : "") + t.category);
                attachDrawerShortcutGesture(item, sc, null, scrollView, panel);
            }
            container.addView(item);
        }
    }

    private void applySidebarIconStyle(@Nullable FrameLayout iconFrame, int style, int accentColor) {
        if (iconFrame == null) return;
        if (style == 1) {
            iconFrame.setBackground(null);
        } else if (style == 2) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(themedContext.getColor(R.color.mf_surface_variant));
            gd.setStroke(EdgeToEdgeHelper.dpToPx(this, 1), accentColor);
            iconFrame.setBackground(gd);
        } else {
            iconFrame.setBackgroundResource(R.drawable.bg_pill_chip);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachDrawerShortcutGesture(
            View item,
            SidebarShortcut shortcut,
            @Nullable InstalledAppInfo appInfo,
            ScrollView scrollView,
            View panel) {
        final int touchSlop = ViewConfiguration.get(themedContext).getScaledTouchSlop();
        final int minDragOutPx = EdgeToEdgeHelper.dpToPx(this, 48);

        item.setOnTouchListener(new View.OnTouchListener() {
            float downRawX, downRawY;
            float lastRawX, lastRawY;
            boolean dragStarted;
            boolean scrollDetected;

            final Runnable longPressDragRunnable = () -> {
                if (!scrollDetected && !dragStarted) {
                    dragStarted = true;
                    if (scrollView != null) scrollView.requestDisallowInterceptTouchEvent(true);
                    item.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    item.setAlpha(0.45f);
                    showDragGhost(shortcut, appInfo, lastRawX, lastRawY, !isPointInsideView(panel, lastRawX, lastRawY));
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        setSidebarInputFocusable(false);
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        lastRawX = downRawX;
                        lastRawY = downRawY;
                        dragStarted = false;
                        scrollDetected = false;
                        v.setPressed(true);
                        v.postDelayed(longPressDragRunnable, 175L);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        lastRawX = event.getRawX();
                        lastRawY = event.getRawY();
                        float dx = lastRawX - downRawX;
                        float dy = lastRawY - downRawY;

                        if (!dragStarted && !scrollDetected) {
                            if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 0.9f) {
                                v.removeCallbacks(longPressDragRunnable);
                                dragStarted = true;
                                if (scrollView != null) scrollView.requestDisallowInterceptTouchEvent(true);
                                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                v.setAlpha(0.45f);
                                showDragGhost(shortcut, appInfo, lastRawX, lastRawY, !isPointInsideView(panel, lastRawX, lastRawY));
                            } else if (Math.abs(dy) > touchSlop && Math.abs(dy) >= Math.abs(dx)) {
                                v.removeCallbacks(longPressDragRunnable);
                                v.setPressed(false);
                                scrollDetected = true;
                                return false;
                            }
                        }

                        if (dragStarted) {
                            boolean outsidePanel = !isPointInsideView(panel, lastRawX, lastRawY);
                            updateDragGhost(lastRawX, lastRawY, outsidePanel);
                            return true;
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.removeCallbacks(longPressDragRunnable);
                        v.setPressed(false);
                        v.setAlpha(1f);
                        if (scrollView != null) scrollView.requestDisallowInterceptTouchEvent(false);

                        if (dragStarted) {
                            float releaseX = event.getRawX();
                            float releaseY = event.getRawY();
                            boolean outsidePanel = !isPointInsideView(panel, releaseX, releaseY);
                            float dist = (float) Math.hypot(releaseX - downRawX, releaseY - downRawY);
                            removeDragGhost();
                            if (event.getActionMasked() == MotionEvent.ACTION_UP && (outsidePanel || dist > minDragOutPx)) {
                                collapseSidebar(true);
                                launchSidebarShortcut(shortcut, appInfo,
                                        new Point(Math.round(releaseX), Math.round(releaseY)));
                            }
                            return true;
                        }

                        if (!scrollDetected && event.getActionMasked() == MotionEvent.ACTION_UP) {
                            float dist = (float) Math.hypot(event.getRawX() - downRawX, event.getRawY() - downRawY);
                            if (dist <= touchSlop) {
                                collapseSidebar(true);
                                launchSidebarShortcut(shortcut, appInfo, null);
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void launchSidebarShortcut(
            SidebarShortcut shortcut,
            @Nullable InstalledAppInfo appInfo,
            @Nullable Point dropCenter) {
        if (shortcut.isExternalApp && shortcut.packageName != null) {
            Rect bounds = ExternalAppLauncher.computeDefaultFreeformBounds(this, dropCenter);
            ExternalAppLauncher.LaunchStatus status = ExternalAppLauncher.launchExternalApp(
                    this, shortcut.packageName, bounds, true);
            if (status == ExternalAppLauncher.LaunchStatus.LAUNCHED_FREEFORM) {
                Toast.makeText(this,
                        "Launched " + (appInfo != null ? appInfo.appName : shortcut.packageName) + " in freeform window",
                        Toast.LENGTH_SHORT).show();
            } else if (status == ExternalAppLauncher.LaunchStatus.LAUNCHED_NORMAL) {
                Toast.makeText(this,
                        "Opened " + (appInfo != null ? appInfo.appName : shortcut.packageName)
                                + " full screen. Your phone does not allow floating windows for other apps (see Multitasking Setup Center).",
                        Toast.LENGTH_LONG).show();
            } else if (status == ExternalAppLauncher.LaunchStatus.APP_UNINSTALLED) {
                String label = appInfo != null ? appInfo.appName : shortcut.packageName;
                Toast.makeText(this, getString(R.string.app_uninstalled_removed, label), Toast.LENGTH_LONG).show();
                populateSidebarTools();
            } else {
                Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show();
            }
        } else if (shortcut.toolType != null) {
            openToolWindow(shortcut.toolType, null, dropCenter, false);
        }
    }

    private boolean isPointInsideView(View view, float rawX, float rawY) {
        if (view == null || view.getVisibility() != View.VISIBLE || !view.isAttachedToWindow()) {
            return false;
        }
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        return rawX >= loc[0] && rawX <= loc[0] + view.getWidth()
                && rawY >= loc[1] && rawY <= loc[1] + view.getHeight();
    }

    private void showDragGhost(SidebarShortcut shortcut, @Nullable InstalledAppInfo appInfo,
                               float rawX, float rawY, boolean outsidePanel) {
        removeDragGhost();
        dragGhostView = themedInflater.inflate(R.layout.overlay_drag_ghost, null);
        ImageView icon = dragGhostView.findViewById(R.id.iv_drag_ghost_icon);
        TextView title = dragGhostView.findViewById(R.id.tv_drag_ghost_title);
        tvDragGhostHint = dragGhostView.findViewById(R.id.tv_drag_ghost_hint);

        if (shortcut.isExternalApp && appInfo != null) {
            icon.setImageTintList(null);
            if (appInfo.icon != null) icon.setImageDrawable(appInfo.icon);
            else icon.setImageResource(R.drawable.ic_apps);
            title.setText(appInfo.appName);
        } else if (shortcut.toolType != null) {
            icon.setImageResource(shortcut.toolType.iconRes);
            icon.setImageTintList(ColorStateList.valueOf(themedContext.getColor(R.color.mf_cyan_text)));
            title.setText(shortcut.toolType.titleRes);
        }
        updateGhostDropState(outsidePanel);

        int halfW = EdgeToEdgeHelper.dpToPx(this, 86);
        int halfH = EdgeToEdgeHelper.dpToPx(this, 30);

        dragGhostParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        dragGhostParams.gravity = Gravity.TOP | Gravity.START;
        dragGhostParams.x = Math.round(rawX) - halfW;
        dragGhostParams.y = Math.round(rawY) - halfH;

        windowManager.addView(dragGhostView, dragGhostParams);
        dragGhostView.setAlpha(0f);
        dragGhostView.setScaleX(0.9f);
        dragGhostView.setScaleY(0.9f);
        dragGhostView.animate().alpha(0.96f).scaleX(1f).scaleY(1f).setDuration(120).start();
    }

    private void updateDragGhost(float rawX, float rawY, boolean outsidePanel) {
        if (dragGhostView == null || dragGhostParams == null || !dragGhostView.isAttachedToWindow()) {
            return;
        }
        int halfW = dragGhostView.getWidth() > 0 ? dragGhostView.getWidth() / 2 : EdgeToEdgeHelper.dpToPx(this, 86);
        int halfH = dragGhostView.getHeight() > 0 ? dragGhostView.getHeight() / 2 : EdgeToEdgeHelper.dpToPx(this, 30);
        dragGhostParams.x = Math.round(rawX) - halfW;
        dragGhostParams.y = Math.round(rawY) - halfH;
        windowManager.updateViewLayout(dragGhostView, dragGhostParams);
        updateGhostDropState(outsidePanel);
    }

    private void updateGhostDropState(boolean outsidePanel) {
        if (dragGhostView == null || tvDragGhostHint == null) return;
        tvDragGhostHint.setText(outsidePanel ? R.string.drawer_drag_hint_release : R.string.drawer_drag_hint_move);
        tvDragGhostHint.setTextColor(themedContext.getColor(outsidePanel ? R.color.mf_success : R.color.mf_cyan_text));
        dragGhostView.setScaleX(outsidePanel ? 1.05f : 0.96f);
        dragGhostView.setScaleY(outsidePanel ? 1.05f : 0.96f);
    }

    private void removeDragGhost() {
        if (dragGhostView != null) {
            try { windowManager.removeView(dragGhostView); } catch (Exception ignored) {}
            dragGhostView = null;
            dragGhostParams = null;
            tvDragGhostHint = null;
        }
    }

    private void toggleFlashlight() {
        CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (cm == null) return;
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics chars = cm.getCameraCharacteristics(id);
                Boolean hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    torchOn = !torchOn;
                    cm.setTorchMode(id, torchOn);
                    Toast.makeText(this, torchOn ? "Flashlight ON" : "Flashlight OFF", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "No camera flash available", Toast.LENGTH_SHORT).show();
        } catch (CameraAccessException | SecurityException e) {
            Toast.makeText(this, "Unable to toggle flashlight", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeSidebar() {
        mainHandler.removeCallbacks(reassertOverlaysRunnable);
        removeDragGhost();
        sidebarExpanded = false;
        if (sidebarView != null) {
            try { windowManager.removeView(sidebarView); } catch (Exception ignored) {}
            sidebarView = null;
        }
    }

    // ==================== FLOATING WINDOW ENGINE ====================

    @Nullable
    private FloatingWindowInstance findExistingWindowByTool(ToolType toolType) {
        for (String id : zOrderManager.getTopToBottomIds()) {
            FloatingWindowInstance inst = windows.get(id);
            if (inst != null && inst.state.toolType == toolType) {
                return inst;
            }
        }
        return null;
    }

    private void openToolWindow(ToolType toolType, @Nullable FloatingWindowState restoredState) {
        openToolWindow(toolType, restoredState, null, false);
    }

    private void openToolWindow(ToolType toolType, @Nullable FloatingWindowState restoredState,
                                @Nullable Point dropCenter, boolean forceNewInstance) {
        openToolWindow(toolType, restoredState, dropCenter, forceNewInstance, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void openToolWindow(ToolType toolType, @Nullable FloatingWindowState restoredState,
                                @Nullable Point dropCenter, boolean forceNewInstance,
                                @Nullable Bundle savedMiniAppState) {
        // Prevent unintended duplicate windows for the same tool by default
        if (restoredState == null && !forceNewInstance && !preferences.isAllowDuplicateWindows()) {
            FloatingWindowInstance existing = findExistingWindowByTool(toolType);
            if (existing != null) {
                if (dropCenter != null && !existing.state.maximized) {
                    Rect safe = WindowBoundsHelper.getSafeScreenBounds(windowManager, this);
                    WindowBoundsHelper.ClampedPoint clamped = WindowBoundsHelper.clampWindowPosition(
                            dropCenter.x - existing.params.width / 2,
                            dropCenter.y - existing.params.height / 2,
                            existing.params.width, existing.params.height, safe);
                    existing.params.x = clamped.x;
                    existing.params.y = clamped.y;
                    existing.state.x = clamped.x;
                    existing.state.y = clamped.y;
                    if (existing.rootView.isAttachedToWindow()) {
                        windowManager.updateViewLayout(existing.rootView, existing.params);
                    }
                }
                restoreOrFocusWindow(existing.state.windowId);
                return;
            }
        }

        MultiFloatApp.get().getToolsRepository().recordToolOpened(toolType);

        Rect safe = WindowBoundsHelper.getSafeScreenBounds(windowManager, this);
        float scale = preferences.getDefaultWindowScalePercent() / 100f;
        int minW = EdgeToEdgeHelper.dpToPx(this, 220);
        int minH = EdgeToEdgeHelper.dpToPx(this, 180);
        int defaultW = Math.max(minW, Math.min(safe.width(), Math.round(EdgeToEdgeHelper.dpToPx(this, toolType.defaultWidthDp) * scale)));
        int defaultH = Math.max(minH, Math.min(safe.height(), Math.round(EdgeToEdgeHelper.dpToPx(this, toolType.defaultHeightDp) * scale)));

        int offset = (windows.size() % 4) * EdgeToEdgeHelper.dpToPx(this, 14);
        int x = safe.left + EdgeToEdgeHelper.dpToPx(this, 10) + offset;
        int y = safe.top + EdgeToEdgeHelper.dpToPx(this, 10) + offset;
        int w = defaultW;
        int h = defaultH;
        float alpha = preferences.getWindowOpacityPercent() / 100f;

        if (restoredState != null) {
            w = Math.max(minW, Math.min(safe.width(), restoredState.widthPx));
            h = Math.max(minH, Math.min(safe.height(), restoredState.heightPx));
            x = restoredState.x;
            y = restoredState.y;
            alpha = restoredState.alpha;
        } else if (dropCenter != null) {
            x = dropCenter.x - w / 2;
            y = dropCenter.y - h / 2;
        } else if (preferences.isRememberWindowPositions()) {
            int[] saved = preferences.getSavedToolGeometry(toolType.id);
            if (saved != null) {
                if (saved[2] >= minW) w = Math.min(safe.width(), saved[2]);
                if (saved[3] >= minH) h = Math.min(safe.height(), saved[3]);
                x = saved[0];
                y = saved[1];
            }
        }

        WindowBoundsHelper.ClampedPoint clamped = WindowBoundsHelper.clampWindowPosition(x, y, w, h, safe);
        x = clamped.x;
        y = clamped.y;

        if (dropCenter != null && restoredState == null) {
            preferences.saveToolPosition(toolType.id, x, y, w, h);
        }

        String winId = restoredState != null && !restoredState.windowId.isEmpty()
                ? restoredState.windowId
                : UUID.randomUUID().toString();
        String title = getString(toolType.titleRes);
        FloatingWindowState state = new FloatingWindowState(winId, toolType, title, x, y, w, h, alpha);
        if (restoredState != null) {
            state.minimized = restoredState.minimized;
            state.maximized = restoredState.maximized;
            state.savedX = restoredState.savedX;
            state.savedY = restoredState.savedY;
            state.savedWidthPx = restoredState.savedWidthPx;
            state.savedHeightPx = restoredState.savedHeightPx;
        }

        View winView = themedInflater.inflate(R.layout.overlay_floating_window, null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                w, h, overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;

        ImageView ivIcon = winView.findViewById(R.id.iv_window_icon);
        TextView tvTitle = winView.findViewById(R.id.tv_window_title);
        ivIcon.setImageResource(toolType.iconRes);
        tvTitle.setText(title);

        MiniApp miniApp = MiniAppRegistry.getInstance().create(toolType, themedContext, new MiniApp.HostCallback() {
            @Override
            public void onInputFocusRequested(boolean focusable) {
                setWindowInputFocusable(winId, focusable);
            }

            @Override
            public void onOpenSettingsRequested() {
                Intent i = new Intent(FloatingWindowService.this, GeneralSettingsActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }

            @Override
            public void onRequestCameraPermission() {
                Intent i = new Intent(FloatingWindowService.this, PermissionSetupActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }

            @Override
            public void onOpenFullScreenTool(@NonNull ToolType type) {
                if (type == ToolType.QR_SCANNER) {
                    Intent i = new Intent(FloatingWindowService.this, QrScannerActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            }
        });

        FrameLayout contentHost = winView.findViewById(R.id.window_content_container);
        MiniAppContainerHost miniAppHost = new MiniAppContainerHost();
        miniAppHost.attach(miniApp, themedInflater, contentHost, savedMiniAppState, state.minimized);

        FloatingWindowInstance instance = new FloatingWindowInstance(state, winView, params, miniAppHost);
        windows.put(winId, instance);
        zOrderManager.registerWindow(winId);
        syncWindowZIndicesAndChrome(winId);
        winView.setAlpha(alpha);

        if (winView instanceof FloatingWindowFrame) {
            ((FloatingWindowFrame) winView).setOnWindowTouchListener(new FloatingWindowFrame.OnWindowTouchListener() {
                @Override
                public void onWindowTouchDown() {
                    if (!zOrderManager.isTopWindow(winId)) {
                        promoteWindowZOrderLogical(winId);
                        pendingBringToFrontWindowId = winId;
                    } else {
                        promoteWindowZOrderLogical(winId);
                    }
                }

                @Override
                public void onWindowTouchFinished() {
                    if (winId.equals(pendingBringToFrontWindowId)) {
                        pendingBringToFrontWindowId = null;
                        commitWindowZOrderToWindowManager(winId);
                    }
                }
            });
        }

        // Title Bar Dragging & Focus with touchSlop + system-inset safe bounds clamping
        final int dragTouchSlop = ViewConfiguration.get(themedContext).getScaledTouchSlop();
        View titleBar = winView.findViewById(R.id.window_title_bar);
        titleBar.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY;
            float downX, downY;
            boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        setWindowInputFocusable(winId, false);
                        initX = params.x;
                        initY = params.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (!dragging && Math.hypot(dx, dy) > dragTouchSlop) {
                            dragging = true;
                        }
                        if (dragging) {
                            Rect curSafe = WindowBoundsHelper.getSafeScreenBounds(windowManager, FloatingWindowService.this);
                            WindowBoundsHelper.ClampedPoint p = WindowBoundsHelper.clampWindowPosition(
                                    initX + Math.round(dx), initY + Math.round(dy),
                                    params.width, params.height, curSafe);
                            params.x = p.x;
                            params.y = p.y;
                            state.x = p.x;
                            state.y = p.y;
                            if (winView.isAttachedToWindow()) {
                                windowManager.updateViewLayout(winView, params);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (dragging) {
                            if (preferences.isEdgeSnapEnabled()) {
                                snapWindowToEdge(instance);
                            }
                            preferences.saveToolPosition(toolType.id, state.x, state.y, state.widthPx, state.heightPx);
                            saveSessionSnapshot();
                        }
                        return true;
                }
                return false;
            }
        });

        // 8-Direction Corner & Edge Visual Resize Handles
        attachAllResizeHandles(instance);

        // Opacity Quick Slider
        SeekBar seekOpacity = winView.findViewById(R.id.seek_win_opacity);
        seekOpacity.setProgress(Math.round((alpha - 0.35f) * 100f));
        seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float a = 0.35f + (progress / 100f);
                    state.alpha = Math.min(1.0f, a);
                    winView.setAlpha(state.alpha);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { saveSessionSnapshot(); }
        });

        winView.findViewById(R.id.btn_win_opacity).setOnClickListener(v ->
                instance.opacityPanel.setVisibility(
                        instance.opacityPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        winView.findViewById(R.id.btn_win_minimize).setOnClickListener(v -> minimizeWindow(winId));
        winView.findViewById(R.id.btn_win_maximize).setOnClickListener(v -> toggleMaximizeWindow(winId));
        winView.findViewById(R.id.btn_win_close).setOnClickListener(v -> closeWindow(winId));

        if (state.minimized) {
            winView.setVisibility(View.GONE);
        }

        windowManager.addView(winView, params);
        if (state.minimized) {
            refreshTaskbarDock(null);
        }
        scheduleReassertSystemOverlays();

        if (!state.minimized && restoredState == null && preferences.isWindowAnimationEnabled()) {
            winView.setScaleX(0.86f);
            winView.setScaleY(0.86f);
            winView.setAlpha(0f);
            winView.animate().scaleX(1f).scaleY(1f).alpha(state.alpha).setDuration(200).start();
        }

        updateBubbleBadge();
        saveSessionSnapshot();
        broadcastWindowStates();
        updateNotification();
    }

    private void snapWindowToEdge(FloatingWindowInstance inst) {
        Rect safe = WindowBoundsHelper.getSafeScreenBounds(windowManager, this);
        int snapThreshold = EdgeToEdgeHelper.dpToPx(this, 22);
        if (Math.abs(inst.params.x - safe.left) < snapThreshold) {
            inst.params.x = safe.left;
        } else if (Math.abs((inst.params.x + inst.params.width) - safe.right) < snapThreshold) {
            inst.params.x = safe.right - inst.params.width;
        }
        inst.state.x = inst.params.x;
        if (inst.rootView.isAttachedToWindow()) {
            windowManager.updateViewLayout(inst.rootView, inst.params);
        }
    }

    private void attachAllResizeHandles(FloatingWindowInstance inst) {
        View root = inst.rootView;
        attachDirectionalResize(inst, root.findViewById(R.id.resize_edge_left),
                root.findViewById(R.id.pill_resize_left), WindowResizeCalculator.Direction.LEFT);
        attachDirectionalResize(inst, root.findViewById(R.id.resize_edge_right),
                root.findViewById(R.id.pill_resize_right), WindowResizeCalculator.Direction.RIGHT);
        attachDirectionalResize(inst, root.findViewById(R.id.resize_edge_top),
                root.findViewById(R.id.pill_resize_top), WindowResizeCalculator.Direction.TOP);
        attachDirectionalResize(inst, root.findViewById(R.id.resize_edge_bottom),
                root.findViewById(R.id.pill_resize_bottom), WindowResizeCalculator.Direction.BOTTOM);

        attachDirectionalResize(inst, root.findViewById(R.id.resize_corner_top_left),
                root.findViewById(R.id.iv_resize_tl), WindowResizeCalculator.Direction.TOP_LEFT);
        attachDirectionalResize(inst, root.findViewById(R.id.resize_corner_top_right),
                root.findViewById(R.id.iv_resize_tr), WindowResizeCalculator.Direction.TOP_RIGHT);
        attachDirectionalResize(inst, root.findViewById(R.id.resize_corner_bottom_left),
                root.findViewById(R.id.iv_resize_bl), WindowResizeCalculator.Direction.BOTTOM_LEFT);
        attachDirectionalResize(inst, root.findViewById(R.id.resize_corner_bottom_right),
                root.findViewById(R.id.iv_resize_handle), WindowResizeCalculator.Direction.BOTTOM_RIGHT);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachDirectionalResize(FloatingWindowInstance inst, @Nullable View touchTarget,
                                         @Nullable View visualGlyph, WindowResizeCalculator.Direction dir) {
        if (touchTarget == null) return;
        final TextView tvHint = inst.rootView.findViewById(R.id.tv_win_hint);

        touchTarget.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY, initW, initH;
            float downX, downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        setWindowInputFocusable(inst.state.windowId, false);
                        initX = inst.params.x;
                        initY = inst.params.y;
                        initW = inst.params.width;
                        initH = inst.params.height;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        if (visualGlyph != null) {
                            visualGlyph.animate().scaleX(1.25f).scaleY(1.25f).alpha(1f).setDuration(100).start();
                        }
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        Rect raw = WindowBoundsHelper.getRawScreenBounds(windowManager);
                        Rect safe = WindowBoundsHelper.getSafeScreenBounds(windowManager, FloatingWindowService.this);
                        int minW = EdgeToEdgeHelper.dpToPx(FloatingWindowService.this, 220);
                        int minH = EdgeToEdgeHelper.dpToPx(FloatingWindowService.this, 180);
                        int maxW = Math.max(minW, safe.width());
                        int maxH = Math.max(minH, safe.height());
                        int sideMargin = Math.max(0, safe.left);
                        int topMargin = Math.max(0, safe.top);
                        int bottomMargin = Math.max(0, raw.bottom - safe.bottom);

                        WindowResizeCalculator.Bounds b = WindowResizeCalculator.computeResizedBounds(
                                dir,
                                initX, initY, initW, initH,
                                Math.round(event.getRawX() - downX),
                                Math.round(event.getRawY() - downY),
                                minW, minH, maxW, maxH,
                                raw.width(), raw.height(),
                                sideMargin, topMargin, bottomMargin
                        );

                        inst.params.x = b.x;
                        inst.params.y = b.y;
                        inst.params.width = b.width;
                        inst.params.height = b.height;
                        inst.state.x = b.x;
                        inst.state.y = b.y;
                        inst.state.widthPx = b.width;
                        inst.state.heightPx = b.height;
                        inst.state.maximized = false;
                        if (inst.rootView.isAttachedToWindow()) {
                            windowManager.updateViewLayout(inst.rootView, inst.params);
                        }

                        if (tvHint != null) {
                            DisplayMetrics dm = screenMetrics();
                            float density = dm.density > 0 ? dm.density : 1f;
                            int wDp = Math.round(b.width / density);
                            int hDp = Math.round(b.height / density);
                            tvHint.setText("Size: " + wDp + " × " + hDp + " dp");
                            tvHint.setTextColor(preferences.getAccentColorInt());
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (visualGlyph != null) {
                            float restAlpha = inst.state.focused ? 0.9f : 0.55f;
                            visualGlyph.animate().scaleX(1f).scaleY(1f).alpha(restAlpha).setDuration(120).start();
                        }
                        if (tvHint != null) {
                            tvHint.setText(R.string.win_hint_move_resize);
                            tvHint.setTextColor(themedContext.getColor(R.color.mf_text_muted));
                        }
                        preferences.saveToolPosition(inst.state.toolType.id,
                                inst.state.x, inst.state.y, inst.state.widthPx, inst.state.heightPx);
                        saveSessionSnapshot();
                        return true;
                }
                return false;
            }
        });
    }

    private void applyWindowChromeStyling(FloatingWindowInstance inst, boolean isFocused) {
        int radiusPx = EdgeToEdgeHelper.dpToPx(this, preferences.getWindowCornerRadiusDp());
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(radiusPx);
        bg.setColor(themedContext.getColor(R.color.mf_window_bg));
        int strokeWidth = EdgeToEdgeHelper.dpToPx(this, isFocused ? 2 : 1);
        int accentColor = preferences.getAccentColorInt();
        int strokeColor = isFocused
                ? accentColor
                : themedContext.getColor(R.color.mf_card_stroke);
        bg.setStroke(strokeWidth, strokeColor);
        inst.cardInner.setBackground(bg);
        inst.cardInner.setClipToOutline(true);
        inst.rootView.setElevation(EdgeToEdgeHelper.dpToPx(this, isFocused ? 18 : 6));
        inst.focusDot.setAlpha(isFocused ? 1.0f : 0.35f);

        int handleColor = isFocused ? accentColor : themedContext.getColor(R.color.mf_resize_rest);
        float handleAlpha = isFocused ? 0.9f : 0.55f;
        ColorStateList tintList = ColorStateList.valueOf(handleColor);
        int[] cornerIds = new int[]{
                R.id.iv_resize_tl, R.id.iv_resize_tr, R.id.iv_resize_bl, R.id.iv_resize_handle
        };
        for (int id : cornerIds) {
            ImageView iv = inst.rootView.findViewById(id);
            if (iv != null) {
                iv.setImageTintList(tintList);
                iv.setAlpha(handleAlpha);
            }
        }
        int[] pillIds = new int[]{
                R.id.pill_resize_left, R.id.pill_resize_right, R.id.pill_resize_top, R.id.pill_resize_bottom
        };
        for (int id : pillIds) {
            View pill = inst.rootView.findViewById(id);
            if (pill != null) {
                pill.setBackgroundTintList(tintList);
                pill.setAlpha(handleAlpha);
            }
        }
    }

    private void setWindowInputFocusable(String windowId, boolean focusable) {
        FloatingWindowInstance inst = windows.get(windowId);
        if (inst == null || !inst.rootView.isAttachedToWindow()) return;
        int prevFlags = inst.params.flags;
        if (focusable) {
            inst.params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            inst.params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        if (prevFlags != inst.params.flags) {
            windowManager.updateViewLayout(inst.rootView, inst.params);
        }
    }

    private void promoteWindowZOrderLogical(String windowId) {
        FloatingWindowInstance target = windows.get(windowId);
        if (target == null) return;
        zOrderManager.bringToFront(windowId);
        syncWindowZIndicesAndChrome(windowId);
        broadcastWindowStates();
    }

    private void syncWindowZIndicesAndChrome(@Nullable String focusedWindowId) {
        for (FloatingWindowInstance inst : windows.values()) {
            int z = zOrderManager.getZIndex(inst.state.windowId);
            inst.state.zIndex = Math.max(0, z);
            boolean isTarget = focusedWindowId != null && focusedWindowId.equals(inst.state.windowId) && !inst.state.minimized;
            inst.state.focused = isTarget;
            inst.miniAppHost.setFocused(isTarget);
            applyWindowChromeStyling(inst, isTarget);
        }
    }

    private void commitWindowZOrderToWindowManager(String windowId) {
        FloatingWindowInstance target = windows.get(windowId);
        if (target == null || !target.rootView.isAttachedToWindow()) return;
        if (windows.size() > 1) {
            View focusedChild = target.rootView.findFocus();
            try {
                windowManager.removeViewImmediate(target.rootView);
                windowManager.addView(target.rootView, target.params);
                if (focusedChild != null) {
                    focusedChild.post(focusedChild::requestFocus);
                }
            } catch (Exception ignored) {}
            scheduleReassertSystemOverlays();
        }
        saveSessionSnapshot();
        broadcastWindowStates();
    }

    private void scheduleReassertSystemOverlays() {
        mainHandler.removeCallbacks(reassertOverlaysRunnable);
        View sidebarPanel = sidebarView != null ? sidebarView.findViewById(R.id.sidebar_panel) : null;
        if (sidebarPanel != null && sidebarPanel.getVisibility() == View.VISIBLE) {
            mainHandler.postDelayed(reassertOverlaysRunnable, 220L);
        } else {
            reassertSystemOverlaysOnTop();
        }
    }

    private void reassertSystemOverlaysOnTop() {
        bringOverlayAboveWindows(taskbarDockView, taskbarDockParams);
        bringOverlayAboveWindows(bubbleView, bubbleParams);
        bringOverlayAboveWindows(sidebarView, sidebarParams);
        bringOverlayAboveWindows(bubbleMenuView, bubbleMenuParams);
        bringOverlayAboveWindows(dragGhostView, dragGhostParams);
    }

    private void bringOverlayAboveWindows(@Nullable View overlay, @Nullable WindowManager.LayoutParams params) {
        if (overlay != null && params != null && overlay.isAttachedToWindow()) {
            try {
                windowManager.removeViewImmediate(overlay);
                windowManager.addView(overlay, params);
            } catch (Exception ignored) {}
        }
    }

    // ==================== BOTTOM TASKBAR DOCK ====================

    @Nullable
    private View refreshTaskbarDock(@Nullable String newlyMinimizedWindowId) {
        List<FloatingWindowInstance> minimized = new ArrayList<>();
        for (String id : zOrderManager.getBottomToTopIds()) {
            FloatingWindowInstance inst = windows.get(id);
            if (inst != null && inst.state.minimized) {
                minimized.add(inst);
            }
        }

        if (minimized.isEmpty()) {
            removeTaskbarDockAnimated();
            return null;
        }

        boolean newlyCreated = false;
        if (taskbarDockView == null) {
            taskbarDockView = themedInflater.inflate(R.layout.overlay_taskbar_dock, null);
            taskbarDockParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    overlayWindowType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            taskbarDockParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            taskbarDockParams.y = EdgeToEdgeHelper.dpToPx(this, 24);
            windowManager.addView(taskbarDockView, taskbarDockParams);
            newlyCreated = true;
        } else {
            taskbarDockView.animate().cancel();
            taskbarDockView.setTranslationY(0f);
            taskbarDockView.setAlpha(1f);
            taskbarDockView.setScaleX(1f);
            taskbarDockView.setScaleY(1f);
        }

        LinearLayout container = taskbarDockView.findViewById(R.id.taskbar_dock_items_container);
        container.removeAllViews();

        View targetAnchor = null;
        for (FloatingWindowInstance inst : minimized) {
            View item = themedInflater.inflate(R.layout.item_taskbar_dock_icon, container, false);
            item.setTag(inst.state.windowId);
            ImageView ivIcon = item.findViewById(R.id.iv_dock_icon);
            TextView tvLabel = item.findViewById(R.id.tv_dock_label);
            View indicator = item.findViewById(R.id.view_dock_indicator);

            ivIcon.setImageResource(inst.state.toolType.iconRes);
            ivIcon.setImageTintList(ColorStateList.valueOf(themedContext.getColor(R.color.mf_cyan_text)));
            tvLabel.setText(inst.state.title);
            indicator.setBackgroundColor(preferences.getAccentColorInt());

            item.setOnClickListener(v -> restoreOrFocusWindow(inst.state.windowId));
            item.setOnLongClickListener(v -> {
                String closedTitle = inst.state.title;
                closeWindow(inst.state.windowId);
                Toast.makeText(this, "Closed " + closedTitle, Toast.LENGTH_SHORT).show();
                return true;
            });

            if (newlyMinimizedWindowId != null && newlyMinimizedWindowId.equals(inst.state.windowId)) {
                targetAnchor = item;
                if (preferences.isWindowAnimationEnabled()) {
                    item.setScaleX(0.35f);
                    item.setScaleY(0.35f);
                    item.setAlpha(0f);
                    item.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setStartDelay(130L)
                            .setDuration(220L)
                            .setInterpolator(new OvershootInterpolator(1.5f))
                            .start();
                }
            }
            container.addView(item);
        }

        if (newlyCreated && preferences.isWindowAnimationEnabled()) {
            taskbarDockView.setTranslationY(EdgeToEdgeHelper.dpToPx(this, 24));
            taskbarDockView.setAlpha(0f);
            taskbarDockView.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(200L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        return targetAnchor;
    }

    @Nullable
    private View findDockIconView(String windowId) {
        if (taskbarDockView == null || windowId == null) return null;
        LinearLayout container = taskbarDockView.findViewById(R.id.taskbar_dock_items_container);
        if (container == null) return null;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (windowId.equals(child.getTag())) {
                return child;
            }
        }
        return null;
    }

    private Point getDockTargetCenter(@Nullable View anchorItem) {
        if (anchorItem != null && anchorItem.isAttachedToWindow() && anchorItem.getWidth() > 0) {
            int[] loc = new int[2];
            anchorItem.getLocationOnScreen(loc);
            return new Point(loc[0] + anchorItem.getWidth() / 2, loc[1] + anchorItem.getHeight() / 2);
        }
        Rect safe = WindowBoundsHelper.getSafeScreenBounds(windowManager, this);
        return new Point(safe.centerX(), safe.bottom - EdgeToEdgeHelper.dpToPx(this, 36));
    }

    private void removeTaskbarDockAnimated() {
        if (taskbarDockView == null) return;
        final View viewToRemove = taskbarDockView;
        if (preferences.isWindowAnimationEnabled() && viewToRemove.isAttachedToWindow()) {
            viewToRemove.animate()
                    .translationY(EdgeToEdgeHelper.dpToPx(this, 24))
                    .alpha(0f)
                    .setDuration(160L)
                    .withEndAction(() -> {
                        if (taskbarDockView == viewToRemove) {
                            removeTaskbarDock();
                        }
                    })
                    .start();
        } else {
            removeTaskbarDock();
        }
    }

    private void removeTaskbarDock() {
        if (taskbarDockView != null) {
            taskbarDockView.animate().cancel();
            try { windowManager.removeView(taskbarDockView); } catch (Exception ignored) {}
            taskbarDockView = null;
            taskbarDockParams = null;
        }
    }

    private void cancelWindowTransition(FloatingWindowInstance inst) {
        if (inst.transitionAnimator != null) {
            inst.transitionAnimator.cancel();
            inst.transitionAnimator = null;
        }
        inst.rootView.animate().cancel();
        inst.rootView.setScaleX(1f);
        inst.rootView.setScaleY(1f);
        inst.rootView.setAlpha(inst.state.alpha);
        inst.params.x = inst.state.x;
        inst.params.y = inst.state.y;
    }

    @Nullable
    private String findTopVisibleWindowId() {
        for (String id : zOrderManager.getTopToBottomIds()) {
            FloatingWindowInstance inst = windows.get(id);
            if (inst != null && !inst.state.minimized) {
                return id;
            }
        }
        return null;
    }

    private void minimizeWindow(String windowId) {
        FloatingWindowInstance inst = windows.get(windowId);
        if (inst == null) return;
        if (windowId.equals(pendingBringToFrontWindowId)) {
            pendingBringToFrontWindowId = null;
        }
        cancelWindowTransition(inst);
        setWindowInputFocusable(windowId, false);
        inst.state.minimized = true;
        inst.state.focused = false;
        inst.miniAppHost.setMinimized(true);

        String nextFocused = findTopVisibleWindowId();
        syncWindowZIndicesAndChrome(nextFocused);
        View dockAnchor = refreshTaskbarDock(windowId);
        saveSessionSnapshot();
        broadcastWindowStates();

        animateWindowMinimizeToDock(inst, dockAnchor);
    }

    private void animateWindowMinimizeToDock(FloatingWindowInstance inst, @Nullable View dockAnchor) {
        if (preferences.isWindowAnimationEnabled()
                && inst.rootView.isAttachedToWindow()
                && inst.rootView.getVisibility() == View.VISIBLE) {
            final int startX = inst.params.x;
            final int startY = inst.params.y;
            final float startAlpha = inst.state.alpha;
            final Point targetCenter = getDockTargetCenter(dockAnchor);
            final int endX = targetCenter.x - inst.params.width / 2;
            final int endY = targetCenter.y - inst.params.height / 2;

            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(260L);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.addUpdateListener(a -> {
                if (!inst.rootView.isAttachedToWindow()) return;
                float t = (float) a.getAnimatedValue();
                inst.params.x = Math.round(startX + (endX - startX) * t);
                inst.params.y = Math.round(startY + (endY - startY) * t);
                try {
                    windowManager.updateViewLayout(inst.rootView, inst.params);
                } catch (Exception ignored) {}
                float scale = 1.0f - (0.82f * t);
                inst.rootView.setScaleX(scale);
                inst.rootView.setScaleY(scale);
                inst.rootView.setAlpha(startAlpha * (1.0f - 0.88f * t));
            });
            anim.addListener(new AnimatorListenerAdapter() {
                boolean canceled = false;

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (inst.transitionAnimator == anim) {
                        inst.transitionAnimator = null;
                    }
                    if (!canceled && inst.state.minimized) {
                        inst.rootView.setVisibility(View.GONE);
                        inst.rootView.setScaleX(1f);
                        inst.rootView.setScaleY(1f);
                        inst.rootView.setAlpha(inst.state.alpha);
                        inst.params.x = inst.state.x;
                        inst.params.y = inst.state.y;
                        if (inst.rootView.isAttachedToWindow()) {
                            try {
                                windowManager.updateViewLayout(inst.rootView, inst.params);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            });
            inst.transitionAnimator = anim;
            anim.start();
        } else {
            inst.rootView.setVisibility(View.GONE);
        }
    }

    private void restoreOrFocusWindow(String windowId) {
        FloatingWindowInstance inst = windows.get(windowId);
        if (inst == null) return;
        boolean wasMinimized = inst.state.minimized;
        Point dockOriginCenter = wasMinimized ? getDockTargetCenter(findDockIconView(windowId)) : null;

        cancelWindowTransition(inst);
        inst.state.minimized = false;
        inst.miniAppHost.setMinimized(false);
        refreshTaskbarDock(null);

        inst.rootView.setVisibility(View.VISIBLE);
        boolean moved = zOrderManager.bringToFront(windowId);
        syncWindowZIndicesAndChrome(windowId);
        if (moved || windows.size() > 1) {
            commitWindowZOrderToWindowManager(windowId);
        }

        if (wasMinimized && preferences.isWindowAnimationEnabled() && inst.rootView.isAttachedToWindow()) {
            final int startX = dockOriginCenter.x - inst.params.width / 2;
            final int startY = dockOriginCenter.y - inst.params.height / 2;
            final int endX = inst.state.x;
            final int endY = inst.state.y;
            final float endAlpha = inst.state.alpha;

            inst.params.x = startX;
            inst.params.y = startY;
            inst.rootView.setScaleX(0.2f);
            inst.rootView.setScaleY(0.2f);
            inst.rootView.setAlpha(0.15f);
            try {
                windowManager.updateViewLayout(inst.rootView, inst.params);
            } catch (Exception ignored) {}

            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(240L);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(a -> {
                if (!inst.rootView.isAttachedToWindow()) return;
                float t = (float) a.getAnimatedValue();
                inst.params.x = Math.round(startX + (endX - startX) * t);
                inst.params.y = Math.round(startY + (endY - startY) * t);
                try {
                    windowManager.updateViewLayout(inst.rootView, inst.params);
                } catch (Exception ignored) {}
                float scale = 0.2f + (0.8f * t);
                inst.rootView.setScaleX(scale);
                inst.rootView.setScaleY(scale);
                inst.rootView.setAlpha(0.15f + (endAlpha - 0.15f) * t);
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (inst.transitionAnimator == anim) {
                        inst.transitionAnimator = null;
                    }
                    inst.params.x = endX;
                    inst.params.y = endY;
                    inst.rootView.setScaleX(1f);
                    inst.rootView.setScaleY(1f);
                    inst.rootView.setAlpha(endAlpha);
                    if (inst.rootView.isAttachedToWindow()) {
                        try {
                            windowManager.updateViewLayout(inst.rootView, inst.params);
                        } catch (Exception ignored) {}
                    }
                }
            });
            inst.transitionAnimator = anim;
            anim.start();
        } else if (preferences.isWindowAnimationEnabled()) {
            inst.rootView.setScaleX(0.92f);
            inst.rootView.setScaleY(0.92f);
            inst.rootView.animate().scaleX(1f).scaleY(1f).alpha(inst.state.alpha).setDuration(180).start();
        }
        saveSessionSnapshot();
        broadcastWindowStates();
    }

    private void toggleMaximizeWindow(String windowId) {
        FloatingWindowInstance inst = windows.get(windowId);
        if (inst == null) return;
        Rect safe = WindowBoundsHelper.getSafeScreenBounds(windowManager, this);
        if (!inst.state.maximized) {
            inst.state.savedX = inst.params.x;
            inst.state.savedY = inst.params.y;
            inst.state.savedWidthPx = inst.params.width;
            inst.state.savedHeightPx = inst.params.height;
            inst.state.maximized = true;
            inst.params.x = safe.left;
            inst.params.y = safe.top;
            inst.params.width = safe.width();
            inst.params.height = safe.height();
        } else {
            inst.state.maximized = false;
            int minW = EdgeToEdgeHelper.dpToPx(this, 220);
            int minH = EdgeToEdgeHelper.dpToPx(this, 180);
            int rw = Math.max(minW, Math.min(safe.width(), inst.state.savedWidthPx));
            int rh = Math.max(minH, Math.min(safe.height(), inst.state.savedHeightPx));
            WindowBoundsHelper.ClampedPoint clamped = WindowBoundsHelper.clampWindowPosition(
                    inst.state.savedX, inst.state.savedY, rw, rh, safe);
            inst.params.x = clamped.x;
            inst.params.y = clamped.y;
            inst.params.width = rw;
            inst.params.height = rh;
        }
        inst.state.x = inst.params.x;
        inst.state.y = inst.params.y;
        inst.state.widthPx = inst.params.width;
        inst.state.heightPx = inst.params.height;
        if (inst.rootView.isAttachedToWindow()) {
            windowManager.updateViewLayout(inst.rootView, inst.params);
        }
        saveSessionSnapshot();
    }

    private void closeWindow(String windowId) {
        FloatingWindowInstance inst = windows.remove(windowId);
        if (inst == null) return;
        if (windowId.equals(pendingBringToFrontWindowId)) {
            pendingBringToFrontWindowId = null;
        }
        cancelWindowTransition(inst);
        zOrderManager.removeWindow(windowId);
        inst.miniAppHost.destroy();
        if (preferences.isWindowAnimationEnabled() && inst.rootView.getVisibility() == View.VISIBLE) {
            inst.rootView.animate().scaleX(0.85f).scaleY(0.85f).alpha(0f).setDuration(150)
                    .withEndAction(() -> {
                        try { windowManager.removeView(inst.rootView); } catch (Exception ignored) {}
                    }).start();
        } else {
            try { windowManager.removeView(inst.rootView); } catch (Exception ignored) {}
        }
        refreshTaskbarDock(null);
        String nextFocused = findTopVisibleWindowId();
        syncWindowZIndicesAndChrome(nextFocused);
        updateBubbleBadge();
        saveSessionSnapshot();
        broadcastWindowStates();
        updateNotification();
    }

    private void minimizeAllWindows() {
        pendingBringToFrontWindowId = null;
        List<FloatingWindowInstance> toAnimate = new ArrayList<>();
        for (FloatingWindowInstance inst : windows.values()) {
            if (!inst.state.minimized) {
                toAnimate.add(inst);
            }
            setWindowInputFocusable(inst.state.windowId, false);
            inst.state.minimized = true;
            inst.state.focused = false;
            inst.miniAppHost.setMinimized(true);
        }
        syncWindowZIndicesAndChrome(null);
        refreshTaskbarDock(null);
        for (FloatingWindowInstance inst : toAnimate) {
            cancelWindowTransition(inst);
            animateWindowMinimizeToDock(inst, findDockIconView(inst.state.windowId));
        }
        saveSessionSnapshot();
        broadcastWindowStates();
    }

    private void closeAllWindows() {
        pendingBringToFrontWindowId = null;
        zOrderManager.clear();
        List<String> ids = new ArrayList<>(windows.keySet());
        for (String id : ids) {
            FloatingWindowInstance inst = windows.remove(id);
            if (inst != null) {
                cancelWindowTransition(inst);
                inst.miniAppHost.destroy();
                try { windowManager.removeView(inst.rootView); } catch (Exception ignored) {}
            }
        }
        removeTaskbarDock();
        updateBubbleBadge();
        saveSessionSnapshot();
        broadcastWindowStates();
        updateNotification();
    }

    private void applyPreferencesRefresh() {
        boolean themeChanged = (appliedThemeMode != preferences.getThemeMode());
        if (themeChanged) {
            rebuildThemedContext();
        }

        removeBubble();
        removeBubbleMenu();
        removeSidebar();
        removeTaskbarDock();

        if (preferences.isBubbleEnabled()) createBubble();
        if (preferences.isSidebarEnabled()) createSidebar();

        if (themeChanged && !windows.isEmpty()) {
            // Preserve Z-order, geometry, and transient MiniApp Bundle state across theme recreation
            List<FloatingWindowState> preserved = new ArrayList<>();
            Map<String, Bundle> preservedStates = new HashMap<>();
            for (String id : zOrderManager.getBottomToTopIds()) {
                FloatingWindowInstance inst = windows.get(id);
                if (inst != null) {
                    preserved.add(inst.state);
                    preservedStates.put(id, inst.miniAppHost.captureState());
                }
            }
            closeAllWindows();
            for (FloatingWindowState st : preserved) {
                openToolWindow(st.toolType, st, null, true, preservedStates.get(st.windowId));
            }
        } else {
            for (FloatingWindowInstance inst : windows.values()) {
                applyWindowChromeStyling(inst, inst.state.focused);
            }
            refreshTaskbarDock(null);
        }
    }

    private void saveSessionSnapshot() {
        try {
            JSONArray arr = new JSONArray();
            for (String id : zOrderManager.getBottomToTopIds()) {
                FloatingWindowInstance inst = windows.get(id);
                if (inst != null) {
                    arr.put(inst.state.toJson());
                }
            }
            preferences.saveSessionJson(arr.toString());
        } catch (Exception ignored) {}
    }

    private void restorePreviousSession() {
        try {
            JSONArray arr = new JSONArray(preferences.getSavedSessionJson());
            List<FloatingWindowState> restored = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                restored.add(FloatingWindowState.fromJson(arr.getJSONObject(i)));
            }
            Collections.sort(restored, Comparator.comparingInt(a -> a.zIndex));
            for (FloatingWindowState st : restored) {
                if (!windows.containsKey(st.windowId)) {
                    openToolWindow(st.toolType, st, null, true);
                }
            }
        } catch (Exception ignored) {}
    }

    private void broadcastWindowStates() {
        List<FloatingWindowState> list = new ArrayList<>();
        for (String id : zOrderManager.getTopToBottomIds()) {
            FloatingWindowInstance inst = windows.get(id);
            if (inst != null) {
                list.add(inst.state);
            }
        }
        FloatingEngineBus.updateWindows(list);
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        closeAllWindows();
        removeBubble();
        removeBubbleMenu();
        removeTaskbarDock();
        removeSidebar();
        FloatingEngineBus.updateRunning(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
