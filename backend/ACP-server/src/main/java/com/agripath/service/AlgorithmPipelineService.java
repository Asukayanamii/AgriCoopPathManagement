package com.agripath.service;

import com.agripath.acpcommon.utils.AStarNative;
import com.agripath.acpcommon.utils.KMeansClusterUtil;
import com.agripath.acpcommon.utils.ResourceSearchNative;
import com.agripath.dto.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlgorithmPipelineService {

    public Map<String, Object> cluster(ClusterRequestDTO dto) {
        List<int[]> points = dto.getPoints();
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("points must not be empty");
        }
        int nodeNum = points.size();
        int[][] nodes = points.toArray(new int[0][]);

        int[][][] nativeResult = KMeansClusterUtil.kMeans(
                1000, 1000, nodeNum,
                dto.getSpaceCluster(), dto.getDeviation(), dto.getIterationCount(),
                nodes
        );

        if (nativeResult == null || nativeResult.length < 3) {
            throw new RuntimeException("K-means native call returned null");
        }

        int[][] clusterResult = nativeResult[0];
        int[][] centerIds = nativeResult[1];
        int clusterCount = nativeResult[2][0][0];

        Map<Integer, List<int[]>> grouped = new LinkedHashMap<>();
        for (int[] row : clusterResult) {
            int meanid = row[2];
            grouped.computeIfAbsent(meanid, k -> new ArrayList<>())
                   .add(new int[]{row[0], row[1]});
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clusterCount", clusterCount);
        result.put("clusterCenters", Arrays.stream(centerIds).map(r -> r[0]).collect(Collectors.toList()));
        result.put("clusterResult", Arrays.stream(clusterResult)
                .map(r -> new int[]{r[0], r[1], r[2]})
                .collect(Collectors.toList()));
        result.put("groupedClusters", new ArrayList<>(grouped.values()));
        return result;
    }

    public Map<String, Object> resourceSearch(ResourceSearchRequestDTO dto) {
        List<ResourceSearchRequestDTO.ResourceDTO> resources = dto.getResources();
        List<ResourceSearchRequestDTO.TargetDTO> targets = dto.getTargets();
        if (resources == null || resources.isEmpty()) {
            throw new IllegalArgumentException("resources must not be empty");
        }
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("targets must not be empty");
        }

        int k = resources.size();
        int[] rx = new int[k], ry = new int[k], rid = new int[k];
        boolean[] rstate = new boolean[k];
        for (int i = 0; i < k; i++) {
            rx[i] = resources.get(i).getX();
            ry[i] = resources.get(i).getY();
            rid[i] = resources.get(i).getId();
            rstate[i] = resources.get(i).isAvailable();
        }

        int q = targets.size();
        int[] tx = new int[q], ty = new int[q], treq = new int[q];
        for (int i = 0; i < q; i++) {
            tx[i] = targets.get(i).getX();
            ty[i] = targets.get(i).getY();
            treq[i] = targets.get(i).getRequired();
        }

        int[][] nativeResult = ResourceSearchNative.searchResources(
                dto.getMapWidth(), dto.getMapHeight(), k,
                rx, ry, rid, rstate,
                q, tx, ty, treq
        );

        List<Map<String, Object>> searchResults = new ArrayList<>();
        for (int i = 0; i < q; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("target", new int[]{targets.get(i).getX(), targets.get(i).getY()});
            item.put("required", targets.get(i).getRequired());
            int[] allocated = nativeResult[i];
            if (allocated.length == 1 && allocated[0] == -1) {
                item.put("status", "failed");
                item.put("message", "No sufficient resources found");
            } else {
                item.put("status", "success");
                item.put("allocatedCount", allocated[0]);
                List<Integer> ids = new ArrayList<>();
                for (int j = 1; j < allocated.length; j++) ids.add(allocated[j]);
                item.put("resourceIds", ids);
            }
            searchResults.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("searchResults", searchResults);
        return result;
    }

    public Map<String, Object> pathPlanning(PathPlanRequestDTO dto) {
        List<int[]> obstacles = dto.getObstacles() != null ? dto.getObstacles() : new ArrayList<>();

        int[] ox = new int[obstacles.size()];
        int[] oy = new int[obstacles.size()];
        for (int i = 0; i < obstacles.size(); i++) {
            ox[i] = obstacles.get(i)[0];
            oy[i] = obstacles.get(i)[1];
        }

        int[][] path = AStarNative.findPath(
                dto.getMapWidth(), dto.getMapHeight(),
                dto.getStartX(), dto.getStartY(),
                dto.getEndX(), dto.getEndY(),
                obstacles.size(), ox, oy,
                dto.getGridResolution()
        );

        double distance = 0;
        for (int i = 1; i < path.length; i++) {
            double dx = path[i][0] - path[i - 1][0];
            double dy = path[i][1] - path[i - 1][1];
            distance += Math.sqrt(dx * dx + dy * dy);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", path);
        result.put("distance", Math.round(distance * 100.0) / 100.0);
        result.put("pathPoints", path.length);
        return result;
    }

    public Map<String, Object> runPipeline(PipelineRequestDTO dto) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> steps = new LinkedHashMap<>();

        List<int[]> taskPoints = dto.getTaskPoints();
        List<PipelineRequestDTO.ResourceDTO> resList = dto.getResources();
        List<PipelineRequestDTO.TargetDTO> specTargets = dto.getSpecificTargets();

        int q = taskPoints != null ? taskPoints.size() : 0;
        steps.put("totalTaskPoints", q);

        if (q > 0) {
            int[][] nodes = taskPoints.toArray(new int[0][]);
            int[][][] clusterNative = KMeansClusterUtil.kMeans(
                    dto.getMapWidth(), dto.getMapHeight(), q,
                    dto.getSpaceCluster(), dto.getDeviation(), dto.getIterationCount(),
                    nodes
            );

            int[][] clusterResult = clusterNative[0];
            int clusterCount = clusterNative[2][0][0];

            Map<Integer, List<int[]>> grouped = new LinkedHashMap<>();
            for (int[] row : clusterResult) {
                int mid = row[2];
                grouped.computeIfAbsent(mid, k -> new ArrayList<>()).add(new int[]{row[0], row[1]});
            }

            List<Map<String, Object>> clusterInfo = new ArrayList<>();
            List<int[]> endpoints = new ArrayList<>();
            for (Map.Entry<Integer, List<int[]>> entry : grouped.entrySet()) {
                List<int[]> pts = entry.getValue();
                int cx = pts.stream().mapToInt(p -> p[0]).sum() / pts.size();
                int cy = pts.stream().mapToInt(p -> p[1]).sum() / pts.size();
                Map<String, Object> ci = new LinkedHashMap<>();
                ci.put("clusterId", entry.getKey());
                ci.put("pointCount", pts.size());
                ci.put("avgX", cx);
                ci.put("avgY", cy);
                ci.put("points", pts);
                clusterInfo.add(ci);
                endpoints.add(new int[]{cx, cy});
            }

            steps.put("cluster", clusterInfo);
            steps.put("endpoints", endpoints);

            if (resList != null && !resList.isEmpty()) {
                int k = resList.size();
                int[] rx = new int[k], ry = new int[k], rid = new int[k];
                boolean[] rstate = new boolean[k];
                for (int i = 0; i < k; i++) {
                    rx[i] = resList.get(i).getX();
                    ry[i] = resList.get(i).getY();
                    rid[i] = resList.get(i).getId();
                    rstate[i] = resList.get(i).isAvailable();
                }

                int epCount = endpoints.size();
                int[] epX = new int[epCount], epY = new int[epCount], epReq = new int[epCount];
                for (int i = 0; i < epCount; i++) {
                    epX[i] = endpoints.get(i)[0];
                    epY[i] = endpoints.get(i)[1];
                    epReq[i] = 1;
                }

                int[][] searchNative = ResourceSearchNative.searchResources(
                        dto.getMapWidth(), dto.getMapHeight(), k,
                        rx, ry, rid, rstate,
                        epCount, epX, epY, epReq
                );

                List<Map<String, Object>> resourceSearchInfo = new ArrayList<>();
                for (int i = 0; i < epCount; i++) {
                    Map<String, Object> si = new LinkedHashMap<>();
                    si.put("clusterId", i + 1);
                    si.put("target", endpoints.get(i));
                    int[] allocated = searchNative[i];
                    if (allocated.length == 1 && allocated[0] == -1) {
                        si.put("status", "failed");
                    } else {
                        si.put("status", "success");
                        List<Integer> ids = new ArrayList<>();
                        for (int j = 1; j < allocated.length; j++) ids.add(allocated[j]);
                        si.put("resourceIds", ids);
                    }
                    resourceSearchInfo.add(si);
                }
                steps.put("resourceSearch", resourceSearchInfo);

                if (specTargets != null && !specTargets.isEmpty()) {
                    List<Map<String, Object>> pathPlans = new ArrayList<>();
                    for (PipelineRequestDTO.TargetDTO t : specTargets) {
                        int nearestResId = -1, minDist = Integer.MAX_VALUE;
                        int srcX = 0, srcY = 0;
                        for (PipelineRequestDTO.ResourceDTO r : resList) {
                            if (!r.isAvailable()) continue;
                            int dist = Math.abs(r.getX() - t.getX()) + Math.abs(r.getY() - t.getY());
                            if (dist < minDist) {
                                minDist = dist;
                                nearestResId = r.getId();
                                srcX = r.getX();
                                srcY = r.getY();
                            }
                        }
                        int[][] path = AStarNative.findPath(
                                dto.getMapWidth(), dto.getMapHeight(),
                                srcX, srcY, t.getX(), t.getY(),
                                0, new int[0], new int[0],
                                dto.getGridResolution()
                        );
                        Map<String, Object> pi = new LinkedHashMap<>();
                        pi.put("from", new int[]{srcX, srcY});
                        pi.put("to", new int[]{t.getX(), t.getY()});
                        pi.put("resourceId", nearestResId);
                        pi.put("path", path);
                        pi.put("pathLength", path.length);
                        pathPlans.add(pi);
                    }
                    steps.put("pathPlanning", pathPlans);
                }
            }
        }

        result.put("pipelineSteps", steps);
        result.put("totalSteps", 3);
        return result;
    }
}
