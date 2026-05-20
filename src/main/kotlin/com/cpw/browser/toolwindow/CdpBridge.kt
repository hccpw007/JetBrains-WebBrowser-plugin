package com.cpw.browser.toolwindow

import com.google.gson.JsonParser
import kotlin.concurrent.thread
import org.cef.browser.CefBrowser
import org.cef.browser.CefDevToolsClient
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * CEF DevTools 桥接器。
 *
 * 提供 HTTP 服务 + WebSocket 服务器 + CefDevToolsClient，使 DevTools 前端能通过 HTTP
 * 与页面 DevTools 通信，完全绕过 JCEF 远程代理的 WebSocket 连接限制。
 *
 * 架构：
 *   DevTools 浏览器（CEF 子进程）
 *     -> HTTP 请求到本服务器（通过 JCEF 代理 -> 主进程 -> 本机 IP:端口）
 *     -> POST /cdp 执行命令 / GET /events 接收事件 / GET / (static) 提供前端文件
 *     -> CefDevToolsClient（CEF 内部 IPC，不走网络）
 *     -> 被检查页面
 */
class CdpBridge(private val inspectedBrowser: CefBrowser, private val cdpPort: Int? = null) {

    private val serverSocket: ServerSocket
    val port: Int
    private val devToolsClient: CefDevToolsClient
    private val eventQueue = ConcurrentLinkedQueue<String>()
    private var eventSeq = 0L
    @Volatile
    private var running = true

