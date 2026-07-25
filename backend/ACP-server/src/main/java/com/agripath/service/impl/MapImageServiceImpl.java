package com.agripath.service.impl;

import com.agripath.entity.MapImage;
import com.agripath.mapper.MapImageMapper;
import com.agripath.service.MapImageService;
import com.agripath.vo.MapImageVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MapImageServiceImpl implements MapImageService {

    @Value("${acp.upload.map-dir}")
    private String mapDir;

    @Autowired
    private MapImageMapper mapImageMapper;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(mapDir));
            log.info("Map upload directory ready: {}", mapDir);
        } catch (IOException e) {
            log.warn("Failed to create map upload directory: {}", e.getMessage());
        }
    }

    @Override
    public MapImageVO upload(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !"image/png".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("仅支持 PNG 格式");
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String originName = file.getOriginalFilename();
        if (originName == null || originName.isBlank()) {
            originName = "unknown.png";
        }
        String ext = "png";
        String uuidName = uuid + "." + ext;

        try {
            BufferedImage img = ImageIO.read(file.getInputStream());
            if (img == null) {
                throw new IllegalArgumentException("无法读取图片，请确认是有效的 PNG 文件");
            }
            int w = img.getWidth();
            int h = img.getHeight();

            Path dir = Paths.get(mapDir);
            Files.createDirectories(dir);

            // 保存原图
            Path dest = dir.resolve(uuidName);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            long fileSize = Files.size(dest);

            // 生成缩略图（宽 200px，等比例缩放）
            int thumbW = 200;
            int thumbH = (int) (200.0 * h / w);
            BufferedImage thumbImg = new BufferedImage(thumbW, thumbH, BufferedImage.TYPE_INT_RGB);
            thumbImg.getGraphics().drawImage(
                    img.getScaledInstance(thumbW, thumbH, BufferedImage.SCALE_SMOOTH), 0, 0, null);
            String thumbName = uuid + "_thumb.jpg";
            ImageIO.write(thumbImg, "JPEG", dir.resolve(thumbName).toFile());
            long thumbSize = Files.size(dir.resolve(thumbName));

            // 入库
            MapImage entity = MapImage.builder()
                    .uuidName(uuidName)
                    .originName(originName)
                    .width(w)
                    .height(h)
                    .fileSize(fileSize)
                    .thumbSize(thumbSize)
                    .createdAt(LocalDateTime.now())
                    .build();
            mapImageMapper.insert(entity);

            log.info("Map image uploaded: {} ({}x{}, {} bytes)", originName, w, h, fileSize);

            return toVO(entity);

        } catch (IOException e) {
            throw new RuntimeException("图片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource getImage(Long id, boolean thumbnail) {
        MapImage entity = mapImageMapper.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("地图不存在: " + id);
        }
        String name = thumbnail ? entity.getUuidName().replace(".png", "_thumb.jpg") : entity.getUuidName();
        Path file = Paths.get(mapDir, name);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("图片文件不存在");
        }
        return new FileSystemResource(file);
    }

    @Override
    public void delete(Long id) {
        MapImage entity = mapImageMapper.getById(id);
        if (entity == null) throw new IllegalArgumentException("地图不存在: " + id);
        try {
            // Delete original file
            Path original = Paths.get(mapDir, entity.getUuidName());
            Files.deleteIfExists(original);
            // Delete thumbnail
            String thumbName = entity.getUuidName().replace(".png", "_thumb.jpg");
            Files.deleteIfExists(Paths.get(mapDir, thumbName));
        } catch (IOException e) {
            log.warn("Failed to delete map image files: {}", e.getMessage());
        }
        mapImageMapper.deleteById(id);
        log.info("Map image deleted: id={}, name={}", id, entity.getOriginName());
    }

    @Override
    public List<MapImageVO> listAll() {
        List<MapImage> list = mapImageMapper.listAll();
        return list.stream().map(this::toVO).toList();
    }

    private MapImageVO toVO(MapImage entity) {
        return MapImageVO.builder()
                .id(entity.getId())
                .uuidName(entity.getUuidName())
                .originName(entity.getOriginName())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .fileSize(entity.getFileSize())
                .url("/api/map/image/" + entity.getId())
                .thumbUrl("/api/map/image/" + entity.getId() + "/thumb")
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
