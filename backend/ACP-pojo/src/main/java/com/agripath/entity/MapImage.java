package com.agripath.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MapImage {
    private Long id;
    private String uuidName;
    private String originName;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private Long thumbSize;
    private LocalDateTime createdAt;
}
