package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.dto.FileUploadResponse;
import com.school.management.entity.FileInfo;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.FileInfoMapper;
import com.school.management.service.FileService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileInfoMapper fileInfoMapper;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.max-size}")
    private Long maxSize;

    @Value("${file.upload.allowed-types}")
    private String allowedTypes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResponse uploadFile(MultipartFile file, String businessType, Long businessId) {
        // 验证文件
        validateFile(file);

        // 生成文件存储信息
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String storedName = generateFileName(fileExtension);

        // 创建日期目录
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = dateDir + "/" + storedName;
        Path fullPath = Paths.get(uploadPath, relativePath);

        try {
            // 创建目录
            Files.createDirectories(fullPath.getParent());

            // 保存文件
            file.transferTo(fullPath.toFile());

            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setOriginalName(originalFilename);
            fileInfo.setStoredName(storedName);
            fileInfo.setFilePath(relativePath);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFileType(fileExtension);
            fileInfo.setMimeType(file.getContentType());
            fileInfo.setBusinessType(businessType);
            fileInfo.setBusinessId(businessId);
            fileInfo.setUploaderId(SecurityUtils.getCurrentUserId());
            // 先不设置fileUrl,插入后再设置(需要ID)
            fileInfo.setFileUrl(null);

            fileInfoMapper.insert(fileInfo);

            // 插入后设置正确的fileUrl(使用fileId)
            fileInfo.setFileUrl("/api/files/download/" + fileInfo.getId());
            fileInfoMapper.updateById(fileInfo);

            return buildFileUploadResponse(fileInfo);

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FileUploadResponse> uploadFiles(List<MultipartFile> files, String businessType, Long businessId) {
        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(uploadFile(file, businessType, businessId));
        }
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        // 删除物理文件
        Path filePath = Paths.get(uploadPath, fileInfo.getFilePath());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("删除物理文件失败: {}", e.getMessage(), e);
        }

        // 删除数据库记录
        fileInfoMapper.deleteById(fileId);
    }

    @Override
    public List<FileUploadResponse> getFilesByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getBusinessType, businessType)
               .eq(FileInfo::getBusinessId, businessId)
               .orderByDesc(FileInfo::getCreatedAt);

        List<FileInfo> fileInfos = fileInfoMapper.selectList(wrapper);
        return fileInfos.stream()
                .map(this::buildFileUploadResponse)
                .toList();
    }

    @Override
    public byte[] downloadFile(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        Path filePath = Paths.get(uploadPath, fileInfo.getFilePath());
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage(), e);
            throw new BusinessException("文件读取失败");
        }
    }

    @Override
    public FileUploadResponse getFileInfo(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        return buildFileUploadResponse(fileInfo);
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }

        // 验证文件类型
        String fileExtension = getFileExtension(file.getOriginalFilename());
        List<String> allowedTypeList = Arrays.asList(allowedTypes.split(","));
        if (!allowedTypeList.contains(fileExtension.toLowerCase())) {
            throw new BusinessException("不支持的文件类型: " + fileExtension);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    /**
     * 构建文件上传响应
     */
    private FileUploadResponse buildFileUploadResponse(FileInfo fileInfo) {
        return FileUploadResponse.builder()
                .fileId(fileInfo.getId())
                .originalName(fileInfo.getOriginalName())
                .storedName(fileInfo.getStoredName())
                .fileSize(fileInfo.getFileSize())
                .fileType(fileInfo.getFileType())
                .fileUrl(fileInfo.getFileUrl())
                .uploadTime(fileInfo.getCreatedAt() != null ?
                        fileInfo.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .build();
    }
}
