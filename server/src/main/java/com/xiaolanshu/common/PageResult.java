package com.xiaolanshu.common;

import java.util.List;

/** 统一分页结构：{ list, page, size, total, hasMore } */
public record PageResult<T>(List<T> list, int page, int size, long total, boolean hasMore) {

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list, page, size, total, (long) page * size < total);
    }
}
