-- 地图图片表
CREATE TABLE IF NOT EXISTS map_image (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid_name   VARCHAR(64)  NOT NULL COMMENT '文件系统上的 UUID 文件名',
    origin_name VARCHAR(255) NOT NULL COMMENT '用户上传时的原始文件名',
    width       INT          NOT NULL COMMENT '图片像素宽度',
    height      INT          NOT NULL COMMENT '图片像素高度',
    file_size   BIGINT       NOT NULL COMMENT '原图字节数',
    thumb_size  BIGINT       DEFAULT NULL COMMENT '缩略图字节数',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地图图片';

-- ============================================================
-- 算法交互流程 数据库表（2026-07 新版）
-- ============================================================

-- 路点（岔路口 + 拐点），由前端标记，encode_nodes 生成编码
CREATE TABLE IF NOT EXISTS road_nodes (
    id      INTEGER PRIMARY KEY AUTO_INCREMENT,
    x       DOUBLE NOT NULL COMMENT '逻辑坐标 x (0~1000)',
    y       DOUBLE NOT NULL COMMENT '逻辑坐标 y (0~1000)',
    code    VARCHAR(4) NOT NULL COMMENT '4位十六进制编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路点（岔路口+拐点）';

-- 路段（边），由前端逐条标记，encode_edges 生成编码
CREATE TABLE IF NOT EXISTS road_edges (
    id          INTEGER PRIMARY KEY AUTO_INCREMENT,
    from_node   INTEGER NOT NULL REFERENCES road_nodes(id),
    to_node     INTEGER NOT NULL REFERENCES road_nodes(id),
    weight      DOUBLE NOT NULL COMMENT '栅格距离',
    code        VARCHAR(4) NOT NULL COMMENT '4位十六进制编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路段（边）';

-- 标准距离地图（A* 启发数据）
CREATE TABLE IF NOT EXISTS standmap (
    id          INTEGER PRIMARY KEY AUTO_INCREMENT,
    center_node INTEGER NOT NULL COMMENT '聚类中心节点',
    target_node INTEGER NOT NULL COMMENT '目标节点',
    distance    DOUBLE NOT NULL COMMENT '最短距离'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准距离地图';

-- 车辆资源
CREATE TABLE IF NOT EXISTS resources (
    id           INTEGER PRIMARY KEY AUTO_INCREMENT,
    x            DOUBLE NOT NULL COMMENT '逻辑坐标 x',
    y            DOUBLE NOT NULL COMMENT '逻辑坐标 y',
    state        INTEGER NOT NULL DEFAULT 1 COMMENT '1=空闲, 0=占用',
    belong_node  INTEGER DEFAULT NULL COMMENT '当前所在图节点编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆资源';

-- 任务点
CREATE TABLE IF NOT EXISTS task_points (
    id          INTEGER PRIMARY KEY AUTO_INCREMENT,
    x           DOUBLE NOT NULL COMMENT '逻辑坐标 x',
    y           DOUBLE NOT NULL COMMENT '逻辑坐标 y',
    code        VARCHAR(4) NOT NULL COMMENT '4位十六进制编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务点';

-- 聚类结果
CREATE TABLE IF NOT EXISTS task_clusters (
    id          INTEGER PRIMARY KEY AUTO_INCREMENT,
    task_id     INTEGER NOT NULL REFERENCES task_points(id),
    cluster_id  INTEGER NOT NULL COMMENT '簇编号',
    center_id   INTEGER NOT NULL COMMENT '聚类中心编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聚类结果';

-- 人工标注的聚类优先级 + 执行状态
CREATE TABLE IF NOT EXISTS cluster_priority (
    id          INTEGER PRIMARY KEY AUTO_INCREMENT,
    cluster_id  INTEGER NOT NULL UNIQUE,
    priority    INTEGER NOT NULL COMMENT '越小优先级越高',
    center_x    DOUBLE DEFAULT NULL COMMENT '簇中心x',
    center_y    DOUBLE DEFAULT NULL COMMENT '簇中心y',
    done        INTEGER NOT NULL DEFAULT 0 COMMENT '0=未处理, 1=已完成'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聚类优先级与执行状态';

