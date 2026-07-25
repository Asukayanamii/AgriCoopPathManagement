package com.agripath.controller;

import com.agripath.dto.ClusterActionDTO;
import com.agripath.result.Result;
import com.agripath.service.ExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/execute")
@Slf4j
public class ExecuteController {

    @Autowired
    private ExecuteService executeService;

    @PostMapping("/next")
    public Result<Map<String, Object>> executeNext(@RequestParam Long mapId) {
        log.info("执行下一个未处理簇 (map={})", mapId);
        try {
            return Result.success(executeService.executeNext(mapId));
        } catch (Exception e) {
            log.error("执行失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/car-arrived")
    public Result<Map<String, Object>> carArrived(@RequestParam Long mapId, @RequestBody ClusterActionDTO dto) {
        log.info("小车到达簇 {} (map={})", dto.getClusterId(), mapId);
        try {
            return Result.success(executeService.carArrived(mapId, dto.getClusterId()));
        } catch (Exception e) {
            log.error("TSP 规划失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/drone-done")
    public Result<Map<String, Object>> droneDone(@RequestParam Long mapId, @RequestBody ClusterActionDTO dto) {
        log.info("无人机完成簇 {} (map={})", dto.getClusterId(), mapId);
        return Result.success(executeService.droneDone(mapId, dto.getClusterId()));
    }

    @GetMapping("/progress")
    public Result<Map<String, Object>> getProgress(@RequestParam Long mapId) {
        return Result.success(executeService.getProgress(mapId));
    }
}