    init {
        serverSocket = ServerSocket(0, 50, InetAddress.getByName("0.0.0.0"))
        port = serverSocket.localPort
        devToolsClient = CefDevToolsClient(inspectedBrowser)
        System.err.println("[CdpBridge] Started on 0.0.0.0:$port")

        // 监听 CDP 事件，推送到 SSE 事件队列
        devToolsClient.addEventListener(object : CefDevToolsClient.EventListener {
            override fun onEvent(method: String, params: String) {
                val json = """{"method":"${esc(method)}","params":$params}"""
                eventQueue.add(json)
            }
        })

        thread(name = "cdp-bridge-accept", isDaemon = true) {
            while (running && !serverSocket.isClosed) {
                try {
                    val client = serverSocket.accept()
                    thread(name = "cdp-bridge-session", isDaemon = true) {
                        handleClient(client)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 30000
            val input = client.getInputStream()
            val output = client.getOutputStream()

            val raw = readHttpRequest(input) ?: return
            val req = String(raw, StandardCharsets.UTF_8)
            val statusLine = req.substringBefore("\r\n")
            val headers = parseHeaders(req)
            val method = statusLine.substringBefore(" ")
            val rawRequestUri = statusLine.substringAfter(" ").substringBeforeLast(" ")

            // 处理绝对 URL（JCEF 代理转发时可能使用 http://host/path 格式）
            val requestPath = try {
                val uri = URI(rawRequestUri)
                val q = uri.rawQuery
                (uri.path ?: rawRequestUri) + if (q != null) "?$q" else ""
            } catch (_: Exception) {
                rawRequestUri
            }

            System.err.println("[CdpBridge] $method $requestPath")

            // WebSocket 升级
            if (headers["Upgrade"]?.equals("websocket", ignoreCase = true) == true) {
                handleWebSocket(input, output, headers)
                return
            }

            // CDP 命令（POST）
            if (method == "POST" && requestPath == "/cdp") {
                handleCdpPost(raw, output)
                return
            }

            // SSE 事件流
            if (method == "GET" && requestPath == "/events") {
                handleEvents(output)
                return
            }

            // DevTools 前端文件：从 CDP 服务器代理
            if (method == "GET" && requestPath.startsWith("/cdp-server/")) {
                proxyFromCdpServer(requestPath.removePrefix("/cdp-server"), output)
                return
            }

            // 前端文件：从 CDN 代理
            if (method == "GET" && requestPath.startsWith("/")) {
                proxyFromCdn(requestPath, output)
                return
            }

            // 其他 -> 404
            sendHttp(output, 404, "text/plain", "Not Found")
        } catch (e: Exception) {
            System.err.println("[CdpBridge] handleClient error: ${e.message}")
            try { client.close() } catch (_: Exception) {}
        }
    }

    // ---- WebSocket 处理（CDP/WS 桥接） ----

    private fun handleWebSocket(input: InputStream, output: OutputStream, headers: Map<String, String>) {
        val wsKey = headers["Sec-WebSocket-Key"] ?: return
        val accept = computeWsAcceptKey(wsKey)
        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\nConnection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: $accept\r\n\r\n"
        output.write(response.toByteArray())
        output.flush()

        val buf = ByteArray(8192)
        while (running) {
            try {
                val b1 = input.read() ?: break
                val b2 = input.read() ?: break
                val opcode = b1 and 0x0F
                val masked = (b2 and 0x80) != 0
                var len = (b2 and 0x7F).toLong()

                if (len == 126L) { len = (input.read() shl 8 or input.read()).toLong() }
                else if (len == 127L) {
                    len = 0; for (i in 0..7) len = (len shl 8) or (readByte(input)!!.toLong() and 0xFF)
                }

                if (opcode == 0x08) { writeFrame(output, 0x08, byteArrayOf(3, -24)); return }
                if (opcode == 0x09) { writeFrame(output, 0x0A, ByteArray(0)); continue }
                if (opcode == 0x0A) continue
                if (opcode != 0x01) continue

                val maskKey = if (masked) ByteArray(4).also { readFully(input, it) } else null
                val payload = ByteArray(len.toInt()).also { readFully(input, it) }
                if (masked && maskKey != null) {
                    for (i in payload.indices) payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
                }
                val msg = String(payload, StandardCharsets.UTF_8)
                val resp = cdpExecute(msg)
                if (resp != null) writeFrame(output, 0x01, resp.toByteArray(StandardCharsets.UTF_8))
            } catch (_: Exception) { break }
        }
    }

    // ---- HTTP CDP 命令（POST /cdp） ----

    private fun handleCdpPost(raw: ByteArray, output: OutputStream) {
        val str = String(raw, StandardCharsets.UTF_8)
        val bodyStart = str.indexOf("\r\n\r\n") + 4
        val body = str.substring(bodyStart)
        if (body.isBlank()) {
            sendHttp(output, 400, "text/plain", "Empty body")
            return
        }
        val resp = cdpExecute(body)
        if (resp != null) {
            sendHttp(output, 200, "application/json", resp)
        } else {
            sendHttp(output, 500, "text/plain", "CDP error")
        }
    }

    // ---- SSE 事件流（GET /events） ----

    private fun handleEvents(output: OutputStream) {
        val headers_ = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/event-stream\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Connection: keep-alive\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n"
        output.write(headers_.toByteArray())
        output.flush()

        var localSeq = 0L
        while (running) {
            try {
                while (!eventQueue.isEmpty()) {
                    val event = eventQueue.poll() ?: break
                    localSeq++
                    val sse = "id: $localSeq\ndata: $event\n\n"
                    output.write(sse.toByteArray())
                    output.flush()
                }
                // 发送心跳保持连接
                output.write(": heartbeat\n\n".toByteArray())
                output.flush()
                Thread.sleep(2000)
            } catch (_: Exception) { break }
        }
    }

    // ---- CDN 代理（前端文件） ----

    private val cdnHost = "chrome-devtools-frontend.appspot.com"

    private fun proxyFromCdn(path: String, output: OutputStream) {
        try {
            val url = URL("https://$cdnHost$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val contentType = conn.contentType ?: "application/octet-stream"
            val data = conn.inputStream.readBytes()

            // 对 HTML 注入 WebSocket polyfill
            val body = if (contentType.contains("text/html")) {
                val html = String(data, StandardCharsets.UTF_8)
                injectPolyfill(html)
            } else {
                String(data, StandardCharsets.UTF_8)
            }

            sendHttp(output, 200, contentType, body)
        } catch (e: Exception) {
            System.err.println("[CdpBridge] Proxy error for $path: ${e.message}")
            sendHttp(output, 502, "text/plain", "Proxy error: ${e.message}")
        }
    }

    /**
     * 从 CDP 服务器代理 DevTools 前端文件。
     * 仅在 `devtoolsFrontendUrl` 不可用时的 fallback。
     */
    private fun proxyFromCdpServer(path: String, output: OutputStream) {
        if (cdpPort == null) {
            sendHttp(output, 502, "text/plain", "CDP server port not configured")
            return
        }
        try {
            val url = URL("http://127.0.0.1:$cdpPort$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val contentType = conn.contentType ?: "application/octet-stream"
            val data = conn.inputStream.readBytes()
            val body = if (contentType.contains("text/html")) {
                injectPolyfill(String(data, StandardCharsets.UTF_8))
            } else {
                String(data, StandardCharsets.UTF_8)
            }
            sendHttp(output, 200, contentType, body)
        } catch (e: Exception) {
            System.err.println("[CdpBridge] CDP server proxy error for $path: ${e.message}")
            sendHttp(output, 502, "text/plain", "Proxy error: ${e.message}")
        }
    }

    /**
     * 在 inspector.html 中注入 WebSocket polyfill，
     * 使得 DevTools 前端使用 HTTP POST + SSE 替代 WebSocket 与 CDP 通信。
     */
    private fun injectPolyfill(html: String): String {
        val polyfill = """
<script>
// DevTools WebSocket polyfill - use HTTP POST + SSE instead of WebSocket for CDP communication
(function() {
    var base = location.origin;

    var WS = function(url) {
        var self = this;
        this.readyState = 0;
        this.url = url;
        this._listeners = {};

        this.addEventListener = function(type, fn) {
            (self._listeners[type] || (self._listeners[type] = [])).push(fn);
        };
        this.removeEventListener = function(type, fn) {
            var a = self._listeners[type];
            if (a) self._listeners[type] = a.filter(function(l) { return l !== fn; });
        };
        this._fire = function(event) {
            if (self['on' + event.type]) self['on' + event.type](event);
            (self._listeners[event.type] || []).forEach(function(l) { l(event); });
        };

        // SSE receives CDP events pushed from the bridge
        this._es = new EventSource(base + '/events');
        this._es.onmessage = function(e) { self._fire({type: 'message', data: e.data}); };
        this._es.onerror = function() { self.close(); };

        this.readyState = 1;
        setTimeout(function() { self._fire({type: 'open'}); }, 0);

        this.send = function(data) {
            var xhr = new XMLHttpRequest();
            xhr.open('POST', base + '/cdp', true);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.onload = function() { self._fire({type: 'message', data: xhr.responseText}); };
            xhr.onerror = function() { self._fire({type: 'error'}); };
            xhr.send(data);
        };

        this.close = function() {
            this.readyState = 3;
            if (this._es) { this._es.close(); this._es = null; }
            self._fire({type: 'close', code: 1000, reason: 'closed', wasClean: true});
        };
    };
    window.WebSocket = WS;
})();
</script>
</head>
""".trimIndent()

        // 在 </head> 之前注入
        return html.replace("</head>", "$polyfill")
    }

    // ---- CDP 命令执行 ----

    private fun cdpExecute(message: String): String? {
        try {
            val obj = JsonParser.parseString(message).asJsonObject
            val id = obj.get("id")?.asInt ?: return null
            val method = obj.get("method")?.asString ?: return null
            val params = obj.get("params")?.toString()

            val future = if (params != null) {
                devToolsClient.executeDevToolsMethod(method, params)
            } else {
                devToolsClient.executeDevToolsMethod(method)
            }

            // 同步等待执行结果
            val result = try {
                future.get(10, java.util.concurrent.TimeUnit.SECONDS)
            } catch (e: Exception) {
                null
            }

            return if (result != null) {
                """{"id":$id,"result":$result}"""
            } else {
                """{"id":$id,"error":{"code":-32000,"message":"CDP execution failed or timed out"}}"""
            }
        } catch (e: Exception) {
            System.err.println("[CdpBridge] CDP error: ${e.message}")
            return null
        }
    }

    fun close() {
        running = false
        try { serverSocket.close() } catch (_: Exception) {}
        try { devToolsClient.close() } catch (_: Exception) {}
        eventQueue.clear()
    }

    // ---- HTTP 工具 ----

    private fun sendHttp(output: OutputStream, code: Int, contentType: String, body: String) {
        val data = body.toByteArray(StandardCharsets.UTF_8)
        val response = "HTTP/1.1 $code ${statusText(code)}\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${data.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(response.toByteArray())
        output.write(data)
        output.flush()
    }

    private fun writeFrame(output: OutputStream, opcode: Int, data: ByteArray) {
        val header = ByteArrayOutputStream()
        header.write(0x80 or opcode)
        when {
            data.size < 126 -> header.write(data.size)
            data.size < 65536 -> {
                header.write(126); header.write(data.size shr 8); header.write(data.size)
            }
            else -> {
                header.write(127)
                for (i in 7 downTo 0) header.write(((data.size.toLong() shr (i * 8)) and 0xFF).toInt())
            }
        }
        output.write(header.toByteArray())
        output.write(data)
        output.flush()
    }

    private fun computeWsAcceptKey(key: String): String {
        val guid = "258EAFA5-E914-47DA-95CA-5AB5DC49B1EA"
        val sha1 = MessageDigest.getInstance("SHA-1").digest((key + guid).toByteArray())
        return Base64.getEncoder().encodeToString(sha1)
    }

    companion object {
        fun readHttpRequest(input: InputStream): ByteArray? {
            val buf = ByteArray(8192)
            var total = 0
            while (total < buf.size) {
                val n = input.read(buf, total, buf.size - total)
                if (n < 0) return if (total > 0) buf.copyOf(total) else null
                total += n
                val s = String(buf, 0, total, StandardCharsets.UTF_8)
                if (s.contains("\r\n\r\n")) {
                    val headerEnd = s.indexOf("\r\n\r\n") + 4
                    val cl = parseHeaders(s.substring(0, headerEnd))["Content-Length"]?.toIntOrNull() ?: 0
                    if (cl > 0) {
                        val bodyRead = total - headerEnd
                        val remaining = cl - bodyRead
                        if (remaining > 0) {
                            val rest = ByteArray(remaining)
                            readFully(input, rest)
                            val result = ByteArray(total + remaining)
                            System.arraycopy(buf, 0, result, 0, total)
                            System.arraycopy(rest, 0, result, total, remaining)
                            return result
                        }
                    }
                    return buf.copyOf(total)
                }
            }
            return buf.copyOf(total)
        }

        fun parseHeaders(request: String): Map<String, String> {
            val lines = request.split("\r\n")
            val h = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) break
                val c = line.indexOf(":")
                if (c > 0) h[line.substring(0, c).trim()] = line.substring(c + 1).trim()
            }
            return h
        }

        fun readByte(input: InputStream): Int? {
            val b = input.read(); return if (b < 0) null else b
        }

        fun readFully(input: InputStream, b: ByteArray) {
            var off = 0
            while (off < b.size) {
                val n = input.read(b, off, b.size - off)
                if (n < 0) throw java.io.EOFException()
                off += n
            }
        }

        private fun statusText(code: Int): String = when (code) {
            200 -> "OK"; 400 -> "Bad Request"; 404 -> "Not Found"
            500 -> "Internal Server Error"; 502 -> "Bad Gateway"
            else -> "Unknown"
        }
    }

    private fun esc(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
