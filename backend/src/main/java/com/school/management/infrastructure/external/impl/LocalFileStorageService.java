package com.school.management.infrastructure.external.impl;

import com.school.management.infrastructure.external.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储服务实现
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/api/files}")
    private String urlPrefix;

    @Override
    public String upload(MultipartFile file, String directory) {
        try {
            return upload(file.getInputStream(), file.getOriginalFilename(), directory);
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public String upload(InputStream inputStream, String fileName, String directory) {
        try {
            // 生成存储路径: directory/yyyy/MM/dd/uuid_filename
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String storagePath = directory + "/" + datePath;

            // 生成唯一文件名
            String extension = getFileExtension(fileName);
            String newFileName = UUID.randomUUID().toString() + extension;

            // 创建目录
            Path directoryPath = Paths.get(uploadPath, storagePath);
            Files.createDirectories(directoryPath);

            // 保存文件
            Path filePath = directoryPath.resolve(newFileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            // 返回访问URL
            String relativePath = storagePath + "/" + newFileName;
            log.info("File uploaded: {}", relativePath);
            return urlPrefix + "/" + relativePath;

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public boolean delete(String fileUrl) {
        try {
            if (fileUrl == null || !fileUrl.startsWith(urlPrefix)) {
                return false;
            }

            String relativePath = fileUrl.substring(urlPrefix.length() + 1);
            Path filePath = Paths.get(uploadPath, relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted: {}", relativePath);
                return true;
            }
            return false;

        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
            return false;
        }
    }

    @Override
    public String getUrl(String filePath) {
        return urlPrefix + "/" + filePath;
    }

    @Override
    public boolean exists(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(urlPrefix)) {
            return false;
        }

        String relativePath = fileUrl.substring(urlPrefix.length() + 1);
        Path filePath = Paths.get(uploadPath, relativePath);
        return Files.exists(filePath);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }
}
