package com.cpw.browser;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

// 插件安装后首次打开项目时，弹窗提示用户重启 IDE
public class PluginFirstRunActivity implements StartupActivity {

    private static final String FIRST_RUN_KEY = "com.cpw.browser.firstRun";

    @Override
    public void runActivity(@NotNull Project project) {
        // 检查是否为首次运行
        if (isFirstRun(project)) {
            markFirstRunDone(project);
            showRestartDialog(project);
        }
    }

    // 判断是否为首次运行
    private boolean isFirstRun(Project project) {
        return !PropertiesComponent.getInstance(project).getBoolean(FIRST_RUN_KEY, false);
    }

    // 标记首次运行已完成
    private void markFirstRunDone(Project project) {
        PropertiesComponent.getInstance(project).setValue(FIRST_RUN_KEY, true);
    }

    // 显示重启确认对话框
    private void showRestartDialog(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            int result = Messages.showOkCancelDialog(
                    project,
                    "WebBrowser 插件已安装，需要重启 IDE 才能生效。\n是否立即重启？",
                    "WebBrowser 插件",
                    "立即重启",
                    "稍后再说",
                    Messages.getInformationIcon()
            );
            // 用户点击了"立即重启"
            if (result == Messages.OK) {
                com.intellij.openapi.application.Application app = ApplicationManager.getApplication();
                if (app.isRestartCapable()) {
                    app.restart();
                }
            }
        });
    }
}
