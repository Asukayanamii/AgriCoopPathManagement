/**
 * @file ACPJNIAdapter.cpp
 * @brief JNI 适配器：桥接 Java 与 UAVLibrary.dll
 *        运行时通过 LoadLibrary 加载 UAVLibrary.dll
 */
#include <jni.h>
#include <windows.h>
#include <cstring>
#include <cstdlib>
#include <vector>
#include <string>
using namespace std;

/* ==================== DLL 函数指针类型 ==================== */

// CarPathResult matches UAVLibrary.h
struct CarPathResult {
    char** paths;
    int count;
};

typedef void* (*FN_create_graph)(int, const int*, const int*, const int*, int);
typedef void* (*FN_create_standmap)(const int*, const int*, int, void*);
typedef void  (*FN_free_graph)(void*);
typedef void  (*FN_free_standmap)(void*);
typedef const char* (*FN_encode_id)(int);
typedef void  (*FN_encode_nodes)(const int*, int, char*);
typedef void  (*FN_encode_edges)(const int*, int, char*);
typedef void  (*FN_kmeans)(const int*, const int*, int, int, int, int, int*, int*);
typedef CarPathResult (*FN_car_planning)(
    const int*, const int*, int,
    const int*, const int*, const int*, const int*, int,
    const int*, const int*, const char* const*, int,
    void*, void*);
typedef void  (*FN_free_car_path)(CarPathResult*);
typedef const char* (*FN_tsp_plan)(const int*, const int*, const char* const*, int);
typedef int*   (*FN_export_standmap)(void*, int*);
typedef void*  (*FN_create_standmap_from_data)(const int*, int);
typedef void   (*FN_free_array)(void*);

/* ==================== DLL 句柄与函数指针 ==================== */

static HMODULE g_dll = nullptr;
static FN_create_graph       fn_create_graph       = nullptr;
static FN_create_standmap    fn_create_standmap    = nullptr;
static FN_free_graph         fn_free_graph         = nullptr;
static FN_free_standmap      fn_free_standmap      = nullptr;
static FN_encode_id          fn_encode_id          = nullptr;
static FN_encode_nodes       fn_encode_nodes       = nullptr;
static FN_encode_edges       fn_encode_edges       = nullptr;
static FN_kmeans             fn_kmeans             = nullptr;
static FN_car_planning       fn_car_planning       = nullptr;
static FN_free_car_path      fn_free_car_path      = nullptr;
static FN_tsp_plan           fn_tsp_plan           = nullptr;
static FN_export_standmap    fn_export_standmap    = nullptr;
static FN_create_standmap_from_data fn_create_standmap_from_data = nullptr;
static FN_free_array         fn_free_array         = nullptr;

