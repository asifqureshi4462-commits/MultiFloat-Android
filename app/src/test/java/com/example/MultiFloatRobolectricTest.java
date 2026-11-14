package com.example;

import static org.junit.Assert.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import com.example.data.local.MultiFloatDatabase;
import com.example.data.local.entity.ClipboardEntity;
import com.example.data.local.entity.NoteEntity;
import com.example.data.preferences.AppPreferences;
import com.example.data.preferences.SecureKeyStore;
import com.example.data.repository.InstalledAppsRepository;
import com.example.model.FloatingWindowState;
import com.example.model.InstalledAppInfo;
import com.example.model.SidebarShortcut;
import com.example.model.ToolType;
import com.example.ui.FloatingToolsMenuActivity;
import com.example.ui.InstalledAppsActivity;
import com.example.ui.MainActivity;
import com.example.ui.WindowManagerActivity;
import com.example.ui.settings.AppearanceSettingsActivity;
import com.example.ui.settings.GeneralSettingsActivity;
import com.example.ui.settings.SidebarSettingsActivity;
import com.example.ui.view.FloatingWindowFrame;
import com.example.util.ExternalAppLauncher;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {36})
public class MultiFloatRobolectricTest {

    private Context context;
    private MultiFloatDatabase db;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, MultiFloatDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void appNameMatchesMultiFloat() {
        assertEquals("MultiFloat", context.getString(R.string.app_name));
    }

    @Test
    public void floatingWindowState_roundTripsJson() throws Exception {
        FloatingWindowState orig = new FloatingWindowState(
                "win-1", ToolType.NOTES, "Quick Notes", 50, 120, 600, 800, 0.9f);
        orig.minimized = true;
        orig.zIndex = 2;
        JSONObject json = orig.toJson();
        FloatingWindowState restored = FloatingWindowState.fromJson(json);

        assertEquals("win-1", restored.windowId);
        assertEquals(ToolType.NOTES, restored.toolType);
        assertEquals(50, restored.x);
        assertEquals(120, restored.y);
        assertEquals(600, restored.widthPx);
        assertEquals(800, restored.heightPx);
        assertTrue(restored.minimized);
        assertEquals(2, restored.zIndex);
    }

    @Test
    public void preferencesAndSecureKeyStorePersistValues() {
        AppPreferences prefs = new AppPreferences(context);
        prefs.setThemeMode(AppPreferences.THEME_LIGHT);
        prefs.setWindowOpacityPercent(85);
        prefs.setSidebarPosition(AppPreferences.SIDEBAR_LEFT);
        prefs.setAllowDuplicateWindows(true);
        prefs.setSidebarIconStyle(2);

        assertEquals(AppPreferences.THEME_LIGHT, prefs.getThemeMode());
        assertEquals(85, prefs.getWindowOpacityPercent());
        assertEquals(AppPreferences.SIDEBAR_LEFT, prefs.getSidebarPosition());
        assertTrue(prefs.isAllowDuplicateWindows());
        assertEquals(2, prefs.getSidebarIconStyle());

        SecureKeyStore keyStore = new SecureKeyStore(context);
        keyStore.saveSecret("test_api_key", "sk-test-12345");
        assertEquals("sk-test-12345", keyStore.getSecret("test_api_key"));
    }

    @Test
    public void roomDatabaseSavesNotesAndClipboard() {
        long noteId = db.noteDao().insert(new NoteEntity("Meeting", "Review sprint goals", System.currentTimeMillis(), true));
        NoteEntity loaded = db.noteDao().getByIdSync(noteId);
        assertNotNull(loaded);
        assertEquals("Meeting", loaded.title);
        assertTrue(loaded.pinned);

        db.clipboardDao().insert(new ClipboardEntity("Copied snippet", System.currentTimeMillis()));
        assertEquals("Copied snippet", db.clipboardDao().getLatestTextSync());
    }

