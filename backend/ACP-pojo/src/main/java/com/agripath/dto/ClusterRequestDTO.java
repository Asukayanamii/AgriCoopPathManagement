package com.agripath.dto;

import lombok.Data;
import java.util.List;

@Data
public class ClusterRequestDTO {
    private List<int[]> points;
    private int spaceCluster = 8;
    private int deviation = 2;
    private int iterationCount = 20;
}
