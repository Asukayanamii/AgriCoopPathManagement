package com.agripath.mapper;

import com.agripath.entity.StandmapEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StandmapMapper {
    void insertBatch(@Param("list") List<StandmapEntry> list);
    List<StandmapEntry> getByMapId(@Param("mapId") Long mapId);
    void deleteByMapId(@Param("mapId") Long mapId);
}
