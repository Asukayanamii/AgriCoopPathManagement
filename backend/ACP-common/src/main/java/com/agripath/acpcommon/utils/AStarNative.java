package com.agripath.acpcommon.utils;

public class AStarNative {
    static {
        NativeLibLoader.loadLibraries();
    }

    public static native int[][] findPath(
        int mapW, int mapH,
        int startX, int startY, int endX, int endY,
        int obstacleCount, int[] ox, int[] oy,
        int gridRes
    );
}
