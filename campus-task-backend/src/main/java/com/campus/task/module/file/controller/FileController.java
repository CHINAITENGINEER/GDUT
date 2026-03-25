package com.campus.task.module.file.controller;

import com.campus.task.common.result.R;
import com.campus.task.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传接口
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "单张图片上传")
    @PostMapping("/upload")
    public R<Object> upload(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadFile(file);
        return R.ok(java.util.Map.of("url", url));
    }

    @Operation(summary = "批量图片上传（最多9张）")
    @PostMapping("/upload/batch")
    public R<Object> uploadBatch(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = fileService.uploadFiles(files);
        return R.ok(java.util.Map.of("urls", urls));
    }
}
