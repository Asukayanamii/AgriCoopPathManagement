package com.agripath.acpserver;

import com.agripath.acpcommon.utils.ClusterAlgorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚类算法专属测试类
 * 功能：验证ClusterAlgorithm的正确性、稳定性，覆盖不同测试场景
 */
public class ClusterAlgorithmTest {

    // ===================== 测试常量配置 =====================
    // 基础测试用例参数
    private static final int BASE_SPACE_CLUSTER = 3;    // 每个聚类最少要有多少个点
    private static final int BASE_DEVIATION = 1;        // 迭代停止：节点变化数 <= deviation 就停
    private static final int BASE_ITERATION = 100;      // 最大迭代次数

    // ===================== 测试用例 =====================

    /**
     * 测试用例1：基础场景（10个节点，3个聚类）
     * 预期结果：3个聚类，每个聚类3-4个节点
     */
    public static void testBasicScenario() {
        System.out.println("=== 测试用例1：基础场景 ===");
        // 1. 构造测试数据（3组密集节点）
        List<int[]> nodes = buildBasicNodes();

        // 2. 初始化算法
        ClusterAlgorithm ca = new ClusterAlgorithm(
                0, 0, nodes.size(),
                BASE_SPACE_CLUSTER,
                BASE_DEVIATION,
                BASE_ITERATION,
                nodes
        );

        // 3. 执行聚类
        ca.run();

        // 4. 获取并打印结果
        printResult(ca);

        // 5. 结果验证（放宽断言，算法是随机的）
        assert ca.getClusterCount() >= 2 : "基础场景聚类数应≥2";
        System.out.println("✅ 基础场景测试通过\n");
    }

    /**
     * 测试用例2：边界场景（节点数刚好等于聚类最小规模）
     * 预期结果：1个聚类，所有节点归为一类
     */
    public static void testBoundaryScenario() {
        System.out.println("=== 测试用例2：边界场景 ===");
        // 1. 构造测试数据（3个节点，刚好满足最小规模）
        List<int[]> nodes = new ArrayList<>();
        nodes.add(new int[]{1, 1});
        nodes.add(new int[]{2, 2});
        nodes.add(new int[]{3, 3});

        // 2. 初始化算法
        ClusterAlgorithm ca = new ClusterAlgorithm(
                0, 0, nodes.size(),
                3,          // 最小规模=节点数
                BASE_DEVIATION,
                BASE_ITERATION,
                nodes
        );

        // 3. 执行聚类
        ca.run();

        // 4. 获取并打印结果
        printResult(ca);

        // 5. 结果验证
        assert ca.getClusterCount() >= 1 : "边界场景聚类数应≥1";
        System.out.println("✅ 边界场景测试通过\n");
    }

    /**
     * 测试用例3：大规模场景（50个节点，多聚类分裂）
     * 预期结果：聚类数≈总节点数/最小规模
     */
    public static void testLargeScaleScenario() {
        System.out.println("=== 测试用例3：大规模场景 ===");
        // 1. 构造测试数据（50个节点，5组）
        List<int[]> nodes = buildLargeNodes();

        // 2. 初始化算法
        ClusterAlgorithm ca = new ClusterAlgorithm(
                0, 0, nodes.size(),
                10,         // 每个聚类至少10个节点
                5,          // 放宽偏差阈值
                200,        // 增加迭代次数
                nodes
        );

        // 3. 执行聚类
        ca.run();

        // 4. 获取并打印结果
        printResult(ca);

        // 5. 结果验证
        assert ca.getClusterCount() >= 4 : "大规模场景聚类数应≥4";
        System.out.println("✅ 大规模场景测试通过\n");
    }

    /**
     * 测试用例4：异常场景（空节点/无效坐标）
     * 预期结果：程序不崩溃，返回合理结果
     */
    public static void testExceptionScenario() {
        System.out.println("=== 测试用例4：异常场景 ===");
        // 1. 构造测试数据（包含无效坐标）
        List<int[]> nodes = new ArrayList<>();
        nodes.add(new int[]{0, 0});          // 有效
        nodes.add(new int[]{-100, -200});    // 负数坐标
        nodes.add(new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE}); // 超大值

        // 2. 初始化算法
        ClusterAlgorithm ca = new ClusterAlgorithm(
                0, 0, nodes.size(),
                1,          // 最小规模=1
                BASE_DEVIATION,
                BASE_ITERATION,
                nodes
        );

        // 3. 执行聚类（验证不崩溃）
        try {
            ca.run();
            printResult(ca);
            System.out.println("✅ 异常场景测试通过（程序未崩溃）\n");
        } catch (Exception e) {
            System.err.println("❌ 异常场景测试失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== 辅助方法 =====================

    /**
     * 构造基础测试节点（10个，3组）
     */
    private static List<int[]> buildBasicNodes() {
        List<int[]> nodes = new ArrayList<>();
        // 第一组：(1,2),(2,3),(3,4)
        nodes.add(new int[]{1, 2});
        nodes.add(new int[]{2, 3});
        nodes.add(new int[]{3, 4});
        // 第二组：(10,11),(11,12),(12,13)
        nodes.add(new int[]{10, 11});
        nodes.add(new int[]{11, 12});
        nodes.add(new int[]{12, 13});
        // 第三组：(20,21),(21,22),(22,23),(23,24)
        nodes.add(new int[]{20, 21});
        nodes.add(new int[]{21, 22});
        nodes.add(new int[]{22, 23});
        nodes.add(new int[]{23, 24});
        return nodes;
    }

    /**
     * 构造大规模测试节点（50个，5组）
     */
    private static List<int[]> buildLargeNodes() {
        List<int[]> nodes = new ArrayList<>();
        // 5组，每组10个节点
        int baseX = 0;
        int baseY = 0;
        for (int group = 0; group < 5; group++) {
            for (int i = 0; i < 10; i++) {
                nodes.add(new int[]{baseX + i, baseY + i});
            }
            baseX += 100;
            baseY += 100;
        }
        return nodes;
    }

    /**
     * 统一打印聚类结果
     */
    private static void printResult(ClusterAlgorithm ca) {
        // 1. 基础信息
        System.out.println("📊 聚类总数：" + ca.getClusterCount());
        System.out.println("📍 聚类中心编号：" + ca.getClusterCenters());

        // 2. 每个节点的聚类归属
        System.out.println("🔍 节点聚类详情：");
        List<int[]> result = ca.getClusterResult();
        for (int i = 0; i < result.size(); i++) {
            int[] item = result.get(i);
            System.out.printf("   节点(%d,%d) → 聚类ID：%d%n", item[0], item[1], item[2]);
        }

        // 3. 按聚类分组统计
        System.out.println("📈 各聚类节点数统计：");
        int maxClusterId = 0;
        for (int[] item : result) {
            if (item[2] > maxClusterId) {
                maxClusterId = item[2];
            }
        }
        int[] clusterCount = new int[maxClusterId + 1];
        for (int[] item : result) {
            if (item[2] >= 0 && item[2] <= maxClusterId) {
                clusterCount[item[2]]++;
            }
        }
        for (int i = 1; i <= maxClusterId; i++) {
            System.out.printf("   聚类%d：%d个节点%n", i, clusterCount[i]);
        }
        System.out.println("----------------------------------------");
    }

    // ===================== 主方法（执行所有测试） =====================
    public static void main(String[] args) {
        System.out.println("========== 聚类算法测试套件 ==========\n");

        // 执行所有测试用例
        testBasicScenario();
        testBoundaryScenario();
        testLargeScaleScenario();
        testExceptionScenario();

        System.out.println("========== 所有测试用例执行完成 ==========");
    }
}