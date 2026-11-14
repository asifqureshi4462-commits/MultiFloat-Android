package com.example.service;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
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
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.data.preferences.AppPreferences;
import com.example.miniapps.BaseMiniAppController;
import com.example.miniapps.MiniAppFactory;
import com.example.model.FloatingWindowState;
import com.example.model.ToolType;
import com.example.ui.MainActivity;
import com.example.ui.PermissionSetupActivity;
import com.example.ui.settings.GeneralSettingsActivity;
import com.example.ui.tools.QrScannerActivity;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private LayoutInflater themedInflater;
    private Context themedContext;
    private AppPreferences preferences;

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

    private View dragGhostView;
    private WindowManager.LayoutParams dragGhostParams;
    private TextView tvDragGhostHint;

    private final Map<String, FloatingWindowInstance> windows = new LinkedHashMap<>();

    private class FloatingWindowInstance {
        final FloatingWindowState state;
        final View rootView;
        final WindowManager.LayoutParams params;
        final BaseMiniAppController controller;
        final LinearLayout cardInner;
        final View focusDot;
        final View opacityPanel;

        FloatingWindowInstance(FloatingWindowState state, View rootView,
                               WindowManager.LayoutParams params, BaseMiniAppController controller) {
            this.state = state;
            this.rootView = rootView;
            this.params = params;
            this.controller = controller;
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
        themedContext = new ContextThemeWrapper(this, R.style.Theme_MultiFloat);
        themedInflater = LayoutInflater.from(themedContext);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!PermissionHelper.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startAsForeground();
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
                removeSidebar();
                FloatingEngineBus.updateRunning(false);
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return START_NOT_STICKY;
            case ACTION_OPEN_TOOL:
                String toolId = intent.getStringExtra(EXTRA_TOOL_ID);
                openToolWindow(ToolType.fromId(toolId), null);
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
                if (bubbleView == null) createBubble();
                else removeBubble();
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
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
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

        DisplayMetrics dm = screenMetrics();
        bubbleParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = preferences.getBubbleX(dm.widthPixels - sizePx - 24);
        bubbleParams.y = preferences.getBubbleY(dm.heightPixels / 3);

        orb.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY;
            float downX, downY;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
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
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true;
                        bubbleParams.x = Math.max(0, Math.min(dm.widthPixels - sizePx, initX + (int) dx));
                        bubbleParams.y = Math.max(0, Math.min(dm.heightPixels - sizePx - 60, initY + (int) dy));
                        windowManager.updateViewLayout(bubbleView, bubbleParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            toggleBubbleMenu();
                        } else {
                            // Snap bubble to nearest horizontal screen edge
                            int targetX = (bubbleParams.x + sizePx / 2 < dm.widthPixels / 2)
                                    ? 16 : dm.widthPixels - sizePx - 16;
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
        }
    }

    private void toggleBubbleMenu() {
        if (bubbleMenuView != null) {
            removeBubbleMenu();
            return;
        }
        bubbleMenuView = themedInflater.inflate(R.layout.overlay_bubble_menu, null);
        DisplayMetrics dm = screenMetrics();
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
        LinearLayout winContainer = bubbleMenuView.findViewById(R.id.container_bm_windows);
        if (!windows.isEmpty()) {
            tvWinHeader.setVisibility(View.VISIBLE);
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
                action.setTextColor(themedContext.getColor(R.color.electric_cyan));
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
            name.setText(type.titleRes);
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = EdgeToEdgeHelper.dpToPx(this, 84);
            gp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            cell.setLayoutParams(gp);
            cell.setOnClickListener(v -> {
                removeBubbleMenu();
                openToolWindow(type, null);
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
        int widthDp = sizeLevel == 0 ? 186 : (sizeLevel == 2 ? 244 : 214);
        ViewGroup.LayoutParams panelLp = panel.getLayoutParams();
        panelLp.width = EdgeToEdgeHelper.dpToPx(this, widthDp);
        panel.setLayoutParams(panelLp);

        if (scrollTools != null) {
            DisplayMetrics dm = screenMetrics();
            ViewGroup.LayoutParams scrollLp = scrollTools.getLayoutParams();
            scrollLp.height = Math.min(EdgeToEdgeHelper.dpToPx(this, 310), Math.round(dm.heightPixels * 0.48f));
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
                switch (event.getAction()) {
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

        sidebarView.findViewById(R.id.tab_sb_all).setOnClickListener(v -> {
            currentDrawerFilter = ToolType.DrawerFilter.ALL;
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
        sidebarView.findViewById(R.id.btn_sb_dashboard).setOnClickListener(v -> {
            collapseSidebar(true);
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });
        sidebarView.findViewById(R.id.btn_sb_settings).setOnClickListener(v -> {
            collapseSidebar(true);
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        windowManager.addView(sidebarView, sidebarParams);
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
        if (widthPx <= 0) widthPx = EdgeToEdgeHelper.dpToPx(this, 214);
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
        if (widthPx <= 0) widthPx = EdgeToEdgeHelper.dpToPx(this, 214);
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
        TextView tabProd = sidebarView.findViewById(R.id.tab_sb_productivity);
        TextView tabFav = sidebarView.findViewById(R.id.tab_sb_favorites);

        int activeColor = themedContext.getColor(R.color.electric_cyan);
        int inactiveColor = themedContext.getColor(R.color.mf_text_secondary);

        if (tabAll != null) {
            tabAll.setTextColor(currentDrawerFilter == ToolType.DrawerFilter.ALL ? activeColor : inactiveColor);
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

        Set<String> favs = preferences.getFavoriteTools();
        List<ToolType> ordered = ToolType.getOrderedDrawerTools(currentDrawerFilter, favs);

        if (ordered.isEmpty()) {
            TextView empty = new TextView(themedContext);
            empty.setText("No favorite tools selected yet.\nStar tools from All Floating Tools.");
            empty.setTextColor(themedContext.getColor(R.color.mf_text_secondary));
            empty.setTextSize(11f);
            empty.setGravity(Gravity.CENTER);
            int pad = EdgeToEdgeHelper.dpToPx(this, 16);
            empty.setPadding(pad, pad, pad, pad);
            container.addView(empty);
            return;
        }

        for (ToolType t : ordered) {
            View item = themedInflater.inflate(R.layout.item_sidebar_drawer_tool, container, false);
            ImageView icon = item.findViewById(R.id.iv_drawer_tool_icon);
            TextView label = item.findViewById(R.id.tv_drawer_tool_name);
            TextView cat = item.findViewById(R.id.tv_drawer_tool_cat);
            icon.setImageResource(t.iconRes);
            label.setText(t.titleRes);
            boolean isFav = favs.contains(t.id);
            cat.setText((isFav ? "★ " : "") + t.category);

            attachDrawerToolGesture(item, t, scrollView, panel);
            container.addView(item);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachDrawerToolGesture(View item, ToolType toolType, ScrollView scrollView, View panel) {
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
                    showDragGhost(toolType, lastRawX, lastRawY, !isPointInsideView(panel, lastRawX, lastRawY));
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
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
                                showDragGhost(toolType, lastRawX, lastRawY, !isPointInsideView(panel, lastRawX, lastRawY));
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
                                openToolWindow(toolType, null, new Point(Math.round(releaseX), Math.round(releaseY)));
                            }
                            return true;
                        }

                        if (!scrollDetected && event.getActionMasked() == MotionEvent.ACTION_UP) {
                            float dist = (float) Math.hypot(event.getRawX() - downRawX, event.getRawY() - downRawY);
                            if (dist <= touchSlop) {
                                collapseSidebar(true);
                                openToolWindow(toolType, null, null);
                            }
                        }
                        return true;
                }
                return false;
            }
        });
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

    private void showDragGhost(ToolType toolType, float rawX, float rawY, boolean outsidePanel) {
        removeDragGhost();
        dragGhostView = themedInflater.inflate(R.layout.overlay_drag_ghost, null);
        ImageView icon = dragGhostView.findViewById(R.id.iv_drag_ghost_icon);
        TextView title = dragGhostView.findViewById(R.id.tv_drag_ghost_title);
        tvDragGhostHint = dragGhostView.findViewById(R.id.tv_drag_ghost_hint);

        icon.setImageResource(toolType.iconRes);
        title.setText(toolType.titleRes);
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
        tvDragGhostHint.setTextColor(themedContext.getColor(outsidePanel ? R.color.mf_success : R.color.electric_cyan));
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
        removeDragGhost();
        sidebarExpanded = false;
        if (sidebarView != null) {
            try { windowManager.removeView(sidebarView); } catch (Exception ignored) {}
            sidebarView = null;
        }
    }

    // ==================== FLOATING WINDOW ENGINE ====================

    private void openToolWindow(ToolType toolType, @Nullable FloatingWindowState restoredState) {
        openToolWindow(toolType, restoredState, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void openToolWindow(ToolType toolType, @Nullable FloatingWindowState restoredState,
                                @Nullable Point dropCenter) {
        MultiFloatApp.get().getToolsRepository().recordToolOpened(toolType);

        DisplayMetrics dm = screenMetrics();
        float scale = preferences.getDefaultWindowScalePercent() / 100f;
        int defaultW = Math.min(dm.widthPixels - 40, Math.round(EdgeToEdgeHelper.dpToPx(this, toolType.defaultWidthDp) * scale));
        int defaultH = Math.min(dm.heightPixels - 160, Math.round(EdgeToEdgeHelper.dpToPx(this, toolType.defaultHeightDp) * scale));

        int offset = (windows.size() % 4) * 32;
        int x = 36 + offset;
        int y = 140 + offset;
        int w = defaultW;
        int h = defaultH;
        float alpha = preferences.getWindowOpacityPercent() / 100f;

        if (restoredState != null) {
            x = restoredState.x;
            y = restoredState.y;
            w = restoredState.widthPx;
            h = restoredState.heightPx;
            alpha = restoredState.alpha;
        } else if (dropCenter != null) {
            x = Math.max(16, Math.min(dm.widthPixels - w - 16, dropCenter.x - w / 2));
            y = Math.max(60, Math.min(dm.heightPixels - h - 80, dropCenter.y - h / 2));
            preferences.saveToolPosition(toolType.id, x, y, w, h);
        } else if (preferences.isRememberWindowPositions()) {
            int[] saved = preferences.getSavedToolGeometry(toolType.id);
            if (saved != null) {
                x = Math.max(0, Math.min(dm.widthPixels - 200, saved[0]));
                y = Math.max(0, Math.min(dm.heightPixels - 200, saved[1]));
                if (saved[2] > 200) w = Math.min(dm.widthPixels - 20, saved[2]);
                if (saved[3] > 200) h = Math.min(dm.heightPixels - 100, saved[3]);
            }
        }

        String winId = restoredState != null && !restoredState.windowId.isEmpty()
                ? restoredState.windowId
                : UUID.randomUUID().toString();
        String title = getString(toolType.titleRes);
        FloatingWindowState state = new FloatingWindowState(winId, toolType, title, x, y, w, h, alpha);
        if (restoredState != null) {
            state.minimized = restoredState.minimized;
            state.maximized = restoredState.maximized;
        }

        View winView = themedInflater.inflate(R.layout.overlay_floating_window, null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                w, h, overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;

        ImageView ivIcon = winView.findViewById(R.id.iv_window_icon);
        TextView tvTitle = winView.findViewById(R.id.tv_window_title);
        ivIcon.setImageResource(toolType.iconRes);
        tvTitle.setText(title);

        BaseMiniAppController controller = MiniAppFactory.create(toolType, themedContext, new BaseMiniAppController.HostCallback() {
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
            public void onOpenFullScreenTool(ToolType type) {
                if (type == ToolType.QR_SCANNER) {
                    Intent i = new Intent(FloatingWindowService.this, QrScannerActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            }
        });

        FrameLayout contentHost = winView.findViewById(R.id.window_content_container);
        contentHost.addView(controller.createView(themedInflater, contentHost));

        FloatingWindowInstance instance = new FloatingWindowInstance(state, winView, params, controller);
        windows.put(winId, instance);

        applyWindowChromeStyling(instance, true);
        winView.setAlpha(alpha);

        // Title Bar Dragging & Focus
        View titleBar = winView.findViewById(R.id.window_title_bar);
        titleBar.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY;
            float downX, downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        focusWindow(winId);
                        setWindowInputFocusable(winId, false);
                        initX = params.x;
                        initY = params.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int nx = initX + (int) (event.getRawX() - downX);
                        int ny = initY + (int) (event.getRawY() - downY);
                        DisplayMetrics currentDm = screenMetrics();
                        params.x = Math.max(-params.width / 2, Math.min(currentDm.widthPixels - 120, nx));
                        params.y = Math.max(40, Math.min(currentDm.heightPixels - 140, ny));
                        state.x = params.x;
                        state.y = params.y;
                        windowManager.updateViewLayout(winView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (preferences.isEdgeSnapEnabled()) {
                            snapWindowToEdge(instance);
                        }
                        preferences.saveToolPosition(toolType.id, state.x, state.y, state.widthPx, state.heightPx);
                        saveSessionSnapshot();
                        return true;
                }
                return false;
            }
        });

        // Bottom-Right Resize Handle
        View resizeHandle = winView.findViewById(R.id.iv_resize_handle);
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            int initW, initH;
            float downX, downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        focusWindow(winId);
                        initW = params.width;
                        initH = params.height;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        DisplayMetrics currentDm = screenMetrics();
                        int minW = EdgeToEdgeHelper.dpToPx(FloatingWindowService.this, 230);
                        int minH = EdgeToEdgeHelper.dpToPx(FloatingWindowService.this, 190);
                        int nw = Math.max(minW, Math.min(currentDm.widthPixels - 24, initW + (int) (event.getRawX() - downX)));
                        int nh = Math.max(minH, Math.min(currentDm.heightPixels - 120, initH + (int) (event.getRawY() - downY)));
                        params.width = nw;
                        params.height = nh;
                        state.widthPx = nw;
                        state.heightPx = nh;
                        state.maximized = false;
                        windowManager.updateViewLayout(winView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        preferences.saveToolPosition(toolType.id, state.x, state.y, state.widthPx, state.heightPx);
                        saveSessionSnapshot();
                        return true;
                }
                return false;
            }
        });

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
        focusWindow(winId);

        if (!state.minimized && preferences.isWindowAnimationEnabled()) {
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
        DisplayMetrics dm = screenMetrics();
        int snapThreshold = EdgeToEdgeHelper.dpToPx(this, 24);
        if (inst.params.x < snapThreshold && inst.params.x > -snapThreshold) {
            inst.params.x = 12;
        } else if (Math.abs((inst.params.x + inst.params.width) - dm.widthPixels) < snapThreshold) {
            inst.params.x = dm.widthPixels - inst.params.width - 12;
        }
        inst.state.x = inst.params.x;
        if (inst.rootView.isAttachedToWindow()) {
            windowManager.updateViewLayout(inst.rootView, inst.params);
        }
    }

    private void applyWindowChromeStyling(FloatingWindowInstance inst, boolean isFocused) {
        int radiusPx = EdgeToEdgeHelper.dpToPx(this, preferences.getWindowCornerRadiusDp());
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(radiusPx);
        bg.setColor(themedContext.getColor(R.color.mf_window_bg));
        int strokeWidth = EdgeToEdgeHelper.dpToPx(this, isFocused ? 2 : 1);
        int strokeColor = isFocused
                ? preferences.getAccentColorInt()
                : themedContext.getColor(R.color.mf_card_stroke);
        bg.setStroke(strokeWidth, strokeColor);
        inst.cardInner.setBackground(bg);
        inst.cardInner.setClipToOutline(true);
        inst.focusDot.setAlpha(isFocused ? 1.0f : 0.25f);
    }

    private void setWindowInputFocusable(String windowId, boolean focusable) {
        FloatingWindowInstance inst = windows.get(windowId);
        if (inst == null || !inst.rootView.isAttachedToWindow()) return;
        if (focusable) {
            inst.params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            inst.params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        windowManager.updateViewLayout(inst.rootView, inst.params);
    }

    private void focusWindow(String windowId) {
        FloatingWindowInstance target = windows.get(windowId);
        if (target == null) return;
        for (FloatingWindowInstance inst : windows.values()) {
            boolean isTarget = inst == target;
            inst.state.focused = isTarget;
            applyWindowChromeStyling(inst, isTarget);
        }
        // Bring target to top of WindowManager z-order
        if (target.rootView.isAttachedToWindow() && windows.size() > 1) {
            try {
                windowManager.removeViewImmediate(target.rootView);
                windowManager.addView(target.rootView, target.params);
            } catch (Exception ignored) {}
        }
        broadcastWindowStates();
    }

    private void minimizeWindow(String windowId) {
        FloatingWindowInstance inst = windows.get(windowId);
        if (inst == null) return;
        setWindowInputFocusable(windowId, false);
        inst.state.minimized = true;
        inst.state.focused = false;
        inst.rootView.setVisibility(View.GONE);
        saveSessionSnapshot();
        broadcastWindowStates();
    }

    private void restoreOrFocusWindow(String windowId) {
        FloatingWindowInstance inst = windows.get(windowId);
        if (inst == null) return;
        inst.state.minimized = false;
        inst.rootView.setVisibility(View.VISIBLE);
        focusWindow(windowId);
        if (preferences.isWindowAnimationEnabled()) {
            inst.rootView.setScaleX(0.9f);
            inst.rootView.setScaleY(0.9f);
            inst.rootView.animate().scaleX(1f).scaleY(1f).alpha(inst.state.alpha).setDuration(180).start();
        }
        saveSessionSnapshot();
        broadcastWindowStates();
    }

    private void toggleMaximizeWindow(String windowId) {
        FloatingWindowInstance inst = windows.get(windowId);
        if (inst == null) return;
        DisplayMetrics dm = screenMetrics();
        if (!inst.state.maximized) {
            inst.state.savedX = inst.params.x;
            inst.state.savedY = inst.params.y;
            inst.state.savedWidthPx = inst.params.width;
            inst.state.savedHeightPx = inst.params.height;
            inst.state.maximized = true;
            inst.params.x = 16;
            inst.params.y = 80;
            inst.params.width = dm.widthPixels - 32;
            inst.params.height = dm.heightPixels - 180;
        } else {
            inst.state.maximized = false;
            inst.params.x = inst.state.savedX;
            inst.params.y = inst.state.savedY;
            inst.params.width = inst.state.savedWidthPx;
            inst.params.height = inst.state.savedHeightPx;
        }
        inst.state.x = inst.params.x;
        inst.state.y = inst.params.y;
        inst.state.widthPx = inst.params.width;
        inst.state.heightPx = inst.params.height;
        windowManager.updateViewLayout(inst.rootView, inst.params);
        saveSessionSnapshot();
    }

    private void closeWindow(String windowId) {
        FloatingWindowInstance inst = windows.remove(windowId);
        if (inst == null) return;
        inst.controller.onDetached();
        if (preferences.isWindowAnimationEnabled() && inst.rootView.getVisibility() == View.VISIBLE) {
            inst.rootView.animate().scaleX(0.85f).scaleY(0.85f).alpha(0f).setDuration(150)
                    .withEndAction(() -> {
                        try { windowManager.removeView(inst.rootView); } catch (Exception ignored) {}
                    }).start();
        } else {
            try { windowManager.removeView(inst.rootView); } catch (Exception ignored) {}
        }
        updateBubbleBadge();
        saveSessionSnapshot();
        broadcastWindowStates();
        updateNotification();
    }

    private void minimizeAllWindows() {
        for (FloatingWindowInstance inst : windows.values()) {
            inst.state.minimized = true;
            inst.state.focused = false;
            inst.rootView.setVisibility(View.GONE);
        }
        saveSessionSnapshot();
        broadcastWindowStates();
    }

    private void closeAllWindows() {
        List<String> ids = new ArrayList<>(windows.keySet());
        for (String id : ids) {
            FloatingWindowInstance inst = windows.remove(id);
            if (inst != null) {
                inst.controller.onDetached();
                try { windowManager.removeView(inst.rootView); } catch (Exception ignored) {}
            }
        }
        updateBubbleBadge();
        saveSessionSnapshot();
        broadcastWindowStates();
        updateNotification();
    }

    private void applyPreferencesRefresh() {
        removeBubble();
        removeSidebar();
        if (preferences.isBubbleEnabled()) createBubble();
        if (preferences.isSidebarEnabled()) createSidebar();
        for (FloatingWindowInstance inst : windows.values()) {
            applyWindowChromeStyling(inst, inst.state.focused);
        }
    }

    private void saveSessionSnapshot() {
        try {
            JSONArray arr = new JSONArray();
            for (FloatingWindowInstance inst : windows.values()) {
                arr.put(inst.state.toJson());
            }
            preferences.saveSessionJson(arr.toString());
        } catch (Exception ignored) {}
    }

    private void restorePreviousSession() {
        try {
            JSONArray arr = new JSONArray(preferences.getSavedSessionJson());
            for (int i = 0; i < arr.length(); i++) {
                FloatingWindowState st = FloatingWindowState.fromJson(arr.getJSONObject(i));
                if (!windows.containsKey(st.windowId)) {
                    openToolWindow(st.toolType, st);
                }
            }
        } catch (Exception ignored) {}
    }

    private void broadcastWindowStates() {
        List<FloatingWindowState> list = new ArrayList<>();
        for (FloatingWindowInstance inst : windows.values()) {
            list.add(inst.state);
        }
        FloatingEngineBus.updateWindows(list);
    }

    @Override
    public void onDestroy() {
        closeAllWindows();
        removeBubble();
        removeBubbleMenu();
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
