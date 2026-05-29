package com.agripath.dto;

import lombok.Data;
import java.util.List;

@Data
public class PathPlanRequestDTO {
    private int mapWidth;
    private int mapHeight;
    private int startX, startY;
    private int endX, endY;
    private List<int[]> obstacles;
}