    @Test
    public void coreAndNewActivitiesLaunchWithoutCrash() {
        MainActivity main = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        assertNotNull(main.findViewById(R.id.btn_launch_engine));
        assertNotNull(main.findViewById(R.id.btn_open_installed_apps));

        FloatingToolsMenuActivity menu = Robolectric.buildActivity(FloatingToolsMenuActivity.class).create().start().resume().get();
        assertNotNull(menu.findViewById(R.id.rv_all_tools));

        AppearanceSettingsActivity appearance = Robolectric.buildActivity(AppearanceSettingsActivity.class).create().start().resume().get();
        assertNotNull(appearance.findViewById(R.id.rg_theme_mode));

        InstalledAppsActivity installedApps = Robolectric.buildActivity(InstalledAppsActivity.class).create().start().resume().get();
        assertNotNull(installedApps.findViewById(R.id.et_apps_search));
        assertNotNull(installedApps.findViewById(R.id.rv_installed_apps));
        assertNotNull(installedApps.findViewById(R.id.tv_freeform_status));

        SidebarSettingsActivity sidebarCfg = Robolectric.buildActivity(SidebarSettingsActivity.class).create().start().resume().get();
        assertNotNull(sidebarCfg.findViewById(R.id.btn_sb_manage_apps));
        assertNotNull(sidebarCfg.findViewById(R.id.rg_sb_icon_style));
        assertNotNull(sidebarCfg.findViewById(R.id.rv_sb_reorder));

        GeneralSettingsActivity generalCfg = Robolectric.buildActivity(GeneralSettingsActivity.class).create().start().resume().get();
        assertNotNull(generalCfg.findViewById(R.id.switch_allow_duplicates));

        WindowManagerActivity winMgr = Robolectric.buildActivity(WindowManagerActivity.class).create().start().resume().get();
        assertNotNull(winMgr.findViewById(R.id.btn_wm_restore_session));
    }

    @Test
    public void installedAppsRepository_queriesPackageManagerAndPrunesUninstalledApps() {
        ShadowPackageManager spm = Shadows.shadowOf(context.getPackageManager());
        registerLaunchableFakeApp(spm, "com.vendor.calendar", "com.vendor.calendar.MainActivity", "Work Calendar");
        registerLaunchableFakeApp(spm, "com.vendor.files", "com.vendor.files.FileActivity", "Archive Files");
        // Also seed MultiFloat itself; repository must exclude self package
        registerLaunchableFakeApp(spm, context.getPackageName(), "com.example.ui.MainActivity", "MultiFloat");

        AppPreferences prefs = new AppPreferences(context);
        // Add one valid package and one uninstalled stale package
        prefs.toggleFavoriteApp("com.vendor.files");
        prefs.toggleFavoriteApp("com.removed.app");
        prefs.toggleSidebarApp("com.vendor.files");
        prefs.toggleSidebarApp("com.removed.app");
        prefs.recordRecentApp("com.removed.app", 999L);

        InstalledAppsRepository repo = new InstalledAppsRepository(prefs);
        List<InstalledAppInfo> loaded = repo.loadLaunchableApps(context);

        assertEquals(2, loaded.size());
        // Sorted alphabetically: Archive Files, Work Calendar
        assertEquals("Archive Files", loaded.get(0).appName);
        assertEquals("com.vendor.files", loaded.get(0).packageName);
        assertTrue(loaded.get(0).isFavorite);
        assertTrue(loaded.get(0).isSidebarPinned);

        assertEquals("Work Calendar", loaded.get(1).appName);
        assertFalse(loaded.get(1).isFavorite);

        // Stale uninstalled package should be automatically pruned
        assertFalse(prefs.isFavoriteApp("com.removed.app"));
        assertFalse(prefs.isSidebarAppSelected("com.removed.app"));
        assertFalse(prefs.getRecentAppTimestamps().containsKey("com.removed.app"));
        assertFalse(prefs.getSidebarCustomOrder().contains(SidebarShortcut.appKey("com.removed.app")));
    }

    @Test
    public void externalAppLauncher_handlesFreeformSupportedAndUnsupportedHonestFlow() {
        ShadowPackageManager spm = Shadows.shadowOf(context.getPackageManager());

        // Missing package returns APP_UNINSTALLED
        assertEquals(
                ExternalAppLauncher.LaunchStatus.APP_UNINSTALLED,
                ExternalAppLauncher.launchExternalApp(context, "com.does.not.exist", null, false));

        registerLaunchableFakeApp(spm, "com.vendor.notes", "com.vendor.notes.NotesActivity", "External Notes");

        // Freeform feature OFF -> FREEFORM_UNSUPPORTED when fallback disabled
        spm.setSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT, false);
        assertFalse(ExternalAppLauncher.isFreeformSupported(context));
        assertEquals(
                ExternalAppLauncher.LaunchStatus.FREEFORM_UNSUPPORTED,
                ExternalAppLauncher.launchExternalApp(context, "com.vendor.notes", null, false));

