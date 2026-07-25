package com.agripath.entity;

import lombok.Data;

@Data
public class RoadNode {
    private Long id;
    private Long mapId;
    private Double x;
    private Double y;
    private String code;
}
