package com.xiaolanshu.album;

import com.xiaolanshu.album.dto.AlbumView;
import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.post.dto.PostCardView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    /** 我的专辑列表（含笔记数） */
    @GetMapping
    public ApiResponse<List<AlbumView>> list(HttpServletRequest request) {
        return ApiResponse.ok(albumService.list(CurrentUser.require(request)));
    }

    /** 创建专辑 */
    @PostMapping
    public ApiResponse<AlbumView> create(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(albumService.create(CurrentUser.require(request), body.get("name")));
    }

    /** 删除专辑（仅本人，级联清空关联） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        albumService.delete(CurrentUser.require(request), id);
        return ApiResponse.ok(null);
    }

    /** 专辑内笔记（分页） */
    @GetMapping("/{id}/posts")
    public ApiResponse<PageResult<PostCardView>> posts(
            @PathVariable long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(albumService.posts(CurrentUser.require(request), id, PageParams.of(page, size)));
    }

    /** 把笔记加入专辑 */
    @PostMapping("/{id}/posts")
    public ApiResponse<Void> addPost(@PathVariable long id, HttpServletRequest request,
                                     @RequestBody Map<String, Long> body) {
        Long postId = body.get("postId");
        if (postId == null) {
            throw ApiException.badRequest("缺少 postId");
        }
        albumService.addPost(CurrentUser.require(request), id, postId);
        return ApiResponse.ok(null);
    }

    /** 从专辑移除笔记 */
    @DeleteMapping("/{id}/posts/{postId}")
    public ApiResponse<Void> removePost(@PathVariable long id, @PathVariable long postId,
                                        HttpServletRequest request) {
        albumService.removePost(CurrentUser.require(request), id, postId);
        return ApiResponse.ok(null);
    }
}
