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
