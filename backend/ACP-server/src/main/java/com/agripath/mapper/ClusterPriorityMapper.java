package com.agripath.mapper;

import com.agripath.entity.ClusterPriority;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClusterPriorityMapper {
    void insertOrUpdate(ClusterPriority priority);
    void insertBatch(@Param("list") List<ClusterPriority> list);
    List<ClusterPriority> getByMapIdOrdered(@Param("mapId") Long mapId);
    ClusterPriority getNextUndone(@Param("mapId") Long mapId);
    void updateDone(@Param("clusterId") Integer clusterId, @Param("done") Integer done);
    void updatePriority(@Param("clusterId") Integer clusterId, @Param("priority") Integer priority);
    void deleteByMapId(@Param("mapId") Long mapId);
}
