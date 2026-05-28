package com.agripath.acpcommon.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// 对应C++中的节点结构体
class Node {
    int x;          // x坐标
    int y;          // y坐标
    int father;     // 父节点（聚类中心）ID
    int w;          // 聚类包含的节点数
    int meanid;     // 聚类ID
    long dis;       // 到聚类中心的距离（用long避免溢出）

    public Node() {
        this.father = -1;
        this.w = 0;
        this.meanid = 0;
        this.dis = Integer.MAX_VALUE;
    }
}

// 核心聚类算法类
public class ClusterAlgorithm {
    private int n;                  // 原始参数n
    private int m;                  // 原始参数m
    private int q;                  // 节点总数
    private int space_cluster;      // 聚类规模阈值
    private int deviation;          // 偏差阈值
    private int iteration_count;    // 最大迭代次数
    private int tot;                // 聚类总数
    private List<Node> a;           // 节点数组（索引从1开始）
    private List<Integer> que;      // 聚类中心ID列表（动态扩容，无需预分配）
    private Random random;          // 随机数生成器

    // 构造函数
    public ClusterAlgorithm(int _n, int _m, int _q, int _space_cluster, int _deviation, int _iteration_count, List<int[]> nodes) {
        // 初始化参数
        this.n = _n;
        this.m = _m;
        this.q = _q;
        this.space_cluster = _space_cluster;
        this.deviation = _deviation;
        this.iteration_count = _iteration_count;
        this.tot = 0;

        // 初始化随机数生成器
        this.random = new Random(System.currentTimeMillis());

        // 初始化节点数组（索引从1开始，0位置占位）
        this.a = new ArrayList<>();
        this.a.add(new Node()); // 索引0占位

        for (int i = 1; i <= q; i++) {
            Node node = new Node();
            node.x = nodes.get(i-1)[0];
            node.y = nodes.get(i-1)[1];
            this.a.add(node);
        }

        // 初始化聚类中心列表（空列表，动态扩容）
        this.que = new ArrayList<>();
        this.que.add(0); // 索引0占位
    }

    // 计算节点距离
    private long conclude_dis(int mean, int node) {
        if (mean < 1 || mean >= a.size() || node < 1 || node >= a.size()) {
            return Long.MAX_VALUE; // 容错：无效索引返回极大值
        }
        long meanx = a.get(mean).x;
        long meany = a.get(mean).y;
        long nodex = a.get(node).x;
        long nodey = a.get(node).y;

        long dx = Math.abs(meanx - nodex);
        long dy = Math.abs(meany - nodey);
        return dx * dx + dy * dy; // 替代pow，避免浮点运算
    }

    // 更新新诞生的聚类中心
    private int update_new_centure(int k) {
        if (k < 1 || k >= a.size()) {
            return 0; // 容错：无效聚类中心
        }

        int change = 0;
        for (int i = 1; i < a.size(); i++) {
            Node node = a.get(i);
            if (node.father != 0) {
                long new_dis = conclude_dis(k, i);
                if (new_dis < node.dis) {
                    if (node.meanid != a.get(k).meanid) {
                        // 容错：确保meanid对应的que索引有效
                        if (node.meanid >= 1 && node.meanid < que.size()) {
                            a.get(que.get(node.meanid)).w--;
                        }
                        a.get(k).w++;
                        change++;
                        node.meanid = a.get(k).meanid;
                    }
                    node.father = k;
                    node.dis = new_dis;
                }
            }
        }
        return change;
    }

    // 对节点记录的距离初始化
    private void clean() {
        for (int i = 1; i < a.size(); i++) {
            a.get(i).dis = Integer.MAX_VALUE;
        }
    }

    // 检查每个聚类是否合格
    private int qualifed() {
        for (int i = 1; i < que.size(); i++) {
            // 容错：确保que[i]是有效节点
            if (que.get(i) < 1 || que.get(i) >= a.size()) {
                continue;
            }
            if (a.get(que.get(i)).w >= space_cluster) {
                return i;
            }
        }
        return 0;
    }

    // 添加一个新的聚类中心（核心修复：动态扩容que）
    private void add_a_node(int k) {
        if (k < 1 || k >= a.size()) {
            return; // 容错：无效节点
        }

        Node node = a.get(k);
        node.father = 0;
        node.meanid = ++tot;
        node.w = 1;
        node.dis = 0;

        // 核心修复：动态扩容que列表，确保索引可达tot
        while (que.size() <= tot) {
            que.add(0); // 不足则补占位符
        }
        que.set(node.meanid, k);
    }

    // 按随机权值+距离权值随机分裂一个新聚类中心
    private int split_a_new_node(int k) {
        if (k < 1 || k >= a.size()) {
            return 1; // 容错：返回第一个节点
        }

        long ans = 0;
        int ansid = k;
        int targetMeanId = a.get(k).meanid;

        for (int i = 1; i < a.size(); i++) {
            Node node = a.get(i);
            if (node.meanid == targetMeanId) {
                long num = random.nextInt(1000000) + 1; // 1~1000000的随机数
                if (num * node.dis > ans) {
                    ans = num * node.dis;
                    ansid = i;
                }
            }
        }
        return ansid;
    }

