// 浏览器编辑器标签页的文件类型，用于在编辑区打开浏览器面板
package com.cpw.browser.editor;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.Icon;

public final class BrowserFileType implements FileType {

    // 单例实例
    public static final BrowserFileType INSTANCE = new BrowserFileType();

    // 编辑器标签页图标
    private static final Icon TAB_ICON = IconLoader.getIcon("/icons/webbrowser_tab.svg", BrowserFileType.class);

    private BrowserFileType() {
        // 单例，禁止外部实例化
    }

    // 获取文件类型名称
    @Override
    public @NotNull String getName() {
        return "WebBrowser";
    }

    // 获取文件类型描述
    @Override
    public @NotNull String getDescription() {
        return "Web Browser";
    }

    // 获取默认扩展名
    @Override
    public @NotNull String getDefaultExtension() {
        return "webbrowser";
    }

    // 是否为二进制文件
    @Override
    public boolean isBinary() {
        return false;
    }

    // 获取文件类型图标
    @Override
    public @Nullable Icon getIcon() {
        return TAB_ICON;
    }

    // 获取文件字符编码
    @Override
    public @Nullable String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
        return null;
    }
}
