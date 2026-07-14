package com.agripath.dto;

import lombok.Data;
import java.util.List;

@Data
public class PipelineRequestDTO {
    private int mapWidth;
    private int mapHeight;
    private int gridResolution = 10;
    private List<int[]> taskPoints;
    private int spaceCluster = 8;
    private int deviation = 2;
    private int iterationCount = 20;
    private List<ResourceDTO> resources;
    private List<TargetDTO> specificTargets;

    @Data
    public static class ResourceDTO {
        private int x, y, id;
        private boolean available;
    }

    @Data
    public static class TargetDTO {
        private int x, y, required;
    }
}
