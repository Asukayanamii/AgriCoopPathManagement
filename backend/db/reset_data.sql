DELETE FROM road_edges;
DELETE FROM road_nodes;
DELETE FROM resources;
DELETE FROM cluster_priority;
DELETE FROM task_clusters;
DELETE FROM task_points;

ALTER TABLE road_nodes AUTO_INCREMENT = 1;
ALTER TABLE road_edges AUTO_INCREMENT = 1;
ALTER TABLE resources AUTO_INCREMENT = 1;
ALTER TABLE task_points AUTO_INCREMENT = 1;
