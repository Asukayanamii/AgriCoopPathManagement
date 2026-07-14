package com.agripath.dto;

import lombok.Data;
import java.util.List;

@Data
public class PathPlanRequestDTO {
    private int mapWidth = 1000;
    private int mapHeight = 1000;
    private int gridResolution = 10;
    private int startX, startY;
    private int endX, endY;
    private List<int[]> obstacles;
}
