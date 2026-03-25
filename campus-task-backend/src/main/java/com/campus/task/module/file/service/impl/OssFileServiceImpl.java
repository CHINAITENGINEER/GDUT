package com.campus.task.module.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.config.OssConfig;
import com.campus.task.module.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage-type", havingValue = "oss")
public class OssFileServiceImpl implements FileService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public String uploadFile(MultipartFile file) {
        validateFile(file);
        
        // 按日期分目录：2026/03/14/uuid.jpg
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExt(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        String objectName = dateDir + "/" + filename;
        
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putRequest = new PutObjectRequest(
                    ossConfig.getBucketName(), 
                    objectName, 
                    inputStream
            );
            ossClient.putObject(putRequest);
            
            // 返回完整访问URL
            String url = ossConfig.getUrlPrefix() + objectName;
            log.info("文件上传成功: {}", url);
            return url;
            
        } catch (IOException e) {
            log.error("OSS上传失败", e);
            throw new BusinessException("文件上传失败");
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
