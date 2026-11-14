package com.example;

import static org.junit.Assert.*;

import com.example.data.repository.InstalledAppsRepository;
import com.example.model.InstalledAppInfo;
import com.example.model.SidebarShortcut;
import com.example.model.ToolType;
import com.example.service.WindowBoundsHelper;
import com.example.service.WindowResizeCalculator;
import com.example.service.WindowZOrderManager;
import com.example.util.MathEvaluator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class MultiFloatUnitTest {

    @Test
    public void calculator_basicAndScientificOperations_evaluateCorrectly() {
        assertEquals("14", MathEvaluator.evaluateFormatted("2 + 3 × 4"));
        assertEquals("5", MathEvaluator.evaluateFormatted("sqrt(25)"));
        assertEquals("8", MathEvaluator.evaluateFormatted("2^3"));
        assertEquals("1", MathEvaluator.evaluateFormatted("sin(90)"));
        assertEquals("2", MathEvaluator.evaluateFormatted("log(100)"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void calculator_divideByZero_throws() {
        MathEvaluator.evaluateFormatted("10 ÷ 0");
    }

    @Test
    public void drawerTools_filterAndOrderWithFavoritesFirst() {
        Set<String> favs = new HashSet<>();
        favs.add("todo");
        favs.add("ai_assistant");

        List<ToolType> all = ToolType.getOrderedDrawerTools(ToolType.DrawerFilter.ALL, favs);
        assertEquals(ToolType.values().length, all.size());
        assertTrue(favs.contains(all.get(0).id));
        assertTrue(favs.contains(all.get(1).id));

        List<ToolType> prod = ToolType.getOrderedDrawerTools(ToolType.DrawerFilter.PRODUCTIVITY, favs);
        assertEquals(4, prod.size());
        assertEquals(ToolType.TODO, prod.get(0));
        for (ToolType t : prod) {
            assertTrue(t.isProductivity());
        }

        List<ToolType> onlyFavs = ToolType.getOrderedDrawerTools(ToolType.DrawerFilter.FAVORITES, favs);
        assertEquals(2, onlyFavs.size());

        List<ToolType> appsOnlyFilter = ToolType.getOrderedDrawerTools(ToolType.DrawerFilter.APPS, favs);
        assertTrue(appsOnlyFilter.isEmpty());
    }

    @Test
    public void windowZOrderManager_bringsBackgroundWindowToFrontAndMaintainsStack() {
        WindowZOrderManager zm = new WindowZOrderManager();
        assertEquals(0, zm.registerWindow("win-a"));
        assertEquals(1, zm.registerWindow("win-b"));
        assertEquals(2, zm.registerWindow("win-c"));

        assertTrue(zm.isTopWindow("win-c"));
        assertFalse(zm.isTopWindow("win-a"));

        assertTrue(zm.bringToFront("win-a"));
        assertTrue(zm.isTopWindow("win-a"));
        assertEquals(0, zm.getZIndex("win-b"));
        assertEquals(1, zm.getZIndex("win-c"));
        assertEquals(2, zm.getZIndex("win-a"));
        assertEquals(Arrays.asList("win-a", "win-c", "win-b"), zm.getTopToBottomIds());

        assertFalse(zm.bringToFront("win-a"));

        assertEquals("win-c", zm.removeWindow("win-a"));
        assertTrue(zm.isTopWindow("win-c"));
    }

    @Test
    public void windowResizeCalculator_handlesEdgesCornersAndClamping() {
        int screenW = 1080, screenH = 2400;
        int minW = 400, minH = 350, maxW = 1000, maxH = 2000;

        WindowResizeCalculator.Bounds tl = WindowResizeCalculator.computeResizedBounds(
                WindowResizeCalculator.Direction.TOP_LEFT,
                200, 300, 600, 700,
                100, 80,
                minW, minH, maxW, maxH,
                screenW, screenH, 10, 40, 80
        );
        assertEquals(300, tl.x);
        assertEquals(380, tl.y);
        assertEquals(500, tl.width);
        assertEquals(620, tl.height);

        WindowResizeCalculator.Bounds leftMin = WindowResizeCalculator.computeResizedBounds(
                WindowResizeCalculator.Direction.LEFT,
                200, 300, 600, 700,
                500, 0,
                minW, minH, maxW, maxH,
                screenW, screenH, 10, 40, 80
        );
        assertEquals(400, leftMin.width);
        assertEquals(400, leftMin.x);

        WindowResizeCalculator.Bounds br = WindowResizeCalculator.computeResizedBounds(
                WindowResizeCalculator.Direction.BOTTOM_RIGHT,
                200, 300, 600, 700,
                900, 2000,
                minW, minH, maxW, maxH,
                screenW, screenH, 10, 40, 80
        );
        assertEquals(200, br.x);
        assertEquals(300, br.y);
        assertEquals(1080 - 200 - 10, br.width);
        assertEquals(2000, br.height);
    }

    @Test
    public void windowBoundsHelper_clampsWindowInsideSafeBounds() {
        int safeLeft = 18, safeTop = 90, safeRight = 1062, safeBottom = 2280;
        int winW = 700, winH = 850;

        // Way off-screen negative
        WindowBoundsHelper.ClampedPoint neg = WindowBoundsHelper.clampWindowPosition(
                -500, -200, winW, winH, safeLeft, safeTop, safeRight, safeBottom);
        assertEquals(safeLeft, neg.x);
        assertEquals(safeTop, neg.y);

        // Way off-screen bottom-right
        WindowBoundsHelper.ClampedPoint far = WindowBoundsHelper.clampWindowPosition(
                2000, 3000, winW, winH, safeLeft, safeTop, safeRight, safeBottom);
        assertEquals(safeRight - winW, far.x);
        assertEquals(safeBottom - winH, far.y);

        // Valid middle position remains unchanged
        WindowBoundsHelper.ClampedPoint mid = WindowBoundsHelper.clampWindowPosition(
                180, 400, winW, winH, safeLeft, safeTop, safeRight, safeBottom);
        assertEquals(180, mid.x);
        assertEquals(400, mid.y);
    }

    @Test
    public void sidebarShortcut_roundTripsToolAndAppKeys() {
        String toolKey = SidebarShortcut.toolKey(ToolType.CALCULATOR);
        assertTrue(SidebarShortcut.isToolKey(toolKey));
        SidebarShortcut parsedTool = SidebarShortcut.fromKey(toolKey);
        assertNotNull(parsedTool);
        assertFalse(parsedTool.isExternalApp);
        assertEquals(ToolType.CALCULATOR, parsedTool.toolType);

        String appKey = SidebarShortcut.appKey("com.example.notesapp");
        assertTrue(SidebarShortcut.isAppKey(appKey));
        SidebarShortcut parsedApp = SidebarShortcut.fromKey(appKey);
        assertNotNull(parsedApp);
        assertTrue(parsedApp.isExternalApp);
        assertEquals("com.example.notesapp", parsedApp.packageName);

        assertNull(SidebarShortcut.fromKey("invalid:key"));
        assertNull(SidebarShortcut.fromKey(null));
    }

    @Test
    public void installedApps_filterAndSort_supportsSearchAlphaRecentFavoritesSidebarAndEmptyState() {
        List<InstalledAppInfo> apps = Arrays.asList(
                new InstalledAppInfo("com.zeta.music", "Main", "Zeta Player", null, 1000L, false, true),
                new InstalledAppInfo("com.alpha.mail", "Main", "Alpha Mail", null, 5000L, true, false),
                new InstalledAppInfo("com.beta.browser", "Main", "Beta Surf", null, 0L, true, true)
        );

        // ALL sorts A-Z
        List<InstalledAppInfo> all = InstalledAppsRepository.filterAndSort(apps, "", InstalledAppsRepository.FilterMode.ALL);
        assertEquals(3, all.size());
        assertEquals("Alpha Mail", all.get(0).appName);
        assertEquals("Beta Surf", all.get(1).appName);
        assertEquals("Zeta Player", all.get(2).appName);

        // Search by package name
        List<InstalledAppInfo> byPkg = InstalledAppsRepository.filterAndSort(apps, "com.zeta", InstalledAppsRepository.FilterMode.ALL);
        assertEquals(1, byPkg.size());
        assertEquals("Zeta Player", byPkg.get(0).appName);

        // RECENT excludes zero timestamp & sorts newest first
        List<InstalledAppInfo> recent = InstalledAppsRepository.filterAndSort(apps, "", InstalledAppsRepository.FilterMode.RECENT);
        assertEquals(2, recent.size());
        assertEquals("Alpha Mail", recent.get(0).appName);
        assertEquals("Zeta Player", recent.get(1).appName);

        // FAVORITES
        List<InstalledAppInfo> favs = InstalledAppsRepository.filterAndSort(apps, "", InstalledAppsRepository.FilterMode.FAVORITES);
        assertEquals(2, favs.size());

        // SIDEBAR
        List<InstalledAppInfo> sidebar = InstalledAppsRepository.filterAndSort(apps, "", InstalledAppsRepository.FilterMode.SIDEBAR);
        assertEquals(2, sidebar.size());
        assertEquals("Beta Surf", sidebar.get(0).appName);
        assertEquals("Zeta Player", sidebar.get(1).appName);

        // Empty query result
        List<InstalledAppInfo> empty = InstalledAppsRepository.filterAndSort(apps, "nonexistent_app_xyz", InstalledAppsRepository.FilterMode.ALL);
        assertTrue(empty.isEmpty());
    }
}
