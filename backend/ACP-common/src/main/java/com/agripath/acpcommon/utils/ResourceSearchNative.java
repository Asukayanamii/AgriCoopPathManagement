package com.agripath.acpcommon.utils;

public class ResourceSearchNative {
    static {
        NativeLibLoader.loadLibraries();
    }

    public static native int[][] searchResources(
        int mapW, int mapH, int resourceCount,
        int[] rx, int[] ry, int[] rid, boolean[] rstate,
        int targetCount, int[] tx, int[] ty, int[] treq
    );
}
