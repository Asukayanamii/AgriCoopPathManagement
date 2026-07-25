package com.agripath.dto;

import lombok.Data;

@Data
public class EdgeDTO {
    private Long fromNode;
    private Long toNode;
    private Double weight;
}
