package com.agripath.entity;

import lombok.Data;

@Data
public class RoadEdge {
    private Long id;
    private Long mapId;
    private Long fromNode;
    private Long toNode;
    private Double weight;
    private String code;
}
