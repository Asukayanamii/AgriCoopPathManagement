#include <jni.h>
#include "k_means.h"
#include <vector>
#include <cstdlib>
#include <ctime>

// 映射Java的Node结构体到C++
struct JavaNode {
    int x;
    int y;
    int w;
    int father;
    int meanid;
    long long dis;
};

#ifdef __cplusplus
extern "C" {
#endif

// JNI方法命名规则：Java_包名_类名_方法名
// 包名：com.agripath.acpcommon.utils
// 类名：ClusterAlgorithmUtil
// 方法名：runKMeans

/**
 * JNI接口：执行KMeans聚类
 * 参数说明：
 * env: JNI环境指针
 * obj: 调用该方法的Java对象
 * n: 地图宽度
 * m: 地图高度
 * q: 节点总数
 * space_cluster: 每个聚类最大节点数
 * deviation: 迭代收敛阈值
 * iteration_count: 最大迭代次数
 * nodes: Java层传入的节点数组（自定义Node数组）
 * 返回值：封装聚类结果的二维数组（每行格式：聚类编号,节点ID,x,y,是否为中心）
 */
JNIEXPORT jobjectArray JNICALL Java_com_agripath_acpcommon_utils_ClusterAlgorithmUtil_runKMeans
  (JNIEnv *env, jobject obj, jint n, jint m, jint q, jint space_cluster, jint deviation, jint iteration_count, jobjectArray nodes) {

    // 1. 获取Java的Node类信息
    jclass javaNodeClass = env->FindClass("com/agripath/acpcommon/utils/ClusterAlgorithmUtil$Node");
    if (javaNodeClass == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "找不到Node类");
        return nullptr;
    }

    // 2. 获取Java Node类的字段ID
    jfieldID xField = env->GetFieldID(javaNodeClass, "x", "I");
    jfieldID yField = env->GetFieldID(javaNodeClass, "y", "I");
    jfieldID wField = env->GetFieldID(javaNodeClass, "w", "I");
    jfieldID fatherField = env->GetFieldID(javaNodeClass, "father", "I");
    jfieldID meanidField = env->GetFieldID(javaNodeClass, "meanid", "I");
    jfieldID disField = env->GetFieldID(javaNodeClass, "dis", "J");

    // 3. 转换Java节点数组到C++ Node数组
    MyClass::Node* cNodes = new MyClass::Node[q + 1]; // C++节点数组（1开始）
    for (int i = 1; i <= q; i++) {
        jobject javaNode = env->GetObjectArrayElement(nodes, i - 1); // Java数组0开始
        if (javaNode == nullptr) {
            delete[] cNodes;
            env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "节点数组元素为空");
            return nullptr;
        }

        // 读取Java Node字段值并赋值给C++ Node
        cNodes[i].x = env->GetIntField(javaNode, xField);
        cNodes[i].y = env->GetIntField(javaNode, yField);
        cNodes[i].w = env->GetIntField(javaNode, wField);
        cNodes[i].father = env->GetIntField(javaNode, fatherField);
        cNodes[i].meanid = env->GetIntField(javaNode, meanidField);
        cNodes[i].dis = env->GetLongField(javaNode, disField);

        env->DeleteLocalRef(javaNode); // 释放局部引用
    }

    // 4. 初始化并运行C++ KMeans算法
    MyClass kmeans(n, m, q, space_cluster, deviation, iteration_count, cNodes);
    kmeans.RunMyClass();

    // 5. 获取聚类结果并封装为Java二维字符串数组返回
    std::vector<MyClass::Node> resultNodes = kmeans.geta();
    std::vector<int> que = kmeans.getque();
    int tot = kmeans.gettot();

    // 5.1 创建返回的二维数组（每行是一个字符串，格式：聚类编号,节点ID,x,y,是否为中心）
    jobjectArray jResult = env->NewObjectArray(q, env->FindClass("java/lang/String"), nullptr);
    if (jResult == nullptr) {
        delete[] cNodes;
        return nullptr;
    }

    // 5.2 遍历所有节点，封装结果
    for (int i = 1; i <= q; i++) {
        MyClass::Node node = resultNodes[i];
        // 判断是否为聚类中心（father=0表示中心）
        bool isCenter = (node.father == 0);
        // 拼接结果字符串
        char buffer[128];
        snprintf(buffer, sizeof(buffer), "%d,%d,%d,%d,%d", 
                 node.meanid, i, node.x, node.y, isCenter ? 1 : 0);
        jstring jStr = env->NewStringUTF(buffer);
        env->SetObjectArrayElement(jResult, i - 1, jStr);
        env->DeleteLocalRef(jStr);
    }

    // 6. 释放资源
    delete[] cNodes;
    env->DeleteLocalRef(javaNodeClass);

    return jResult;
}

#ifdef __cplusplus
}
#endif