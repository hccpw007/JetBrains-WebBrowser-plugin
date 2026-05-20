package com.cpw.browser

import com.cpw.browser.toolwindow.BrowserToolWindowPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class BrowserToolWindowFactory : ToolWindowFactory {

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = BrowserToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.getContent(), null, false)
        toolWindow.contentManager.addContent(content)

        // 初始隐藏侧边栏按钮，由 ToggleBrowserAction 控制
        toolWindow.setAvailable(false, null)

        // 注册键盘快捷键
        registerShortcuts(panel, toolWindow)

        // 关闭工具窗口时释放所有 JCEF 资源
        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoved(event: com.intellij.ui.content.ContentManagerEvent) {
                panel.dispose()
            }
        })
    }

    private fun registerShortcuts(panel: BrowserToolWindowPanel, toolWindow: ToolWindow) {
        val component = panel.getContent()

        // Ctrl+L — 聚焦地址栏
        val focusAddressBarAction = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                panel.focusAddressBar()
            }
        }
        focusAddressBarAction.registerCustomShortcutSet(
            CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK)),
            component
        )

        // Ctrl+Shift+I — 打开开发者工具
        val openDevToolsAction = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                panel.openDevTools()
            }
        }
        openDevToolsAction.registerCustomShortcutSet(
            CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
            component
        )
    }
}