/* ==================== JNI_OnLoad：加载 DLL ==================== */

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    // First try: DLL already loaded by System.load()
    g_dll = GetModuleHandleA("UAVLibrary.dll");

    // Second try: load from same directory as this adapter
    if (!g_dll) {
        char selfPath[MAX_PATH];
        HMODULE hm = nullptr;
        if (GetModuleHandleExA(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS |
                               GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                               (LPCSTR)&JNI_OnLoad, &hm) && hm) {
            GetModuleFileNameA(hm, selfPath, MAX_PATH);
            string dir(selfPath);
            size_t pos = dir.find_last_of('\\');
            if (pos != string::npos) {
                dir = dir.substr(0, pos + 1) + "UAVLibrary.dll";
                g_dll = LoadLibraryA(dir.c_str());
            }
        }
    }

    if (!g_dll) return JNI_ERR;

    fn_create_graph    = (FN_create_graph)   GetProcAddress(g_dll, "create_graph");
    fn_create_standmap = (FN_create_standmap) GetProcAddress(g_dll, "create_standmap");
    fn_free_graph      = (FN_free_graph)     GetProcAddress(g_dll, "free_graph");
    fn_free_standmap   = (FN_free_standmap)  GetProcAddress(g_dll, "free_standmap");
    fn_encode_id       = (FN_encode_id)      GetProcAddress(g_dll, "encode_id");
    fn_encode_nodes    = (FN_encode_nodes)   GetProcAddress(g_dll, "encode_nodes");
    fn_encode_edges    = (FN_encode_edges)   GetProcAddress(g_dll, "encode_edges");
    fn_kmeans          = (FN_kmeans)         GetProcAddress(g_dll, "kmeans");
    fn_car_planning    = (FN_car_planning)   GetProcAddress(g_dll, "car_planning");
    fn_free_car_path   = (FN_free_car_path)  GetProcAddress(g_dll, "free_car_path");
    fn_tsp_plan        = (FN_tsp_plan)       GetProcAddress(g_dll, "tsp_plan");
    fn_export_standmap = (FN_export_standmap)GetProcAddress(g_dll, "export_standmap");
    fn_create_standmap_from_data = (FN_create_standmap_from_data)GetProcAddress(g_dll, "create_standmap_from_data");
    fn_free_array      = (FN_free_array)     GetProcAddress(g_dll, "free_array");

    if (!fn_create_graph || !fn_create_standmap || !fn_free_graph || !fn_free_standmap)
        return JNI_ERR;

    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    if (g_dll) { FreeLibrary(g_dll); g_dll = nullptr; }
}

/* ==================== 辅助函数 ==================== */

static jstring toJavaString(JNIEnv* env, const char* s) {
    return env->NewStringUTF(s ? s : "");
}

/* ==================== JNI 方法 ==================== */

extern "C" {

/* --- createGraph --- */
JNIEXPORT jlong JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_createGraph(
    JNIEnv* env, jclass clazz,
    jint nodeCount, jintArray jfrom, jintArray jto, jintArray jweight, jint edgeCount)
{
    jint* from    = env->GetIntArrayElements(jfrom, nullptr);
    jint* to      = env->GetIntArrayElements(jto, nullptr);
    jint* weight  = env->GetIntArrayElements(jweight, nullptr);
    void* h = fn_create_graph(nodeCount, from, to, weight, edgeCount);
    env->ReleaseIntArrayElements(jfrom, from, 0);
    env->ReleaseIntArrayElements(jto, to, 0);
    env->ReleaseIntArrayElements(jweight, weight, 0);
    return (jlong)(intptr_t)h;
}

/* --- createStandmap --- */
JNIEXPORT jlong JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_createStandmap(
    JNIEnv* env, jclass clazz,
    jintArray jpx, jintArray jpy, jint n, jlong graph)
{
    jint* px = env->GetIntArrayElements(jpx, nullptr);
    jint* py = env->GetIntArrayElements(jpy, nullptr);
    void* h = fn_create_standmap(px, py, n, (void*)(intptr_t)graph);
    env->ReleaseIntArrayElements(jpx, px, 0);
    env->ReleaseIntArrayElements(jpy, py, 0);
    return (jlong)(intptr_t)h;
}

/* --- freeGraph --- */
JNIEXPORT void JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_freeGraph(
    JNIEnv* env, jclass clazz, jlong graph)
{
    fn_free_graph((void*)(intptr_t)graph);
}

/* --- freeStandmap --- */
JNIEXPORT void JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_freeStandmap(
    JNIEnv* env, jclass clazz, jlong sm)
{
    fn_free_standmap((void*)(intptr_t)sm);
}

/* --- encodeId --- */
JNIEXPORT jstring JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_encodeId(
    JNIEnv* env, jclass clazz, jint id)
{
    return toJavaString(env, fn_encode_id(id));
}

/* --- encodeNodes --- */
JNIEXPORT jobjectArray JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_encodeNodes(
    JNIEnv* env, jclass clazz, jintArray jids)
{
    jsize n = env->GetArrayLength(jids);
    if (n <= 0) return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);

    jint* ids = env->GetIntArrayElements(jids, nullptr);
    char* buf = (char*)malloc(n * 5);
    fn_encode_nodes(ids, (int)n, buf);
    env->ReleaseIntArrayElements(jids, ids, 0);

    jobjectArray result = env->NewObjectArray(n, env->FindClass("java/lang/String"), nullptr);
    for (int i = 0; i < n; i++)
        env->SetObjectArrayElement(result, i, toJavaString(env, buf + i * 5));
    free(buf);
    return result;
}

