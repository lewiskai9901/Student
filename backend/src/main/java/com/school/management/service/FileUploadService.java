package com.school.management.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface FileUploadService {

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @return 图片访问URL
     * @throws com.school.management.exception.BusinessException 上传失败时抛出业务异常
     */
    String uploadImage(MultipartFile file);

    /**
     * 上传字体文件
     *
     * @param file 字体文件 (支持 .ttf, .woff, .woff2)
     * @return 字体文件访问URL
     * @throws com.school.management.exception.BusinessException 上传失败时抛出业务异常
     */
    String uploadFont(MultipartFile file);
}
