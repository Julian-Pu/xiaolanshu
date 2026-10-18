package com.xiaolanshu.app.ui.state

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 全局跨页状态（技术债 3 缓解）：页面间共享的轻量状态容器。
 * - notifUnread：通知未读数（底部「消息」tab 角标，消息页收到 notify 帧/已读操作时更新，
 *   任何页面均可收集，无需回调层层传参）
 * - msgUnread：私信会话未读总数（预留；会话列表加载后汇总）
 * 状态生命周期为进程级（登录态切换时由入口显式重置）。
 */
object AppState {

    /** 通知未读数（跨页角标） */
    val notifUnread = MutableStateFlow(0)

    /** 私信未读总数 */
    val msgUnread = MutableStateFlow(0)

    /** 登录切换时重置 */
    fun reset() {
        notifUnread.value = 0
        msgUnread.value = 0
    }
}
