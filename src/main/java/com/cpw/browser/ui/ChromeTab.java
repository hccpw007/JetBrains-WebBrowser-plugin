package com.cpw.browser.ui;

import com.cpw.browser.toolwindow.BrowserTabPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

// Chrome 风格的标签页组件，使用 custom painting 绘制标签形状
public class ChromeTab extends JPanel {

    // 标签页高度（不含 tabStrip 顶部内边距）
    public static final int TAB_HEIGHT = 26;
    // 顶部圆角半径
    private static final int CR = 10;
    // 非活跃标签底部内凹幅度
    private static final int BC = 4;

    // ---- 主题色 ----
    public static final JBColor ACTIVE_BG = new JBColor(0xFFFFFF, 0x454749);
    private static final JBColor INACTIVE_BG = new JBColor(0xC8C8C8, 0x222222);
    private static final JBColor HOVER_BG = new JBColor(0xDCDCDC, 0x303030);
    public static final JBColor BORDER = new JBColor(0xC0C0C0, 0x4A4A4A);
    public static final JBColor STRIP_BG = new JBColor(0xC8C8C8, 0x222222);

    // 关联的浏览器标签页面板
    public final BrowserTabPanel browserTab;
    // 标签选中时的回调
    private final Runnable onSelect;
    // 标签关闭时的回调
    private final Runnable onClose;

    // 当前是否为活跃标签
    private boolean active;

    // 标签标题文本
    public final JBLabel titleLabel;

    // 鼠标是否悬停在标签上
    private boolean hovered;

    public ChromeTab(
            BrowserTabPanel browserTab,
            Runnable onSelect,
            Runnable onClose
    ) {
        this.browserTab = browserTab;
        this.onSelect = onSelect;
        this.onClose = onClose;
        this.active = false;
        this.hovered = false;

        // 初始化标题标签
        titleLabel = new JBLabel(browserTab.getTabTitle());
        titleLabel.setOpaque(false);
        titleLabel.setFont(titleLabel.getFont().deriveFont(12f));

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(new EmptyBorder(4, 10, 0, 6));

        add(titleLabel);
        add(Box.createRigidArea(new Dimension(3, 0)));

        // 关闭按钮
        JButton closeBtn = new JButton("×");
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setFont(closeBtn.getFont().deriveFont(16f));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setToolTipText("关闭标签页");
        closeBtn.setPreferredSize(new Dimension(16, 16));
        closeBtn.setMaximumSize(new Dimension(16, 16));
        closeBtn.setMinimumSize(new Dimension(16, 16));
        closeBtn.addActionListener(e -> onClose.run());
        add(closeBtn);

        // 标签点击和悬停事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSelect.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    // 获取当前是否为活跃标签
    public boolean isActive() {
        return active;
    }

    // 设置活跃状态，状态变化时触发重绘
    public void setActive(boolean active) {
        // 切换活跃状态时触发重绘
        if (this.active != active) {
            this.active = active;
            repaint();
        }
    }

    // 获取首选尺寸，固定高度为 TAB_HEIGHT
    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        return new Dimension(pref.width, TAB_HEIGHT);
    }

    // 获取最小尺寸，宽度最小 70px
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(70, TAB_HEIGHT);
    }

    // 获取最大尺寸，宽度最大 240px
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(240, TAB_HEIGHT);
    }

    // 自定义绘制标签外观（背景色和边框）
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        int w = getWidth();
        int h = getHeight();

        // 选择背景色：活跃 > 悬停 > 非活跃
        JBColor bg;
        // 如果标签处于活跃状态，使用活跃背景色
        if (active) {
            bg = ACTIVE_BG;
        } else if (hovered) { // 如果鼠标悬停，使用悬停背景色
            bg = HOVER_BG;
        } else { // 否则使用非活跃背景色
            bg = INACTIVE_BG;
        }

        // 填充背景
        g2.setColor(bg);
        g2.fill(buildTabShape(w, h, active));

        // 描边：活跃标签不画底边（与内容区相连）
        g2.setColor(BORDER);
        // 如果是活跃标签，只绘制顶部和两侧的边框（不画底边）
        if (active) {
            Path2D borderPath = new Path2D.Float();
            borderPath.moveTo(0.0f, h);
            borderPath.lineTo(0.0f, CR);
            borderPath.quadTo(0.0f, 0.0f, CR, 0.0f);
            borderPath.lineTo(w - CR, 0.0f);
            borderPath.quadTo(w, 0.0f, w, CR);
            borderPath.lineTo(w, h);
            g2.draw(borderPath);
        } else { // 非活跃标签直接绘制完整轮廓
            g2.draw(buildTabShape(w, h, false));
        }

        g2.dispose();
    }

    // 构建 Chrome 风格标签的外形路径
    private Path2D buildTabShape(int w, int h, boolean active) {
        Path2D path = new Path2D.Float();
        path.moveTo(0.0f, CR);
        path.quadTo(0.0f, 0.0f, CR, 0.0f);
        path.lineTo(w - CR, 0.0f);
        path.quadTo(w, 0.0f, w, CR);
        // 活跃标签直接画直角底边，非活跃标签底部内凹
        if (active) {
            path.lineTo(w, h);
            path.lineTo(0.0f, h);
        } else { // 非活跃标签绘制底部内凹曲线
            path.lineTo(w, h - BC);
            path.quadTo(w, h, w - BC, h);
            path.lineTo(BC, h);
            path.quadTo(0.0f, h, 0.0f, h - BC);
        }
        path.closePath();
        return path;
    }
}
