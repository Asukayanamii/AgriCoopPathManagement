package com.agripath.service;

import com.agripath.entity.Resource;
import com.agripath.mapper.ResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    @Transactional
    public List<Resource> saveResources(Long mapId, List<Map<String, Object>> resources) {
        resourceMapper.deleteByMapId(mapId);
        List<Resource> list = new ArrayList<>();
        for (Map<String, Object> r : resources) {
            Resource res = new Resource();
            res.setMapId(mapId);
            res.setX(((Number) r.get("x")).doubleValue());
            res.setY(((Number) r.get("y")).doubleValue());
            res.setBelongNode(r.get("belongNode") != null ? ((Number) r.get("belongNode")).longValue() : null);
            res.setState(1);
            list.add(res);
        }
        if (!list.isEmpty()) resourceMapper.insertBatch(list);
        return list;
    }

    public List<Resource> getResources(Long mapId) {
        return resourceMapper.getByMapId(mapId);
    }

    @Transactional
    public void updateState(Long id, Integer state) {
        resourceMapper.updateState(id, state);
    }
}
