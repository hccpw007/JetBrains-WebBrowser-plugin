package com.cpw.browser

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.cef.JCefAppConfig

/**
 * 在 JCEF 初始化前启用远程调试端口（auto-select=0），
 * 使 JCEF 生成 DevToolsActivePort 文件供 CDP 嵌入式 DevTools 使用。
 *
 * 该 Activity 在 IDE 启动时运行，早于第一个 JBCefBrowser 的创建，
 * 确保远程调试端口在 JCEF 初始化前被设置。
 */
internal class DevToolsStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        enableJcefRemoteDebugging()
    }

    private fun enableJcefRemoteDebugging() {
        try {
            val settings = JCefAppConfig.getInstance().cefSettings
            if (settings.remote_debugging_port == 0) {
                settings.remote_debugging_port = 0
                System.err.println("[WebBrowser] Enabled JCEF remote debugging port (auto-select=0)")
            }
        } catch (t: Throwable) {
            System.err.println("[WebBrowser] Failed to enable JCEF remote debugging: ${t.message}")
        }
    }
}
