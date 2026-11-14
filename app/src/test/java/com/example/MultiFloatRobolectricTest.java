package com.example;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import com.example.data.local.MultiFloatDatabase;
import com.example.data.local.entity.ClipboardEntity;
import com.example.data.local.entity.NoteEntity;
import com.example.data.preferences.AppPreferences;
import com.example.data.preferences.SecureKeyStore;
import com.example.model.FloatingWindowState;
import com.example.model.ToolType;
import com.example.ui.FloatingToolsMenuActivity;
import com.example.ui.MainActivity;
import com.example.ui.settings.AppearanceSettingsActivity;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
        JSONObject json = orig.toJson();
        FloatingWindowState restored = FloatingWindowState.fromJson(json);

        assertEquals("win-1", restored.windowId);
        assertEquals(ToolType.NOTES, restored.toolType);
        assertEquals(50, restored.x);
        assertEquals(120, restored.y);
        assertEquals(600, restored.widthPx);
        assertEquals(800, restored.heightPx);
        assertTrue(restored.minimized);
    }

    @Test
    public void preferencesAndSecureKeyStorePersistValues() {
        AppPreferences prefs = new AppPreferences(context);
        prefs.setThemeMode(AppPreferences.THEME_LIGHT);
        prefs.setWindowOpacityPercent(85);
        prefs.setSidebarPosition(AppPreferences.SIDEBAR_LEFT);

        assertEquals(AppPreferences.THEME_LIGHT, prefs.getThemeMode());
        assertEquals(85, prefs.getWindowOpacityPercent());
        assertEquals(AppPreferences.SIDEBAR_LEFT, prefs.getSidebarPosition());

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
    public void coreActivitiesLaunchWithoutCrash() {
        MainActivity main = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        assertNotNull(main.findViewById(R.id.btn_launch_engine));

        FloatingToolsMenuActivity menu = Robolectric.buildActivity(FloatingToolsMenuActivity.class).create().start().resume().get();
        assertNotNull(menu.findViewById(R.id.rv_all_tools));

        AppearanceSettingsActivity appearance = Robolectric.buildActivity(AppearanceSettingsActivity.class).create().start().resume().get();
        assertNotNull(appearance.findViewById(R.id.rg_theme_mode));
    }

    @Test
    public void slideOutDrawerLayouts_inflateWithAllControls() {
        android.view.ContextThemeWrapper themed = new android.view.ContextThemeWrapper(context, R.style.Theme_MultiFloat);
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(themed);

        android.view.View sidebar = inflater.inflate(R.layout.overlay_sidebar, null);
        assertNotNull(sidebar.findViewById(R.id.sidebar_handle));
        assertNotNull(sidebar.findViewById(R.id.sidebar_panel));
        assertNotNull(sidebar.findViewById(R.id.tab_sb_all));
        assertNotNull(sidebar.findViewById(R.id.tab_sb_productivity));
        assertNotNull(sidebar.findViewById(R.id.tab_sb_favorites));
        assertNotNull(sidebar.findViewById(R.id.sidebar_tools_container));
        assertNotNull(sidebar.findViewById(R.id.btn_sb_dashboard));

        android.view.View drawerItem = inflater.inflate(R.layout.item_sidebar_drawer_tool, null);
        assertNotNull(drawerItem.findViewById(R.id.iv_drawer_tool_icon));
        assertNotNull(drawerItem.findViewById(R.id.tv_drawer_tool_name));
        assertNotNull(drawerItem.findViewById(R.id.tv_drawer_tool_cat));
        assertNotNull(drawerItem.findViewById(R.id.iv_drawer_drag_grip));

        android.view.View ghost = inflater.inflate(R.layout.overlay_drag_ghost, null);
        assertNotNull(ghost.findViewById(R.id.iv_drag_ghost_icon));
        assertNotNull(ghost.findViewById(R.id.tv_drag_ghost_title));
        assertNotNull(ghost.findViewById(R.id.tv_drag_ghost_hint));

        MainActivity main = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        assertNotNull(main.findViewById(R.id.btn_dash_open_drawer));
    }
}
