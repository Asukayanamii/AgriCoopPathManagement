package com.agripath.acpcommon.utils;

public class KMeansClusterUtil {
    static {
        NativeLibLoader.loadLibraries();
    }

    public static native int[][][] kMeans(
        int mapW, int mapH, int nodeNum,
        int minNodeNumForEachCluster, int deviation, int maxIterationCount,
        int[][] nodes
    );
}
