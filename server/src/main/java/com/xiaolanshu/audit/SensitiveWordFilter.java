package com.xiaolanshu.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 文本机审：内置演示敏感词库（赌博/色情/诈骗/违禁品类），
 * 命中即拦截（发布/编辑/评论统一返回 400，并写入 audit_logs 供人工复核）。
 * 生产可替换为第三方内容安全 API（机审 + 人工双层流水线）。
 */
@Component
public class SensitiveWordFilter {

    /** 演示词库（红线词，仅用于本地演示拦截效果） */
    private static final List<String> DEFAULT_WORDS = List.of(
            "博彩", "时时彩", "百家乐", "澳门赌场", "赌球", "赌博",
            "黄色视频", "约炮", "一夜情", "裸聊",
            "刷单", "兼职刷单", "高利贷", "办假证",
            "冰毒", "大麻", "海洛因", "枪支", "炸药", "砍人");

    private final boolean enabled;
    private final List<String> words;

    public SensitiveWordFilter(@Value("${xiaolanshu.audit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
        this.words = DEFAULT_WORDS;
    }

    /** 返回命中的敏感词列表；开关关闭或文本安全时返回空列表 */
    public List<String> findHits(String text) {
        List<String> hits = new ArrayList<>();
        if (!enabled || text == null || text.isBlank()) {
            return hits;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String w : words) {
            if (lower.contains(w.toLowerCase(Locale.ROOT))) {
                hits.add(w);
            }
        }
        return hits;
    }
}
