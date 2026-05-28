#include<bits/stdc++.h>
#include<cstdlib>
#include<iostream>
#include<random>
#include<ctime>
#include"k_means.h"
using namespace std;
typedef long long ll;

class MyClass
{
    public:
        //定义地图需要处理的每个节点
        struct Node
        {
            int x;  //地图横坐标
            int y;  //地图纵坐标
            int w;  //若是聚类中心，则为聚类节点数
            int father;  //节点所在聚类中心，若其为聚类中心，则定义为0
            int meanid;  //所在聚类编号
            ll dis;  //节点到最近聚类中心的距离，实时更新
        };

private:
    int n,m,q,space_cluster,deviation,iteration_count;
    int tot=0; //聚类个数
    vector<int> que;
    
    vector<Node> a;

    //计算节点距离（欧氏距离的平方）
    ll conclude_dis(int mean,int node)
    {
        ll meanx=a[mean].x,meany=a[mean].y;
        ll nodex=a[node].x,nodey=a[node].y;
        return (ll)pow(abs(meanx-nodex),2)+(ll)pow(abs(meany-nodey),2);
    }

    //更新新诞生的聚类中心，并返回此次迭代产生变化的节点
    int update_new_centure(int k)
    {
        int change=0;
        for (int i=1;i<=q;i++)
        if (a[i].father!=0)
        {
            ll new_dis=conclude_dis(k,i);
            if (new_dis<a[i].dis) 
            {
                if (a[i].meanid!=a[k].meanid)
                {  
                    a[que[a[i].meanid]].w--;
                    a[k].w++;
                    change++;
                    a[i].meanid=a[k].meanid;
                }
                a[i].father=k;
                a[i].dis=new_dis;
            }
        }
        return change;
    }

    //对节点记录的距离初始化，便于聚类数不变的时候更新
    void clean()
    {
        for (int i=1;i<=q;i++) a[i].dis=LLONG_MAX; // 修改为LLONG_MAX避免溢出
    }

    //检查每个聚类是否合格，合格返回0，不合格返回不合格的那个聚类编号
    int qualifed()
    {
        for (int i=1;i<=tot;i++)
        {
            // 防止que[i]越界
            if (i >= que.size()) continue;
            if (a[que[i]].w >= space_cluster) return i;
        }
        return 0;
    }

    //添加一个新的聚类中心
    void add_a_node(int k)
    {
        a[k].father=0;
        a[k].meanid=++tot;
        a[k].w=1;
        a[k].dis=0;
        // 确保que的大小足够
        if (tot >= que.size()) {
            que.push_back(k);
        } else {
            que[tot] = k;
        }
    }

    //按随机权值+距离权值随机分裂一个新聚类中心
    int split_a_new_node(int k)
    {
        //创建随机数引擎和分布器
        std::random_device rd;//硬件随机数生成器
        std::mt19937 gen(rd());//以随机设备种子初始化梅森旋转算法

        //定义不同的分布范围
        std::uniform_int_distribution<int> dist(1,1000000);

        long long ans=0;
        int ansid=k;
        for (int i=1;i<=q;i++)
        if (a[i].meanid==a[k].meanid)
        {
            long long num=dist(gen);
            if (num*a[i].dis>ans)
            {
                ans=num*a[i].dis;
                ansid=i;
            }
        }
        return ansid;
    }

    //更新新的点
    void switch_node(int pre,int now,int id)
    {
        a[now].w = a[pre].w;
        a[pre].w = 0;
        a[pre].father = -1;
        a[now].father = 0;
        // 确保id不越界
        if (id < que.size()) {
            que[id] = now;
        } else {
            while (que.size() <= id) que.push_back(0);
            que[id] = now;
        }
    }

    //寻找新的平均中心点
    void conclude_mean()
    {
        if (tot == 0) return;
        
        //计算平均值
        vector<ll> meanx(tot+1,0);  // 修改为ll防止溢出
        vector<ll> meany(tot+1,0);
        vector<int> count(tot+1,0); // 记录每个聚类的节点数
        
        for (int i=1;i<=q;i++)
        {
            if (a[i].meanid > 0 && a[i].meanid <= tot) {
                meanx[a[i].meanid] += a[i].x;
                meany[a[i].meanid] += a[i].y;
                count[a[i].meanid]++;
            }
        }

        // 计算每个聚类的中心坐标
        vector<pair<ll, ll>> center(tot+1);
        for (int i=1;i<=tot;i++)
        {
            if (count[i] > 0) {
                center[i].first = meanx[i] / count[i];
                center[i].second = meany[i] / count[i];
            } else {
                center[i] = {a[que[i]].x, a[que[i]].y};
            }
        }

        //寻找距离平均值最近的点
        vector<ll> minn(tot+1, LLONG_MAX);
        vector<int> new_centure(tot+1,0);
        
        for (int i=1;i<=q;i++)
        {
            int mid = a[i].meanid;
            if (mid <= 0 || mid > tot) continue;
            
            ll dis = abs(a[i].x - center[mid].first) + abs(a[i].y - center[mid].second);
            if (dis < minn[mid])
            {
                minn[mid] = dis;
                new_centure[mid] = i;
            }
        }
    
        //新聚类中心更换旧聚类中心
        for (int i=1;i<=tot;i++) 
        {
            if (new_centure[i] != 0) {
                switch_node(que[i], new_centure[i], i);
            }
        }
    }

