#include<bits/stdc++.h>
#include <string>  // 需要string支持
#include <queue>   // 需要priority_queue
#include <functional>
using namespace std;

class A_
{
private:

//单个聚类节点数，误差，迭代次数,边的条数,中心聚点
int q,space_cluster,deviation,iteration_count,n,m;
int tot=0;
int t; //请求数目
vector<int> start_node;
vector<int> end_node;
priority_queue<int> que;
vector<string> ans;

struct Node
{
    int x;  //地图横坐标
    int y;  //地图纵坐标
    int w;  //若是聚类中心，则为聚类节点数
    int father;  //节点所在聚类中心，若其为聚类中心，则定义为0
    int meanid;  //所在聚类编号
};
Node a[100000];

//边表
struct Line
{
    int node;  //端点
    int next_line;  //下一条边
    int Lenth;  //边的长度
};
Line cnt[1000000];
int head[100000];

//中心节点
struct Centure
{
    int id;  //中心点编号
    int dis[100000];  //中心点最短路径
};
Centure cen[1000];

//链式前向星
void add_line(int x,int y,int l)
{
    cnt[++tot].next_line=head[x];
    head[x]=tot;
    cnt[tot].node=y;
    cnt[tot].Lenth=l;
}

//预处理部分
//Dijkstra计算最短路径，并存储在cen.dis中
struct Node1
{
    int id;
    int dist;
    Node1(int i,int v) : id(i),dist(v) {}
    bool operator<(const Node1& other) const {
        return dist > other.dist;  // 小根堆
    }
};

//最短路径计算，作为标准值
void Dijkstra(int k)
{
    cen[k].dis[k]=0;
    priority_queue<Node1> qq;
    qq.push(Node1(k,0));
    
    while (!qq.empty())
    {
        Node1 now=qq.top();
        qq.pop();
        if (now.dist>cen[k].dis[now.id]) continue;
        for (int i=head[now.id];i!=-1;i=cnt[i].next_line)
        if (cen[k].dis[cnt[i].node]>now.dist+cnt[i].Lenth)
        {
            cen[k].dis[cnt[i].node]=now.dist+cnt[i].Lenth;
            qq.push(Node1(cnt[i].node,cen[k].dis[cnt[i].node]));
        }
    }
}


//启发式搜索部分
//启发式搜索的节点
struct Node2
{
    int id;
    int Standard_value;
    string ans;
    bool operator<(const Node2& other) const {
        return Standard_value > other.Standard_value;  // 小根堆
    }
};

//计算标准值
int conclude_standard_value(int x,int y)
{
    int value=-1;
    for (int i=1;i<=m;i++)
    if (abs(cen[i].dis[x]-cen[i].dis[y])>value)
      value=abs(cen[i].dis[x]-cen[i].dis[y]);
    return value;
}

//把十进制转化为4为十六进制的字符串方便记录规划路径
std::string Switch(int x) {
    // 确保数值在有效范围内（0-65535）
    x = x & 0xFFFF;
    std::string hexChars = "0123456789ABCDEF";
    std::string result(4, '0');  // 初始化4个'0'
    // 从低位到高位处理
    for (int i = 3; i >= 0; i--) {
        int digit = x & 0xF;  // 取最低4位
        result[i] = hexChars[digit];
        x >>= 4;  // 右移4位
    }
    return result;
}

//A*启发式搜索
string Heuristic_search(int start_node,int end_node)
{
    priority_queue<Node2> open_list;
    vector<bool> b(q+1,false);  //重复路径的标记
    Node2 new_node;
    new_node.id=start_node;
    new_node.Standard_value=conclude_standard_value(start_node,end_node);
    new_node.ans="";
    b[new_node.id]=true;
    open_list.push(new_node);
    while (!open_list.empty())
    {
        Node2 now=open_list.top();
        open_list.pop();
        if (b[now.id]==true) continue;
        b[now.id]=true;
        if (now.id==end_node) return now.ans;
        for (int i=head[now.id];i!=-1;i=cnt[i].next_line)
        {
            new_node.Standard_value-=conclude_standard_value(new_node.id,end_node);
            new_node.id=cnt[i].node;
            new_node.Standard_value+=cnt[i].Lenth+conclude_standard_value(new_node.id,end_node);
            new_node.ans=now.ans+Switch(i);
            open_list.push(new_node);
        }
    }
}

void run()
{
    ans.push_back("");
    for (int i=1;i<=t;i++)
    {
        ans.push_back(Heuristic_search(start_node[i],end_node[i]));
    }
}

public:

//输入：聚类个数，边数，中心聚点，x坐标，y坐标，聚类规模，中心点，各聚类中心编号，所属聚类编号,左节点，右节点，线段长度
//0无实际意义
A_(int q,int n,int m,int xx[],int yy[],int ww[],int cen_node[],int fa[],int mean[],int nodex[],int nodey[],int Len[])
{
    this->q=q;
    this->n=n;
    this->m=m;
    for (int i=1;i<=m;i++)
    {
        cen[i].id=cen_node[i];
        for (int j=1;j<=q;j++) cen[i].dis[j]=INT_MAX; //初始化
    }
    //输入节点
    for (int i=1;i<=q;i++)
    {
        a[i].x=xx[i];
        a[i].y=yy[i];
        a[i].w=ww[i];
        a[i].meanid=mean[i];
        a[i].father=fa[i];
    }
    //输入边
    for (int i=1;i<=m;i++) head[i]=-1;
    for (int i=1;i<=n;i++)
    {
        add_line(nodex[i],nodey[i],Len[i]);
        add_line(nodey[i],nodex[i],Len[i]);
    }

    //最短路径预处理
    for (int i=1;i<=m;i++) Dijkstra(cen[i].id);

}

A_(int t,int st_node[],int en_node[])
{
    this->t=t;
    start_node.push_back(0);
    end_node.push_back(0);
    for (int i=1;i<=t;i++)
    {
        start_node.push_back(st_node[i]);
        end_node.push_back(en_node[i]);
    }
}

void RunA_()
{
    run();
}

vector<string> getans()
{
    return ans;
    //0无意义，返回所规划的路径
}
};


int main()
{
    return 0;
}