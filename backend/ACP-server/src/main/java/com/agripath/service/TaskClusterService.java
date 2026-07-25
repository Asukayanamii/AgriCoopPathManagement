package com.agripath.service;

import com.agripath.acpcommon.utils.UAVLibraryNative;
import com.agripath.entity.ClusterPriority;
import com.agripath.entity.TaskCluster;
import com.agripath.entity.TaskPoint;
import com.agripath.mapper.ClusterPriorityMapper;
import com.agripath.mapper.TaskClusterMapper;
import com.agripath.mapper.TaskPointMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskClusterService {

    @Autowired
    private TaskPointMapper taskPointMapper;
    @Autowired
    private TaskClusterMapper taskClusterMapper;
    @Autowired
    private ClusterPriorityMapper clusterPriorityMapper;

    @Transactional
    public List<TaskPoint> createTasks(Long mapId, List<double[]> points) {
        clusterPriorityMapper.deleteByMapId(mapId);
        taskClusterMapper.deleteByMapId(mapId);
        taskPointMapper.deleteByMapId(mapId);

        List<TaskPoint> taskPoints = new ArrayList<>();
        for (double[] p : points) {
            TaskPoint tp = new TaskPoint();
            tp.setMapId(mapId);
            tp.setX(p[0]);
            tp.setY(p[1]);
            tp.setCode("");
            taskPoints.add(tp);
        }
        if (!taskPoints.isEmpty()) {
            taskPointMapper.insertBatch(taskPoints);
            int[] ids = taskPoints.stream().mapToInt(t -> t.getId().intValue()).toArray();
            String[] codes = UAVLibraryNative.encodeNodes(ids);
            for (int i = 0; i < taskPoints.size() && i < codes.length; i++) {
                taskPointMapper.updateCode(taskPoints.get(i).getId(), codes[i]);
                taskPoints.get(i).setCode(codes[i]);
            }
        }
        return taskPoints;
    }

    @Transactional
    public Map<String, Object> runKmeans(Long mapId, int space, int deviation, int maxIter) {
        List<TaskPoint> points = taskPointMapper.getByMapId(mapId);
        if (points.isEmpty()) throw new IllegalStateException("No task points");

        int n = points.size();
        int[] px = new int[n];
        int[] py = new int[n];
        for (int i = 0; i < n; i++) {
            px[i] = (int) Math.round(points.get(i).getX());
            py[i] = (int) Math.round(points.get(i).getY());
        }

        int[][] result = UAVLibraryNative.kmeans(px, py, n, space, deviation, maxIter);
        int[] clusterIds = result[0];
        int[] centerIds = result[1];

        taskClusterMapper.deleteByMapId(mapId);
        List<TaskCluster> clusters = new ArrayList<>();
        Map<Integer, List<int[]>> grouped = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            TaskCluster tc = new TaskCluster();
            tc.setMapId(mapId);
            tc.setTaskId(points.get(i).getId());
            tc.setClusterId(clusterIds[i]);
            tc.setCenterId(centerIds[i]);
            clusters.add(tc);
            grouped.computeIfAbsent(clusterIds[i], k -> new ArrayList<>())
                    .add(new int[]{(int)Math.round(points.get(i).getX()), (int)Math.round(points.get(i).getY())});
        }
        if (!clusters.isEmpty()) taskClusterMapper.insertBatch(clusters);

        clusterPriorityMapper.deleteByMapId(mapId);
        List<ClusterPriority> priorities = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<Integer, List<int[]>> entry : grouped.entrySet()) {
            List<int[]> pts = entry.getValue();
            double cx = pts.stream().mapToDouble(p -> p[0]).average().orElse(0);
            double cy = pts.stream().mapToDouble(p -> p[1]).average().orElse(0);
            ClusterPriority cp = new ClusterPriority();
            cp.setMapId(mapId);
            cp.setClusterId(entry.getKey());
            cp.setPriority(idx++);
            cp.setCenterX(cx);
            cp.setCenterY(cy);
            cp.setDone(0);
            priorities.add(cp);
        }
        if (!priorities.isEmpty()) clusterPriorityMapper.insertBatch(priorities);

        // Build per-point assignments
        List<Map<String, Object>> assignments = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("taskId", points.get(i).getId());
            a.put("clusterId", clusterIds[i]);
            a.put("centerId", centerIds[i]);
            a.put("x", Math.round(points.get(i).getX()));
            a.put("y", Math.round(points.get(i).getY()));
            assignments.add(a);
        }

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("clusterCount", grouped.size());
        ret.put("assignments", assignments);
        ret.put("clusters", grouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clusterId", e.getKey());
            m.put("pointCount", e.getValue().size());
            return m;
        }).collect(Collectors.toList()));
        return ret;
    }

    @Transactional
    public void savePriority(Long mapId, List<Map<String, Object>> priorities) {
        for (Map<String, Object> p : priorities) {
            Integer clusterId = ((Number) p.get("clusterId")).intValue();
            Integer priority = ((Number) p.get("priority")).intValue();
            clusterPriorityMapper.updatePriority(clusterId, priority);
        }
    }

    public List<ClusterPriority> getPriority(Long mapId) {
        return clusterPriorityMapper.getByMapIdOrdered(mapId);
    }

    public List<TaskPoint> getTasks(Long mapId) {
        return taskPointMapper.getByMapId(mapId);
    }

    public List<Map<String, Object>> getClusters(Long mapId) {
        List<TaskCluster> all = taskClusterMapper.getByMapId(mapId);
        Map<Integer, List<Long>> grouped = new LinkedHashMap<>();
        for (TaskCluster tc : all) {
            grouped.computeIfAbsent(tc.getClusterId(), k -> new ArrayList<>()).add(tc.getTaskId());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Long>> e : grouped.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clusterId", e.getKey());
            m.put("taskCount", e.getValue().size());
            result.add(m);
        }
        return result;
    }
}