    // 更新新的点
    private void switch_node(int pre, int now, int id) {
        // 容错：确保所有索引有效
        if (pre < 1 || pre >= a.size() || now < 1 || now >= a.size() || id < 1 || id >= que.size()) {
            return;
        }

        Node preNode = a.get(pre);
        Node nowNode = a.get(now);

        nowNode.w = preNode.w;
        preNode.w = 0;
        preNode.father = -1;
        nowNode.father = 0;
        que.set(id, now);
    }

    // 寻找新的平均中心点（彻底修复索引越界）
    private void conclude_mean() {
        if (tot <= 0 || que.size() <= 1) {
            return; // 没有聚类中心，直接返回
        }

        // 初始化meanx和meany，动态长度
        long[] meanx = new long[tot + 1];
        long[] meany = new long[tot + 1];

        // 计算每个聚类的坐标总和偏移量
        for (int i = 1; i < a.size(); i++) {
            Node node = a.get(i);
            int meanId = node.meanid;

            // 容错：确保meanId有效
            if (meanId < 1 || meanId > tot || node.father < 1 || node.father >= a.size()) {
                continue;
            }

            meanx[meanId] += (node.x - a.get(node.father).x);
            meany[meanId] += (node.y - a.get(node.father).y);
        }

        // 计算每个聚类的平均坐标
        for (int i = 1; i <= tot; i++) {
            // 容错：确保que[i]有效
            if (i >= que.size()) {
                continue;
            }
            int centerId = que.get(i);
            if (centerId < 1 || centerId >= a.size()) {
                continue;
            }

            Node centerNode = a.get(centerId);
            if (centerNode.w > 0) { // 避免除以0
                meanx[i] = centerNode.x + meanx[i] / centerNode.w;
                meany[i] = centerNode.y + meany[i] / centerNode.w;
            } else {
                meanx[i] = centerNode.x;
                meany[i] = centerNode.y;
            }
        }

        // 寻找距离平均值最近的点
        int[] minn = new int[tot + 1];
        int[] new_centure = new int[tot + 1];

        // 初始化最小值为极大值
        for (int i = 1; i <= tot; i++) {
            minn[i] = Integer.MAX_VALUE;
            // 默认使用原中心（容错）
            new_centure[i] = (i < que.size()) ? que.get(i) : 1;
        }

        for (int i = 1; i < a.size(); i++) {
            Node node = a.get(i);
            int meanId = node.meanid;

            // 容错：确保meanId有效
            if (meanId < 1 || meanId > tot || meanId >= que.size()) {
                continue;
            }

            int k = que.get(meanId);
            // 容错：确保聚类中心有效
            if (k < 1 || k >= a.size()) {
                continue;
            }

            int distance = Math.abs(a.get(k).x - node.x) + Math.abs(a.get(k).y - node.y);

            if (distance < minn[meanId]) {
                minn[meanId] = distance;
                new_centure[meanId] = i;
            }
        }

        // 新聚类中心更换旧聚类中心
        for (int i = 1; i <= tot; i++) {
            if (i < que.size()) {
                switch_node(que.get(i), new_centure[i], i);
            }
        }
    }

    // 执行聚类算法
    public void run() {
        if (a.size() <= 1) {
            return; // 无节点，直接返回
        }

        // 随机第一个聚类中心
        int fis = random.nextInt(q) + 1; // 1~q的随机数
        add_a_node(fis);
        update_new_centure(fis);

        // 从第一个聚类开始分裂，直到合格
        int change_node = qualifed();
        while (change_node != 0) {
            // 容错：确保change_node有效
            if (change_node >= que.size()) {
                break;
            }
            int new_node = split_a_new_node(que.get(change_node));
            add_a_node(new_node);
            int different_node = update_new_centure(new_node);
            int count = 0;

            while (different_node > deviation && count <= iteration_count) {
                different_node = 0;
                count++;
                clean();
                conclude_mean();

                for (int i = 1; i <= tot && i < que.size(); i++) {
                    different_node += update_new_centure(que.get(i));
                }
            }
            change_node = qualifed();
        }
    }

    // 获取聚类结果：二维数组，每行格式为 [x, y, meanid]
    public List<int[]> getClusterResult() {
        List<int[]> result = new ArrayList<>();
        for (int i = 1; i < a.size(); i++) {
            Node node = a.get(i);
            result.add(new int[]{node.x, node.y, node.meanid});
        }
        return result;
    }

    // 获取聚类中心编号列表
    public List<Integer> getClusterCenters() {
        List<Integer> centers = new ArrayList<>();
        for (int i = 1; i < que.size(); i++) {
            int centerId = que.get(i);
            if (centerId > 0 && centerId < a.size()) { // 只返回有效中心
                centers.add(centerId);
            }
        }
        return centers;
    }

    // 获取聚类总数
    public int getClusterCount() {
        return tot;
    }
}