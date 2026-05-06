package com.school.management.interfaces.rest.file;

import com.school.management.infrastructure.external.FileStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadControllerTest {

    @Test
    void upload_returnsFilenameAndUrlForWhitelistedDirectory() {
        FileStorageService storage = Mockito.mock(FileStorageService.class);
        Mockito.when(storage.upload(Mockito.any(), Mockito.eq("inspection")))
               .thenReturn("https://cdn.test/inspection/abc.jpg");

        FileUploadController controller = new FileUploadController(storage);
        MockMultipartFile file = new MockMultipartFile("file", "abc.jpg", "image/jpeg", new byte[]{1, 2, 3});

        var result = controller.upload(file, "inspection");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getFileName()).isEqualTo("abc.jpg");
        assertThat(result.getData().getFileUrl()).isEqualTo("https://cdn.test/inspection/abc.jpg");
        assertThat(result.getData().getSize()).isEqualTo(3L);
    }

    @Test
    void upload_rejectsDirectoryNotInWhitelist() {
        FileStorageService storage = Mockito.mock(FileStorageService.class);
        FileUploadController controller = new FileUploadController(storage);
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> controller.upload(file, "../../etc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("directory");
    }

    @Test
    void upload_defaultsToInspectionWhenDirectoryNull() {
        FileStorageService storage = Mockito.mock(FileStorageService.class);
        Mockito.when(storage.upload(Mockito.any(), Mockito.eq("inspection")))
               .thenReturn("https://cdn.test/inspection/x.jpg");

        FileUploadController controller = new FileUploadController(storage);
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[]{0});

        var result = controller.upload(file, null);
        assertThat(result.getData().getFileUrl()).isEqualTo("https://cdn.test/inspection/x.jpg");
    }
}
