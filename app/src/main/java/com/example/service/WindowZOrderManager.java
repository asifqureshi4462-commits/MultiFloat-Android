package com.example.service;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks the bottom-to-top z-index stacking order of active floating windows.
 * Index 0 represents the backmost window; (size - 1) represents the frontmost window.
 */
public class WindowZOrderManager {

    private final List<String> stack = new ArrayList<>();

    public synchronized int registerWindow(String windowId) {
        if (windowId == null || windowId.isEmpty()) return -1;
        stack.remove(windowId);
        stack.add(windowId);
        return stack.size() - 1;
    }

    /**
     * Moves the specified window to the front of the stack.
     *
     * @return true if its position in the stack actually changed, false if already frontmost or missing.
     */
    public synchronized boolean bringToFront(String windowId) {
        if (windowId == null) return false;
        int idx = stack.indexOf(windowId);
        if (idx < 0 || idx == stack.size() - 1) {
            return false;
        }
        stack.remove(idx);
        stack.add(windowId);
        return true;
    }

    /**
     * Removes a window from the stack and returns the current top window ID (or null if empty).
     */
    @Nullable
    public synchronized String removeWindow(String windowId) {
        if (windowId != null) {
            stack.remove(windowId);
        }
        return getTopWindowId();
    }

    public synchronized int getZIndex(String windowId) {
        if (windowId == null) return -1;
        return stack.indexOf(windowId);
    }

    public synchronized boolean isTopWindow(String windowId) {
        if (windowId == null || stack.isEmpty()) return false;
        return windowId.equals(stack.get(stack.size() - 1));
    }

    @Nullable
    public synchronized String getTopWindowId() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    public synchronized List<String> getBottomToTopIds() {
        return Collections.unmodifiableList(new ArrayList<>(stack));
    }

    public synchronized List<String> getTopToBottomIds() {
        List<String> copy = new ArrayList<>(stack);
        Collections.reverse(copy);
        return Collections.unmodifiableList(copy);
    }

    public synchronized int size() {
        return stack.size();
    }

    public synchronized void clear() {
        stack.clear();
    }
}
