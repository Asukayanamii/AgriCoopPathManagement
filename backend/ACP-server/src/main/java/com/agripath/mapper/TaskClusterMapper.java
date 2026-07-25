package com.agripath.mapper;

import com.agripath.entity.TaskCluster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskClusterMapper {
    void insertBatch(@Param("list") List<TaskCluster> list);
    List<TaskCluster> getByMapId(@Param("mapId") Long mapId);
    List<TaskCluster> getByClusterId(@Param("mapId") Long mapId, @Param("clusterId") Integer clusterId);
    void deleteByMapId(@Param("mapId") Long mapId);
}
