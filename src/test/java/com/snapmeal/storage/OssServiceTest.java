package com.snapmeal.storage;

import com.snapmeal.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OssService 上传校验单测：Content-Type + 扩展名 + 魔数三重校验。 */
class OssServiceTest {

    private final OssService oss = new OssService("local", "", "", "", "");

    private static final byte[] PNG = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] WEBP = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    private static final byte[] HTML = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

    @AfterEach
    void cleanUploads() throws Exception {
        Path dir = Paths.get("uploads");
        if (Files.exists(dir)) {
            try (Stream<Path> list = Files.list(dir)) {
                for (Path p : list.toArray(Path[]::new)) Files.deleteIfExists(p);
            }
        }
    }

    @Test
    void acceptsRealImageContent() throws Exception {
        Map<String, String> r = oss.upload(new MockMultipartFile("file", "a.png", "image/png", PNG));
        assertTrue(r.get("url").endsWith(".png"));
    }

    @Test
    void acceptsJpegAndWebp() throws Exception {
        assertTrue(oss.upload(new MockMultipartFile("file", "b.jpg", "image/jpeg", JPEG)).get("url").endsWith(".jpg"));
        assertTrue(oss.upload(new MockMultipartFile("file", "c.webp", "image/webp", WEBP)).get("url").endsWith(".webp"));
    }

    @Test
    void rejectsHtmlFileWithImageContentType() {
        // 声明 image/png 但文件名是 .html：扩展名白名单拦截
        assertThrows(BusinessException.class, () -> oss.upload(new MockMultipartFile("file", "x.html", "image/png", HTML)));
    }

    @Test
    void rejectsHtmlContentType() {
        assertThrows(BusinessException.class, () -> oss.upload(new MockMultipartFile("file", "x.png", "text/html", HTML)));
    }

    @Test
    void rejectsSvg() {
        assertThrows(BusinessException.class, () -> oss.upload(new MockMultipartFile("file", "x.svg", "image/svg+xml", HTML)));
    }

    @Test
    void rejectsMismatchedExtensionAndContent() {
        // 内容确为 PNG，但扩展名伪装成 .html：扩展名校验应优先拦截
        assertThrows(BusinessException.class, () -> oss.upload(new MockMultipartFile("file", "x.html", "image/png", PNG)));
    }

    @Test
    void rejectsSpoofedImageContent() {
        // 扩展名/类型都像图片，但内容不是任何受支持图片格式
        assertThrows(BusinessException.class, () -> oss.upload(new MockMultipartFile("file", "x.png", "image/png", HTML)));
    }
}
