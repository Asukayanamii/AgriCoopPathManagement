#include<bits/stdc++.h>
using namespace std;

class Resource_research
{
private:

//地图长宽，资源数，请求数
int n,m,k,q;
vector<vector<int>> ans; 

struct Resource
{
    int x;
    int y;
	int id;
	bool state;
};
vector<Resource> resource;

bool cmp(const Resource a,const Resource b)
{
	return a.id<b.id;
}

int find(int x,int y,int l)
{
	int s=0;
	for (int i=1;i<=k;i++)
	  if (resource[i].x>=x-l && resource[i].x<=x+l && resource[i].y>=y-l && resource[i].y<=y+l && resource[i].state==true) s++;
	return s;
}

struct node
{
	int w;
	int id;
};

bool cmp1(const node a,const node b)
{
	return a.w<b.w;
}

void run(int i,int target_x,int target_y,int requir)
{
	int l=0,r=min(n,m);
	ans.push_back({INT_MAX});
	while (l<=r)
	{
		int mid=(l+r)/2;
		int now=find(target_x,target_y,mid);
		if (now>=requir && now<=ans[i][0])
		{
			ans[i].clear();
			ans[i].push_back(now);
			for (int j=1;j<=k;j++)
			  if (resource[j].x>=target_x-l && resource[i].x<=target_x+l && resource[i].y>=target_y-l && resource[i].y<=target_y+l && resource[i].state==true)
			      ans[i].push_back(resource[j].id);
		}
	}
	if (ans[i][0]=INT_MAX)
	{
		ans[i][0]=-1;
		return;
	}
	vector<node> h;
	for (int j=1;j<=ans[i][0];j++)
	{
		node s;
		s.w=abs(target_x-resource[ans[i][j]].x)+abs(target_y-resource[ans[i][j]].y);
		s.id=resource[ans[i][j]].id;
		h.push_back(s);
	}
	sort(h.begin(),h.end(),cmp1);

	ans[i][0]=requir;
	for (int j=1;j<=requir;j++) ans[i][j]=h[j-1].id;
}

public:

Resource_research(int n,int m,int k,int x[],int y[],int id[],bool state[])
{
	this->n=n;
	this->m=m;
	this->k=k;
	Resource node;
	resource.push_back(node);
	for (int i=1;i<=k;i++)
	{
		node.x=x[i];
		node.y=y[i];
		node.id=id[i];
		node.state=state[i];
	}
	sort(resource.begin()+1,resource.end(),cmp);
}

Resource_research(int q,int target_x[],int target_y[],int requir[])
{
	this->q=q;
	ans.clear();
	ans.push_back({});
	for (int i=1;i<=q;i++) run(i,target_x[i],target_y[i],requir[i]);
}

vector<vector<int>> getans()
{
	return ans;
	//每个请求返回一个数组，0位置存储k,表示包含的资源个数,后k个数表示编号，其余多余元素为无效元素
	//当第一个数为-1时，表示在场所存在资源无法满足该请求
}

};

int main()
{
    return 0;
}