package com.agripath.controller;

import com.agripath.dto.PriorityDTO;
import com.agripath.dto.TaskPointDTO;
import com.agripath.result.Result;
import com.agripath.service.TaskClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
@Slf4j
public class TaskController {

    @Autowired
    private TaskClusterService taskClusterService;

    @PostMapping
    public Result<Map<String, Object>> createTasks(@RequestParam Long mapId, @RequestBody List<TaskPointDTO> tasks) {
        log.info("创建 {} 个任务点 (map={})", tasks.size(), mapId);
        List<double[]> points = tasks.stream().map(t -> new double[]{t.getX(), t.getY()}).collect(Collectors.toList());
        var saved = taskClusterService.createTasks(mapId, points);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", saved.size());
        result.put("ids", saved.stream().map(s -> s.getId()).collect(Collectors.toList()));
        result.put("codes", saved.stream().map(s -> s.getCode()).collect(Collectors.toList()));
        return Result.success(result);
    }

    @PostMapping("/cluster")
    public Result<Map<String, Object>> runCluster(@RequestParam Long mapId, @RequestBody(required = false) Map<String, Integer> params) {
        int space = params != null && params.containsKey("space") ? params.get("space") : 8;
        int deviation = params != null && params.containsKey("deviation") ? params.get("deviation") : 2;
        int maxIter = params != null && params.containsKey("maxIter") ? params.get("maxIter") : 50;
        log.info("执行聚类 (map={}), space={}, deviation={}, maxIter={}", mapId, space, deviation, maxIter);
        try {
            return Result.success(taskClusterService.runKmeans(mapId, space, deviation, maxIter));
        } catch (Exception e) {
            log.error("聚类失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    public Result<List<com.agripath.entity.TaskPoint>> getTasks(@RequestParam Long mapId) {
        return Result.success(taskClusterService.getTasks(mapId));
    }

    @GetMapping("/clusters")
    public Result<List<Map<String, Object>>> getClusters(@RequestParam Long mapId) {
        return Result.success(taskClusterService.getClusters(mapId));
    }

    @PutMapping("/priority")
    public Result<String> savePriority(@RequestParam Long mapId, @RequestBody List<PriorityDTO> priorities) {
        log.info("保存 {} 条优先级排序 (map={})", priorities.size(), mapId);
        List<Map<String, Object>> list = priorities.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clusterId", p.getClusterId());
            m.put("priority", p.getPriority());
            return m;
        }).collect(Collectors.toList());
        taskClusterService.savePriority(mapId, list);
        return Result.success("ok");
    }

    @GetMapping("/priority")
    public Result<List<com.agripath.entity.ClusterPriority>> getPriority(@RequestParam Long mapId) {
        return Result.success(taskClusterService.getPriority(mapId));
    }
}
