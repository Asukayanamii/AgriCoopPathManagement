package com.agripath.mapper;

import com.agripath.entity.RoadEdge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoadEdgeMapper {
    void insert(RoadEdge edge);
    void insertBatch(@Param("list") List<RoadEdge> list);
    void updateCode(@Param("id") Long id, @Param("code") String code);
    List<RoadEdge> getByMapId(@Param("mapId") Long mapId);
    void deleteByMapId(@Param("mapId") Long mapId);
}
