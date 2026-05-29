package com.agripath.controller;

import com.agripath.dto.*;
import com.agripath.result.Result;
import com.agripath.service.AlgorithmPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/algorithm")
@Slf4j
public class AlgorithmController {

    @Autowired
    private AlgorithmPipelineService algorithmPipelineService;

    @PostMapping("/cluster")
    public Result<Map<String, Object>> cluster(@RequestBody ClusterRequestDTO dto) {
        log.info("执行聚类算法, points={}", dto.getPoints() != null ? dto.getPoints().size() : 0);
        Map<String, Object> result = algorithmPipelineService.cluster(dto);
        return Result.success(result);
    }

    @PostMapping("/resource-search")
    public Result<Map<String, Object>> resourceSearch(@RequestBody ResourceSearchRequestDTO dto) {
        log.info("执行资源搜索算法, resources={}, targets={}",
                dto.getResources() != null ? dto.getResources().size() : 0,
                dto.getTargets() != null ? dto.getTargets().size() : 0);
        Map<String, Object> result = algorithmPipelineService.resourceSearch(dto);
        return Result.success(result);
    }

    @PostMapping("/path-planning")
    public Result<Map<String, Object>> pathPlanning(@RequestBody PathPlanRequestDTO dto) {
        log.info("执行路径规划算法, start=({},{}), end=({},{})",
                dto.getStartX(), dto.getStartY(), dto.getEndX(), dto.getEndY());
        Map<String, Object> result = algorithmPipelineService.pathPlanning(dto);
        return Result.success(result);
    }

    @PostMapping("/pipeline")
    public Result<Map<String, Object>> pipeline(@RequestBody PipelineRequestDTO dto) {
        log.info("执行完整流水线, taskPoints={}, resources={}",
                dto.getTaskPoints() != null ? dto.getTaskPoints().size() : 0,
                dto.getResources() != null ? dto.getResources().size() : 0);
        Map<String, Object> result = algorithmPipelineService.runPipeline(dto);
        return Result.success(result);
    }
}
