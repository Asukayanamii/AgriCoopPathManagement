package com.agripath.controller;

import com.agripath.result.Result;
import com.agripath.service.MapImageService;
import com.agripath.vo.MapImageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/map/image")
@Slf4j
public class MapImageController {

    @Autowired
    private MapImageService mapImageService;

    @PostMapping("/upload")
    public Result<MapImageVO> upload(@RequestParam("file") MultipartFile file) {
        log.info("上传地图图片: {}", file.getOriginalFilename());
        if (file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        try {
            MapImageVO vo = mapImageService.upload(file);
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
        try {
            Resource resource = mapImageService.getImage(id, false);
            String contentType = Files.probeContentType(resource.getFile().toPath());
            if (contentType == null) contentType = "image/png";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/thumb")
    public ResponseEntity<Resource> getThumbnail(@PathVariable Long id) {
        try {
            Resource resource = mapImageService.getImage(id, true);
            String contentType = "image/jpeg";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/list")
    public Result<List<MapImageVO>> list() {
        List<MapImageVO> list = mapImageService.listAll();
        return Result.success(list);
    }
}
