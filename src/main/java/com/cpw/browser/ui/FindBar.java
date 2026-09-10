// 页面内查找栏（Chrome 风格浮层）：在网页中查找关键字并高亮全部匹配，支持上一个/下一个与关闭
package com.cpw.browser.ui;

import com.cpw.browser.WebBrowserIcons;
import com.cpw.browser.util.TranslationUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;

// 页面内查找栏组件，以圆角浮层形式显示在网页右上角
public class FindBar extends JPanel {

    // 查找输入框宽度（像素）
    private static final int FIELD_WIDTH = 150;
    // 输入框高度（像素）
    private static final int FIELD_HEIGHT = 24;
    // 图标按钮边长（像素）
    private static final int BUTTON_SIZE = 24;
    // 浮层圆角半径
    private static final int PANEL_ARC = 12;
    // 按钮悬停背景圆角半径
    private static final int BUTTON_ARC = 8;
    // 浮层背景色（浅色主题白色、深色主题深灰）
    private static final Color PANEL_BG = new JBColor(0xFFFFFF, 0x2B2D30);
    // 浮层描边色
    private static final Color PANEL_BORDER = new JBColor(0xC9CCD6, 0x4E5157);
    // 按钮悬停背景色
    private static final Color BUTTON_HOVER_BG = new JBColor(0xE8E8E8, 0x3C3F41);
    // 匹配数量标签宽度（固定宽度，避免数字位数变化导致布局跳动）
    private static final int COUNT_LABEL_WIDTH = 66;
    // 匹配数量文字颜色（相对次要的提示色）
    private static final Color COUNT_TEXT = new JBColor(0x6C707E, 0x9DA0A8);

    // 查找动作处理器，由宿主面板实现以驱动浏览器执行查找
    public interface FindHandler {

        // 在页面内执行查找
        // text 为查找关键字
        // forward 为 true 时向后查找，false 时向前查找
        // findNext 为 true 时沿用上次查找继续，false 时开始新的查找
        void find(String text, boolean forward, boolean findNext);

        // 结束查找并清除页面内高亮
        void stop();

        // 统计页面内匹配数量
        // text 为查找关键字
        // callback 为统计结果回调，参数为匹配数量
        void count(String text, IntConsumer callback);
    }

    // 扁平图标按钮，鼠标悬停时显示圆角背景
    private static class IconButton extends JButton {

        // 悬停时的背景色
        private final Color hoverColor;

