package com.xiaolanshu.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 举报笔记请求：{ postId, reason, detail } */
public record CreateReportRequest(
        @NotNull(message = "笔记 ID 不能为空")
        Long postId,

        @NotBlank(message = "举报理由不能为空")
        @Pattern(regexp = "^(spam|porn|illegal|other)$", message = "举报理由不合法")
        String reason,

        @Size(max = 500, message = "补充说明最长 500 字")
        String detail
) {
}
