package com.snapmeal.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.snapmeal.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OssService {
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList("image/jpeg", "image/png", "image/webp"));
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png", ".webp"));
    private final String mode;
    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucketName;

    public OssService(@Value("${sky.integrations.oss.mode:${sky.integrations.oss-mode:local}}") String mode,
                      @Value("${sky.integrations.oss.endpoint:}") String endpoint,
                      @Value("${sky.integrations.oss.access-key-id:}") String accessKeyId,
                      @Value("${sky.integrations.oss.access-key-secret:}") String accessKeySecret,
                      @Value("${sky.integrations.oss.bucket-name:}") String bucketName) {
        this.mode = mode;
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
    }

    public Map<String, String> upload(MultipartFile file) throws IOException {
        validate(file);
        if ("oss".equalsIgnoreCase(mode)) return uploadToOss(file);
        return uploadToLocal(file);
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("请选择图片");
        if (!ALLOWED.contains(file.getContentType())) throw new BusinessException("仅支持 JPG、PNG 或 WebP 图片");
        String original = file.getOriginalFilename();
        String ext = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(ext)) throw new BusinessException("仅支持 JPG、PNG 或 WebP 图片");
        if (sniffImageType(file) == null) throw new BusinessException("图片内容校验失败，仅支持 JPG、PNG 或 WebP");
    }

    /** 按文件头魔数识别真实图片类型：JPEG(FF D8 FF)、PNG(89 50 4E 47)、WebP(RIFF....WEBP)；识别失败返回 null。 */
    private static String sniffImageType(MultipartFile file) {
        try {
            byte[] head = file.getBytes();
            if (head.length < 12) return null;
            if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) return "jpg";
            if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') return "png";
            if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') return "webp";
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private Map<String, String> uploadToLocal(MultipartFile file) throws IOException {
        String name = buildFileName(file);
        Path dir = Paths.get("uploads");
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Map<String, String> out = new HashMap<>();
        out.put("url", "/uploads/" + name);
        out.put("storage", "local-mock-oss");
        return out;
    }

    private Map<String, String> uploadToOss(MultipartFile file) throws IOException {
        if (isBlank(endpoint) || isBlank(accessKeyId) || isBlank(accessKeySecret) || isBlank(bucketName)) {
            throw new BusinessException("OSS 配置不完整");
        }
        String objectName = "uploads/" + buildFileName(file);
        OSS oss = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            oss.putObject(bucketName, objectName, file.getInputStream());
        } finally {
            oss.shutdown();
        }
        Map<String, String> out = new HashMap<>();
        out.put("url", publicUrl(objectName));
        out.put("storage", "oss");
        return out;
    }

    private String buildFileName(MultipartFile file) {
        // 扩展名以魔数识别结果为准，不信任原始文件名
        String sniffed = sniffImageType(file);
        String ext = sniffed == null ? ".bin" : "." + sniffed;
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private String publicUrl(String objectName) {
        String cleanEndpoint = endpoint.replaceFirst("^https?://", "");
        return "https://" + bucketName + "." + cleanEndpoint + "/" + objectName;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
