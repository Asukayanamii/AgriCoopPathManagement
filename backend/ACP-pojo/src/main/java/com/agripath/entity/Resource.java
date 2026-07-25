package com.agripath.entity;

import lombok.Data;

@Data
public class Resource {
    private Long id;
    private Long mapId;
    private Double x;
    private Double y;
    private Integer state;
    private Long belongNode;
}
