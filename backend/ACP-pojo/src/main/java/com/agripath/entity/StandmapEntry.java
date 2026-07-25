package com.agripath.entity;

import lombok.Data;

@Data
public class StandmapEntry {
    private Long id;
    private Long mapId;
    private Integer centerNode;
    private Integer targetNode;
    private Double distance;
}
