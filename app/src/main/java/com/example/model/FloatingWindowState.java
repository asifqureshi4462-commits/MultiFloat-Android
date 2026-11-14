package com.example.model;

import org.json.JSONException;
import org.json.JSONObject;

public class FloatingWindowState {
    public String windowId;
    public ToolType toolType;
    public String title;
    public int x;
    public int y;
    public int widthPx;
    public int heightPx;
    public float alpha;
    public boolean minimized;
    public boolean maximized;
    public boolean focused;
    public int savedX;
    public int savedY;
    public int savedWidthPx;
    public int savedHeightPx;

    public FloatingWindowState(String windowId, ToolType toolType, String title,
                               int x, int y, int widthPx, int heightPx, float alpha) {
        this.windowId = windowId;
        this.toolType = toolType;
        this.title = title;
        this.x = x;
        this.y = y;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.alpha = alpha;
        this.minimized = false;
        this.maximized = false;
        this.focused = true;
        this.savedX = x;
        this.savedY = y;
        this.savedWidthPx = widthPx;
        this.savedHeightPx = heightPx;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("windowId", windowId);
        obj.put("toolId", toolType.id);
        obj.put("title", title);
        obj.put("x", x);
        obj.put("y", y);
        obj.put("widthPx", widthPx);
        obj.put("heightPx", heightPx);
        obj.put("alpha", alpha);
        obj.put("minimized", minimized);
        obj.put("maximized", maximized);
        return obj;
    }

    public static FloatingWindowState fromJson(JSONObject obj) {
        String id = obj.optString("windowId", "");
        ToolType type = ToolType.fromId(obj.optString("toolId", ToolType.CALCULATOR.id));
        String title = obj.optString("title", "Window");
        int x = obj.optInt("x", 60);
        int y = obj.optInt("y", 180);
        int w = obj.optInt("widthPx", 800);
        int h = obj.optInt("heightPx", 1000);
        float alpha = (float) obj.optDouble("alpha", 0.95);
        FloatingWindowState state = new FloatingWindowState(id, type, title, x, y, w, h, alpha);
        state.minimized = obj.optBoolean("minimized", false);
        state.maximized = obj.optBoolean("maximized", false);
        return state;
    }
}
