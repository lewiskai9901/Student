package com.school.management.infrastructure.external;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param file       文件
     * @param directory  存储目录
     * @return 文件访问URL
     */
    String upload(MultipartFile file, String directory);

    /**
     * 上传文件
     *
     * @param inputStream 文件流
     * @param fileName    文件名
     * @param directory   存储目录
     * @return 文件访问URL
     */
    String upload(InputStream inputStream, String fileName, String directory);

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    boolean delete(String fileUrl);

    /**
     * 获取文件访问URL
     *
     * @param filePath 文件路径
     * @return 访问URL
     */
    String getUrl(String filePath);

    /**
     * 检查文件是否存在
     *
     * @param fileUrl 文件URL
     * @return 是否存在
     */
    boolean exists(String fileUrl);

    /**
     * 存储目录常量
     */
    interface Directory {
        String AVATAR = "avatars";
        String ATTACHMENT = "attachments";
        String INSPECTION = "inspection";
        String EXPORT = "exports";
        String TEMP = "temp";
    }
}
