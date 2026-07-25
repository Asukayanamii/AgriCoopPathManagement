package com.agripath.entity;

import lombok.Data;

@Data
public class TaskCluster {
    private Long id;
    private Long mapId;
    private Long taskId;
    private Integer clusterId;
    private Integer centerId;
}
