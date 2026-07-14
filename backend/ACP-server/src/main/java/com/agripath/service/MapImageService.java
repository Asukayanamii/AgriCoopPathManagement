package com.agripath.service;

import com.agripath.vo.MapImageVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MapImageService {
    MapImageVO upload(MultipartFile file);
    Resource getImage(Long id, boolean thumbnail);
    List<MapImageVO> listAll();
}
