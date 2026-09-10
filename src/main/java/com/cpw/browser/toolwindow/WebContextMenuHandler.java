// 网页右键菜单处理器：为网页内右键提供与 IDE 风格一致的上下文菜单
// CEF 未注册本处理器时网页内不显示任何菜单，且菜单内容与渲染均须由处理器接管（runContextMenu）
package com.cpw.browser.toolwindow;

import com.cpw.browser.util.TranslationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.awt.RelativePoint;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.callback.CefRunContextMenuCallback;
import org.cef.handler.CefContextMenuHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

// 网页右键菜单处理器
final class WebContextMenuHandler extends CefContextMenuHandlerAdapter {

    // 自定义菜单项 ID 起始值，CEF 约定自定义命令从 MENU_ID_USER_FIRST 起取值
    private static final int CUSTOM_MENU_ID_BASE = CefMenuModel.MenuId.MENU_ID_USER_FIRST;
    // 分隔线的占位 ID，与任何真实命令 ID 均不冲突
    private static final int SEPARATOR_ID = 0;

    // 在链接上右键：在新标签页打开链接
    private static final int MENU_ID_OPEN_LINK_NEW_TAB = CUSTOM_MENU_ID_BASE + 1;
    // 在链接上右键：复制链接地址
    private static final int MENU_ID_COPY_LINK = CUSTOM_MENU_ID_BASE + 2;
    // 后退一页
    private static final int MENU_ID_GO_BACK = CUSTOM_MENU_ID_BASE + 3;
    // 前进一页
    private static final int MENU_ID_GO_FORWARD = CUSTOM_MENU_ID_BASE + 4;
    // 重新加载当前页面
    private static final int MENU_ID_RELOAD_PAGE = CUSTOM_MENU_ID_BASE + 5;
    // 查看网页源代码
    private static final int MENU_ID_VIEW_SOURCE = CUSTOM_MENU_ID_BASE + 6;
    // 在页面中查找
    private static final int MENU_ID_FIND_IN_PAGE = CUSTOM_MENU_ID_BASE + 7;
    // 检查元素（内嵌在当前页面下方）
    private static final int MENU_ID_DEVTOOLS_EMBEDDED = CUSTOM_MENU_ID_BASE + 8;
    // 检查元素（独立窗口）
    private static final int MENU_ID_DEVTOOLS_WINDOW = CUSTOM_MENU_ID_BASE + 9;

    // 所属标签页，提供导航能力与菜单弹出的坐标基准组件
    private final BrowserTabPanel tab;
    // 主面板注入的外部操作能力，默认空实现以免菜单项执行时判空
    private ContextMenuActions actions = new ContextMenuActions() {
        // 未注入时不执行任何操作
        @Override
        public void openLinkInNewTab(String url) {
        }

        // 未注入时不执行任何操作
        @Override
        public void openEmbeddedDevTools() {
        }

        // 未注入时不执行任何操作
        @Override
        public void openDevToolsWindow() {
        }

        // 未注入时不执行任何操作
        @Override
        public void showFindBar() {
        }
    };

    // 右键菜单所需的外部操作能力，由主面板实现并注入
    interface ContextMenuActions {
        // 在新标签页打开指定链接
        // url 为链接地址
        void openLinkInNewTab(String url);

        // 打开内嵌在当前页面下方的开发者工具
        void openEmbeddedDevTools();

        // 打开独立窗口形式的开发者工具
        void openDevToolsWindow();

        // 显示页面内查找栏
        void showFindBar();
    }

    // 构造处理器并把自身注册到标签页的浏览器客户端
    // tab 为所属标签页
    WebContextMenuHandler(BrowserTabPanel tab) {
        this.tab = tab;
        tab.browser.getJBCefClient().addContextMenuHandler(this, tab.browser.getCefBrowser());
    }

    // 注入右键菜单所需的外部操作能力
    // actions 为主面板提供的操作实现
    void setActions(ContextMenuActions actions) {
        this.actions = actions;
    }

    // 构建菜单内容并填充 CEF 的菜单模型
    // cefBrowser 为触发菜单的浏览器
    // frame 为触发菜单的框架
    // params 为右键位置的上下文信息
    // model 为待填充的菜单模型，模型为空时 CEF 不会显示菜单
    @Override
    public void onBeforeContextMenu(CefBrowser cefBrowser, CefFrame frame,
                                    CefContextMenuParams params, CefMenuModel model) {
        try {
            // 清空 CEF 默认菜单项，菜单内容完全由本插件定义
            model.clear();
            // 按定义填充模型，同时保证模型非空以触发 runContextMenu
            for (MenuEntry entry : buildEntries(params)) {
                // 分隔线单独添加
                if (entry.isSeparator()) {
                    model.addSeparator();
                    continue;
                }
                model.addItem(entry.id, TranslationUtil.getText(entry.textKey));
                model.setEnabled(entry.id, entry.enabled);
            }
        } catch (Throwable t) {
            System.err.println("[WebBrowser] Build context menu error: " + t.getMessage());
        }
    }

