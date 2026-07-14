#include <jni.h>
#include <bits/stdc++.h>
#include <algorithm>
using namespace std;

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_agripath_acpcommon_utils_ResourceSearchNative_searchResources(
    JNIEnv* env, jclass clazz,
    jint mapW, jint mapH, jint resourceCount,
    jintArray jrx, jintArray jry, jintArray jrid, jbooleanArray jrstate,
    jint targetCount, jintArray jtx, jintArray jty, jintArray jtreq)
{
    int n = (int)mapW, m = (int)mapH;
    int k = (int)resourceCount;

    // Read resource data from Java
    jint* rxArr = env->GetIntArrayElements(jrx, nullptr);
    jint* ryArr = env->GetIntArrayElements(jry, nullptr);
    jint* ridArr = env->GetIntArrayElements(jrid, nullptr);
    jboolean* rsArr = env->GetBooleanArrayElements(jrstate, nullptr);

    struct Resource { int x, y, id; bool state; };
    vector<Resource> resources(1);
    for (int i = 0; i < k; i++) {
        resources.push_back({(int)rxArr[i], (int)ryArr[i], (int)ridArr[i], rsArr[i] == JNI_TRUE});
    }

    env->ReleaseIntArrayElements(jrx, rxArr, 0);
    env->ReleaseIntArrayElements(jry, ryArr, 0);
    env->ReleaseIntArrayElements(jrid, ridArr, 0);
    env->ReleaseBooleanArrayElements(jrstate, rsArr, 0);

    sort(resources.begin() + 1, resources.end(), [](const Resource& a, const Resource& b) {
        return a.id < b.id;
    });

    int q = (int)targetCount;
    jint* txArr = env->GetIntArrayElements(jtx, nullptr);
    jint* tyArr = env->GetIntArrayElements(jty, nullptr);
    jint* treqArr = env->GetIntArrayElements(jtreq, nullptr);

    // Results: each target gets a flat array [allocCount, id1, id2, ...]
    vector<vector<int>> allResults;

    for (int ti = 1; ti <= q; ti++) {
        int targetX = (int)txArr[ti - 1];
        int targetY = (int)tyArr[ti - 1];
        int require = (int)treqArr[ti - 1];

        vector<int> cur;
        cur.push_back(INT_MAX);
        int l = 0, r = min(n, m);

        while (l <= r) {
            int mid = (l + r) / 2;
            int cnt = 0;
            for (int i = 1; i <= k; i++) {
                if (resources[i].state &&
                    resources[i].x >= targetX - mid && resources[i].x <= targetX + mid &&
                    resources[i].y >= targetY - mid && resources[i].y <= targetY + mid) {
                    cnt++;
                }
            }
            if (cnt >= require && cnt <= cur[0]) {
                cur.clear();
                cur.push_back(cnt);
                for (int i = 1; i <= k; i++) {
                    if (resources[i].state &&
                        resources[i].x >= targetX - mid && resources[i].x <= targetX + mid &&
                        resources[i].y >= targetY - mid && resources[i].y <= targetY + mid) {
                        cur.push_back(resources[i].id);
                    }
                }
            }
            if (cnt < require) l = mid + 1;
            else r = mid - 1;
        }

        if (cur[0] == INT_MAX) {
            allResults.push_back({-1});
        } else {
            vector<pair<int,int>> distList;
            for (int j = 1; j <= cur[0]; j++) {
                int rid = cur[j];
                for (auto& res : resources) {
                    if (res.id == rid) {
                        int d = abs(targetX - res.x) + abs(targetY - res.y);
                        distList.push_back({d, rid});
                        break;
                    }
                }
            }
            sort(distList.begin(), distList.end());

            vector<int> finalResult;
            finalResult.push_back(require);
            for (int j = 0; j < require && j < (int)distList.size(); j++) {
                finalResult.push_back(distList[j].second);
            }
            allResults.push_back(finalResult);
        }
    }

    env->ReleaseIntArrayElements(jtx, txArr, 0);
    env->ReleaseIntArrayElements(jty, tyArr, 0);
    env->ReleaseIntArrayElements(jtreq, treqArr, 0);

    // Convert to Java int[][]
    jclass intArrayClass = env->FindClass("[I");
    jobjectArray result2D = env->NewObjectArray((jsize)allResults.size(), intArrayClass, nullptr);

    for (size_t i = 0; i < allResults.size(); i++) {
        jintArray row = env->NewIntArray((jsize)allResults[i].size());
        env->SetIntArrayRegion(row, 0, (jsize)allResults[i].size(), (jint*)allResults[i].data());
        env->SetObjectArrayElement(result2D, (jsize)i, row);
        env->DeleteLocalRef(row);
    }

    return result2D;
}
