package com.example.service;

/**
 * Pure-Java geometry calculator for 8-direction floating window resizing
 * (4 corners + 4 edges) with anchored opposite edges and min/max screen clamping.
 */
public final class WindowResizeCalculator {

    public enum Direction {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public static final class Bounds {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public Bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private WindowResizeCalculator() {}

    public static Bounds computeResizedBounds(
            Direction dir,
            int initX, int initY, int initW, int initH,
            int dx, int dy,
            int minW, int minH,
            int maxW, int maxH,
            int screenW, int screenH,
            int sideMarginPx, int topMarginPx, int bottomMarginPx) {

        int x = initX;
        int y = initY;
        int w = initW;
        int h = initH;

        boolean hasLeft = dir == Direction.LEFT || dir == Direction.TOP_LEFT || dir == Direction.BOTTOM_LEFT;
        boolean hasRight = dir == Direction.RIGHT || dir == Direction.TOP_RIGHT || dir == Direction.BOTTOM_RIGHT;
        boolean hasTop = dir == Direction.TOP || dir == Direction.TOP_LEFT || dir == Direction.TOP_RIGHT;
        boolean hasBottom = dir == Direction.BOTTOM || dir == Direction.BOTTOM_LEFT || dir == Direction.BOTTOM_RIGHT;

        if (hasLeft) {
            int rightEdge = initX + initW;
            int minAllowedX = Math.max(sideMarginPx, rightEdge - maxW);
            int maxAllowedX = Math.max(minAllowedX, rightEdge - minW);
            x = clamp(initX + dx, minAllowedX, maxAllowedX);
            w = rightEdge - x;
        } else if (hasRight) {
            int availableMaxW = Math.max(minW, Math.min(maxW, screenW - initX - sideMarginPx));
            w = clamp(initW + dx, minW, availableMaxW);
        }

        if (hasTop) {
            int bottomEdge = initY + initH;
            int minAllowedY = Math.max(topMarginPx, bottomEdge - maxH);
            int maxAllowedY = Math.max(minAllowedY, bottomEdge - minH);
            y = clamp(initY + dy, minAllowedY, maxAllowedY);
            h = bottomEdge - y;
        } else if (hasBottom) {
            int availableMaxH = Math.max(minH, Math.min(maxH, screenH - initY - bottomMarginPx));
            h = clamp(initH + dy, minH, availableMaxH);
        }

        return new Bounds(x, y, w, h);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
