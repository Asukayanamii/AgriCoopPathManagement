package com.agripath.mapper;

import com.agripath.entity.MapImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MapImageMapper {
    void insert(MapImage mapImage);
    MapImage getById(Long id);
    List<MapImage> listAll();
}