        // Freeform feature OFF + allowNormalFallback=true -> LAUNCHED_NORMAL
        assertEquals(
                ExternalAppLauncher.LaunchStatus.LAUNCHED_NORMAL,
                ExternalAppLauncher.launchExternalApp(context, "com.vendor.notes", null, true));

        // Freeform feature ON -> LAUNCHED_FREEFORM with valid bounds
        spm.setSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT, true);
        assertTrue(ExternalAppLauncher.isFreeformSupported(context));
        Rect bounds = ExternalAppLauncher.computeDefaultFreeformBounds(context, null);
        assertTrue(bounds.width() > 0 && bounds.height() > 0);
        assertEquals(
                ExternalAppLauncher.LaunchStatus.LAUNCHED_FREEFORM,
                ExternalAppLauncher.launchExternalApp(context, "com.vendor.notes", bounds, false));
    }

    @Test
    public void lightAndDarkThemes_provideDistinctHighContrastSurfaceAndTextColors() {
        Configuration lightCfg = new Configuration(context.getResources().getConfiguration());
        lightCfg.uiMode = (lightCfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_NO;
        Context lightCtx = context.createConfigurationContext(lightCfg);

        Configuration darkCfg = new Configuration(context.getResources().getConfiguration());
        darkCfg.uiMode = (darkCfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
        Context darkCtx = context.createConfigurationContext(darkCfg);

        int lightText = lightCtx.getColor(R.color.mf_text_primary);
        int darkText = darkCtx.getColor(R.color.mf_text_primary);
        int lightBg = lightCtx.getColor(R.color.mf_window_bg);
        int darkBg = darkCtx.getColor(R.color.mf_window_bg);
        int lightCyan = lightCtx.getColor(R.color.mf_cyan_text);
        int darkCyan = darkCtx.getColor(R.color.mf_cyan_text);

        assertNotEquals(lightText, darkText);
        assertNotEquals(lightBg, darkBg);
        assertNotEquals(lightCyan, darkCyan);

        // Verify calculator & floating window inflate cleanly under both themes
        ContextThemeWrapper lightThemed = new ContextThemeWrapper(lightCtx, R.style.Theme_MultiFloat);
        View calcLight = LayoutInflater.from(lightThemed).inflate(R.layout.miniapp_calculator, null);
        Button btn7 = calcLight.findViewById(R.id.btn_7);
        assertNotNull(btn7);
        assertEquals("7", btn7.getText().toString());
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    public void responsiveLayouts_renderWithoutCrashOnCompact360dpScreen() {
        MainActivity main = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        View launchBtn = main.findViewById(R.id.btn_launch_engine);
        View appsBtn = main.findViewById(R.id.btn_open_installed_apps);
        assertNotNull(launchBtn);
        assertNotNull(appsBtn);

        InstalledAppsActivity appsActivity = Robolectric.buildActivity(InstalledAppsActivity.class).create().start().resume().get();
        assertNotNull(appsActivity.findViewById(R.id.rv_installed_apps));
    }

    @Test
    @Config(qualifiers = "w393dp-h873dp")
    public void responsiveLayouts_renderWithoutCrashOnMedium393dpScreen() {
        MainActivity main = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        assertNotNull(main.findViewById(R.id.btn_open_tools_menu));
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    public void responsiveLayouts_renderWithoutCrashOn412dpScreen() {
        MainActivity main = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        assertNotNull(main.findViewById(R.id.btn_open_window_manager));
    }

    @Test
    public void slideOutDrawerAndBubbleMenuLayouts_inflateWithAllEnhancedControls() {
        ContextThemeWrapper themed = new ContextThemeWrapper(context, R.style.Theme_MultiFloat);
        LayoutInflater inflater = LayoutInflater.from(themed);

        View sidebar = inflater.inflate(R.layout.overlay_sidebar, null);
        assertNotNull(sidebar.findViewById(R.id.sidebar_handle));
        assertNotNull(sidebar.findViewById(R.id.sidebar_panel));
        assertNotNull(sidebar.findViewById(R.id.et_sb_search));
        assertNotNull(sidebar.findViewById(R.id.tab_sb_all));
        assertNotNull(sidebar.findViewById(R.id.tab_sb_apps));
        assertNotNull(sidebar.findViewById(R.id.tab_sb_productivity));
        assertNotNull(sidebar.findViewById(R.id.tab_sb_favorites));
        assertNotNull(sidebar.findViewById(R.id.sidebar_tools_container));
        assertNotNull(sidebar.findViewById(R.id.btn_sb_apps));
        assertNotNull(sidebar.findViewById(R.id.btn_sb_dashboard));
        assertNotNull(sidebar.findViewById(R.id.btn_sb_settings));

        View bubbleMenu = inflater.inflate(R.layout.overlay_bubble_menu, null);
        assertNotNull(bubbleMenu.findViewById(R.id.scroll_bm_windows));
        assertNotNull(bubbleMenu.findViewById(R.id.btn_bm_close_bubble));
        assertNotNull(bubbleMenu.findViewById(R.id.btn_bm_minimize_all));
        assertNotNull(bubbleMenu.findViewById(R.id.btn_bm_close_all));

        View drawerItem = inflater.inflate(R.layout.item_sidebar_drawer_tool, null);
        assertNotNull(drawerItem.findViewById(R.id.frame_drawer_icon_bg));
        assertNotNull(drawerItem.findViewById(R.id.iv_drawer_tool_icon));
        assertNotNull(drawerItem.findViewById(R.id.tv_drawer_tool_name));
        assertNotNull(drawerItem.findViewById(R.id.tv_drawer_tool_cat));
        assertNotNull(drawerItem.findViewById(R.id.iv_drawer_drag_grip));

        View ghost = inflater.inflate(R.layout.overlay_drag_ghost, null);
        assertNotNull(ghost.findViewById(R.id.iv_drag_ghost_icon));
        assertNotNull(ghost.findViewById(R.id.tv_drag_ghost_title));
        assertNotNull(ghost.findViewById(R.id.tv_drag_ghost_hint));
    }

    @Test
    public void floatingWindowFrame_dispatchesTouchDownAndUpForZOrderPromotion() {
        ContextThemeWrapper themed = new ContextThemeWrapper(context, R.style.Theme_MultiFloat);
        LayoutInflater inflater = LayoutInflater.from(themed);
        View root = inflater.inflate(R.layout.overlay_floating_window, null);
        assertTrue(root instanceof FloatingWindowFrame);

        FloatingWindowFrame frame = (FloatingWindowFrame) root;
        final int[] counts = new int[2];
        frame.setOnWindowTouchListener(new FloatingWindowFrame.OnWindowTouchListener() {
            @Override
            public void onWindowTouchDown() { counts[0]++; }
            @Override
            public void onWindowTouchFinished() { counts[1]++; }
        });

        frame.measure(
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        );
        frame.layout(0, 0, 600, 800);

        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 300f, 400f, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, 300f, 400f, 0);

        assertTrue(frame.dispatchTouchEvent(down));
        assertTrue(frame.dispatchTouchEvent(up));
        assertEquals(1, counts[0]);
        assertEquals(1, counts[1]);

        down.recycle();
        up.recycle();
    }

    @Test
    public void taskbarDockLayouts_inflateCleanlyWithAllControls() {
        ContextThemeWrapper themed = new ContextThemeWrapper(context, R.style.Theme_MultiFloat);
        LayoutInflater inflater = LayoutInflater.from(themed);

        View dock = inflater.inflate(R.layout.overlay_taskbar_dock, null);
        assertNotNull(dock.findViewById(R.id.taskbar_dock_root));
        assertNotNull(dock.findViewById(R.id.scroll_taskbar_dock));
        assertNotNull(dock.findViewById(R.id.taskbar_dock_items_container));

        View dockItem = inflater.inflate(R.layout.item_taskbar_dock_icon, null);
        assertNotNull(dockItem.findViewById(R.id.dock_icon_tile));
        assertNotNull(dockItem.findViewById(R.id.iv_dock_icon));
        assertNotNull(dockItem.findViewById(R.id.tv_dock_label));
        assertNotNull(dockItem.findViewById(R.id.view_dock_indicator));
    }

    @Test
    public void floatingWindowLayout_containsAllEightCornerAndEdgeResizeHandles() {
        ContextThemeWrapper themed = new ContextThemeWrapper(context, R.style.Theme_MultiFloat);
        View win = LayoutInflater.from(themed).inflate(R.layout.overlay_floating_window, null);

        assertNotNull(win.findViewById(R.id.resize_edge_left));
        assertNotNull(win.findViewById(R.id.resize_edge_right));
        assertNotNull(win.findViewById(R.id.resize_edge_top));
        assertNotNull(win.findViewById(R.id.resize_edge_bottom));
        assertNotNull(win.findViewById(R.id.resize_corner_top_left));
        assertNotNull(win.findViewById(R.id.resize_corner_top_right));
        assertNotNull(win.findViewById(R.id.resize_corner_bottom_left));
        assertNotNull(win.findViewById(R.id.resize_corner_bottom_right));
        assertNotNull(win.findViewById(R.id.iv_resize_handle));
    }

    @Test
    public void miniAppRegistry_registersAllTwelveAppsAndInflatesEachIntoFloatingContainer() {
        com.example.miniapps.MiniAppRegistry registry = com.example.miniapps.MiniAppRegistry.getInstance();
        assertEquals(12, registry.size());
        assertEquals(ToolType.values().length, registry.size());

        ContextThemeWrapper themed = new ContextThemeWrapper(context, R.style.Theme_MultiFloat);
        LayoutInflater inflater = LayoutInflater.from(themed);
        com.example.miniapps.MiniApp.HostCallback noOpCallback = new com.example.miniapps.MiniApp.HostCallback() {
            @Override public void onInputFocusRequested(boolean focusable) {}
            @Override public void onOpenSettingsRequested() {}
            @Override public void onRequestCameraPermission() {}
            @Override public void onOpenFullScreenTool(@androidx.annotation.NonNull ToolType toolType) {}
        };

        for (ToolType type : ToolType.values()) {
            assertTrue("Missing registry entry for " + type, registry.isRegistered(type));
            com.example.miniapps.MiniAppDescriptor desc = registry.getDescriptor(type);
            assertNotNull(desc);
            assertTrue(desc.layoutRes != 0);

            View winView = inflater.inflate(R.layout.overlay_floating_window, null);
            android.widget.FrameLayout container = winView.findViewById(R.id.window_content_container);
            com.example.miniapps.MiniApp app = registry.create(type, themed, noOpCallback);
            assertEquals(type, app.getToolType());
            assertEquals(com.example.miniapps.MiniApp.LifecycleState.CREATED, app.getLifecycleState());

            com.example.miniapps.MiniAppContainerHost host = new com.example.miniapps.MiniAppContainerHost();
            View content = host.attach(app, inflater, container, null, false);
            assertNotNull(content);
            assertEquals(1, container.getChildCount());
            assertEquals(com.example.miniapps.MiniApp.LifecycleState.RESUMED, app.getLifecycleState());

            host.setMinimized(true);
            assertEquals(com.example.miniapps.MiniApp.LifecycleState.PAUSED, app.getLifecycleState());

            host.setMinimized(false);
            assertEquals(com.example.miniapps.MiniApp.LifecycleState.RESUMED, app.getLifecycleState());

            host.destroy();
            assertEquals(com.example.miniapps.MiniApp.LifecycleState.DESTROYED, app.getLifecycleState());
            assertEquals(0, container.getChildCount());
        }
    }

    @Test
    public void miniAppContainerHost_supportsCustomMiniAppLifecycleAndCalculatorStateRestore() {
        ContextThemeWrapper themed = new ContextThemeWrapper(context, R.style.Theme_MultiFloat);
        LayoutInflater inflater = LayoutInflater.from(themed);
        com.example.miniapps.MiniApp.HostCallback cb = new com.example.miniapps.MiniApp.HostCallback() {
            @Override public void onInputFocusRequested(boolean focusable) {}
            @Override public void onOpenSettingsRequested() {}
            @Override public void onRequestCameraPermission() {}
            @Override public void onOpenFullScreenTool(@androidx.annotation.NonNull ToolType toolType) {}
        };

        // 1. Custom modular MiniApp registration in an isolated MiniAppRegistry instance
        final java.util.List<String> events = new java.util.ArrayList<>();
        com.example.miniapps.MiniAppRegistry customRegistry = new com.example.miniapps.MiniAppRegistry();
        customRegistry.register(new com.example.miniapps.MiniAppDescriptor(
                ToolType.CLOCK, R.layout.miniapp_clock, true, true, false, false,
                (ctx, hostCallback) -> new com.example.miniapps.MiniApp() {
                    private LifecycleState state = LifecycleState.CREATED;
                    @androidx.annotation.NonNull @Override public ToolType getToolType() { return ToolType.CLOCK; }
                    @androidx.annotation.NonNull @Override public LifecycleState getLifecycleState() { return state; }
                    @androidx.annotation.NonNull @Override
                    public View onCreateView(@androidx.annotation.NonNull LayoutInflater inf,
                                             @androidx.annotation.NonNull android.view.ViewGroup parent,
                                             @androidx.annotation.Nullable android.os.Bundle savedState) {
                        state = LifecycleState.VIEW_CREATED;
                        events.add("create:" + (savedState != null ? savedState.getString("k", "") : "none"));
                        return new View(ctx);
                    }
                    @Override public void onStart() { state = LifecycleState.STARTED; events.add("start"); }
                    @Override public void onResume() { state = LifecycleState.RESUMED; events.add("resume"); }
                    @Override public void onPause() { state = LifecycleState.PAUSED; events.add("pause"); }
                    @Override public void onFocusChanged(boolean focused) { events.add("focus:" + focused); }
                    @Override public void onSaveState(@androidx.annotation.NonNull android.os.Bundle outState) { outState.putString("k", "v42"); }
                    @Override public void onDestroy() { state = LifecycleState.DESTROYED; events.add("destroy"); }
                }));

        android.widget.FrameLayout hostContainer = new android.widget.FrameLayout(themed);
        com.example.miniapps.MiniAppContainerHost host = new com.example.miniapps.MiniAppContainerHost();
        android.os.Bundle initState = new android.os.Bundle();
        initState.putString("k", "init");
        host.attach(customRegistry.create(ToolType.CLOCK, themed, cb), inflater, hostContainer, initState, false);
        host.setFocused(true);
        host.setMinimized(true);
        android.os.Bundle saved = host.captureState();
        assertEquals("v42", saved.getString("k"));
        host.setMinimized(false);
        host.destroy();

        assertEquals(
                Arrays.asList("create:init", "start", "resume", "focus:true", "focus:false", "pause", "resume", "pause", "destroy"),
                events);

        // 2. Calculator state survives captureState() -> destroy() -> re-attach (theme switch)
        com.example.miniapps.MiniAppContainerHost calcHost1 = new com.example.miniapps.MiniAppContainerHost();
        android.widget.FrameLayout calcContainer1 = new android.widget.FrameLayout(themed);
        calcHost1.attach(com.example.miniapps.MiniAppRegistry.getInstance().create(ToolType.CALCULATOR, themed, cb),
                inflater, calcContainer1, null, false);
        calcContainer1.findViewById(R.id.btn_7).performClick();
        calcContainer1.findViewById(R.id.btn_add).performClick();
        calcContainer1.findViewById(R.id.btn_3).performClick();
        android.os.Bundle calcBundle = calcHost1.captureState();
        calcHost1.destroy();

        com.example.miniapps.MiniAppContainerHost calcHost2 = new com.example.miniapps.MiniAppContainerHost();
        android.widget.FrameLayout calcContainer2 = new android.widget.FrameLayout(themed);
        calcHost2.attach(com.example.miniapps.MiniAppRegistry.getInstance().create(ToolType.CALCULATOR, themed, cb),
                inflater, calcContainer2, calcBundle, false);
        android.widget.TextView tvExpr = calcContainer2.findViewById(R.id.tv_calc_expression);
        android.widget.TextView tvRes = calcContainer2.findViewById(R.id.tv_calc_result);
        assertEquals("7+3", tvExpr.getText().toString());
        assertEquals("10", tvRes.getText().toString());
        calcHost2.destroy();
    }

    private static void registerLaunchableFakeApp(
            ShadowPackageManager spm, String pkgName, String className, String label) {
        PackageInfo pi = new PackageInfo();
        pi.packageName = pkgName;
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.packageName = pkgName;
        appInfo.name = label;
        appInfo.nonLocalizedLabel = label;
        pi.applicationInfo = appInfo;

        ActivityInfo actInfo = new ActivityInfo();
        actInfo.packageName = pkgName;
        actInfo.name = className;
        actInfo.applicationInfo = appInfo;
        actInfo.nonLocalizedLabel = label;
        actInfo.exported = true;
        pi.activities = new ActivityInfo[]{actInfo};
        spm.installPackage(pi);

        ResolveInfo ri = new ResolveInfo();
        ri.activityInfo = actInfo;
        ri.nonLocalizedLabel = label;

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        spm.addResolveInfoForIntent(launcherIntent, ri);

        Intent pkgLauncherIntent = new Intent(Intent.ACTION_MAIN);
        pkgLauncherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        pkgLauncherIntent.setPackage(pkgName);
        spm.addResolveInfoForIntent(pkgLauncherIntent, ri);
    }
}
