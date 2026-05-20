package com.cpw.browser.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBuilder
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
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

    // 嵌入式 DevTools
    private var embeddedDevTools: JBCefBrowser? = null
    private var pendingDevToolsCallback: ((JBCefBrowser?) -> Unit)? = null

    val isEmbeddedDevToolsOpen: Boolean get() = embeddedDevTools != null

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

        // 拦截 DevTools 弹窗浏览器，转为嵌入式
        browser.jbCefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser) {
                if (browser.isPopup && pendingDevToolsCallback != null) {
                    val devCefBrowser = browser
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val jbDevTools = JBCefBrowserBuilder()
                                .setCefBrowser(devCefBrowser)
                                .setClient(this@BrowserTabPanel.browser.jbCefClient)
                                .build()
                            devCefBrowser.setWindowVisibility(false)
                            embeddedDevTools = jbDevTools
                            pendingDevToolsCallback?.invoke(jbDevTools)
                        } catch (e: Exception) {
                            pendingDevToolsCallback?.invoke(null)
                        }
                        pendingDevToolsCallback = null
                    }
                }
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

    fun openEmbeddedDevTools(callback: (JBCefBrowser?) -> Unit) {
        if (embeddedDevTools != null) {
            callback(embeddedDevTools)
            return
        }
        pendingDevToolsCallback = callback
        browser.cefBrowser.openDevTools()
    }

    fun closeEmbeddedDevTools() {
        embeddedDevTools?.let { devTools ->
            devTools.dispose()
            embeddedDevTools = null
        }
        browser.cefBrowser.closeDevTools()
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
