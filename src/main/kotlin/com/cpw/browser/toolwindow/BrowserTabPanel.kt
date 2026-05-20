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
    var onPopupUrl: ((String) -> Unit)? = null

    // 嵌入式 DevTools（直接加载 CDP 返回的 devtoolsFrontendUrl）
    private var embeddedDevTools: JBCefBrowser? = null

    val isEmbeddedDevToolsOpen: Boolean get() = embeddedDevTools != null

    init {
        if (initialUrl != "about:blank") {
            pushHistory(initialUrl)
        }

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
                return true
            }
        }, browser.cefBrowser)

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
     *
     * 从 CDP /json 端点获取 devtoolsFrontendUrl（指向 Chrome DevTools 前端 CDN），
     * 直接在新 JBCefBrowser 中加载。DevTools 前端的 WebSocket 连接通过
     * --remote-allow-origins=* 和 --allow-running-insecure-content 标志允许。
     *
     * WebSocket 连接发生在 CEF 子进程内（ws://127.0.0.1:PORT → 同一子进程的 CDP 服务器），
     * 不受 JCEF 远程代理 127.0.0.1 绕过问题的影响。
     */
    fun openEmbeddedDevTools(callback: (JBCefBrowser?) -> Unit) {
        if (embeddedDevTools != null) {
            callback(embeddedDevTools)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val port = findDevToolsPort()
            if (port != null && port > 0) {
                connectDevTools(port, callback)
            } else {
                System.err.println("[WebBrowser] DevTools port not found")
                ApplicationManager.getApplication().invokeLater { callback(null) }
            }
        }
    }

    /**
     * 查找 JCEF 远程调试端口。
     * 依次尝试：JBCefApp API → 已知路径的 DevToolsActivePort 文件 → 系统属性
     */
    private fun findDevToolsPort(): Int? {
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
                    System.err.println("[WebBrowser] DevTools port via JBCefApp: $port")
                    return port
                }
            }
        } catch (t: Throwable) {
            System.err.println("[WebBrowser] JBCefApp.getRemoteDebuggingPort failed: ${t.message}")
        }

        try {
            val candidates = mutableListOf<Path>()
            Path.of(PathManager.getSystemDir()!!.toString(), "jcef_cache").let { candidates.add(it) }
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
                        System.err.println("[WebBrowser] DevTools port via file: $port (${found.get()})")
                        return port
                    }
                }
            }
        } catch (t: Throwable) {
            System.err.println("[WebBrowser] DevToolsActivePort file search failed: ${t.message}")
        }

        return null
    }

    private fun connectDevTools(port: Int, callback: (JBCefBrowser?) -> Unit) {
        try {
            val json = URI("http://127.0.0.1:$port/json").toURL().readText()
            System.err.println("[WebBrowser] CDP /json (port=$port): ${json.take(500)}")
            val pages = JsonParser.parseString(json).asJsonArray

            // 按当前 URL 匹配 page 目标
            var matchedPage: com.google.gson.JsonObject? = null
            for (page in pages) {
                val obj = page.asJsonObject
                if (obj.get("type")?.asString == "page") {
                    val pageUrl = obj.get("url")?.asString ?: ""
                    if (pageUrl == currentUrl ||
                        (currentUrl != "about:blank" && pageUrl.contains(currentUrl)) ||
                        (currentUrl == "about:blank" && pages.size() == 1)) {
                        matchedPage = obj
                        break
                    }
                }
            }

            if (matchedPage == null) {
                System.err.println("[WebBrowser] No page matched '$currentUrl', using first available")
                for (page in pages) {
                    val obj = page.asJsonObject
                    if (obj.get("type")?.asString == "page") {
                        matchedPage = obj
                        break
                    }
                }
            }

            if (matchedPage != null) {
                val devtoolsUrl = matchedPage.get("devtoolsFrontendUrl")?.asString
                if (!devtoolsUrl.isNullOrBlank()) {
                    System.err.println("[WebBrowser] Loading DevTools frontend: $devtoolsUrl")
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val devBrowser = JBCefBrowser(devtoolsUrl)
                            embeddedDevTools = devBrowser
                            callback(devBrowser)
                        } catch (e: Exception) {
                            System.err.println("[WebBrowser] Failed to create DevTools browser: ${e.message}")
                            callback(null)
                        }
                    }
                } else {
                    System.err.println("[WebBrowser] No devtoolsFrontendUrl in /json response: $matchedPage")
                    ApplicationManager.getApplication().invokeLater { callback(null) }
                }
            } else {
                System.err.println("[WebBrowser] No page found in /json list")
                ApplicationManager.getApplication().invokeLater { callback(null) }
            }
        } catch (e: Exception) {
            System.err.println("[WebBrowser] DevTools connection failed: ${e.message}")
            ApplicationManager.getApplication().invokeLater { callback(null) }
        }
    }

    fun closeEmbeddedDevTools() {
        embeddedDevTools?.let { devTools ->
            val disposeRunnable = Runnable {
                devTools.dispose()
                embeddedDevTools = null
            }
            if (ApplicationManager.getApplication().isDispatchThread) {
                disposeRunnable.run()
            } else {
                ApplicationManager.getApplication().invokeLater(disposeRunnable)
            }
        }
    }

    fun getEmbeddedDevToolsComponent(): JComponent? = embeddedDevTools?.component

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
