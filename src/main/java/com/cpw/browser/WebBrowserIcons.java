// 浏览器插件使用的图标常量
package com.cpw.browser;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

public final class WebBrowserIcons {

    public static final Icon BACK = load("back");
    public static final Icon FORWARD = load("forward");
    public static final Icon REFRESH = load("refresh");
    public static final Icon HOME = load("home");
    public static final Icon DEV_TOOLS = load("devtools");
    public static final Icon BOOKMARK_ADD = load("bookmark-add");
    public static final Icon BOOKMARK_REMOVE = load("bookmark-remove");
    public static final Icon NEW_TAB = load("new-tab");
    public static final Icon TOOL_WINDOW = load("toolwindow");
    public static final Icon STAR = load("star");
    public static final Icon STAR_FILLED = load("star-filled");
    public static final Icon SHOW_BOOKMARK = load("show-bookmark");
    public static final Icon ZOOM_IN = load("zoom-in");
    public static final Icon ZOOM_OUT = load("zoom-out");
    public static final Icon GOOGLE = load("google");

    private WebBrowserIcons() {
        // 工具类，禁止实例化
    }

    private static Icon load(String name) {
        return IconLoader.getIcon("/icons/" + name + ".svg", WebBrowserIcons.class);
    }
}
