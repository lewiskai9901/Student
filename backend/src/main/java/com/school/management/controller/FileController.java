package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.FileUploadResponse;
import com.school.management.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件管理控制器
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传单个文件")
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<FileUploadResponse> uploadFile(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务类型") @RequestParam(value = "businessType", required = false) String businessType,
            @Parameter(description = "业务ID") @RequestParam(value = "businessId", required = false) Long businessId) {
        FileUploadResponse response = fileService.uploadFile(file, businessType, businessId);
        return Result.success(response);
    }

    @Operation(summary = "批量上传文件")
    @PostMapping("/upload/batch")
    @PreAuthorize("isAuthenticated()")
    public Result<List<FileUploadResponse>> uploadFiles(
            @Parameter(description = "上传的文件列表") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "业务类型") @RequestParam(value = "businessType", required = false) String businessType,
            @Parameter(description = "业务ID") @RequestParam(value = "businessId", required = false) Long businessId) {
        List<FileUploadResponse> responses = fileService.uploadFiles(files, businessType, businessId);
        return Result.success(responses);
    }

    @Operation(summary = "下载文件")
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "文件ID") @PathVariable Long fileId) {
        byte[] fileData = fileService.downloadFile(fileId);
        FileUploadResponse fileInfo = fileService.getFileInfo(fileId);

        String encodedFilename = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        // 判断是否为图片文件
        String fileType = fileInfo.getFileType().toLowerCase();
        boolean isImage = fileType.equals("jpg") || fileType.equals("jpeg") ||
                         fileType.equals("png") || fileType.equals("gif") ||
                         fileType.equals("bmp") || fileType.equals("webp");

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        if (isImage) {
            // 图片文件: 使用对应的MIME类型, inline方式显示
            MediaType mediaType = switch (fileType) {
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "png" -> MediaType.IMAGE_PNG;
                case "gif" -> MediaType.IMAGE_GIF;
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
            builder.contentType(mediaType)
                   .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename);
        } else {
            // 非图片文件: 强制下载
            builder.contentType(MediaType.APPLICATION_OCTET_STREAM)
                   .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        }

        return builder.body(fileData);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasAnyAuthority('file:delete', 'admin')")
    public Result<Void> deleteFile(
            @Parameter(description = "文件ID") @PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return Result.success();
    }

    @Operation(summary = "获取文件信息")
    @GetMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public Result<FileUploadResponse> getFileInfo(
            @Parameter(description = "文件ID") @PathVariable Long fileId) {
        FileUploadResponse response = fileService.getFileInfo(fileId);
        return Result.success(response);
    }

    @Operation(summary = "根据业务查询文件列表")
    @GetMapping("/business")
    @PreAuthorize("isAuthenticated()")
    public Result<List<FileUploadResponse>> getFilesByBusiness(
            @Parameter(description = "业务类型") @RequestParam String businessType,
            @Parameter(description = "业务ID") @RequestParam Long businessId) {
        List<FileUploadResponse> responses = fileService.getFilesByBusiness(businessType, businessId);
        return Result.success(responses);
    }
}
