#include <jni.h>
#include <vector>
#include <queue>
#include <algorithm>
#include <cstdlib>
using namespace std;

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_agripath_acpcommon_utils_AStarNative_findPath(
    JNIEnv* env, jclass clazz,
    jint mapW, jint mapH,
    jint startX, jint startY, jint endX, jint endY,
    jint obstacleCount, jintArray jox, jintArray joy,
    jint gridRes)
{
    if (gridRes <= 0) gridRes = 10;
    int gw = mapW / gridRes;
    int gh = mapH / gridRes;

    jint* oxArr = obstacleCount > 0 ? env->GetIntArrayElements(jox, nullptr) : nullptr;
    jint* oyArr = obstacleCount > 0 ? env->GetIntArrayElements(joy, nullptr) : nullptr;

    vector<vector<bool>> blocked(gh, vector<bool>(gw, false));
    for (int i = 0; i < (int)obstacleCount; i++) {
        int cx = (int)oxArr[i] / gridRes;
        int cy = (int)oyArr[i] / gridRes;
        if (cx >= 0 && cx < gw && cy >= 0 && cy < gh)
            blocked[cy][cx] = true;
    }

    if (oxArr) env->ReleaseIntArrayElements(jox, oxArr, 0);
    if (oyArr) env->ReleaseIntArrayElements(joy, oyArr, 0);

    int sc = max(0, min(gw - 1, (int)startX / gridRes));
    int sr = max(0, min(gh - 1, (int)startY / gridRes));
    int ec = max(0, min(gw - 1, (int)endX / gridRes));
    int er = max(0, min(gh - 1, (int)endY / gridRes));

    if (blocked[sr][sc] || blocked[er][ec]) {
        jclass arrClass = env->FindClass("[I");
        return env->NewObjectArray(0, arrClass, nullptr);
    }

    vector<vector<int>> g(gh, vector<int>(gw, INT_MAX));
    vector<vector<pair<int,int>>> parent(gh, vector<pair<int,int>>(gw, {-1,-1}));
    g[sr][sc] = 0;

    auto cmp = [](const pair<int,pair<int,int>>& a, const pair<int,pair<int,int>>& b) {
        return a.first > b.first;
    };
    priority_queue<pair<int,pair<int,int>>, vector<pair<int,pair<int,int>>>, decltype(cmp)> pq(cmp);
    pq.push({abs(sc - ec) + abs(sr - er), {sr, sc}});

    int dr[] = {-1, 1, 0, 0};
    int dc[] = {0, 0, -1, 1};
    bool found = false;

    while (!pq.empty()) {
        auto cur = pq.top(); pq.pop();
        int r = cur.second.first, c = cur.second.second;
        if (r == er && c == ec) { found = true; break; }
        for (int d = 0; d < 4; d++) {
            int nr = r + dr[d], nc = c + dc[d];
            if (nr >= 0 && nr < gh && nc >= 0 && nc < gw && !blocked[nr][nc]) {
                int ng = g[r][c] + gridRes;
                if (ng < g[nr][nc]) {
                    g[nr][nc] = ng;
                    parent[nr][nc] = {r, c};
                    int h = abs(nc - ec) + abs(nr - er);
                    pq.push({ng + h * gridRes, {nr, nc}});
                }
            }
        }
    }

    vector<pair<int,int>> path;
    if (found) {
        int r = er, c = ec;
        while (r != -1) {
            path.push_back({r, c});
            auto p = parent[r][c];
            r = p.first;
            c = p.second;
        }
        reverse(path.begin(), path.end());
    } else {
        path.push_back({sr, sc});
    }

    jclass intArrClass = env->FindClass("[I");
    jobjectArray result = env->NewObjectArray((jsize)path.size(), intArrClass, nullptr);
    int half = gridRes / 2;

    for (size_t i = 0; i < path.size(); i++) {
        jintArray row = env->NewIntArray(2);
        jint data[2] = {
            (jint)(path[i].second * gridRes + half),
            (jint)(path[i].first * gridRes + half)
        };
        env->SetIntArrayRegion(row, 0, 2, data);
        env->SetObjectArrayElement(result, (jsize)i, row);
        env->DeleteLocalRef(row);
    }

    return result;
}
