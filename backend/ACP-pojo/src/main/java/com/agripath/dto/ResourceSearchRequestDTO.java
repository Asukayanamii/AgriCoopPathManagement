package com.agripath.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResourceSearchRequestDTO {
    private int mapWidth;
    private int mapHeight;
    private List<ResourceDTO> resources;
    private List<TargetDTO> targets;

    @Data
    public static class ResourceDTO {
        private int x, y, id;
        private boolean available;
    }

    @Data
    public static class TargetDTO {
        private int x, y, required;
    }
}
