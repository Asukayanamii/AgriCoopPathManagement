package com.agripath.controller;

import com.agripath.dto.EdgeDTO;
import com.agripath.dto.NodeDTO;
import com.agripath.result.Result;
import com.agripath.service.MapGraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/map")
@Slf4j
public class MapGraphController {

    @Autowired
    private MapGraphService mapGraphService;

    @PostMapping("/nodes")
    public Result<Map<String, Object>> saveNodes(@RequestParam Long mapId, @RequestBody List<NodeDTO> nodes) {
        log.info("保存 {} 个路点 (map={})", nodes.size(), mapId);
        List<double[]> points = nodes.stream().map(n -> new double[]{n.getX(), n.getY()}).collect(Collectors.toList());
        var saved = mapGraphService.saveNodes(mapId, points);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", saved.size());
        result.put("ids", saved.stream().map(n -> n.getId()).collect(Collectors.toList()));
        return Result.success(result);
    }

    @PostMapping("/edges")
    public Result<Map<String, Object>> saveEdges(@RequestParam Long mapId, @RequestBody List<Map<String, Object>> edges) {
        log.info("保存 {} 条路段 (map={})", edges.size(), mapId);
        var saved = mapGraphService.saveEdges(mapId, edges);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", saved.size());
        return Result.success(result);
    }

    @PostMapping("/build")
    public Result<Map<String, Object>> buildGraph(@RequestParam Long mapId) {
        log.info("建图 + 编码 + 标准地图 (map={})", mapId);
        try {
            Map<String, Object> stats = mapGraphService.buildGraph(mapId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("建图失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/nodes")
    public Result<List<com.agripath.entity.RoadNode>> getNodes(@RequestParam Long mapId) {
        return Result.success(mapGraphService.getNodes(mapId));
    }

    @GetMapping("/edges")
    public Result<List<com.agripath.entity.RoadEdge>> getEdges(@RequestParam Long mapId) {
        return Result.success(mapGraphService.getEdges(mapId));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestParam Long mapId) {
        return Result.success(mapGraphService.getStats(mapId));
    }
}
