package com.agripath.mapper;

import com.agripath.entity.TaskPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskPointMapper {
    void insert(TaskPoint point);
    void insertBatch(@Param("list") List<TaskPoint> list);
    List<TaskPoint> getByMapId(@Param("mapId") Long mapId);
    TaskPoint getById(Long id);
    void updateCode(@Param("id") Long id, @Param("code") String code);
    void deleteByMapId(@Param("mapId") Long mapId);
}
