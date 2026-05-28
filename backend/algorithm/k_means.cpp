#include<bits/stdc++.h>
#include<cstdlib>
#include<iostream>
#include<random>
using namespace std;
typedef long long ll;

class K_means
{
private:
    int n,m,q,space_cluster,deviation,iteration_count;
    int tot=0; //聚类个数
    vector<int> que;
    
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
    vector<Node> a;

    //计算节点距离
    ll conclude_dis(int mean,int node)
    {
        ll meanx=a[mean].x,meany=a[mean].y;
       ll nodex=a[node].x,nodey=a[node].y;
        return pow(abs(meanx-nodex),2)+pow(abs(meany-nodey),2);
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
        for (int i=1;i<=q;i++) a[i].dis=INT_MAX;
    }

    //检查每个聚类是否合格，合格返回0，不合格返回不合格的那个聚类编号
    int qualifed()
    {
        for (int i=1;i<=tot;i++)
          if (a[que[i]].w>=space_cluster) return i;
        return 0;
    }

    //添加一个新的聚类中心
    void add_a_node(int k)
    {
        a[k].father=0;
        a[k].meanid=++tot;
        a[k].w=1;
        a[k].dis=0;
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
        a[now].w=a[pre].w;
        a[pre].w=0;
        a[pre].father=-1;
        a[now].father=0;
        que[id]=now;
    }

    //寻找新的平均中心点
    void conclude_mean()
    {
        //计算平均值
        vector<int> meanx(tot+1,0);
        vector<int> meany(tot+1,0);
        for (int i=1;i<=q;i++)
        {
            meanx[a[i].meanid]+=a[i].x-a[a[i].father].x;
            meany[a[i].meanid]+=a[i].y-a[a[i].father].y;
        }

        for (int i=1;i<=tot;i++)
        {
            meanx[i]=a[que[i]].x+meanx[i]/a[que[i]].w;
            meany[i]=a[que[i]].y+meany[i]/a[que[i]].w;
        }

        //寻找距离平均值最近的点
        vector<int> minn(tot+1,INT_MAX);
        vector<int> new_centure(tot+1,0);
        for (int i=1;i<=q;i++)
        {
            int k=que[a[i].meanid];
            if (abs(a[k].x-a[i].x)+abs(a[k].y-a[i].y)<minn[a[i].meanid])
            {
                minn[a[i].meanid]=abs(a[k].x-a[i].x)+abs(a[k].y-a[i].y);
                new_centure[a[i].meanid]=i;
            }
        }
    
        //新聚类中心更换旧聚类中心
        for (int i=1;i<=tot;i++) switch_node(que[i],new_centure[i],i);
    }

    void run()
    {
        //重置随机数种子
        srand(time(0));

        //随机第一个聚类中心
        int fis=rand()%q+1; //第一个聚类中心
        add_a_node(fis);
        update_new_centure(fis);
        que.push_back(fis);

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
                  different_node+=update_new_centure(que[i]);
            }
        }
    }

public:
    K_means() : tot(0), que(1, 0) { }
    K_means(int n,int m,int q,int space_cluster,int deviation,int iteration_count,int xx[],int yy[])
    {
        this->n=n;
        this->m=m;
        this->q=q;
        this->space_cluster=space_cluster;
        this->deviation=deviation;
        this->iteration_count=iteration_count;
        
        Node new_node;
        a.push_back(new_node);
        //初始化
        new_node.father=-1;
        new_node.w=0;
        new_node.meanid=0;
        new_node.dis=INT_MAX;
        for (int i=1;i<=q;i++) 
        {
            new_node.x=xx[i];
            new_node.y=yy[i];
            a.push_back(new_node);
        }
    }
    
    void RunK_means()
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
        //第0个值是无效值
    }
    vector<Node> geta()
    {
        return a;
        //第0个值是无效值
    }
};

int main()
{
    return 0;
}