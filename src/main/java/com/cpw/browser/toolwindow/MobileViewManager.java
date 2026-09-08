// 手机外壳窄屏预览管理器：把浏览器内容收窄为手机宽度居中显示（两侧灰边），
// 并通过 CDP 对当前页面应用手机 User-Agent 与触摸模拟，还原 Chrome DevTools 的移动设备预览
package com.cpw.browser.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

// 手机/桌面视图管理器，宿主组件由 BrowserTabPanel 暴露给外层布局
public final class MobileViewManager {

    // 手机模式内容区宽度（CSS 像素，约 iPhone 尺寸）
    private static final int MOBILE_WIDTH = 375;
    // 手机外壳左右边框与内边距合计宽度
    private static final int SHELL_PADDING = 12;
    // 手机外壳整体宽度（内容宽度 + 边框与内边距）
    private static final int SHELL_TOTAL_WIDTH = MOBILE_WIDTH + SHELL_PADDING;

    // 所属浏览器标签页，用于获取当前 URL 匹配 CDP 页面目标并刷新页面
    private final BrowserTabPanel tab;
    // 浏览器底层组件，在桌面布局与手机外壳之间移动父子关系
    private final JComponent browserComponent;
    // 对外暴露给外层布局的宿主容器（外层布局始终引用它，手机外壳只在其内部变化）
    private final JPanel hostPanel;
    // 宿主容器内的内容面板（桌面：直接承载浏览器；手机：承载带灰边的窄屏外壳）
    private final JPanel contentPanel;
    // 是否处于手机模式（EDT 写，后台线程在意图检查时读，volatile 保证可见性）
    private volatile boolean mobileMode = false;
    // 串行执行 CDP 命令的任务锁，避免并发发送顺序错乱
    private final Object cdpLock = new Object();

    // 构造管理器，tab 为所属浏览器标签页
    public MobileViewManager(BrowserTabPanel tab) {
        this.tab = tab;
        this.browserComponent = tab.browser.getComponent();
        this.hostPanel = new JPanel(new BorderLayout());
        this.contentPanel = new JPanel(new BorderLayout());
        // 默认桌面模式：浏览器组件铺满主机容器
        contentPanel.add(browserComponent, BorderLayout.CENTER);
        hostPanel.add(contentPanel, BorderLayout.CENTER);
    }

    // 获取对外布局使用的主机组件
    public JComponent getHostComponent() {
        return hostPanel;
    }

    // 是否处于手机模式
    public boolean isMobileMode() {
        return mobileMode;
    }

