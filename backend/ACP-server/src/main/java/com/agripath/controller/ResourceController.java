package com.agripath.controller;

import com.agripath.dto.ResourceDTO;
import com.agripath.result.Result;
import com.agripath.service.ResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/resources")
@Slf4j
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping
    public Result<Map<String, Object>> saveResources(@RequestParam Long mapId, @RequestBody List<ResourceDTO> resources) {
        log.info("注册 {} 辆车 (map={})", resources.size(), mapId);
        List<Map<String, Object>> list = resources.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("x", r.getX());
            m.put("y", r.getY());
            m.put("belongNode", r.getBelongNode());
            return m;
        }).collect(Collectors.toList());
        var saved = resourceService.saveResources(mapId, list);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", saved.size());
        return Result.success(result);
    }

    @GetMapping
    public Result<List<com.agripath.entity.Resource>> getResources(@RequestParam Long mapId) {
        return Result.success(resourceService.getResources(mapId));
    }

    @PutMapping("/{id}/state")
    public Result<String> updateState(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer state = body.get("state");
        log.info("更新车辆 {} 状态为 {}", id, state);
        resourceService.updateState(id, state);
        return Result.success("ok");
    }
}
