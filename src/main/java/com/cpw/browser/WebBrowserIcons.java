// 浏览器插件使用的图标常量
package com.cpw.browser;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

public final class WebBrowserIcons {

    // 后退导航图标
    public static final Icon BACK = load("back");
    // 前进导航图标
    public static final Icon FORWARD = load("forward");
    // 刷新页面图标
    public static final Icon REFRESH = load("refresh");
    // 主页导航图标
    public static final Icon HOME = load("home");
    // 开发者工具图标
    public static final Icon DEV_TOOLS = load("devtools");
    // 添加书签图标
    public static final Icon BOOKMARK_ADD = load("bookmark-add");
    // 移除书签图标
    public static final Icon BOOKMARK_REMOVE = load("bookmark-remove");
    // 新建标签页图标
    public static final Icon NEW_TAB = load("new-tab");
    // 未收藏状态星标图标
    public static final Icon STAR = load("star");
    // 已收藏状态星标填充图标
    public static final Icon STAR_FILLED = load("star-filled");
    // 显示/隐藏书签侧边栏图标
    public static final Icon SHOW_BOOKMARK = load("show-bookmark");
    // 放大图标
    public static final Icon ZOOM_IN = load("zoom-in");
    // 缩小图标
    public static final Icon ZOOM_OUT = load("zoom-out");
    // Google 图标
    public static final Icon GOOGLE = load("google");

    private WebBrowserIcons() {
        // 工具类，禁止实例化
    }

    private static Icon load(String name) {
        return IconLoader.getIcon("/icons/" + name + ".svg", WebBrowserIcons.class);
    }
}