        // 构造图标按钮
        // icon 为按钮图标
        // hoverColor 为悬停时的背景色
        IconButton(Icon icon, Color hoverColor) {
            super(icon);
            this.hoverColor = hoverColor;
            setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            // 悬停时先绘制圆角背景，其余状态保持透明
            if (getModel().isRollover()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hoverColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BUTTON_ARC, BUTTON_ARC);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    // 查找关键字输入框
    private final JBTextField findField;
    // 匹配数量标签（显示共找到多少处匹配）
    private final JBLabel countLabel;
    // 查找动作处理器
    private final FindHandler findHandler;
    // 上一个匹配按钮
    private final IconButton prevButton;
    // 下一个匹配按钮
    private final IconButton nextButton;
    // 关闭查找栏按钮
    private final IconButton closeButton;

    // 构造查找栏
    // findHandler 为查找动作处理器
    public FindBar(FindHandler findHandler) {
        this.findHandler = findHandler;
        // 背景自绘圆角，故面板本身保持透明
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 5));
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));

        // 查找关键字输入框
        findField = new JBTextField();
        findField.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        // 输入框融入浮层：去掉默认边框与不透明背景
        findField.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        findField.setOpaque(false);
        // 输入框无内容时显示的占位提示
        findField.getEmptyText().setText(TranslationUtil.getText("find.placeholder"));
        findField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                startNewFind();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                startNewFind();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                startNewFind();
            }
        });
        findField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // 回车：按 Shift 状态决定查找方向（Shift+回车向前，回车向后）
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    findFromField(!e.isShiftDown());
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { // Esc：关闭查找栏
                    hideBar();
                    e.consume();
                }
            }
        });

        // 匹配数量标签（右对齐，固定宽度）
        countLabel = new JBLabel("", SwingConstants.RIGHT);
        countLabel.setPreferredSize(new Dimension(COUNT_LABEL_WIDTH, FIELD_HEIGHT));
        countLabel.setForeground(COUNT_TEXT);

        // 上一个匹配按钮
        prevButton = new IconButton(WebBrowserIcons.FIND_PREV, BUTTON_HOVER_BG);
        prevButton.addActionListener(e -> {
            findFromField(false);
            // 焦点留在输入框，便于继续输入或回车
            findField.requestFocusInWindow();
        });
        // 下一个匹配按钮
        nextButton = new IconButton(WebBrowserIcons.FIND_NEXT, BUTTON_HOVER_BG);
        nextButton.addActionListener(e -> {
            findFromField(true);
            // 焦点留在输入框，便于继续输入或回车
            findField.requestFocusInWindow();
        });
        // 关闭查找栏按钮
        closeButton = new IconButton(WebBrowserIcons.FIND_CLOSE, BUTTON_HOVER_BG);
        closeButton.addActionListener(e -> hideBar());

        add(findField);
        add(countLabel);
        add(prevButton);
        add(nextButton);
        add(closeButton);
        // 默认隐藏，由宿主面板在触发查找时显示
        setVisible(false);
        refreshLabels();
    }

    // 绘制圆角浮层背景与描边
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(PANEL_BG);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, PANEL_ARC, PANEL_ARC);
        g2.setColor(PANEL_BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, PANEL_ARC, PANEL_ARC);
        g2.dispose();
        super.paintComponent(g);
    }

    // 显示查找栏并把焦点移到输入框（保留上次关键字并全选，便于直接输入新关键字）
    public void showBar() {
        setVisible(true);
        revalidate();
        repaint();
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    // 隐藏查找栏并结束查找（清除页面内高亮）
    public void hideBar() {
        // 已经隐藏时无需重复处理，避免多余的结束查找调用
        if (!isVisible()) return;
        setVisible(false);
        // 清空匹配数量提示，下次显示时重新统计
        countLabel.setText("");
        revalidate();
        repaint();
        findHandler.stop();
    }

    // 刷新界面文案（语言切换后调用）
    public void refreshLabels() {
        prevButton.setToolTipText(TranslationUtil.getText("find.prev"));
        nextButton.setToolTipText(TranslationUtil.getText("find.next"));
        closeButton.setToolTipText(TranslationUtil.getText("find.close"));
        findField.getEmptyText().setText(TranslationUtil.getText("find.placeholder"));
    }

    // 从输入框当前内容开始一次新的查找（从当前位置向后定位第一处匹配）
    private void startNewFind() {
        // 输入为空时结束查找并清空数量提示，清除上一次遗留的高亮
        if (findField.getText().isEmpty()) {
            findHandler.stop();
            showCount(0);
            return;
        }
        // 原生查找负责高亮与定位，JS 统计负责给出匹配总数
        findHandler.find(findField.getText(), true, false);
        findHandler.count(findField.getText(), this::showCount);
    }

    // 更新匹配数量显示
    // count 为匹配数量，0 表示没有匹配
    private void showCount(int count) {
        // 输入为空时不显示数量提示
        if (findField.getText().isEmpty()) {
            countLabel.setText("");
            return;
        }
        // 有匹配时显示数量，无匹配时显示无结果提示
        countLabel.setText(count > 0
                ? TranslationUtil.getText("find.count", String.valueOf(count))
                : TranslationUtil.getText("find.no.result"));
    }

    // 按指定方向沿上次查找继续定位匹配
    // forward 为 true 时向后查找，false 时向前查找
    private void findFromField(boolean forward) {
        // 输入为空时没有可查找的内容
        if (findField.getText().isEmpty()) return;
        findHandler.find(findField.getText(), forward, true);
    }
}
