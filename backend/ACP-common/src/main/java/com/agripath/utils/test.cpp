#include<bits/stdc++.h>
using namespace std;
class MyClass
{
private:
    int n,m,sum;

public:
    void setM(int m)
    {
        this->m=m;
    }
    MyClass(int a,int b)
    {
        this->n=a;
        this->m=b;
    }
    void runAdd()
    {
        this->sum=m+n;
    }
    int getSum()
    {
        return sum;
    }
};

int main()
{
    int x=1;
    int y=2;
    MyClass myclass1(x,y);
    myclass1.runAdd();
    cout<<myclass1.getSum();

}
