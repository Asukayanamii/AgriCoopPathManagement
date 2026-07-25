package com.agripath.mapper;

import com.agripath.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceMapper {
    void insert(Resource resource);
    void insertBatch(@Param("list") List<Resource> list);
    List<Resource> getByMapId(@Param("mapId") Long mapId);
    Resource getById(Long id);
    void updateState(@Param("id") Long id, @Param("state") Integer state);
    void deleteByMapId(@Param("mapId") Long mapId);
}
