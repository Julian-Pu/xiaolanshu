package com.xiaolanshu.upload;

import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final Set<String> ALLOWED_VIDEO_EXT = Set.of("mp4", "webm", "mov", "3gp", "m4v");
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100L * 1024 * 1024;

    private final File imageDir;
    private final File videoDir;
    private final File thumbDir;
    private final ImageProcessor imageProcessor;
    private final StorageService storage;

    public UploadController(@Value("${xiaolanshu.upload-dir}") String uploadDir,
                            ImageProcessor imageProcessor, StorageService storage) {
        File root = new File(uploadDir).getAbsoluteFile();
        this.imageDir = new File(root, "images");
        this.videoDir = new File(root, "videos");
        this.thumbDir = new File(root, "thumbs");
        this.imageProcessor = imageProcessor;
        this.storage = storage;
    }

    public record UploadResult(String url, String thumbUrl) {
    }

    @PostMapping("/image")
    public ApiResponse<UploadResult> image(HttpServletRequest request,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        CurrentUser.require(request);
        if (file.isEmpty()) {
            throw ApiException.badRequest("请选择图片文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw ApiException.badRequest("图片大小超出限制（最大 10MB）");
        }
        String mime = file.getContentType();
        if (mime == null || !mime.startsWith("image/")) {
            throw ApiException.badRequest("仅支持图片文件");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw ApiException.badRequest("不支持的图片格式");
        }
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            throw ApiException.badRequest("存储目录创建失败");
        }
        String filename = System.currentTimeMillis() + "-" + (int) (Math.random() * 1_000_000) + "." + ext;
        // 走存储抽象层落盘（本地实现；生产替换对象存储实现即可）
        String url = storage.save("images", ext, file.getInputStream(), file.getSize());
        String thumbUrl = url;
        // 图片处理管线：jpg/jpeg/png 生成缩略图（含可选水印），其余格式回落原图
        if (imageProcessor.processable(ext)) {
            String thumbName = "thumb-" + filename;
            if (imageProcessor.thumbnail(new File(imageDir, filename), new File(thumbDir, thumbName))) {
                thumbUrl = "/uploads/thumbs/" + thumbName;
            }
        }
        return ApiResponse.ok(new UploadResult(url, thumbUrl));
    }

    /** 视频笔记：视频文件上传（mp4/webm/mov/3gp/m4v，最大 100MB） */
    @PostMapping("/video")
    public ApiResponse<UploadResult> video(HttpServletRequest request,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        CurrentUser.require(request);
        if (file.isEmpty()) {
            throw ApiException.badRequest("请选择视频文件");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw ApiException.badRequest("视频大小超出限制（最大 100MB）");
        }
        String mime = file.getContentType();
        if (mime == null || !mime.startsWith("video/")) {
            throw ApiException.badRequest("仅支持视频文件");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_VIDEO_EXT.contains(ext)) {
            throw ApiException.badRequest("不支持的视频格式（支持 mp4/webm/mov/3gp/m4v）");
        }
        if (!videoDir.exists() && !videoDir.mkdirs()) {
            throw ApiException.badRequest("存储目录创建失败");
        }
        String filename = System.currentTimeMillis() + "-" + (int) (Math.random() * 1_000_000) + "." + ext;
        // 走存储抽象层落盘
        String url = storage.save("videos", ext, file.getInputStream(), file.getSize());
        return ApiResponse.ok(new UploadResult(url, url));
    }

    private String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
