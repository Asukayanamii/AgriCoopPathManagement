package com.agripath.service;

import com.agripath.acpcommon.utils.UAVLibraryNative;
import com.agripath.entity.RoadEdge;
import com.agripath.entity.RoadNode;
import com.agripath.entity.StandmapEntry;
import com.agripath.mapper.RoadEdgeMapper;
import com.agripath.mapper.RoadNodeMapper;
import com.agripath.mapper.StandmapMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MapGraphService {

    @Autowired
    private RoadNodeMapper roadNodeMapper;
    @Autowired
    private RoadEdgeMapper roadEdgeMapper;
    @Autowired
    private StandmapMapper standmapMapper;

    @Transactional
    public List<RoadNode> saveNodes(Long mapId, List<double[]> points) {
        roadEdgeMapper.deleteByMapId(mapId);
        roadNodeMapper.deleteByMapId(mapId);
        List<RoadNode> nodes = new ArrayList<>();
        for (double[] p : points) {
            RoadNode n = new RoadNode();
            n.setMapId(mapId);
            n.setX(p[0]);
            n.setY(p[1]);
            n.setCode("");
            nodes.add(n);
        }
        if (!nodes.isEmpty()) roadNodeMapper.insertBatch(nodes);
        return nodes;
    }

    @Transactional
    public List<RoadEdge> saveEdges(Long mapId, List<Map<String, Object>> edgeDefs) {
        roadEdgeMapper.deleteByMapId(mapId);
        List<RoadEdge> edges = new ArrayList<>();
        for (Map<String, Object> def : edgeDefs) {
            RoadEdge e = new RoadEdge();
            e.setMapId(mapId);
            e.setFromNode(((Number) def.get("fromNode")).longValue());
            e.setToNode(((Number) def.get("toNode")).longValue());
            e.setWeight(((Number) def.get("weight")).doubleValue());
            e.setCode("");
            edges.add(e);
        }
        if (!edges.isEmpty()) roadEdgeMapper.insertBatch(edges);
        return edges;
    }

    @Transactional
    public Map<String, Object> buildGraph(Long mapId) {
        List<RoadNode> nodes = roadNodeMapper.getByMapId(mapId);
        List<RoadEdge> edges = roadEdgeMapper.getByMapId(mapId);
        if (nodes.isEmpty()) throw new IllegalStateException("No road nodes for map " + mapId);
        if (edges.isEmpty()) throw new IllegalStateException("No road edges for map " + mapId);

        int nodeCount = nodes.size();
        int edgeCount = edges.size();

        int[] from = new int[edgeCount];
        int[] to = new int[edgeCount];
        int[] weight = new int[edgeCount];
        int[] px = new int[nodeCount];
        int[] py = new int[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            px[i] = (int)Math.round(nodes.get(i).getX());
            py[i] = (int)Math.round(nodes.get(i).getY());
        }
        for (int i = 0; i < edgeCount; i++) {
            from[i] = edges.get(i).getFromNode().intValue() - 1;
            to[i] = edges.get(i).getToNode().intValue() - 1;
            weight[i] = (int)Math.round(edges.get(i).getWeight());
        }

        long graphHandle = UAVLibraryNative.createGraph(nodeCount, from, to, weight, edgeCount);
        if (graphHandle == 0) throw new RuntimeException("create_graph failed");

        long smHandle = UAVLibraryNative.createStandmap(px, py, nodeCount, graphHandle);
        if (smHandle == 0) { UAVLibraryNative.freeGraph(graphHandle); throw new RuntimeException("create_standmap failed"); }

        // Encode nodes
        int[] nodeIds = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) nodeIds[i] = i;
        String[] nodeCodes = UAVLibraryNative.encodeNodes(nodeIds);
        for (int i = 0; i < nodeCount && i < nodeCodes.length; i++)
            roadNodeMapper.updateCode(nodes.get(i).getId(), nodeCodes[i]);

        // Encode edges
        int[] edgeIds = new int[edgeCount];
        for (int i = 0; i < edgeCount; i++) edgeIds[i] = edges.get(i).getId().intValue();
        String[] edgeCodes = UAVLibraryNative.encodeEdges(edgeIds);
        for (int i = 0; i < edgeCount && i < edgeCodes.length; i++)
            roadEdgeMapper.updateCode(edges.get(i).getId(), edgeCodes[i]);

        // Persist standmap
        standmapMapper.deleteByMapId(mapId);
        int[] smData = UAVLibraryNative.exportStandmap(smHandle);
        if (smData != null) {
            List<StandmapEntry> entries = new ArrayList<>();
            for (int i = 0; i < nodeCount; i++) {
                for (int j = 0; j < nodeCount; j++) {
                    StandmapEntry e = new StandmapEntry();
                    e.setMapId(mapId);
                    e.setCenterNode(i);
                    e.setTargetNode(j);
                    e.setDistance((double) smData[i * nodeCount + j]);
                    entries.add(e);
                }
            }
            standmapMapper.insertBatch(entries);
        }

        UAVLibraryNative.freeStandmap(smHandle);
        UAVLibraryNative.freeGraph(graphHandle);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeCount", nodeCount);
        result.put("edgeCount", edgeCount);
        result.put("nodeCodesSaved", nodeCodes.length);
        result.put("edgeCodesSaved", edgeCodes.length);
        return result;
    }

    public List<RoadNode> getNodes(Long mapId) { return roadNodeMapper.getByMapId(mapId); }
    public List<RoadEdge> getEdges(Long mapId) { return roadEdgeMapper.getByMapId(mapId); }

    public Map<String, Object> getStats(Long mapId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("nodeCount", roadNodeMapper.getByMapId(mapId).size());
        stats.put("edgeCount", roadEdgeMapper.getByMapId(mapId).size());
        stats.put("standmapRows", standmapMapper.getByMapId(mapId).size());
        return stats;
    }
}
