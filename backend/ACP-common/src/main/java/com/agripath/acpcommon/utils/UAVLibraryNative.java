package com.agripath.acpcommon.utils;

/**
 * UAVLibrary.dll 的 Java native 方法声明。
 * 所有方法对应 UAVLibrary.h 中的 12 个导出函数。
 */
public class UAVLibraryNative {

    static {
        NativeLibLoader.loadLibraries();
    }

    // ==================== 图结构 ====================

    /**
     * 根据边表创建无向图
     * @param nodeCount  节点数（编号 0 ~ nodeCount-1）
     * @param from       边起点数组
     * @param to         边终点数组
     * @param weight     边权重数组
     * @param edgeCount  边数量
     * @return 图句柄（long 指针）
     */
    public static native long createGraph(int nodeCount, int[] from, int[] to, int[] weight, int edgeCount);

    /**
     * 构建标准距离地图（A* 启发数据）
     * @param px     节点 x 坐标数组
     * @param py     节点 y 坐标数组
     * @param n      节点数量（必须等于 createGraph 的 nodeCount）
     * @param graph  图句柄
     * @return 标准地图句柄
     */
    public static native long createStandmap(int[] px, int[] py, int n, long graph);

    /** 销毁图 */
    public static native void freeGraph(long graph);

    /** 销毁标准地图 */
    public static native void freeStandmap(long sm);

    /** 导出标准地图数据（用于持久化），返回 n×n 距离矩阵 */
    public static native int[] exportStandmap(long sm);

    /** 从持久化的距离矩阵重建标准地图 */
    public static native long createStandmapFromData(int[] data, int n);

    // ==================== 节点编码 ====================

    /** 节点 ID → 4位十六进制编码 */
    public static native String encodeId(int id);

    /** 批量编码节点，返回 String[]，每条为 4 位 Hex */
    public static native String[] encodeNodes(int[] ids);

    /** 批量编码边，返回 String[]，每条为 4 位 Hex */
    public static native String[] encodeEdges(int[] ids);

    // ==================== K-means 聚类 ====================

    /**
     * K-means 聚类
     * @return int[2][n]: [0]=clusterIds, [1]=centerIds
     */
    public static native int[][] kmeans(int[] px, int[] py, int n,
                                        int space, int deviation, int maxIter);

    // ==================== Car Planning 综合接口 ====================

    /**
     * 综合接口：选目标岔路口 → 搜附近小车 → A* 路径，一步返回
     * @return String[]，每条格式 "小车ID::路径编码"
     */
    public static native String[] carPlanning(
        int[] roadPx, int[] roadPy, int roadCount,
        int[] carX, int[] carY, int[] carState, int[] carBelongNode, int carCount,
        int[] taskPx, int[] taskPy, String[] taskCodes, int taskCount,
        long graph, long sm);

    // ==================== TSP 路径规划 ====================

    /**
     * TSP 路径规划（Held-Karp DP）
     * @param px    px[0] = 起点（岔路口），px[1..n] = 任务点
     * @param py    同上
     * @param codes 编码数组
     * @param n     除起点外的目标数量
     * @return 路径编码字符串
     */
    public static native String tspPlan(int[] px, int[] py, String[] codes, int n);
}
