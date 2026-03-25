package com.campus.task.module.file.service.impl;

import com.campus.task.common.exception.BusinessException;
import com.campus.task.module.file.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 本地文件存储实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "file.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileServiceImpl implements FileService {

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    @Value("${file.access-prefix:/uploads/}")
    private String accessPrefix;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public String uploadFile(MultipartFile file) {
        validateFile(file);
        
        // 按日期分目录存储
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String dirPath = uploadPath + dateDir + "/";
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();
        
        String ext = getExt(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(dirPath + filename);
        
        try {
            file.transferTo(dest);
            String url = accessPrefix + dateDir + "/" + filename;
            log.info("文件保存成功: {}", url);
            return url;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败");
        }
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files) {
        if (files.size() > 9) {
            throw new BusinessException("最多同时上传9张图片");
        }
        
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadFile(file));
        }
        return urls;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("单张图片不能超过5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("不支持的文件格式，仅支持jpg/png/gif/webp");
        }
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}
