package com.school.management.service;

import com.school.management.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 文件上传响应
     */
    FileUploadResponse uploadFile(MultipartFile file, String businessType, Long businessId);

    /**
     * 批量上传文件
     *
     * @param files 文件列表
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 文件上传响应列表
     */
    List<FileUploadResponse> uploadFiles(List<MultipartFile> files, String businessType, Long businessId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     */
    void deleteFile(Long fileId);

    /**
     * 根据业务ID查询文件列表
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 文件列表
     */
    List<FileUploadResponse> getFilesByBusiness(String businessType, Long businessId);

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件字节数组
     */
    byte[] downloadFile(Long fileId);

    /**
     * 获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件上传响应
     */
    FileUploadResponse getFileInfo(Long fileId);
}
