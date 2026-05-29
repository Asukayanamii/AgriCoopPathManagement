#include <jni.h>
#include "k_means_cluster.h"
#include <vector>
#include <utility>
#include <string> // 新增：用于异常信息拼接
#include "ClusterAlgorithm.h" // 引入之前封装的聚类算法头文件

// 注意：函数名格式规则：Java_包名_类名_方法名
// 包名：com.agripath.acpcommon.utils
// 类名：KMeansClusterUtil
// 方法名：kMeans
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_agripath_acpcommon_utils_KMeansClusterUtil_kMeans(
        JNIEnv* env,
        jclass clazz,
        jint Long,          // 地图长度（Java 入参）
        jint wide,          // 地图宽度
        jint nodeNum,       // 节点总数
        jint minNodeNumForEachCluster, // 每个聚类最小节点数
        jint deviation,     // 误差阈值
        jint maxIterationCount, // 最大迭代次数
        jobjectArray nodes  // 核心修正：从 jintArray 改为 jobjectArray（对应 int[][]）
) {
    // -------------------------- 步骤1：解析 Java 入参，转换为 C++ 数据结构 --------------------------
    // 1.1 校验数组非空
    if (nodes == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "节点坐标数组为空");
        return nullptr;
    }

    // 1.2 提取二维数组长度（节点数）
    jsize nodesLength = env->GetArrayLength(nodes);
    if (nodesLength != nodeNum) {
        std::string errMsg = "节点数组长度(" + std::to_string(nodesLength) + ")与指定节点数(" + std::to_string(nodeNum) + ")不匹配";
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), errMsg.c_str());
        return nullptr;
    }

    // 1.3 转换为 C++ vector<pair<int, int>> 格式（核心修正：解析二维数组）
    std::vector<std::pair<int, int>> cppNodes;
    for (int i = 0; i < nodesLength; i++) {
        // 提取第i行的一维数组（int[]）
        jintArray rowArray = static_cast<jintArray>(env->GetObjectArrayElement(nodes, i));
        if (rowArray == nullptr) {
            std::string errMsg = "第" + std::to_string(i) + "行节点坐标为空";
            env->ThrowNew(env->FindClass("java/lang/NullPointerException"), errMsg.c_str());
            return nullptr;
        }

        // 校验行长度（必须是2：x和y）
        jsize rowLen = env->GetArrayLength(rowArray);
        if (rowLen != 2) {
            std::string errMsg = "第" + std::to_string(i) + "行坐标长度错误，必须为2（当前：" + std::to_string(rowLen) + "）";
            env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), errMsg.c_str());
            env->DeleteLocalRef(rowArray); // 释放引用
            return nullptr;
        }

        // 提取行数据
        jint rowData[2];
        env->GetIntArrayRegion(rowArray, 0, 2, rowData);
        
        // 存入C++容器
        int x = static_cast<int>(rowData[0]);
        int y = static_cast<int>(rowData[1]);
        cppNodes.emplace_back(x, y);

        // 释放局部引用（关键：避免内存泄漏）
        env->DeleteLocalRef(rowArray);
    }

    // -------------------------- 步骤2：实例化并执行 C++ 聚类算法 --------------------------
    try {
        // 2.1 创建聚类算法实例
        ClusterAlgorithm clusterAlg(
                static_cast<int>(Long),
                static_cast<int>(wide),
                static_cast<int>(nodeNum),
                static_cast<int>(minNodeNumForEachCluster),
                static_cast<int>(deviation),
                static_cast<int>(maxIterationCount),
                cppNodes
        );

        // 2.2 执行聚类
        clusterAlg.run();

        // 2.3 获取聚类结果
        std::vector<std::vector<int>> clusterResult = clusterAlg.getClusterResult(); // 节点结果：[x,y,meanid]
        std::vector<int> clusterCenters = clusterAlg.getClusterCenters();           // 聚类中心编号
        int clusterCount = clusterAlg.getClusterCount();                            // 聚类总数

        // -------------------------- 步骤3：转换结果为 Java 三维数组 --------------------------
        // Java 返回值格式：int[][][] 
        // - 维度0：结果类型（0=节点聚类结果，1=聚类中心编号，2=聚类总数）
        // - 维度1：具体数据行
        // - 维度2：每行数据列

        // 3.1 创建 Java 三维数组的外层数组（长度3）
        jclass intArrayClass = env->FindClass("[I");   // int[] 类（用于构建二维数组）
        jclass intArray2DClass = env->FindClass("[[I"); // int[][] 类（用于构建三维数组）
        jobjectArray result3D = env->NewObjectArray(3, intArray2DClass, nullptr);
        if (result3D == nullptr) {
            env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "创建三维数组失败");
            return nullptr;
        }

        // 3.2 填充维度0：节点聚类结果（int[nodeNum][3]）
        jobjectArray nodeResult2D = env->NewObjectArray(clusterResult.size(), intArrayClass, nullptr);
        for (int i = 0; i < clusterResult.size(); i++) {
            jintArray row = env->NewIntArray(3);
            jint rowData[3] = {
                    static_cast<jint>(clusterResult[i][0]),  // x
                    static_cast<jint>(clusterResult[i][1]),  // y
                    static_cast<jint>(clusterResult[i][2])   // meanid
            };
            env->SetIntArrayRegion(row, 0, 3, rowData);
            env->SetObjectArrayElement(nodeResult2D, i, row);
            env->DeleteLocalRef(row); // 释放局部引用
        }
        env->SetObjectArrayElement(result3D, 0, nodeResult2D);
        env->DeleteLocalRef(nodeResult2D); // 释放局部引用

        // 3.3 填充维度1：聚类中心编号（int[clusterCount][1]）
        jobjectArray centersResult2D = env->NewObjectArray(clusterCenters.size(), intArrayClass, nullptr);
        for (int i = 0; i < clusterCenters.size(); i++) {
            jintArray row = env->NewIntArray(1);
            jint rowData[1] = {static_cast<jint>(clusterCenters[i])};
            env->SetIntArrayRegion(row, 0, 1, rowData);
            env->SetObjectArrayElement(centersResult2D, i, row);
            env->DeleteLocalRef(row); // 释放局部引用
        }
        env->SetObjectArrayElement(result3D, 1, centersResult2D);
        env->DeleteLocalRef(centersResult2D); // 释放局部引用

        // 3.4 填充维度2：聚类总数（int[1][1]）
        jobjectArray countResult2D = env->NewObjectArray(1, intArrayClass, nullptr);
        jintArray countRow = env->NewIntArray(1);
        jint countData[1] = {static_cast<jint>(clusterCount)};
        env->SetIntArrayRegion(countRow, 0, 1, countData);
        env->SetObjectArrayElement(countResult2D, 0, countRow);
        env->SetObjectArrayElement(result3D, 2, countResult2D);
        env->DeleteLocalRef(countRow);       // 释放局部引用
        env->DeleteLocalRef(countResult2D);  // 释放局部引用

        // 3.5 释放 int[] 类引用
        env->DeleteLocalRef(intArrayClass);

        // -------------------------- 步骤4：返回 Java 三维数组 --------------------------
        return result3D;

    } catch (const std::exception& e) {
        // 捕获 C++ 异常，转换为 Java 异常
        std::string errMsg = "C++算法执行异常：" + std::string(e.what());
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), errMsg.c_str());
        return nullptr;
    } catch (...) {
        // 捕获未知异常
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "C++ 聚类算法执行未知异常");
        return nullptr;
    }
}