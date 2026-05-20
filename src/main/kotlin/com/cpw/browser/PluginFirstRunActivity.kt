package com.cpw.browser

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.Messages

/**
 * 插件安装后首次打开项目时，弹窗提示用户重启 IDE。
 */
internal class PluginFirstRunActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (isFirstRun(project)) {
            markFirstRunDone(project)
            showRestartDialog(project)
        }
    }

    private fun isFirstRun(project: Project): Boolean {
        return !PropertiesComponent.getInstance(project).getBoolean(FIRST_RUN_KEY, false)
    }

    private fun markFirstRunDone(project: Project) {
        PropertiesComponent.getInstance(project).setValue(FIRST_RUN_KEY, true)
    }

    private fun showRestartDialog(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val result = Messages.showOkCancelDialog(
                project,
                "WebBrowser 插件已安装，需要重启 IDE 才能生效。\n是否立即重启？",
                "WebBrowser 插件",
                "立即重启",
                "稍后再说",
                Messages.getInformationIcon()
            )
            if (result == Messages.OK) {
                val app = ApplicationManager.getApplication()
                if (app.isRestartCapable) {
                    app.restart()
                }
            }
        }
    }

    companion object {
        private const val FIRST_RUN_KEY = "com.cpw.browser.firstRun"
    }
}
