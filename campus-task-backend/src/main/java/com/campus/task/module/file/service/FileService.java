package com.campus.task.module.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传服务接口
 */
public interface FileService {
    
    /**
     * 上传单个文件
     */
    String uploadFile(MultipartFile file);
    
    /**
     * 批量上传文件
     */
    List<String> uploadFiles(List<MultipartFile> files);
}
