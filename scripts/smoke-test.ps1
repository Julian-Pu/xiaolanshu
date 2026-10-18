# 小蓝书后端接口冒烟测试脚本（PowerShell）
# 用法：powershell -File smoke-test.ps1
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:3000/api'
$out = @()

function Step($name, $script) {
    try {
        $r = & $script
        $out += "[OK] $name"
        return $r
    } catch {
        $out += "[FAIL] $name :: $($_.Exception.Message)"
        return $null
    }
}

$headers = @{ 'Content-Type' = 'application/json' }

# 1. 登录
$login = Step '登录(13800000001)' {
    Invoke-RestMethod -Uri "$base/auth/login" -Method Post -Headers $headers `
        -Body '{"phone":"13800000001","password":"123456"}' -TimeoutSec 8
}
if (-not $login) { $out; exit 1 }
$token = $login.data.token
$auth = @{ Authorization = "Bearer $token" }

# 2. 发送短信验证码 + 注册(新号)
$sms = Step '发送验证码(13900000099)' {
    Invoke-RestMethod -Uri "$base/auth/sms-code" -Method Post -Headers $headers `
        -Body '{"phone":"13900000099"}' -TimeoutSec 8
}
$smsCode = if ($sms -and $sms.data -and $sms.data.code) { $sms.data.code } else { '' }
if (-not $smsCode) { $out += "[WARN] 验证码未回显（sms.debug=false 时只能看服务器日志）" }
Step '注册(13900000099)' {
    Invoke-RestMethod -Uri "$base/auth/register" -Method Post -Headers $headers `
        -Body ('{"phone":"13900000099","password":"123456","nickname":"冒烟测试用户","smsCode":"' + $smsCode + '"}') -TimeoutSec 8
}

# 3. 信息流
$feed = Step '信息流-推荐' { Invoke-RestMethod -Uri "$base/feed?tab=recommend&page=1&size=5" -Headers $auth -TimeoutSec 8 }
$feedFollowing = Step '信息流-关注' { Invoke-RestMethod -Uri "$base/feed?tab=following&page=1&size=5" -Headers $auth -TimeoutSec 8 }
$firstPost = if ($feed.data.list.Count -gt 0) { $feed.data.list[0].id } else { 1 }

# 4. 笔记详情
Step '笔记详情' { Invoke-RestMethod -Uri "$base/posts/$firstPost" -Headers $auth -TimeoutSec 8 }

# 5. 点赞 / 取消点赞
Step '点赞' { Invoke-RestMethod -Uri "$base/posts/$firstPost/like" -Method Post -Headers $auth -TimeoutSec 8 }
Step '取消点赞' { Invoke-RestMethod -Uri "$base/posts/$firstPost/like" -Method Delete -Headers $auth -TimeoutSec 8 }

# 6. 收藏 / 取消收藏
Step '收藏' { Invoke-RestMethod -Uri "$base/posts/$firstPost/collect" -Method Post -Headers $auth -TimeoutSec 8 }
Step '取消收藏' { Invoke-RestMethod -Uri "$base/posts/$firstPost/collect" -Method Delete -Headers $auth -TimeoutSec 8 }

# 7. 评论
$cmt = Step '发表评论' {
    Invoke-RestMethod -Uri "$base/posts/$firstPost/comments" -Method Post -Headers $auth `
        -Body '{"content":"冒烟测试评论"}' -TimeoutSec 8
}
Step '评论列表' { Invoke-RestMethod -Uri "$base/posts/$firstPost/comments?page=1&size=10" -Headers $auth -TimeoutSec 8 }
if ($cmt -and $cmt.data.id) {
    Step '删除评论' { Invoke-RestMethod -Uri "$base/comments/$($cmt.data.id)" -Method Delete -Headers $auth -TimeoutSec 8 }
}

# 8. 发布笔记（使用 seed 图）
$newPost = Step '发布笔记' {
    Invoke-RestMethod -Uri "$base/posts" -Method Post -Headers $auth `
        -Body '{"title":"冒烟测试笔记","content":"这是一条由自动化脚本发布的测试内容","images":["/uploads/seed/p1.svg"],"topicId":1}' -TimeoutSec 8
}
if ($newPost -and $newPost.data.id) {
    Step '删除笔记' { Invoke-RestMethod -Uri "$base/posts/$($newPost.data.id)" -Method Delete -Headers $auth -TimeoutSec 8 }
}

# 9. 话题 / 搜索
Step '话题列表' { Invoke-RestMethod -Uri "$base/topics" -Headers $auth -TimeoutSec 8 }
Step '搜索-笔记' { Invoke-RestMethod -Uri "$base/search?q=咖啡&type=all" -Headers $auth -TimeoutSec 8 }
Step '搜索-用户' { Invoke-RestMethod -Uri "$base/search?q=蓝&type=user" -Headers $auth -TimeoutSec 8 }

# 10. 用户
Step '用户主页' { Invoke-RestMethod -Uri "$base/users/1" -Headers $auth -TimeoutSec 8 }
Step '用户笔记' { Invoke-RestMethod -Uri "$base/users/1/posts?page=1&size=5" -Headers $auth -TimeoutSec 8 }
Step '关注/取关' { Invoke-RestMethod -Uri "$base/users/2/follow" -Method Post -Headers $auth -TimeoutSec 8 }
Step '取关' { Invoke-RestMethod -Uri "$base/users/2/follow" -Method Delete -Headers $auth -TimeoutSec 8 }

# 11. 通知
Step '通知列表' { Invoke-RestMethod -Uri "$base/notifications?page=1&size=10" -Headers $auth -TimeoutSec 8 }
Step '通知已读' { Invoke-RestMethod -Uri "$base/notifications/read-all" -Method Post -Headers $auth -TimeoutSec 8 }

# 12. 私信
$convs = Step '会话列表' { Invoke-RestMethod -Uri "$base/conversations" -Headers $auth -TimeoutSec 8 }
if ($convs -and $convs.data.Count -gt 0) {
    $cid = $convs.data[0].id
    Step '消息列表' { Invoke-RestMethod -Uri "$base/conversations/$cid/messages?page=1&size=10" -Headers $auth -TimeoutSec 8 }
    Step '发送消息' {
        Invoke-RestMethod -Uri "$base/conversations/$cid/messages" -Method Post -Headers $auth `
            -Body '{"content":"冒烟测试消息"}' -TimeoutSec 8
    }
} else {
    $out += "[SKIP] 无会话，跳过私信测试"
}

# 13. 上传
$tmp = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($tmp, '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="#1F6FFF"/></svg>')
Step '图片上传' {
    $form = @{ file = Get-Item $tmp }
    Invoke-RestMethod -Uri "$base/upload/image" -Method Post -Form $form -Headers @{ Authorization = "Bearer $token" } -TimeoutSec 15
}
Remove-Item $tmp -Force -ErrorAction SilentlyContinue

# 14. 当前用户
Step '当前用户(me)' { Invoke-RestMethod -Uri "$base/auth/me" -Headers $auth -TimeoutSec 8 }

# 15. 未登录访问受保护接口（应拒绝或游客化）
try {
    Invoke-RestMethod -Uri "$base/notifications" -TimeoutSec 8 | Out-Null
    $out += "[WARN] 未登录访问 notifications 未报错"
} catch {
    $out += "[OK] 未登录访问被拦截"
}

$out | ForEach-Object { Write-Output $_ }
Write-Output "`n===== 冒烟测试结束 ====="
