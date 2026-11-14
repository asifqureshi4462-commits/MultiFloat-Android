package com.example.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.example.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum ToolType {
    CALCULATOR("calculator", R.string.tool_calculator, R.drawable.ic_calculator, "Productivity", 310, 440),
    NOTES("notes", R.string.tool_notes, R.drawable.ic_notes, "Productivity", 320, 400),
    BROWSER("browser", R.string.tool_browser, R.drawable.ic_browser, "Utilities", 340, 460),
    STOPWATCH_TIMER("stopwatch", R.string.tool_stopwatch, R.drawable.ic_stopwatch, "Time", 300, 360),
    CLOCK("clock", R.string.tool_clock, R.drawable.ic_clock, "Time", 280, 220),
    TEXT_EDITOR("text_editor", R.string.tool_text_editor, R.drawable.ic_text_editor, "Productivity", 330, 420),
    CLIPBOARD("clipboard", R.string.tool_clipboard, R.drawable.ic_clipboard, "Utilities", 310, 380),
    TODO("todo", R.string.tool_todo, R.drawable.ic_todo, "Productivity", 310, 400),
    QR_SCANNER("qr_scanner", R.string.tool_qr, R.drawable.ic_qr_scanner, "Utilities", 320, 420),
    TRANSLATOR("translator", R.string.tool_translator, R.drawable.ic_translator, "Intelligence", 330, 420),
    AI_ASSISTANT("ai_assistant", R.string.tool_ai, R.drawable.ic_ai_spark, "Intelligence", 340, 460),
    MUSIC_CONTROLS("music_controls", R.string.tool_music, R.drawable.ic_music, "Media", 300, 260);

    public enum DrawerFilter {
        ALL,
        APPS,
        PRODUCTIVITY,
        FAVORITES
    }

    public final String id;
    @StringRes public final int titleRes;
    @DrawableRes public final int iconRes;
    public final String category;
    public final int defaultWidthDp;
    public final int defaultHeightDp;

    ToolType(String id, @StringRes int titleRes, @DrawableRes int iconRes,
             String category, int defaultWidthDp, int defaultHeightDp) {
        this.id = id;
        this.titleRes = titleRes;
        this.iconRes = iconRes;
        this.category = category;
        this.defaultWidthDp = defaultWidthDp;
        this.defaultHeightDp = defaultHeightDp;
    }

    public boolean isProductivity() {
        return "Productivity".equalsIgnoreCase(category);
    }

    public static List<ToolType> getOrderedDrawerTools(DrawerFilter filter, Set<String> favorites) {
        List<ToolType> result = new ArrayList<>();
        if (filter == DrawerFilter.APPS) {
            return result;
        }
        boolean hasFavs = favorites != null && !favorites.isEmpty();

        // Favorites first when matching filter
        if (hasFavs) {
            for (ToolType type : values()) {
                if (favorites.contains(type.id) && matchesFilter(type, filter, favorites)) {
                    result.add(type);
                }
            }
        }
        // Remaining tools matching filter
        for (ToolType type : values()) {
            if (!result.contains(type) && matchesFilter(type, filter, favorites)) {
                result.add(type);
            }
        }
        return result;
    }

    private static boolean matchesFilter(ToolType type, DrawerFilter filter, Set<String> favorites) {
        if (filter == DrawerFilter.APPS) {
            return false;
        }
        if (filter == DrawerFilter.PRODUCTIVITY) {
            return type.isProductivity();
        }
        if (filter == DrawerFilter.FAVORITES) {
            return favorites != null && favorites.contains(type.id);
        }
        return true;
    }

    public static ToolType fromId(String id) {
        if (id == null) return CALCULATOR;
        for (ToolType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return CALCULATOR;
    }
}
