package com.agripath.service;

import com.agripath.acpcommon.utils.UAVLibraryNative;
import com.agripath.entity.*;
import com.agripath.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExecuteService {

    @Autowired
    private ClusterPriorityMapper clusterPriorityMapper;
    @Autowired
    private RoadNodeMapper roadNodeMapper;
    @Autowired
    private RoadEdgeMapper roadEdgeMapper;
    @Autowired
    private StandmapMapper standmapMapper;
    @Autowired
    private ResourceMapper resourceMapper;
    @Autowired
    private TaskPointMapper taskPointMapper;
    @Autowired
    private TaskClusterMapper taskClusterMapper;

    /**
     * 执行下一个优先级最高的未处理簇。
     * 内部：load 数据 → create_graph → create_standmap → car_planning
     */
    @Transactional
    public Map<String, Object> executeNext(Long mapId) {
        // 1. Find next undone cluster
        ClusterPriority cp = clusterPriorityMapper.getNextUndone(mapId);
        if (cp == null) throw new IllegalStateException("All clusters done");

        // 2. Load road network for this map
        List<RoadNode> nodes = roadNodeMapper.getByMapId(mapId);
        List<RoadEdge> edges = roadEdgeMapper.getByMapId(mapId);
        if (nodes.isEmpty() || edges.isEmpty())
            throw new IllegalStateException("Road network not built for map " + mapId);

        int nodeCount = nodes.size();
        int edgeCount = edges.size();

        // 3. Build graph and standmap via DLL
        int[] from = new int[edgeCount];
        int[] to = new int[edgeCount];
        int[] weight = new int[edgeCount];
        int[] px = new int[nodeCount];
        int[] py = new int[nodeCount];
        String[] roadCodes = new String[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            px[i] = (int)Math.round(nodes.get(i).getX());
            py[i] = (int)Math.round(nodes.get(i).getY());
            roadCodes[i] = nodes.get(i).getCode();
        }
        for (int i = 0; i < edgeCount; i++) {
            from[i] = edges.get(i).getFromNode().intValue() - 1;
            to[i] = edges.get(i).getToNode().intValue() - 1;
            weight[i] = (int)Math.round(edges.get(i).getWeight());
        }

        long graph = UAVLibraryNative.createGraph(nodeCount, from, to, weight, edgeCount);
        if (graph == 0) throw new RuntimeException("create_graph failed");

        // Try to load standmap from DB; fall back to DLL computation
        long sm = 0;
        List<StandmapEntry> smEntries = standmapMapper.getByMapId(mapId);
        if (!smEntries.isEmpty() && smEntries.size() >= nodeCount * nodeCount) {
            int[] smData = new int[nodeCount * nodeCount];
            for (StandmapEntry e : smEntries) {
                int idx = e.getCenterNode() * nodeCount + e.getTargetNode();
                if (idx >= 0 && idx < smData.length)
                    smData[idx] = (int)Math.round(e.getDistance());
            }
            sm = UAVLibraryNative.createStandmapFromData(smData, nodeCount);
        }
        if (sm == 0) {
            sm = UAVLibraryNative.createStandmap(px, py, nodeCount, graph);
            if (sm == 0) { UAVLibraryNative.freeGraph(graph); throw new RuntimeException("create_standmap failed"); }
        }

        // 4. Load resources (cars) for this map
        List<Resource> cars = resourceMapper.getByMapId(mapId);
        if (cars.isEmpty()) { UAVLibraryNative.freeStandmap(sm); UAVLibraryNative.freeGraph(graph);
            throw new IllegalStateException("No cars available"); }

        int carCount = cars.size();
        int[] carX = new int[carCount];
        int[] carY = new int[carCount];
        int[] carState = new int[carCount];
        int[] carBelongNode = new int[carCount];
        for (int i = 0; i < carCount; i++) {
            carX[i] = (int)Math.round(cars.get(i).getX());
            carY[i] = (int)Math.round(cars.get(i).getY());
            carState[i] = cars.get(i).getState();
            carBelongNode[i] = cars.get(i).getBelongNode() != null ? cars.get(i).getBelongNode().intValue() - 1 : 0;
        }

        // 5. Load task points for this cluster
        List<TaskCluster> taskClusters = taskClusterMapper.getByClusterId(mapId, cp.getClusterId());
        List<TaskPoint> allTasks = taskPointMapper.getByMapId(mapId);
        Map<Long, TaskPoint> taskMap = allTasks.stream().collect(Collectors.toMap(TaskPoint::getId, t -> t));

        List<TaskPoint> clusterTasks = new ArrayList<>();
        for (TaskCluster tc : taskClusters) {
            TaskPoint tp = taskMap.get(tc.getTaskId());
            if (tp != null) clusterTasks.add(tp);
        }
        if (clusterTasks.isEmpty()) { cleanup(graph, sm);
            throw new IllegalStateException("No task points in cluster " + cp.getClusterId()); }

        int taskCount = clusterTasks.size();
        int[] taskPx = new int[taskCount];
        int[] taskPy = new int[taskCount];
        String[] taskCodes = new String[taskCount];
        for (int i = 0; i < taskCount; i++) {
            taskPx[i] = (int)Math.round(clusterTasks.get(i).getX());
            taskPy[i] = (int)Math.round(clusterTasks.get(i).getY());
            taskCodes[i] = clusterTasks.get(i).getCode();
        }

        // 6. Call car_planning
        // roadPx/roadPy = all road node coordinates (for best_endpoint selection)
        int[] roadPx = px;
        int[] roadPy = py;
        int roadCount = nodeCount;

        String[] carPaths = UAVLibraryNative.carPlanning(
                roadPx, roadPy, roadCount,
                carX, carY, carState, carBelongNode, carCount,
                taskPx, taskPy, taskCodes, taskCount,
                graph, sm
        );

        cleanup(graph, sm);

        // 7. Build response
        List<Map<String, Object>> pathList = new ArrayList<>();
        for (String path : carPaths) {
            Map<String, Object> item = new LinkedHashMap<>();
            // Format: "小车ID::路径编码"
            int sep = path.indexOf("::");
            if (sep > 0) {
                item.put("carId", path.substring(0, sep));
                item.put("pathCode", path.substring(sep + 2));
            } else {
                item.put("carId", "?");
                item.put("pathCode", path);
            }
            pathList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clusterId", cp.getClusterId());
        result.put("carPaths", pathList);
        return result;
    }

    /**
     * 小车到达目标岔路口 → 执行 TSP 规划无人机路径
     */
    public Map<String, Object> carArrived(Long mapId, Integer clusterId) {
        List<TaskCluster> taskClusters = taskClusterMapper.getByClusterId(mapId, clusterId);
        List<TaskPoint> allTasks = taskPointMapper.getByMapId(mapId);
        Map<Long, TaskPoint> taskMap = allTasks.stream().collect(Collectors.toMap(TaskPoint::getId, t -> t));

        List<TaskPoint> clusterTasks = new ArrayList<>();
        for (TaskCluster tc : taskClusters) {
            TaskPoint tp = taskMap.get(tc.getTaskId());
            if (tp != null) clusterTasks.add(tp);
        }
        if (clusterTasks.isEmpty()) throw new IllegalStateException("No tasks in cluster " + clusterId);
        if (clusterTasks.size() > 20)
            throw new IllegalStateException("TSP only supports n ≤ 20");

        // px[0], py[0] = cluster center (drone release point)
        ClusterPriority cp = clusterPriorityMapper.getNextUndone(mapId);
        int n = clusterTasks.size();
        int total = n + 1;
        int[] px = new int[total];
        int[] py = new int[total];
        String[] codes = new String[total];

        // Start point (cluster center)
        px[0] = (int)Math.round(cp.getCenterX());
        py[0] = (int)Math.round(cp.getCenterY());
        codes[0] = "0000";

        // Task points
        for (int i = 0; i < n; i++) {
            px[i + 1] = (int)Math.round(clusterTasks.get(i).getX());
            py[i + 1] = (int)Math.round(clusterTasks.get(i).getY());
            codes[i + 1] = clusterTasks.get(i).getCode();
        }

        String route = UAVLibraryNative.tspPlan(px, py, codes, n);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clusterId", clusterId);
        result.put("droneRoute", route != null ? route : "");
        return result;
    }

    /**
     * 无人机完成当前簇 → 标记 done，释放车辆
     */
    @Transactional
    public Map<String, Object> droneDone(Long mapId, Integer clusterId) {
        clusterPriorityMapper.updateDone(clusterId, 1);

        // Release all cars for this map
        List<Resource> cars = resourceMapper.getByMapId(mapId);
        for (Resource car : cars) {
            resourceMapper.updateState(car.getId(), 1);
        }

        return getProgress(mapId);
    }

    public Map<String, Object> getProgress(Long mapId) {
        List<ClusterPriority> all = clusterPriorityMapper.getByMapIdOrdered(mapId);
        long done = all.stream().filter(p -> p.getDone() == 1).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("done", done);
        result.put("total", all.size());
        return result;
    }

    private void cleanup(long graph, long sm) {
        if (sm != 0) UAVLibraryNative.freeStandmap(sm);
        if (graph != 0) UAVLibraryNative.freeGraph(graph);
    }
}