/* --- encodeEdges --- */
JNIEXPORT jobjectArray JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_encodeEdges(
    JNIEnv* env, jclass clazz, jintArray jids)
{
    jsize n = env->GetArrayLength(jids);
    if (n <= 0) return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);

    jint* ids = env->GetIntArrayElements(jids, nullptr);
    char* buf = (char*)malloc(n * 5);
    fn_encode_edges(ids, (int)n, buf);
    env->ReleaseIntArrayElements(jids, ids, 0);

    jobjectArray result = env->NewObjectArray(n, env->FindClass("java/lang/String"), nullptr);
    for (int i = 0; i < n; i++)
        env->SetObjectArrayElement(result, i, toJavaString(env, buf + i * 5));
    free(buf);
    return result;
}

/* --- kmeans --- */
JNIEXPORT jobjectArray JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_kmeans(
    JNIEnv* env, jclass clazz,
    jintArray jpx, jintArray jpy, jint n,
    jint space, jint deviation, jint maxIter)
{
    jint* px = env->GetIntArrayElements(jpx, nullptr);
    jint* py = env->GetIntArrayElements(jpy, nullptr);

    int* cluster_ids = (int*)calloc(n, sizeof(int));
    int* center_ids  = (int*)calloc(n, sizeof(int));
    fn_kmeans(px, py, n, space, deviation, maxIter, cluster_ids, center_ids);

    env->ReleaseIntArrayElements(jpx, px, 0);
    env->ReleaseIntArrayElements(jpy, py, 0);

    // Return int[2][n]: [0]=clusterIds, [1]=centerIds
    jclass intArrClass = env->FindClass("[I");
    jobjectArray result = env->NewObjectArray(2, intArrClass, nullptr);

    jintArray jCluster = env->NewIntArray(n);
    env->SetIntArrayRegion(jCluster, 0, n, (jint*)cluster_ids);
    env->SetObjectArrayElement(result, 0, jCluster);
    env->DeleteLocalRef(jCluster);

    jintArray jCenter = env->NewIntArray(n);
    env->SetIntArrayRegion(jCenter, 0, n, (jint*)center_ids);
    env->SetObjectArrayElement(result, 1, jCenter);
    env->DeleteLocalRef(jCenter);

    free(cluster_ids);
    free(center_ids);
    return result;
}

