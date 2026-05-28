#ifndef CLUSTER_ALGORITHM_H
#define CLUSTER_ALGORITHM_H

#include <vector>
#include <utility> // 用于pair
#include <climits> // 用于INT_MAX
#include <cstdint> // 用于int64_t/uint64_t

// 防止重复定义，适配不同编译器
#ifndef ll
typedef long long ll;
#endif

/**
 * @brief 聚类算法类
 * 封装了基于分裂的空间聚类算法，支持构造传参、执行聚类、获取结果
 * 核心逻辑：随机初始化聚类中心 -> 分裂超阈值聚类 -> 迭代优化聚类中心
 */
class ClusterAlgorithm {
private:
    // 算法参数
    int n, m, q, space_cluster, deviation, iteration_count;
    int tot; // 聚类个数
    std::vector<int> que;

    // 地图节点结构体：存储坐标、聚类信息、距离等
    struct Node {
        int x;          // 地图横坐标
        int y;          // 地图纵坐标
        int w;          // 聚类中心：聚类节点数；普通节点：无意义
        int father;     // 所属聚类中心编号（0表示自身是中心，-1表示未初始化）
        int meanid;     // 所属聚类编号
        ll dis;         // 到最近聚类中心的距离（平方和）
    };
    std::vector<Node> a; // 节点数组（索引从1开始）

    // 私有核心算法函数（对外隐藏实现细节）
    ll conclude_dis(int mean, int node);
    int update_new_centure(int k);
    void clean();
    int qualifed();
    void add_a_node(int k);
    int split_a_new_node(int k);
    void switch_node(int pre, int now, int id);
    void conclude_mean();

public:
    /**
     * @brief 构造函数：初始化聚类算法参数和节点数据
     * @param _n 地图宽度
     * @param _m 地图高度
     * @param _q 节点总数
     * @param _space_cluster 单个聚类的节点数阈值（超过则分裂）
     * @param _deviation 迭代收敛的误差阈值（变化节点数小于该值则停止迭代）
     * @param _iteration_count 最大迭代次数
     * @param nodes 节点坐标列表（格式：{{x1,y1}, {x2,y2}, ..., {xq,yq}}）
     */
    ClusterAlgorithm(int _n, int _m, int _q, int _space_cluster, int _deviation, int _iteration_count, const std::vector<std::pair<int, int>>& nodes);

    /**
     * @brief 执行聚类算法（核心入口）
     * 调用后会完成整个聚类过程：初始化中心 -> 分裂聚类 -> 迭代优化
     */
    void run();

    /**
     * @brief 获取聚类结果
     * @return 二维数组，每行格式：[x坐标, y坐标, 聚类编号]
     */
    std::vector<std::vector<int>> getClusterResult();

    /**
     * @brief 获取所有聚类中心的节点编号
     * @return 聚类中心编号列表（编号对应nodes参数的索引+1）
     */
    std::vector<int> getClusterCenters();

    /**
     * @brief 获取最终的聚类总数
     * @return 聚类个数
     */
    int getClusterCount();
};

#endif // CLUSTER_ALGORITHM_H