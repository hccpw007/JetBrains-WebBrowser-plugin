// 页面内查找控制器：把查找命令转发给 CEF 浏览器执行，并拦截网页内的查找快捷键
// JBCefBrowser 未暴露查找 API，查找能力来自底层 CefBrowser.find / stopFinding
package com.cpw.browser.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefKeyboardHandler;
import org.cef.handler.CefKeyboardHandlerAdapter;
import org.cef.misc.BoolRef;

import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

// 页面内查找控制器
final class PageFindController {

    // CEF 事件标志：Ctrl 键按下（取值来自 CEF 的 EVENTFLAG_CONTROL_DOWN）
    private static final int CEF_EVENTFLAG_CONTROL_DOWN = 1 << 2;
    // CEF 事件标志：Command 键按下（macOS 专用，取值来自 CEF 的 EVENTFLAG_COMMAND_DOWN）
    private static final int CEF_EVENTFLAG_COMMAND_DOWN = 1 << 7;

    // 所属浏览器实例
    private final JBCefBrowser browser;
    // 网页内按下查找快捷键后的回调
    private Runnable onFindShortcut = null;
    // JS 回传通道：接收页面内统计出的匹配数量（JCEF 不回传原生查找的匹配总数）
    private final JBCefJSQuery countQuery;
    // 统计请求序号：连续输入会发起多次统计，靠序号丢弃过期结果
    private final AtomicInteger countRequestId = new AtomicInteger();
    // 最近一次统计请求的结果回调
    private volatile IntConsumer countCallback = count -> {
    };

    // 构造控制器并注册网页内的快捷键拦截
    // browser 为所属标签页的浏览器实例
    PageFindController(JBCefBrowser browser) {
        this.browser = browser;
        // JS 回传通道：接收页面统计出的匹配数量
        this.countQuery = JBCefJSQuery.create(browser);
        this.countQuery.addHandler(this::onCountReceived);
        registerShortcutHandler(browser);
    }

    // 设置网页内查找快捷键回调
    // onFindShortcut 为按下查找快捷键后执行的回调
    void setOnFindShortcut(Runnable onFindShortcut) {
        this.onFindShortcut = onFindShortcut;
    }

    // 在页面内查找指定文本（使用浏览器原生查找，自动高亮全部匹配并滚动到当前匹配）
    // text 为查找关键字
    // forward 为 true 时向后查找，false 时向前查找
    // findNext 为 true 时沿用上次查找继续，false 时开始一次新的查找
    void findText(String text, boolean forward, boolean findNext) {
        // 关键字为空时无可查找内容
        if (text == null || text.isEmpty()) return;
        try {
            browser.getCefBrowser().find(text, forward, false, findNext);
        } catch (Throwable t) {
            System.err.println("[WebBrowser] Find text error: " + t.getMessage());
        }
    }

    // 结束查找并清除页面内高亮
    void stopFinding() {
        try {
            browser.getCefBrowser().stopFinding(true);
        } catch (Throwable t) {
            System.err.println("[WebBrowser] Stop finding error: " + t.getMessage());
        }
    }

    // 统计页面内匹配数量（原生查找不回传总数，故用 JS 遍历文本节点统计）
    // text 为查找关键字
    // callback 为统计结果回调，参数为匹配数量（0 表示无匹配）
    void countMatches(String text, IntConsumer callback) {
        // 关键字为空时无需统计
        if (text == null || text.isEmpty()) {
            callback.accept(0);
            return;
        }
        try {
            // 递增请求序号，使尚未回传的旧统计结果失效
            int requestId = countRequestId.incrementAndGet();
            countCallback = callback;
            browser.getCefBrowser().executeJavaScript(buildCountScript(text, requestId), "", 0);
        } catch (Throwable t) {
            System.err.println("[WebBrowser] Find count error: " + t.getMessage());
            callback.accept(0);
        }
    }

