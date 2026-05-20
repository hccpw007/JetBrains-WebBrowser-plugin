package com.cpw.browser.toolwindow

import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JComponent

class BrowserTabPanel(private val initialUrl: String = "about:blank") {

    val browser: JBCefBrowser = JBCefBrowser(initialUrl).also {
        // 强制白色背景，避免深色主题下网页背景变黑
        it.setPageBackgroundColor("white")
    }
    val component: JComponent = browser.component

    private val navigationHistory = ArrayDeque<String>()
    var currentHistoryIndex: Int = -1
        private set
    var currentUrl: String = initialUrl
        private set
    var pageTitle: String = "新标签页"
        private set
    var isLoading: Boolean = false
        private set

    var onUrlChanged: ((String) -> Unit)? = null
    var onTitleChanged: ((String) -> Unit)? = null
    var onLoadingStateChanged: ((Boolean) -> Unit)? = null
    // 网页弹窗/新窗口回调 — 传入目标 URL
    var onPopupUrl: ((String) -> Unit)? = null

    // 嵌入式 DevTools — 通过 CDP 远程调试端口实现
    private var cdpDevTools: JBCefBrowser? = null

    val isEmbeddedDevToolsOpen: Boolean get() = cdpDevTools != null

    init {
        if (initialUrl != "about:blank") {
            pushHistory(initialUrl)
        }

        // 拦截网页弹窗和新窗口，转到插件内部新建标签页
        browser.jbCefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser,
                frame: CefFrame,
                targetUrl: String,
                targetFrameName: String
            ): Boolean {
                if (targetUrl.isNotBlank() && targetUrl != "about:blank") {
                    onPopupUrl?.invoke(targetUrl)
                }
                return true // 取消原生弹窗
            }
        }, browser.cefBrowser)

        // 加载完成回调
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) {
                    currentUrl = frame.url
                    onUrlChanged?.invoke(frame.url)
                }
            }

            override fun onLoadingStateChange(
                browser: CefBrowser,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                this@BrowserTabPanel.isLoading = isLoading
                onLoadingStateChanged?.invoke(isLoading)
            }

            override fun onLoadError(
                browser: CefBrowser,
                frame: CefFrame,
                errorCode: CefLoadHandler.ErrorCode,
                errorText: String,
                failedUrl: String
            ) {
                if (frame.isMain) {
                    currentUrl = failedUrl
                    onUrlChanged?.invoke(failedUrl)
                }
            }
        }, browser.cefBrowser)

        // 标题和地址变更回调
        browser.jbCefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onAddressChange(browser: CefBrowser, frame: CefFrame, url: String) {
                if (frame.isMain) {
                    currentUrl = url
                    onUrlChanged?.invoke(url)
                }
            }

            override fun onTitleChange(browser: CefBrowser, title: String) {
                pageTitle = title
                onTitleChanged?.invoke(title)
            }
        }, browser.cefBrowser)
    }

    fun navigate(url: String) {
        pushHistory(url)
        browser.loadURL(url)
    }

    fun goBack() {
        if (canGoBack()) {
            currentHistoryIndex--
            browser.cefBrowser.goBack()
        }
    }

    fun goForward() {
        if (canGoForward()) {
            currentHistoryIndex++
            browser.cefBrowser.goForward()
        }
    }

    fun refresh() {
        browser.cefBrowser.reload()
    }

    fun canGoBack(): Boolean = currentHistoryIndex > 0

    fun canGoForward(): Boolean = currentHistoryIndex < navigationHistory.size - 1

    var zoomLevel: Double = 1.0
        private set

    fun zoomIn() {
        zoomLevel += 0.05
        browser.setZoomLevel(zoomLevel)
    }

    fun zoomOut() {
        zoomLevel -= 0.05
        browser.setZoomLevel(zoomLevel)
    }

    fun zoomReset() {
        zoomLevel = 1.0
        browser.setZoomLevel(1.0)
    }

    fun openDevTools() {
        browser.openDevtools()
    }

    fun getTabTitle(): String {
        val rawTitle = if (pageTitle.isNotBlank() && pageTitle != "about:blank") {
            pageTitle
        } else if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
            currentUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/")
        } else {
            "新标签页"
        }
        return if (rawTitle.length > 20) rawTitle.take(20) + "..." else rawTitle
    }

    /**
     * 通过 CDP（Chrome DevTools Protocol）远程调试端口打开嵌入式 DevTools。
     * 利用 JCEF 的远程调试端口获取 DevTools 前端页面并嵌入到 JSplitPane 中。
     * 如果 CDP 不可用，回调会返回 null。
     */
    fun openEmbeddedDevTools(callback: (JBCefBrowser?) -> Unit) {
        if (cdpDevTools != null) {
            callback(cdpDevTools)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val port = findDevToolsPort()
            if (port != null && port > 0) {
                connectDevToolsViaCDP(port, callback)
            } else {
                System.err.println("[WebBrowser] DevTools port not found (tried JBCefApp + file scan)")
                callback(null)
            }
        }
    }

    /**
     * 查找 JCEF 远程调试端口。
     * 依次尝试：JBCefApp API → 已知路径的 DevToolsActivePort 文件 → 系统属性
     */
    private fun findDevToolsPort(): Int? {
        // 1. 尝试 JBCefApp API（主要 IDE 可用）
        try {
            val app = JBCefApp.getInstance()
            val result = java.util.concurrent.atomic.AtomicReference<Int?>()
            val latch = java.util.concurrent.CountDownLatch(1)
            app.getRemoteDebuggingPort { port ->
                result.set(port)
                latch.countDown()
            }
            if (latch.await(3, java.util.concurrent.TimeUnit.SECONDS)) {
                val port = result.get()
                if (port != null && port > 0) {
                    System.err.println("[WebBrowser] Found DevTools port via JBCefApp: $port")
                    return port
                }
            }
        } catch (t: Throwable) {
            System.err.println("[WebBrowser] JBCefApp.getRemoteDebuggingPort failed: ${t.message}")
        }

        // 2. 尝试读取 DevToolsActivePort 文件
        //    优先搜索当前 sandbox（缓存大小大），再搜索其他已知路径
        try {
            val candidates = mutableListOf<Path>()

            // 2a. 当前 sandbox 的 jcef_cache（最优先 — runIde 的 CDP 端口）
            Path.of(PathManager.getSystemDir()!!.toString(), "jcef_cache").let { candidates.add(it) }

            // 2b. macOS 主 IDE 的 jcef_cache
            try {
                Path.of(System.getProperty("user.home")!!, "Library", "Caches", "JetBrains").let { candidates.add(it) }
            } catch (_: Exception) {}

            for (root in candidates) {
                if (!Files.isDirectory(root)) continue
                val found = Files.walk(root, 6)
                    .filter { it.fileName.toString() == "DevToolsActivePort" }
                    .findFirst()
                if (found.isPresent) {
                    val port = found.get().let { Files.readAllLines(it).firstOrNull()?.trim()?.toIntOrNull() }
                    if (port != null && port > 0) {
                        System.err.println("[WebBrowser] Found DevTools port via file: $port (${found.get()})")
                        return port
                    }
                }
            }
        } catch (t: Throwable) {
            System.err.println("[WebBrowser] DevToolsActivePort file search failed: ${t.message}")
        }

        // 3. 尝试系统属性（用户手动指定）
        val sysPort = System.getProperty("webbrowser.devtools.port")?.toIntOrNull()
        if (sysPort != null && sysPort > 0) {
            System.err.println("[WebBrowser] Found DevTools port via system property: $sysPort")
            return sysPort
        }

        return null
    }

    private fun connectDevToolsViaCDP(port: Int, callback: (JBCefBrowser?) -> Unit) {
        try {
            val json = URI("http://127.0.0.1:$port/json").toURL().readText()
            System.err.println("[WebBrowser] CDP /json response (port=$port): $json")
            val pages = JsonParser.parseString(json).asJsonArray

            // 按当前 URL 匹配 page 目标
            var matchedPage: com.google.gson.JsonObject? = null
            for (page in pages) {
                val obj = page.asJsonObject
                if (obj.get("type")?.asString == "page") {
                    val pageUrl = obj.get("url")?.asString ?: ""
                    if (pageUrl == currentUrl || (currentUrl != "about:blank" && pageUrl.contains(currentUrl))) {
                        matchedPage = obj
                        break
                    }
                }
            }

            // 未精确匹配到则使用第一个 page 类型
            if (matchedPage == null) {
                System.err.println("[WebBrowser] No page matched currentUrl='$currentUrl', using first available page")
                for (page in pages) {
                    val obj = page.asJsonObject
                    if (obj.get("type")?.asString == "page") {
                        matchedPage = obj
                        break
                    }
                }
            }

            if (matchedPage != null) {
                val pageId = matchedPage.get("id")?.asString
                if (pageId.isNullOrBlank()) {
                    System.err.println("[WebBrowser] Page has no id, cannot open DevTools")
                    ApplicationManager.getApplication().invokeLater { callback(null) }
                    return
                }

                // 使用 http://127.0.0.1:{port}/devtools/inspector.html?ws=... 格式，
                // 这样 devtools 前端通过 HTTP 从 CDP 服务器加载（走 JCEF 代理），
                // 避免 devtools:// 协议在远程 CEF 子进程中可能无法连接 WebSocket 的问题
                val devToolsUrl = "http://127.0.0.1:$port/devtools/inspector.html?ws=127.0.0.1:$port/devtools/page/$pageId"
                System.err.println("[WebBrowser] Opening embedded DevTools at: $devToolsUrl (pageId=$pageId)")
                ApplicationManager.getApplication().invokeLater {
                    try {
                        val devBrowser = JBCefBrowser(devToolsUrl)
                        cdpDevTools = devBrowser
                        callback(devBrowser)
                    } catch (e: Exception) {
                        System.err.println("[WebBrowser] Failed to create DevTools browser: ${e.message}")
                        callback(null)
                    }
                }
            } else {
                System.err.println("[WebBrowser] No page type found in /json list")
                ApplicationManager.getApplication().invokeLater { callback(null) }
            }
        } catch (e: Exception) {
            System.err.println("[WebBrowser] CDP connection failed: ${e.message}")
            ApplicationManager.getApplication().invokeLater { callback(null) }
        }
    }

    fun closeEmbeddedDevTools() {
        cdpDevTools?.let { devTools ->
            devTools.dispose()
            cdpDevTools = null
        }
    }

    fun getEmbeddedDevToolsComponent(): JComponent? = cdpDevTools?.component

    fun dispose() {
        closeEmbeddedDevTools()
        val disposeRunnable = Runnable {
            browser.dispose()
        }
        if (ApplicationManager.getApplication().isDispatchThread) {
            disposeRunnable.run()
        } else {
            ApplicationManager.getApplication().invokeLater(disposeRunnable)
        }
    }

    private fun pushHistory(url: String) {
        while (navigationHistory.size > currentHistoryIndex + 1) {
            navigationHistory.removeLast()
        }
        navigationHistory.addLast(url)
        currentHistoryIndex = navigationHistory.size - 1
    }
}
