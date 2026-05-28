#include<bits/stdc++.h>
#include<cstdlib>
#include<iostream>
#include<random>
#include<ctime>
#include"ClusterAlgorithm.h"
using namespace std;
typedef long long ll;

// 【删除原.cpp中重复的ClusterAlgorithm类定义】
// 直接实现头文件中声明的类的成员函数（用::作用域解析）

// 计算节点距离
ll ClusterAlgorithm::conclude_dis(int mean, int node) {
    ll meanx = a[mean].x, meany = a[mean].y;
    ll nodex = a[node].x, nodey = a[node].y;
    return pow(abs(meanx - nodex), 2) + pow(abs(meany - nodey), 2);
}

// 更新新诞生的聚类中心，并返回此次迭代产生变化的节点
int ClusterAlgorithm::update_new_centure(int k) {
    int change = 0;
    for (int i = 1; i <= q; i++) {
        if (a[i].father != 0) {
            ll new_dis = conclude_dis(k, i);
            if (new_dis < a[i].dis) {
                if (a[i].meanid != a[k].meanid) {
                    a[que[a[i].meanid]].w--;
                    a[k].w++;
                    change++;
                    a[i].meanid = a[k].meanid;
                }
                a[i].father = k;
                a[i].dis = new_dis;
            }
        }
    }
    return change;
}

// 对节点记录的距离初始化，便于聚类数不变的时候更新
void ClusterAlgorithm::clean() {
    for (int i = 1; i <= q; i++) {
        a[i].dis = INT_MAX;
    }
}

// 检查每个聚类是否合格，合格返回0，不合格返回不合格的那个聚类编号
int ClusterAlgorithm::qualifed() {
    for (int i = 1; i <= tot; i++) {
        if (a[que[i]].w >= space_cluster) {
            return i;
        }
    }
    return 0;
}

// 添加一个新的聚类中心
void ClusterAlgorithm::add_a_node(int k) {
    a[k].father = 0;
    a[k].meanid = ++tot;
    a[k].w = 1;
    a[k].dis = 0;
}

// 按随机权值+距离权值随机分裂一个新聚类中心
int ClusterAlgorithm::split_a_new_node(int k) {
    // 创建随机数引擎和分布器
    std::random_device rd; // 硬件随机数生成器
    std::mt19937 gen(rd()); // 以随机设备种子初始化梅森旋转算法

    // 定义不同的分布范围
    std::uniform_int_distribution<int> dist(1, 1000000);

    long long ans = 0;
    int ansid = k;
    for (int i = 1; i <= q; i++) {
        if (a[i].meanid == a[k].meanid) {
            long long num = dist(gen);
            if (num * a[i].dis > ans) {
                ans = num * a[i].dis;
                ansid = i;
            }
        }
    }
    return ansid;
}

// 更新新的点
void ClusterAlgorithm::switch_node(int pre, int now, int id) {
    a[now].w = a[pre].w;
    a[pre].w = 0;
    a[pre].father = -1;
    a[now].father = 0;
    que[id] = now;
}

// 寻找新的平均中心点
void ClusterAlgorithm::conclude_mean() {
    // 计算平均值
    vector<int> meanx(tot + 1, 0);
    vector<int> meany(tot + 1, 0);
    for (int i = 1; i <= q; i++) {
        meanx[a[i].meanid] += a[i].x - a[a[i].father].x;
        meany[a[i].meanid] += a[i].y - a[a[i].father].y;
    }

    for (int i = 1; i <= tot; i++) {
        meanx[i] = a[que[i]].x + meanx[i] / a[que[i]].w;
        meany[i] = a[que[i]].y + meany[i] / a[que[i]].w;
    }

    // 寻找距离平均值最近的点
    vector<int> minn(tot + 1, INT_MAX);
    vector<int> new_centure(tot + 1, 0);
    for (int i = 1; i <= q; i++) {
        int k = que[a[i].meanid];
        if (abs(a[k].x - a[i].x) + abs(a[k].y - a[i].y) < minn[a[i].meanid]) {
            minn[a[i].meanid] = abs(a[k].x - a[i].x) + abs(a[k].y - a[i].y);
            new_centure[a[i].meanid] = i;
        }
    }

    // 新聚类中心更换旧聚类中心
    for (int i = 1; i <= tot; i++) {
        switch_node(que[i], new_centure[i], i);
    }
}

// 构造函数实现
ClusterAlgorithm::ClusterAlgorithm(int _n, int _m, int _q, int _space_cluster, int _deviation, int _iteration_count, const vector<pair<int, int>>& nodes) {
    // 初始化参数
    n = _n;
    m = _m;
    q = _q;
    space_cluster = _space_cluster;
    deviation = _deviation;
    iteration_count = _iteration_count;
    
    // 初始化que和节点数组（节点从1开始索引，与原逻辑一致）
    que.resize(1, 0);
    a.resize(q + 1); // a[0] 弃用，a[1~q] 存储节点
    
    // 初始化节点数据
    for (int i = 1; i <= q; i++) {
        a[i].x = nodes[i-1].first;
        a[i].y = nodes[i-1].second;
        a[i].father = -1;
        a[i].w = 0;
        a[i].meanid = 0;
        a[i].dis = INT_MAX;
    }

    // 重置随机数种子
    srand(time(0));
}

// 执行聚类算法（替代原main函数的核心逻辑）
void ClusterAlgorithm::run() {
    // 随机第一个聚类中心
    int fis = rand() % q + 1; // 第一个聚类中心
    add_a_node(fis);
    update_new_centure(fis);
    que.push_back(fis);

    // 从第一个聚类开始分裂，直到合格
    int change_node = qualifed();
    while (change_node) {
        int new_node = split_a_new_node(que[change_node]);
        add_a_node(new_node);
        int different_node = update_new_centure(new_node);
        int count = 0;
        while (different_node > deviation && count <= iteration_count) {
            different_node = 0;
            count++;
            clean();
            conclude_mean();
            for (int i = 1; i <= tot; i++) {
                different_node += update_new_centure(que[i]);
            }
        }
        change_node = qualifed(); // 原代码遗漏这行，导致死循环，此处修复
    }
}

// 获取聚类结果：二维数组，每行格式为 [x, y, meanid]
vector<vector<int>> ClusterAlgorithm::getClusterResult() {
    vector<vector<int>> result;
    for (int i = 1; i <= q; i++) {
        result.push_back({a[i].x, a[i].y, a[i].meanid});
    }
    return result;
}

// 获取聚类中心编号列表
vector<int> ClusterAlgorithm::getClusterCenters() {
    vector<int> centers;
    for (int i = 1; i <= tot; i++) {
        centers.push_back(que[i]);
    }
    return centers;
}

// 获取聚类总数
int ClusterAlgorithm::getClusterCount() {
    return tot;
}