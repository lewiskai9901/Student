package com.school.management.service.impl;

import com.school.management.entity.SystemConfig;
import com.school.management.exception.BusinessException;
import com.school.management.service.FileUploadService;
import com.school.management.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    // 默认允许的图片格式(当配置不存在时使用)
    private static final List<String> DEFAULT_ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // 默认最大文件大小 5MB(当配置不存在时使用)
    private static final long DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 允许的图片文件扩展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    // 允许的字体文件扩展名
    private static final List<String> ALLOWED_FONT_EXTENSIONS = Arrays.asList(
            ".ttf", ".woff", ".woff2"
    );

    // 允许的字体 Content-Type
    private static final List<String> ALLOWED_FONT_CONTENT_TYPES = Arrays.asList(
            "font/ttf", "font/woff", "font/woff2",
            "application/x-font-ttf", "application/x-font-truetype",
            "application/font-woff", "application/font-woff2",
            "application/octet-stream" // 部分浏览器可能发送这个类型
    );

    // 默认最大字体文件大小 10MB
    private static final long DEFAULT_MAX_FONT_SIZE = 10 * 1024 * 1024;

    // 图片文件魔数（用于验证真实文件类型）
    private static final Map<String, byte[]> IMAGE_MAGIC_BYTES = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "gif", new byte[]{0x47, 0x49, 0x46, 0x38},
            "webp", new byte[]{0x52, 0x49, 0x46, 0x46}  // RIFF header
    );

    private final SystemConfigService systemConfigService;

    @Value("${file.upload.path:D:/学生管理系统/uploads}")
    private String uploadBasePath;

    @Value("${file.upload.url-prefix:http://localhost:8080/uploads}")
    private String urlPrefix;

    @Override
    public String uploadImage(MultipartFile file) {
        // 1. 验证文件
        validateImageFile(file);

        // 2. 生成文件路径
        String relativePath = generateFilePath(file);

        // 3. 保存文件
        String fullPath = uploadBasePath + File.separator + relativePath;
        try {
            saveFile(file, fullPath);
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException("文件保存失败，请稍后重试");
        }

        // 4. 返回访问URL
        String url = urlPrefix + "/" + relativePath.replace("\\", "/");
        log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), url);
        return url;
    }

    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException("非法的文件名");
        }

        // 验证文件扩展名
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件扩展名,仅支持: " + getAllowedTypesDisplay());
        }

        // 获取允许的文件类型配置
        List<String> allowedTypes = getAllowedFileTypes();

        // 验证 Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new BusinessException("不支持的图片格式,仅支持: " + getAllowedTypesDisplay());
        }

        // 验证文件魔数（防止伪造文件类型）
        if (!validateFileMagicBytes(file, extension)) {
            log.warn("文件魔数验证失败: filename={}, contentType={}", originalFilename, contentType);
            throw new BusinessException("文件内容与扩展名不匹配，请上传正确的图片文件");
        }

        // 获取最大文件大小配置
        long maxFileSize = getMaxFileSize();

        // 验证文件大小
        if (file.getSize() > maxFileSize) {
            long maxSizeMB = maxFileSize / (1024 * 1024);
            throw new BusinessException("文件大小不能超过" + maxSizeMB + "MB");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 验证文件魔数
     */
    private boolean validateFileMagicBytes(MultipartFile file, String extension) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12]; // 读取前12个字节
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                return false;
            }

            // 根据扩展名获取预期的魔数
            String extKey = extension.replace(".", "").toLowerCase();
            if ("jpeg".equals(extKey)) {
                extKey = "jpg";
            }

            byte[] expectedMagic = IMAGE_MAGIC_BYTES.get(extKey);
            if (expectedMagic == null) {
                // 未知类型，跳过魔数验证
                return true;
            }

            // 比较魔数
            for (int i = 0; i < expectedMagic.length && i < bytesRead; i++) {
                if (header[i] != expectedMagic[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            log.error("读取文件魔数失败", e);
            return false;
        }
    }

    /**
     * 获取最大文件大小配置(字节)
     */
    private long getMaxFileSize() {
        try {
            SystemConfig config = systemConfigService.getByKey("business.max_file_size");
            if (config != null && config.getConfigValue() != null) {
                // 配置值单位为MB,需要转换为字节
                return Long.parseLong(config.getConfigValue()) * 1024 * 1024;
            }
        } catch (Exception e) {
            log.warn("获取文件大小限制配置失败,使用默认值: {}", e.getMessage());
        }
        return DEFAULT_MAX_FILE_SIZE;
    }

    /**
     * 获取允许的文件类型配置
     */
    private List<String> getAllowedFileTypes() {
        try {
            SystemConfig config = systemConfigService.getByKey("business.allowed_file_types");
            if (config != null && config.getConfigValue() != null) {
                // 配置值格式: "jpg,jpeg,png,gif,webp"
                String[] types = config.getConfigValue().split(",");
                return Arrays.stream(types)
                        .map(type -> "image/" + type.trim().toLowerCase())
                        .toList();
            }
        } catch (Exception e) {
            log.warn("获取允许文件类型配置失败,使用默认值: {}", e.getMessage());
        }
        return DEFAULT_ALLOWED_IMAGE_TYPES;
    }

    /**
     * 获取允许的文件类型显示名称
     */
    private String getAllowedTypesDisplay() {
        try {
            SystemConfig config = systemConfigService.getByKey("business.allowed_file_types");
            if (config != null && config.getConfigValue() != null) {
                return config.getConfigValue();
            }
        } catch (Exception e) {
            // 使用默认值
        }
        return "jpg, jpeg, png, gif, webp";
    }

    /**
     * 生成文件路径
     * 格式: check-photos/2025/01/01/uuid.jpg
     */
    private String generateFilePath(MultipartFile file) {
        // 按日期分目录
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        return "check-photos" + File.separator + dateDir + File.separator + filename;
    }

    /**
     * 保存文件到磁盘
     */
    private void saveFile(MultipartFile file, String fullPath) throws IOException {
        Path path = Paths.get(fullPath);

        // 创建目录
        Files.createDirectories(path.getParent());

        // 保存文件
        file.transferTo(path.toFile());
    }

    @Override
    public String uploadFont(MultipartFile file) {
        // 1. 验证字体文件
        validateFontFile(file);

        // 2. 生成文件路径
        String relativePath = generateFontFilePath(file);

        // 3. 保存文件
        String fullPath = uploadBasePath + File.separator + relativePath;
        try {
            saveFile(file, fullPath);
        } catch (IOException e) {
            log.error("字体文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException("字体文件保存失败，请稍后重试");
        }

        // 4. 返回访问URL
        String url = urlPrefix + "/" + relativePath.replace("\\", "/");
        log.info("字体文件上传成功: {} -> {}", file.getOriginalFilename(), url);
        return url;
    }

    /**
     * 验证字体文件
     */
    private void validateFontFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException("非法的文件名");
        }

        // 验证文件扩展名
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_FONT_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的字体格式,仅支持: ttf, woff, woff2");
        }

        // 验证 Content-Type (字体文件类型检测较宽松)
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_FONT_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("字体文件Content-Type不匹配: {}, filename: {}", contentType, originalFilename);
            // 对于字体文件,我们主要依赖扩展名验证,不强制检查Content-Type
        }

        // 验证文件大小
        if (file.getSize() > DEFAULT_MAX_FONT_SIZE) {
            throw new BusinessException("字体文件不能超过10MB");
        }
    }

    /**
     * 生成字体文件路径
     * 格式: fonts/uuid.ttf
     */
    private String generateFontFilePath(MultipartFile file) {
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        return "fonts" + File.separator + filename;
    }
}