    // 切换手机/桌面模式
    // mobile 为 true 进入手机窄屏模式，false 恢复桌面全宽模式
    public void setMobileMode(boolean mobile) {
        // 状态未变化则忽略，避免无意义的重复重建
        if (mobileMode == mobile) return;
        mobileMode = mobile;
        // 先把浏览器组件从其当前父容器中摘除，避免组件同时挂载在两个容器上
        detachBrowser();
        contentPanel.removeAll();
        // 手机模式：把浏览器放入带灰边的窄屏外壳并居中
        if (mobile) {
            JPanel shell = createMobileShell();
            contentPanel.add(shell, BorderLayout.CENTER);
        } else { // 桌面模式：浏览器直接铺满内容面板
            contentPanel.add(browserComponent, BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
        // 在后台线程应用/清除 CDP 手机 UA 与触摸模拟，命令成功后刷新页面使设置生效
        applyCdpMode(mobile);
    }

    // 把浏览器组件从其当前父容器中移除
    private void detachBrowser() {
        Container parent = browserComponent.getParent();
        // 存在父容器则移除浏览器组件
        if (parent != null) {
            parent.remove(browserComponent);
        }
    }

    // 创建手机外壳面板：灰色背景上放置固定宽度的窄屏浏览器区域（模拟手机屏幕）
    private JPanel createMobileShell() {
        // 手机屏幕面板（承载浏览器，固定为手机宽度）
        JPanel shell = new JPanel(new BorderLayout());
        // 背景模拟手机屏幕（浅色主题为白色，深色主题为深灰）
        shell.setBackground(new JBColor(0xFFFFFF, 0x2B2D30));
        // 四周留出黑色"手机边框"与内边距效果
        shell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new JBColor(0x9AA0A6, 0x5C5F64), 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        // 固定外壳宽度（高度自适应可用空间）
        shell.setMinimumSize(new Dimension(SHELL_TOTAL_WIDTH, 0));
        shell.setPreferredSize(new Dimension(SHELL_TOTAL_WIDTH, SHELL_TOTAL_WIDTH));
        shell.setMaximumSize(new Dimension(SHELL_TOTAL_WIDTH, Integer.MAX_VALUE));
        // 浏览器组件放入屏幕区域
        shell.add(browserComponent, BorderLayout.CENTER);

        // 灰色衬底，两侧透明占位把手机外壳挤到水平中央、垂直拉满可用高度
        JPanel backdrop = new JPanel(new GridBagLayout());
        // 衬底背景为较浅的灰（区分外壳与周围空白）
        backdrop.setBackground(new JBColor(0xE3E4E6, 0x3C3F41));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        // 左侧透明占位（水平弹性，把外壳推向中央）
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        backdrop.add(createSpacer(), gbc);
        // 中部为手机外壳（宽度固定，纵向拉满）
        gbc.weightx = 0;
        gbc.weighty = 1;
        backdrop.add(shell, gbc);
        // 右侧透明占位（水平弹性）
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        backdrop.add(createSpacer(), gbc);
        return backdrop;
    }

    // 创建透明占位面板（用于把外壳在灰色衬底上水平居中）
    private JPanel createSpacer() {
        // 占位面板透明且无内容
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        return spacer;
    }

    // 在后台线程通过 CDP 应用或清除手机模拟，成功后刷新页面
    // mobile 为期望的手机模式状态（仅用于本次命令）
    private void applyCdpMode(boolean mobile) {
        // 记录本次任务发起时的目标状态
        boolean targetMobile = mobile;
        // 在后台线程执行网络与等待操作，避免阻塞 EDT
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // 是否发送成功且需要刷新页面
            boolean shouldReload = false;
            // 串行执行 CDP 命令，避免并发切换时命令乱序
            synchronized (cdpLock) {
                // 若等待期间模式已被后续切换改变，则放弃本次应用（由后续任务负责）
                if (targetMobile != mobileMode) return;
                // 应用或清除手机模拟并返回是否成功
                shouldReload = applyCdpToCurrentPage(targetMobile);
            }
            // 发送成功后刷新页面使 UA/触摸设置生效（UA 变更后需重新请求页面资源）
            if (shouldReload && targetMobile == mobileMode) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        tab.browser.getCefBrowser().reload();
                    } catch (Throwable t) {
                        System.err.println("[WebBrowser] Device mode reload error: " + t.getMessage());
                    }
                });
            }
        });
    }

    // 通过 CDP 对当前页面应用或清除手机模拟，成功返回 true
    // mobile 为 true 设置手机 UA 与触摸，false 恢复桌面设置
    private boolean applyCdpToCurrentPage(boolean mobile) {
        // 查找 JCEF 远程调试端口
        Integer port = CdpSupport.findDevToolsPort();
        // 未找到端口则记录并返回失败
        if (port == null || port <= 0) {
            System.err.println("[WebBrowser] Device mode: DevTools port not found");
            return false;
        }
        // 按当前 URL 匹配页面 WebSocket 地址
        String wsUrl = CdpSupport.findPageWebSocketUrl(port, tab.getCurrentUrl());
        // 未找到页面目标则记录并返回失败
        if (wsUrl == null) {
            System.err.println("[WebBrowser] Device mode: page target not found");
            return false;
        }
        // 发送 CDP 设备模拟命令
        return CdpEmulationClient.applyMobileSimulation(wsUrl, mobile);
    }
}
