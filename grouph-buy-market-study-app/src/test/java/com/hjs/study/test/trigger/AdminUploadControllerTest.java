package com.hjs.study.test.trigger;

import com.hjs.study.api.dto.UploadImageResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.trigger.http.AdminUploadController;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;

/**
 * 商品图片上传的格式、体积与随机文件名测试。
 */
public class AdminUploadControllerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldAcceptPngAndIgnorePathInOriginalName() throws Exception {
        AdminUploadController controller = controller();
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../evil.png", "image/png", pngBytes());

        Response<UploadImageResponseDTO> response = controller.upload(file);

        Assert.assertEquals("0000", response.getCode());
        Assert.assertFalse(response.getData().getFileName().contains(".."));
        Assert.assertTrue(Files.exists(
                temporaryFolder.getRoot().toPath().resolve(response.getData().getFileName())));
    }

    @Test
    public void shouldRejectMismatchedExtensionAndSignature() throws Exception {
        AdminUploadController controller = controller();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", pngBytes());
        Assert.assertEquals("0002", controller.upload(file).getCode());
    }

    @Test
    public void shouldRejectFileLargerThanFiveMegabytes() throws Exception {
        AdminUploadController controller = controller();
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        oversized[0] = (byte) 0x89;
        oversized[1] = 0x50;
        oversized[2] = 0x4e;
        oversized[3] = 0x47;
        MockMultipartFile file = new MockMultipartFile(
                "file", "too-big.png", "image/png", oversized);
        Assert.assertEquals("0002", controller.upload(file).getCode());
    }

    private AdminUploadController controller() {
        AdminUploadController controller = new AdminUploadController();
        ReflectionTestUtils.setField(controller, "uploadDir", temporaryFolder.getRoot().getAbsolutePath());
        return controller;
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
