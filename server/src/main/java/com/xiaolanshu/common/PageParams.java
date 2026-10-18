package com.xiaolanshu.common;

/** 分页参数解析：page 从 1 起，size 上限 50 */
public record PageParams(int page, int size, int offset) {

    public static PageParams of(Integer page, Integer size) {
        int p = Math.max(1, page == null ? 1 : page);
        int s = Math.min(50, Math.max(1, size == null ? 10 : size));
        return new PageParams(p, s, (p - 1) * s);
    }
}
