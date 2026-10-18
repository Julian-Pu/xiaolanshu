package com.xiaolanshu.report;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.report.dto.CreateReportRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 用户举报：POST /api/reports { postId, reason, detail }（需登录） */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(HttpServletRequest request,
                                                   @Valid @RequestBody CreateReportRequest req) {
        long userId = CurrentUser.require(request);
        Map<String, Object> r = reportService.create(userId, req.postId(), req.reason(), req.detail());
        return ApiResponse.ok(r);
    }
}