    // 处理页面回传的匹配数量
    // result 为回传字符串，格式为 "请求序号|匹配数量"
    // 返回 null 表示无需向页面返回应答
    private JBCefJSQuery.Response onCountReceived(String result) {
        try {
            // 分隔符位置，用于拆分序号与数量
            int separator = result.indexOf('|');
            // 格式非法时忽略该结果
            if (separator <= 0) return null;
            int requestId = Integer.parseInt(result.substring(0, separator));
            int count = Integer.parseInt(result.substring(separator + 1));
            // 非最新请求的结果属于过期数据，直接丢弃
            if (requestId != countRequestId.get()) return null;
            // 该回调在 CEF 线程触发，更新界面需切回 EDT
            ApplicationManager.getApplication().invokeLater(() -> countCallback.accept(count));
        } catch (Throwable t) {
            System.err.println("[WebBrowser] Find count handling error: " + t.getMessage());
        }
        return null;
    }

    // 构建统计匹配数量的 JS 脚本
    // text 为查找关键字
    // requestId 为本次统计的请求序号，随结果一并回传
    // 返回可直接执行的 JS 代码
    private String buildCountScript(String text, int requestId) {
        // 统计逻辑：跳过脚本/样式等非正文节点，按不区分大小写统计文本节点内的出现次数
        return "var __wbFindCount = (function(){"
                + "var kw = '" + escapeJsString(text) + "'.toLowerCase();"
                + "if (!kw || !document.body) return 0;"
                + "var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);"
                + "var count = 0, node;"
                + "while ((node = walker.nextNode())) {"
                + "var parent = node.parentNode;"
                + "if (!parent) continue;"
                + "var tag = parent.nodeName;"
                + "if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'NOSCRIPT' || tag === 'TEXTAREA') continue;"
                + "var content = node.nodeValue.toLowerCase();"
                + "var index = content.indexOf(kw);"
                + "while (index !== -1) { count++; index = content.indexOf(kw, index + kw.length); }"
                + "}"
                + "return count;})();"
                + countQuery.inject("'" + requestId + "|' + __wbFindCount");
    }

    // 把关键字转义为可安全嵌入 JS 单引号字符串的形式
    // text 为原始关键字
    // 返回转义后的关键字
    private String escapeJsString(String text) {
        return text.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("</", "<\\/");
    }

    // 注册网页内的查找快捷键拦截，避免按键被网页脚本吞掉导致查找栏无法唤出
    // browser 为所属浏览器实例
    private void registerShortcutHandler(JBCefBrowser browser) {
        browser.getJBCefClient().addKeyboardHandler(new CefKeyboardHandlerAdapter() {
            @Override
            public boolean onPreKeyEvent(CefBrowser cefBrowser, CefKeyboardHandler.CefKeyEvent event, BoolRef isKeyboardShortcut) {
                try {
                    // 仅处理按键按下事件，避免同一次按键被重复处理
                    if (event.type != CefKeyboardHandler.CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN) return false;

                    // 非 F 键不拦截
                    if (event.windows_key_code != KeyEvent.VK_F) return false;

                    // 仅响应本平台的查找修饰键（macOS 为 Command，其他平台为 Ctrl）
                    boolean modifierPressed = SystemInfo.isMac
                            ? (event.modifiers & CEF_EVENTFLAG_COMMAND_DOWN) != 0
                            : (event.modifiers & CEF_EVENTFLAG_CONTROL_DOWN) != 0;

                    // 未按下本平台的查找修饰键则不拦截
                    if (!modifierPressed) return false;

                    // 该回调在 CEF 线程触发，更新界面需切换到 EDT
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            // 已注册回调则通知外部显示查找栏
                            if (onFindShortcut != null) {
                                onFindShortcut.run();
                            }
                        } catch (Throwable t) {
                            System.err.println("[WebBrowser] Find shortcut callback error: " + t.getMessage());
                        }
                    });
                    // 消费该按键，阻止网页收到查找快捷键
                    return true;
                } catch (Throwable t) {
                    System.err.println("[WebBrowser] Find shortcut handling error: " + t.getMessage());
                    return false;
                }
            }
        }, browser.getCefBrowser());
    }
}
