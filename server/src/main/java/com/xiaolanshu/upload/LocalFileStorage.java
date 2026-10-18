package com.xiaolanshu.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储实现：文件落在 <upload-dir>/<category>/ 下，URL 为 /uploads/<category>/<filename>。
 * 生产替换为对象存储实现时，仅需实现 StorageService 接口并保持 URL 前缀 /uploads/ 不变。
 */
@Component
public class LocalFileStorage implements StorageService {

    private final File root;

    public LocalFileStorage(@Value("${xiaolanshu.upload-dir}") String uploadDir) {
        this.root = new File(uploadDir).getAbsoluteFile();
    }

    @Override
    public String save(String category, String ext, InputStream in, long size) throws IOException {
        File dir = new File(root, category);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("存储目录创建失败: " + dir);
        }
        String filename = System.currentTimeMillis() + "-" + (int) (Math.random() * 1_000_000) + "." + ext;
        File target = new File(dir, filename);
        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + category + "/" + filename;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        // /uploads/images/xxx.jpg → <root>/images/xxx.jpg
        String rel = url.substring("/uploads/".length());
        File target = new File(root, rel);
        // 防目录穿越：仅允许 root 内
        try {
            if (target.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
                target.delete();
            }
        } catch (IOException ignored) {
        }
    }
}
