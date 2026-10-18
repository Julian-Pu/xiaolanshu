package com.xiaolanshu.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {

    /** ISO 时间 → 相对时间 */
    fun ago(iso: String): String {
        return runCatching {
            val t = java.time.Instant.parse(iso).toEpochMilli()
            val diff = System.currentTimeMillis() - t
            val min = diff / 60_000
            when {
                min < 1 -> "刚刚"
                min < 60 -> "${min}分钟前"
                min < 60 * 24 -> "${min / 60}小时前"
                min < 60 * 24 * 30 -> "${min / (60 * 24)}天前"
                else -> SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(t))
            }
        }.getOrDefault("")
    }
}
