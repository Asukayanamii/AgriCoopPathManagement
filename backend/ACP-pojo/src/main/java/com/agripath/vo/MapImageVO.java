package com.agripath.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MapImageVO {
    private Long id;
    private String uuidName;
    private String originName;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private String url;
    private String thumbUrl;
    private LocalDateTime createdAt;
}
