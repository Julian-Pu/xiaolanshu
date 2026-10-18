package com.xiaolanshu.upload;

import java.io.IOException;
import java.io.InputStream;

/**
 * 存储抽象：上传文件的落盘/访问统一走该接口。
 * 当前为本地磁盘实现（LocalFileStorage）；生产可直接替换为对象存储实现
 * （阿里云 OSS / 腾讯云 COS / MinIO / S3），URL 约定保持 /uploads/... 不变。
 */
public interface StorageService {

    /**
     * 保存文件，返回可访问的相对 URL（如 /uploads/images/xxx.jpg）。
     *
     * @param category 存储分类：images | videos
     * @param ext      文件扩展名（不含点）
     * @param in       文件内容流
     * @param size     文件大小（字节）
     */
    String save(String category, String ext, InputStream in, long size) throws IOException;

    /** 删除文件（编辑/删除笔记时清理资源用；本地实现幂等） */
    void delete(String url);
}
