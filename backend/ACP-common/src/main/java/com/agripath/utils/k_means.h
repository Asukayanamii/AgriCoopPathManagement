#ifndef K_MEANS_H
#define K_MEANS_H

#include <vector>
#include <climits> // 用于INT_MAX
#include <cstdint> // 用于long long/ll类型

// 类型别名，与源文件保持一致
typedef long long ll;

class MyClass
{
private:
    // 私有成员变量
    int n, m, q, space_cluster, deviation, iteration_count;
    int tot; // 聚类个数
    std::vector<int> que;

    // 节点结构体定义
    struct Node
    {
        int x;          // 地图横坐标
        int y;          // 地图纵坐标
        int w;          // 若是聚类中心，则为聚类节点数
        int father;     // 节点所在聚类中心，若其为聚类中心，则定义为0
        int meanid;     // 所在聚类编号
        ll dis;         // 节点到最近聚类中心的距离，实时更新
    };
    std::vector<Node> a;

    // 私有成员函数声明
    ll conclude_dis(int mean, int node);
    int update_new_centure(int k);
    void clean();
    int qualifed();
    void add_a_node(int k);
    int split_a_new_node(int k);
    void switch_node(int pre, int now, int id);
    void conclude_mean();
    void run();

public:
    // 构造函数声明
    MyClass();
    MyClass(int n, int m, int q, int space_cluster, int deviation, int iteration_count, Node new_node[]);

    // 公有成员函数声明
    void RunMyClass();
    int gettot();
    std::vector<int> getque();
    std::vector<Node> geta();
};

#endif // K_MEANS_H