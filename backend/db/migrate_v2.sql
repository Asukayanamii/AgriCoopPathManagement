-- ============================================================
-- v2: 为所有业务表添加 map_id 外键，按地图隔离数据
-- ============================================================

ALTER TABLE road_nodes ADD COLUMN map_id BIGINT NOT NULL COMMENT '关联地图ID' AFTER id;
ALTER TABLE road_edges ADD COLUMN map_id BIGINT NOT NULL COMMENT '关联地图ID' AFTER id;
ALTER TABLE standmap ADD COLUMN map_id BIGINT NOT NULL COMMENT '关联地图ID' AFTER id;
ALTER TABLE resources ADD COLUMN map_id BIGINT NOT NULL COMMENT '关联地图ID' AFTER id;
ALTER TABLE task_points ADD COLUMN map_id BIGINT NOT NULL COMMENT '关联地图ID' AFTER id;
ALTER TABLE task_clusters ADD COLUMN map_id BIGINT NOT NULL COMMENT '关联地图ID' AFTER id;
ALTER TABLE cluster_priority ADD COLUMN map_id BIGINT NOT NULL COMMENT '关联地图ID' AFTER id;

CREATE INDEX idx_road_nodes_map ON road_nodes(map_id);
CREATE INDEX idx_road_edges_map ON road_edges(map_id);
CREATE INDEX idx_standmap_map ON standmap(map_id);
CREATE INDEX idx_resources_map ON resources(map_id);
CREATE INDEX idx_task_points_map ON task_points(map_id);
CREATE INDEX idx_task_clusters_map ON task_clusters(map_id);
CREATE INDEX idx_cluster_priority_map ON cluster_priority(map_id);