/* --- carPlanning --- */
JNIEXPORT jobjectArray JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_carPlanning(
    JNIEnv* env, jclass clazz,
    jintArray jroadPx, jintArray jroadPy, jint roadCount,
    jintArray jcarX, jintArray jcarY, jintArray jcarState, jintArray jcarBelongNode, jint carCount,
    jintArray jtaskPx, jintArray jtaskPy, jobjectArray jtaskCodes, jint taskCount,
    jlong graph, jlong sm)
{
    jint* roadPx = env->GetIntArrayElements(jroadPx, nullptr);
    jint* roadPy = env->GetIntArrayElements(jroadPy, nullptr);
    jint* carX = env->GetIntArrayElements(jcarX, nullptr);
    jint* carY = env->GetIntArrayElements(jcarY, nullptr);
    jint* carState = env->GetIntArrayElements(jcarState, nullptr);
    jint* carBelongNode = env->GetIntArrayElements(jcarBelongNode, nullptr);
    jint* taskPx = env->GetIntArrayElements(jtaskPx, nullptr);
    jint* taskPy = env->GetIntArrayElements(jtaskPy, nullptr);

    // Build task_codes C array
    const char** taskCodes = nullptr;
    if (jtaskCodes && taskCount > 0) {
        taskCodes = (const char**)malloc(taskCount * sizeof(char*));
        for (int i = 0; i < taskCount; i++) {
            jstring s = (jstring)env->GetObjectArrayElement(jtaskCodes, i);
            const char* cs = s ? env->GetStringUTFChars(s, nullptr) : "";
            taskCodes[i] = cs;
        }
    }

    CarPathResult r = fn_car_planning(
        roadPx, roadPy, roadCount,
        carX, carY, carState, carBelongNode, carCount,
        taskPx, taskPy, taskCodes, taskCount,
        (void*)(intptr_t)graph, (void*)(intptr_t)sm
    );

    // Cleanup input arrays
    env->ReleaseIntArrayElements(jroadPx, roadPx, 0);
    env->ReleaseIntArrayElements(jroadPy, roadPy, 0);
    env->ReleaseIntArrayElements(jcarX, carX, 0);
    env->ReleaseIntArrayElements(jcarY, carY, 0);
    env->ReleaseIntArrayElements(jcarState, carState, 0);
    env->ReleaseIntArrayElements(jcarBelongNode, carBelongNode, 0);
    env->ReleaseIntArrayElements(jtaskPx, taskPx, 0);
    env->ReleaseIntArrayElements(jtaskPy, taskPy, 0);
    for (int i = 0; i < taskCount; i++)
        if (taskCodes && taskCodes[i]) env->ReleaseStringUTFChars((jstring)env->GetObjectArrayElement(jtaskCodes, i), taskCodes[i]);
    free(taskCodes);

    // Build result String[]
    jobjectArray result = env->NewObjectArray(r.count, env->FindClass("java/lang/String"), nullptr);
    for (int i = 0; i < r.count; i++)
        env->SetObjectArrayElement(result, i, toJavaString(env, r.paths[i]));

    fn_free_car_path(&r);
    return result;
}

/* --- tspPlan --- */
JNIEXPORT jstring JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_tspPlan(
    JNIEnv* env, jclass clazz,
    jintArray jpx, jintArray jpy, jobjectArray jcodes, jint n)
{
    jint* px = env->GetIntArrayElements(jpx, nullptr);
    jint* py = env->GetIntArrayElements(jpy, nullptr);

    const char** codes = nullptr;
    int total = n + 1;
    if (jcodes && total > 0) {
        codes = (const char**)malloc(total * sizeof(char*));
        for (int i = 0; i < total; i++) {
            jstring s = (jstring)env->GetObjectArrayElement(jcodes, i);
            codes[i] = s ? env->GetStringUTFChars(s, nullptr) : "";
        }
    }

    const char* path = fn_tsp_plan(px, py, codes, n);

    env->ReleaseIntArrayElements(jpx, px, 0);
    env->ReleaseIntArrayElements(jpy, py, 0);
    for (int i = 0; i < total; i++)
        if (codes && codes[i]) env->ReleaseStringUTFChars((jstring)env->GetObjectArrayElement(jcodes, i), codes[i]);
    free(codes);

    return toJavaString(env, path);
}

/* --- exportStandmap --- */
JNIEXPORT jintArray JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_exportStandmap(
    JNIEnv* env, jclass clazz, jlong sm)
{
    int n = 0;
    int* data = fn_export_standmap((void*)(intptr_t)sm, &n);
    if (!data || n <= 0) return nullptr;

    jintArray result = env->NewIntArray(n * n);
    env->SetIntArrayRegion(result, 0, n * n, (jint*)data);
    fn_free_array(data);
    return result;
}

/* --- createStandmapFromData --- */
JNIEXPORT jlong JNICALL Java_com_agripath_acpcommon_utils_UAVLibraryNative_createStandmapFromData(
    JNIEnv* env, jclass clazz, jintArray jdata, jint n)
{
    jint* data = env->GetIntArrayElements(jdata, nullptr);
    void* h = fn_create_standmap_from_data(data, n);
    env->ReleaseIntArrayElements(jdata, data, 0);
    return (jlong)(intptr_t)h;
}

} // extern "C"
