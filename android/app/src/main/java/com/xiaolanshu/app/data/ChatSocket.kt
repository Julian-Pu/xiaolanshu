package com.xiaolanshu.app.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.xiaolanshu.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * 站内 WebSocket 客户端（/ws）：
 * 连接后发送 auth 帧绑定用户；服务端推送 message / typing / read_ack / notify 帧。
 * C→S 额外支持 {"type":"typing","conversationId":..} 与 {"type":"read","conversationId":..}。
 */
object ChatSocket {

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var listener: ((Message) -> Unit)? = null
    private var frameListener: ((String, Long) -> Unit)? = null
    private var notifyListener: ((JsonObject) -> Unit)? = null

    fun connect(onMessage: (Message) -> Unit, onFrame: ((String, Long) -> Unit)? = null,
                onNotify: ((JsonObject) -> Unit)? = null) {
        listener = onMessage
        frameListener = onFrame
        notifyListener = onNotify
        val token = AuthStore.get().token ?: return
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        val url = base.replaceFirst("http", "ws") + "/ws"
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""{"type":"auth","token":"$token"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val node = gson.fromJson(text, JsonObject::class.java)
                    when (node.get("type")?.asString) {
                        "message" -> {
                            val msg = gson.fromJson(node.get("message"), Message::class.java)
                            msg?.let { listener?.invoke(it) }
                        }
                        "typing", "read_ack" -> {
                            val convId = node.get("conversationId")?.asLong
                            val type = node.get("type")?.asString
                            if (type != null && convId != null) {
                                frameListener?.invoke(type, convId)
                            }
                        }
                        "notify" -> {
                            // 多端实时通知推送：新通知到达（完整列表走 REST 刷新）
                            node.get("notification")?.let { notifyListener?.invoke(it as JsonObject) }
                        }
                    }
                } catch (_: Exception) {
                    // 忽略非私信帧
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // 断线静默，REST 拉取兜底
            }
        })
    }

    fun disconnect() {
        runCatching { webSocket?.close(1000, "bye") }
        webSocket = null
        listener = null
        frameListener = null
        notifyListener = null
    }

    /** 发送任意 JSON 文本帧（typing / read），未连接时静默丢弃 */
    fun send(payload: String) {
        runCatching { webSocket?.send(payload) }
    }
}
