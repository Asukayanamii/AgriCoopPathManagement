package com.agripath.mapper;

import com.agripath.entity.RoadNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoadNodeMapper {
    void insert(RoadNode node);
    void insertBatch(@Param("list") List<RoadNode> list);
    void updateCode(@Param("id") Long id, @Param("code") String code);
    List<RoadNode> getByMapId(@Param("mapId") Long mapId);
    RoadNode getById(Long id);
    void deleteByMapId(@Param("mapId") Long mapId);
}