    // 接管菜单渲染：CEF 自身不绘制菜单，改由本类弹出 IDE 风格菜单
    // cefBrowser 为触发菜单的浏览器
    // frame 为触发菜单的框架
    // params 为右键位置的上下文信息
    // model 为已填充的菜单模型
    // callback 为菜单选择结果的回传通道
    // 返回 true 表示菜单渲染由本类负责，CEF 不再绘制默认菜单
    @Override
    public boolean runContextMenu(CefBrowser cefBrowser, CefFrame frame, CefContextMenuParams params,
                                  CefMenuModel model, CefRunContextMenuCallback callback) {
        // 菜单必须在 EDT 中弹出，故切换线程后异步显示
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                showPopup(params, callback);
            } catch (Throwable t) {
                System.err.println("[WebBrowser] Show context menu error: " + t.getMessage());
                // 弹出失败时取消菜单，避免 CEF 侧的菜单状态悬挂
                callback.cancel();
            }
        });
        return true;
    }

    // 弹出 IDE 风格的右键菜单
    // params 为右键位置的上下文信息，用于决定菜单项内容
    // callback 为菜单选择结果的回传通道
    private void showPopup(CefContextMenuParams params, CefRunContextMenuCallback callback) {
        // Swing 菜单容器
        DefaultActionGroup group = new DefaultActionGroup();

        // 按定义逐个构建菜单项
        for (MenuEntry entry : buildEntries(params)) {
            // 分隔线直接加入
            if (entry.isSeparator()) {
                group.addSeparator();
                continue;
            }
            group.add(createAction(entry, callback));
        }

        // 无菜单项可显示时直接取消，避免弹出空菜单
        if (group.getChildrenCount() == 0) {
            callback.cancel();
            return;
        }

        // 坐标基准组件，CEF 传入的坐标即以其为原点
        JComponent browserComponent = tab.browser.getComponent();
        System.err.println("[WebBrowser] Context menu shown, items=" + group.getChildrenCount());

        // 使用平台统一的动作组弹窗：其内部按运行环境选择重量级窗口，避免被 JCEF 原生视图遮挡
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                group,
                DataContext.EMPTY_CONTEXT,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true);
        popup.show(new RelativePoint(browserComponent, new Point(params.getXCoord(), params.getYCoord())));
    }

    // 把菜单项定义转换为 Swing 动作
    // entry 为菜单项定义
    // callback 为菜单选择结果的回传通道
    // 返回可加入菜单的动作
    private AnAction createAction(MenuEntry entry, CefRunContextMenuCallback callback) {
        // 匿名动作只覆写执行逻辑，可用状态改由模板 Presentation 固化
        AnAction action = new AnAction(TranslationUtil.getText(entry.textKey)) {
            // 菜单项被选中时执行对应命令
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                onMenuChosen(entry, callback);
            }
        };
        // 固化菜单项可用状态，避免覆写 update 触发平台对更新线程的强制约束
        action.getTemplatePresentation().setEnabled(entry.enabled);
        return action;
    }

    // 处理菜单项被选中：CEF 标准命令交回内核执行，自定义命令由本类直接执行
    // entry 为被选中的菜单项
    // callback 为菜单选择结果的回传通道
    private void onMenuChosen(MenuEntry entry, CefRunContextMenuCallback callback) {
        System.err.println("[WebBrowser] Context menu command invoked, id=" + entry.id);
        try {
            // 标准编辑命令（撤销/重做/剪切/复制/粘贴/全选）依赖内核实现，回传命令 ID 交由其执行
            if (entry.id < CUSTOM_MENU_ID_BASE) {
                callback.Continue(entry.id, 0);
                return;
            }
            // 自定义命令直接执行
            if (entry.action != null) {
                entry.action.run();
            }
            // 通知 CEF 菜单已关闭
            callback.cancel();
        } catch (Throwable t) {
            System.err.println("[WebBrowser] Context menu command error: " + t.getMessage());
        }
    }

    // 按右键位置构建菜单项定义列表
    // params 为右键位置的上下文信息
    // 返回按显示顺序排列的菜单项定义
    private List<MenuEntry> buildEntries(CefContextMenuParams params) {
        // 上下文类型标志，用于判断右键位置所处的元素类型
        int typeFlags = params.getTypeFlags();
        // 编辑状态标志，用于判断各项编辑命令是否可用
        int editFlags = params.getEditStateFlags();
        // 右键位置是否位于链接上
        boolean onLink = hasFlag(typeFlags, CefContextMenuParams.TypeFlags.CM_TYPEFLAG_LINK);
        // 右键位置是否存在选中文本
        boolean hasSelection = hasFlag(typeFlags, CefContextMenuParams.TypeFlags.CM_TYPEFLAG_SELECTION);
        // 链接地址，非链接处为空
        String linkUrl = params.getLinkUrl();

        // 菜单项定义列表，按显示顺序追加
        List<MenuEntry> entries = new ArrayList<>();

        // 编辑组：仅在可编辑区域或有选中文本时提供，避免纯浏览页面出现大量置灰项
        if (params.isEditable() || hasSelection) {
            entries.add(item(CefMenuModel.MenuId.MENU_ID_UNDO, "context.menu.undo",
                    hasFlag(editFlags, CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_UNDO), null));
            entries.add(item(CefMenuModel.MenuId.MENU_ID_REDO, "context.menu.redo",
                    hasFlag(editFlags, CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_REDO), null));
            entries.add(item(CefMenuModel.MenuId.MENU_ID_CUT, "context.menu.cut",
                    hasFlag(editFlags, CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_CUT), null));
            entries.add(item(CefMenuModel.MenuId.MENU_ID_COPY, "context.menu.copy",
                    hasFlag(editFlags, CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_COPY), null));
            entries.add(item(CefMenuModel.MenuId.MENU_ID_PASTE, "context.menu.paste",
                    hasFlag(editFlags, CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_PASTE), null));
            entries.add(item(CefMenuModel.MenuId.MENU_ID_SELECT_ALL, "context.menu.select.all",
                    hasFlag(editFlags, CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_SELECT_ALL), null));
        }

        // 链接组：仅在链接上右键且链接地址有效时提供
        if (onLink && linkUrl != null && !linkUrl.isEmpty()) {
            addGroupSeparator(entries);
            entries.add(item(MENU_ID_OPEN_LINK_NEW_TAB, "context.menu.open.link.new.tab", true,
                    () -> actions.openLinkInNewTab(linkUrl)));
            entries.add(item(MENU_ID_COPY_LINK, "context.menu.copy.link", true,
                    () -> copyToClipboard(linkUrl)));
        }

        // 导航组：后退与前进按当前导航栈状态决定是否可用
        addGroupSeparator(entries);
        entries.add(item(MENU_ID_GO_BACK, "action.back", tab.canGoBack(), tab::goBack));
        entries.add(item(MENU_ID_GO_FORWARD, "action.forward", tab.canGoForward(), tab::goForward));
        entries.add(item(MENU_ID_RELOAD_PAGE, "action.refresh", true, tab::refresh));

        // 页面组：查看源码交给 CEF 内核打开，查找复用面板的查找栏
        addGroupSeparator(entries);
        entries.add(item(MENU_ID_VIEW_SOURCE, "context.menu.view.source", true,
                () -> tab.browser.getCefBrowser().viewSource()));
        entries.add(item(MENU_ID_FIND_IN_PAGE, "action.find", true, actions::showFindBar));

        // 开发者组：内嵌与独立窗口两种开发者工具分别提供入口
        addGroupSeparator(entries);
        entries.add(item(MENU_ID_DEVTOOLS_EMBEDDED, "context.menu.devtools.embedded", true,
                actions::openEmbeddedDevTools));
        entries.add(item(MENU_ID_DEVTOOLS_WINDOW, "context.menu.devtools.window", true,
                actions::openDevToolsWindow));

        return entries;
    }

    // 把文本复制到系统剪贴板
    // text 为待复制的文本
    private void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (Throwable t) {
            System.err.println("[WebBrowser] Copy to clipboard error: " + t.getMessage());
        }
    }

    // 创建普通菜单项定义
    // id 为 CEF 命令 ID
    // textKey 为文案的国际化键
    // enabled 为菜单项是否可用
    // action 为选中后执行的动作，null 表示交回 CEF 内核执行
    // 返回菜单项定义
    private static MenuEntry item(int id, String textKey, boolean enabled, Runnable action) {
        return new MenuEntry(id, textKey, enabled, action);
    }

    // 创建分隔线定义
    // 返回分隔线定义
    private static MenuEntry separator() {
        return new MenuEntry(SEPARATOR_ID, null, false, null);
    }

    // 在菜单项列表末尾插入分组分隔线
    // entries 为菜单项列表
    private static void addGroupSeparator(List<MenuEntry> entries) {
        // 列表已有内容时才插入分隔线，避免菜单以分隔线开头
        if (!entries.isEmpty()) {
            entries.add(separator());
        }
    }

    // 判断标志位是否置位
    // flags 为标志集合
    // flag 为待判断的标志
    // 返回 true 表示该标志已置位
    private static boolean hasFlag(int flags, int flag) {
        return (flags & flag) != 0;
    }

    // 菜单项定义：描述一个菜单项的标识、文案、可用状态与选中后的行为
    private static final class MenuEntry {
        // CEF 命令 ID，分隔线使用占位 ID
        final int id;
        // 文案的国际化键，分隔线为 null
        final String textKey;
        // 菜单项是否可用
        final boolean enabled;
        // 选中后执行的动作，null 表示交回 CEF 内核执行
        final Runnable action;

        // 创建菜单项定义
        // id 为 CEF 命令 ID
        // textKey 为文案的国际化键
        // enabled 为菜单项是否可用
        // action 为选中后执行的动作
        MenuEntry(int id, String textKey, boolean enabled, Runnable action) {
            this.id = id;
            this.textKey = textKey;
            this.enabled = enabled;
            this.action = action;
        }

        // 判断是否为分隔线
        // 返回 true 表示该定义是一条分隔线
        boolean isSeparator() {
            return id == SEPARATOR_ID;
        }
    }
}
