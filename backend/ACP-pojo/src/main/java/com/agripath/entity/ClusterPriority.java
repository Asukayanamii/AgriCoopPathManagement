package com.agripath.entity;

import lombok.Data;

@Data
public class ClusterPriority {
    private Long id;
    private Long mapId;
    private Integer clusterId;
    private Integer priority;
    private Double centerX;
    private Double centerY;
    private Integer done;
}
