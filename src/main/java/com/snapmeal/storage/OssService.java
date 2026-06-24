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
        String original = file.getOriginalFilename();
        String ext = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".bin";
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
