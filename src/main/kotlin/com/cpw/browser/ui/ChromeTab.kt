package com.cpw.browser.ui

import com.cpw.browser.toolwindow.BrowserTabPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * Chrome 风格的标签页组件。
 *
 * 使用 custom painting 绘制如下形状：
 *  - 活跃标签：顶部圆角 + 底部平直（与内容区无缝连接）
 *  - 非活跃标签：顶部圆角 + 底部四角内凹曲线（底部分隔线可透出）
 *  - 悬停效果：鼠标进入时变亮
 */
class ChromeTab(
    val browserTab: BrowserTabPanel,
    private val onSelect: () -> Unit,
    private val onClose: () -> Unit
) : JPanel() {

    /** 当前是否为活跃标签 */
    var isActive: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                repaint()
            }
        }

    /** 标签标题文本 */
    val titleLabel: JBLabel = JBLabel(browserTab.getTabTitle()).apply {
        isOpaque = false
        font = font.deriveFont(12f)
    }

    private var hovered = false

    companion object {
        /** 标签页高度（不含 tabStrip 顶部内边距） */
        const val TAB_HEIGHT = 26
        /** 顶部圆角半径 */
        private const val CR = 10
        /** 非活跃标签底部内凹幅度 */
        private const val BC = 4

        // ---- 主题色 ----
        val ACTIVE_BG = JBColor(0xFFFFFF, 0x3C3F41)
        private val INACTIVE_BG = JBColor(0xD6D6D6, 0x282828)
        private val HOVER_BG = JBColor(0xE3E3E3, 0x303030)
        val BORDER = JBColor(0xC0C0C0, 0x4A4A4A)
        val STRIP_BG = JBColor(0xD6D6D6, 0x282828)
    }

    init {
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = EmptyBorder(4, 10, 0, 10)

        add(titleLabel)
        add(Box.createRigidArea(Dimension(6, 0)))

        val closeBtn = JButton("×").apply {
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            font = font.deriveFont(16f)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "关闭标签页"
            preferredSize = Dimension(20, 20)
            maximumSize = Dimension(20, 20)
            minimumSize = Dimension(20, 20)
            addActionListener { onClose() }
        }
        add(closeBtn)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onSelect()
            }
            override fun mouseEntered(e: MouseEvent) {
                hovered = true
                repaint()
            }
            override fun mouseExited(e: MouseEvent) {
                hovered = false
                repaint()
            }
        })
    }

    override fun getPreferredSize(): Dimension {
        val pref = super.getPreferredSize()
        return Dimension(pref.width, TAB_HEIGHT)
    }

    override fun getMinimumSize(): Dimension = Dimension(70, TAB_HEIGHT)

    override fun getMaximumSize(): Dimension = Dimension(240, TAB_HEIGHT)

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)

        val w = width
        val h = height

        val bg = when {
            isActive -> ACTIVE_BG
            hovered -> HOVER_BG
            else -> INACTIVE_BG
        }

        // 填充背景
        g2.color = bg
        g2.fill(buildTabShape(w, h, isActive))

        // 描边：活跃标签不画底边（与内容区相连）
        g2.color = BORDER
        if (isActive) {
            val borderPath = Path2D.Float()
            borderPath.moveTo(0.0f, h.toFloat())
            borderPath.lineTo(0.0f, CR.toFloat())
            borderPath.quadTo(0.0f, 0.0f, CR.toFloat(), 0.0f)
            borderPath.lineTo((w - CR).toFloat(), 0.0f)
            borderPath.quadTo(w.toFloat(), 0.0f, w.toFloat(), CR.toFloat())
            borderPath.lineTo(w.toFloat(), h.toFloat())
            g2.draw(borderPath)
        } else {
            g2.draw(buildTabShape(w, h, false))
        }

        g2.dispose()
    }

    /** 构建 Chrome 风格标签的外形路径 */
    private fun buildTabShape(w: Int, h: Int, active: Boolean): Path2D {
        val path = Path2D.Float()
        path.moveTo(0.0f, CR.toFloat())
        path.quadTo(0.0f, 0.0f, CR.toFloat(), 0.0f)
        path.lineTo((w - CR).toFloat(), 0.0f)
        path.quadTo(w.toFloat(), 0.0f, w.toFloat(), CR.toFloat())
        if (active) {
            path.lineTo(w.toFloat(), h.toFloat())
            path.lineTo(0.0f, h.toFloat())
        } else {
            path.lineTo(w.toFloat(), (h - BC).toFloat())
            path.quadTo(w.toFloat(), h.toFloat(), (w - BC).toFloat(), h.toFloat())
            path.lineTo(BC.toFloat(), h.toFloat())
            path.quadTo(0.0f, h.toFloat(), 0.0f, (h - BC).toFloat())
        }
        path.closePath()
        return path
    }
}