    void run()
    {
        //重置随机数种子
        srand(time(0));

        // 边界检查
        if (q <= 0) return;
        
        //随机第一个聚类中心
        int fis=rand()%q+1; //第一个聚类中心
        add_a_node(fis);
        update_new_centure(fis);
        
        //从第一个聚类开始分裂，直到合格
        int change_node=qualifed();
        while (change_node)
        {
            int new_node=split_a_new_node(que[change_node]);
            add_a_node(new_node);
            int different_node=update_new_centure(new_node);
            int count=0;
            
            while (different_node>deviation && count<=iteration_count)
            {
                different_node=0;
                count++;
                clean();
                conclude_mean();
                
                for (int i=1;i<=tot;i++)
                {
                    if (i < que.size() && que[i] != 0) {
                        different_node+=update_new_centure(que[i]);
                    }
                }
            }
            
            // 更新检查结果
            change_node=qualifed();
        }
    }

public:
    MyClass() : tot(0), que(1, 0) { }
    
    MyClass(int n,int m,int q,int space_cluster,int deviation,int iteration_count,Node new_node[])
    {
        this->n=n;
        this->m=m;
        this->q=q;
        this->space_cluster=space_cluster;
        this->deviation=deviation;
        this->iteration_count=iteration_count;
        
        a.push_back(new_node[0]); // 占位0号元素
        for (int i=1;i<=q;i++) 
        {
            a.push_back(new_node[i]);
            //初始化
            a[i].father=-1;
            a[i].w=0;
            a[i].meanid=0;
            a[i].dis=LLONG_MAX;
        }
        
        // 初始化que数组
        que.resize(1, 0); // que[0] 无效
    }
    
    void RunMyClass()
    {
        run();
    }

    int gettot()
    {
        return tot;
    }
    
    vector<int> getque()
    {
        return que;
    }
    
    vector<Node> geta()
    {
        return a;
    }
    
    // 新增：打印聚类结果
    void printResult() {
        cout << "==================== 聚类结果 ====================" << endl;
        cout << "总聚类数量: " << tot << endl;
        cout << "各聚类中心ID: ";
        for (int i=1; i<=tot; i++) {
            if (i < que.size()) {
                cout << que[i] << " ";
            }
        }
        cout << endl;
        
        // 统计每个聚类的节点数
        vector<int> clusterSize(tot+1, 0);
        for (int i=1; i<=q; i++) {
            if (a[i].meanid > 0 && a[i].meanid <= tot) {
                clusterSize[a[i].meanid]++;
            }
        }
        
        // 打印每个聚类的详细信息
        for (int i=1; i<=tot; i++) {
            cout << "\n聚类 " << i << ":" << endl;
            cout << "  中心节点ID: " << que[i] << endl;
            cout << "  中心坐标: (" << a[que[i]].x << ", " << a[que[i]].y << ")" << endl;
            cout << "  包含节点数: " << clusterSize[i] << endl;
            cout << "  包含节点ID: ";
            for (int j=1; j<=q; j++) {
                if (a[j].meanid == i) {
                    cout << j << "(" << a[j].x << "," << a[j].y << ") ";
                }
            }
            cout << endl;
        }
        cout << "==================================================" << endl;
    }
};

// // 主函数测试
// int main()
// {
//     // 1. 设置算法参数
//     int n = 100;    // 地图宽度
//     int m = 100;    // 地图高度
//     int q = 50;     // 节点总数
//     int space_cluster = 10;  // 每个聚类的最大节点数
//     int deviation = 2;       // 迭代收敛阈值
//     int iteration_count = 10; // 最大迭代次数

//     // 2. 生成测试数据（随机分布的节点）
//     srand(time(0));
//     MyClass::Node* nodes = new MyClass::Node[q+1];
    
//     // 生成5个聚类簇的测试数据（便于验证算法效果）
//     vector<pair<int, int>> centers = {{20,20}, {20,80}, {80,20}, {80,80}, {50,50}};
//     for (int i=1; i<=q; i++) {
//         // 每个簇分配10个节点
//         int cluster_idx = (i-1) / 10;
//         if (cluster_idx >= centers.size()) cluster_idx = centers.size() - 1;
        
//         // 在中心周围添加随机偏移
//         nodes[i].x = centers[cluster_idx].first + rand() % 10 - 5;
//         nodes[i].y = centers[cluster_idx].second + rand() % 10 - 5;
//         nodes[i].w = 0;
//         nodes[i].father = -1;
//         nodes[i].meanid = 0;
//         nodes[i].dis = LLONG_MAX;
//     }

//     // 3. 创建并运行聚类算法
//     MyClass myClass(n, m, q, space_cluster, deviation, iteration_count, nodes);
//     myClass.RunMyClass();

//     // 4. 输出结果
//     myClass.printResult();

//     // 5. 释放内存
//     delete[] nodes;
    
//     return 0;
// }