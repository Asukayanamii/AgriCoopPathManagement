package com.agripath.entity;

import lombok.Data;

@Data
public class TaskPoint {
    private Long id;
    private Long mapId;
    private Double x;
    private Double y;
    private String code;
}
