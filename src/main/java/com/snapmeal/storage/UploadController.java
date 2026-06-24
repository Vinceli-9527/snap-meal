package com.snapmeal.storage;

import com.snapmeal.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/files")
public class UploadController {
    private final OssService oss;

    public UploadController(OssService oss) {
        this.oss = oss;
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam MultipartFile file) throws IOException {
        return ApiResponse.ok(oss.upload(file));
    }
}
