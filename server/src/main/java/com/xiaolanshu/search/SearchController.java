package com.xiaolanshu.search;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.common.PageParams;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ApiResponse<SearchService.SearchResult> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        if (q.isBlank()) {
            return ApiResponse.ok(new SearchService.SearchResult(java.util.List.of(), java.util.List.of()));
        }
        return ApiResponse.ok(searchService.search(q.trim(), type, PageParams.of(page, size), CurrentUser.optional(request)));
    }

    @GetMapping("/hot")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> hot() {
        return ApiResponse.ok(searchService.hot());
    }
}
