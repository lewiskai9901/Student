package com.school.management.interfaces.rest.file;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.external.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private static final Set<String> ALLOWED_DIRS = Set.of("inspection", "attachments");
    private static final String DEFAULT_DIR = "inspection";

    private final FileStorageService storageService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "directory", required = false) String directory) {
        String dir = (directory == null || directory.isBlank()) ? DEFAULT_DIR : directory;
        if (!ALLOWED_DIRS.contains(dir)) {
            throw new IllegalArgumentException("directory '" + dir + "' is not allowed");
        }
        String url = storageService.upload(file, dir);
        return Result.success(UploadResponse.builder()
            .fileName(file.getOriginalFilename())
            .fileUrl(url)
            .size(file.getSize())
            .build());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResponse {
        private String fileName;
        private String fileUrl;
        private Long size;
    }
}
