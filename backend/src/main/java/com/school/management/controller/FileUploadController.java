package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Tag(name = "文件上传接口", description = "文件上传相关接口")
@PreAuthorize("isAuthenticated()")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 上传单个图片
     * 业务异常由 GlobalExceptionHandler 统一处理
     */
    @PostMapping("/image")
    @Operation(summary = "上传单个图片", description = "上传单个图片文件")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("接收到图片上传请求: {}", file.getOriginalFilename());

        String url = fileUploadService.uploadImage(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("name", file.getOriginalFilename());
        return Result.success(result);
    }

    /**
     * 批量上传图片
     * 业务异常由 GlobalExceptionHandler 统一处理
     */
    @PostMapping("/images")
    @Operation(summary = "批量上传图片", description = "批量上传图片文件,最多5张")
    public Result<List<Map<String, String>>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        log.info("接收到批量图片上传请求,数量: {}", files.length);

        if (files.length > 5) {
            return Result.error("最多只能上传5张图片");
        }

        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = fileUploadService.uploadImage(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("name", file.getOriginalFilename());
            results.add(result);
        }
        return Result.success(results);
    }

    /**
     * 上传字体文件
     * 支持 .ttf, .woff, .woff2 格式
     */
    @PostMapping("/font")
    @Operation(summary = "上传字体文件", description = "上传字体文件,支持 ttf/woff/woff2 格式")
    public Result<Map<String, String>> uploadFont(@RequestParam("file") MultipartFile file) {
        log.info("接收到字体上传请求: {}", file.getOriginalFilename());

        String url = fileUploadService.uploadFont(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("name", file.getOriginalFilename());
        return Result.success(result);
    }
}
