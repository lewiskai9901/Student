package com.school.management.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源控制器
 * 用于提供上传文件的访问服务
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
public class StaticResourceController {

    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 获取上传的文件（处理 /uploads/** 路径）
     */
    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> getUploadedFile(HttpServletRequest request) {
        try {
            // 获取完整的请求路径
            String requestURI = request.getRequestURI();
            // 先移除上下文路径 (如 /api)
            String contextPath = request.getContextPath();
            String pathWithoutContext = requestURI.substring(contextPath.length());
            // 提取 /uploads/ 之后的路径
            String filePath = pathWithoutContext.substring("/uploads/".length());

            // 构建完整的文件路径
            String fullPath = uploadPath + filePath;
            Path path = Paths.get(fullPath);
            File file = path.toFile();

            log.info("请求文件: {}, 完整路径: {}", filePath, fullPath);

            if (!file.exists()) {
                log.warn("文件不存在: {}", fullPath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("返回文件: {}, Content-Type: {}", file.getName(), contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("获取文件失败", e);
            return ResponseEntity.notFound().build();
        }
    }
}
