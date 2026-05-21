// 浏览器编辑器标签页的文件类型，用于在编辑区打开浏览器面板
package com.cpw.browser.editor;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.Icon;

public final class BrowserFileType implements FileType {

    public static final BrowserFileType INSTANCE = new BrowserFileType();

    private static final Icon TAB_ICON = IconLoader.getIcon("/icons/webbrowser_tab.svg", BrowserFileType.class);

    private BrowserFileType() {
        // 单例，禁止外部实例化
    }

    @Override
    public @NotNull String getName() {
        return "WebBrowser";
    }

    @Override
    public @NotNull String getDescription() {
        return "Web Browser";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "webbrowser";
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public @Nullable Icon getIcon() {
        return TAB_ICON;
    }

    @Override
    public @Nullable String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
        return null;
    }
}
