package com.snapmeal.storage;
import com.snapmeal.common.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.nio.file.*; import java.util.*;
@RestController @RequestMapping("/api/admin/files")
public class UploadController {
    private static final Set<String> ALLOWED=new HashSet<>(Arrays.asList("image/jpeg","image/png","image/webp"));
    @PostMapping("/upload") public ApiResponse<Map<String,String>> upload(@RequestParam MultipartFile file) throws IOException {if(file.isEmpty())throw new BusinessException("请选择图片");if(!ALLOWED.contains(file.getContentType()))throw new BusinessException("仅支持 JPG、PNG 或 WebP 图片");String ext=file.getOriginalFilename()!=null&&file.getOriginalFilename().contains(".")?file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')):".bin";String name=UUID.randomUUID().toString().replace("-","")+ext;Path dir=Paths.get("uploads");Files.createDirectories(dir);Files.copy(file.getInputStream(),dir.resolve(name),StandardCopyOption.REPLACE_EXISTING);Map<String,String> out=new HashMap<>();out.put("url","/uploads/"+name);out.put("storage","local-mock-oss");return ApiResponse.ok(out);}
}
