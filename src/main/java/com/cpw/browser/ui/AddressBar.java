package com.cpw.browser.ui;

import com.cpw.browser.WebBrowserIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;

// 地址栏组件，包含 URL 输入框和书签星标
public class AddressBar extends JPanel {

    public final JBTextField urlField;
    private final JBLabel starLabel;
    private final Consumer<String> onNavigate;
    private final Predicate<String> isUrlBookmarked;
    private final Consumer<String> onToggleBookmark;

    public AddressBar(
            Consumer<String> onNavigate,
            Predicate<String> isUrlBookmarked,
            Consumer<String> onToggleBookmark
    ) {
        super(new BorderLayout());
        this.onNavigate = onNavigate;
        this.isUrlBookmarked = isUrlBookmarked;
        this.onToggleBookmark = onToggleBookmark;

        // 初始化 URL 输入框
        urlField = new JBTextField();
        urlField.getEmptyText().setText("about:blank");

        // 初始化书签星标
        starLabel = new JBLabel(WebBrowserIcons.STAR);
        starLabel.setOpaque(false);
        starLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        starLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 点击星标触发书签切换
                String url = urlField.getText().trim();
                if (!url.isEmpty() && !"about:blank".equals(url)) {
                    onToggleBookmark.accept(url);
                }
            }
        });

        // 自定义圆角边框：聚焦时不用蓝色
        Border normalBorder = BorderFactory.createCompoundBorder(
                new RoundedBorder(new JBColor(0xF0F0F0, 0x4A4A4A), 15),
                BorderFactory.createEmptyBorder(0, 4, 0, 27)
        );
        Border focusBorder = BorderFactory.createCompoundBorder(
                new RoundedBorder(new JBColor(0xD0D0D0, 0x888888), 15),
                BorderFactory.createEmptyBorder(0, 4, 0, 27)
        );
        urlField.setBorder(normalBorder);
        urlField.setFont(urlField.getFont().deriveFont(13f));

        // 聚焦/失焦时切换边框
        urlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                urlField.setBorder(focusBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                urlField.setBorder(normalBorder);
            }
        });

        // 按下回车时导航
        urlField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigate();
                }
            }
        });

        // 使用 JLayeredPane 将星标叠加在文本字段右侧内部
        JLayeredPane fieldLayer = new JLayeredPane() {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                urlField.setBounds(0, 0, w, h);
                int starSize = 14;
                starLabel.setBounds(w - starSize - 9, (h - starSize) / 2, starSize, starSize);
                setLayer(starLabel, JLayeredPane.PALETTE_LAYER);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension ps = urlField.getPreferredSize();
                return new Dimension(ps.width, ps.height - 4);
            }
        };

        fieldLayer.add(urlField, JLayeredPane.DEFAULT_LAYER);
        fieldLayer.add(starLabel, JLayeredPane.PALETTE_LAYER);

        add(fieldLayer, BorderLayout.CENTER);
    }

    public void setUrl(String url) {
        // 设置地址栏文本并更新星标状态
        String displayText = "about:blank".equals(url) ? "" : url;
        if (!displayText.equals(urlField.getText())) {
            urlField.setText(displayText);
        }
        updateStarIcon(url);
    }

    public void updateStarIcon(String url) {
        // 根据书签状态更新星标图标和提示
        if (!url.isEmpty() && !"about:blank".equals(url)) {
            boolean bookmarked = isUrlBookmarked.test(url);
            starLabel.setIcon(bookmarked ? WebBrowserIcons.STAR_FILLED : WebBrowserIcons.STAR);
            starLabel.setToolTipText(bookmarked ? "从书签中移除" : "添加书签");
        } else {
            starLabel.setIcon(WebBrowserIcons.STAR);
            starLabel.setToolTipText("添加书签");
        }
    }

    public String getUrl() {
        return urlField.getText().trim();
    }

    public void requestFocusOnField() {
        // 聚焦地址栏并全选文本
        urlField.requestFocus();
        urlField.selectAll();
    }

    // 触发地址栏导航
    private void navigate() {
        String text = getUrl();
        if (!text.isEmpty()) {
            onNavigate.accept(text);
        }
    }

    // 圆角边框
    private static class RoundedBorder extends AbstractBorder {
        private final JBColor color;
        private final int radius;

        RoundedBorder(JBColor color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 1;
            insets.left = 1;
            insets.bottom = 1;
            insets.right = 1;
            return insets;
        }
    }
}
