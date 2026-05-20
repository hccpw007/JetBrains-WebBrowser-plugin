package com.cpw.browser.ui

import com.cpw.browser.WebBrowserIcons
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class AddressBar(
    private val onNavigate: (String) -> Unit,
    private val isUrlBookmarked: (String) -> Boolean,
    private val onToggleBookmark: (String) -> Unit
) : JPanel(BorderLayout()) {

    val urlField = JBTextField()
    private val starButton = JButton(WebBrowserIcons.Star).apply {
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
        isOpaque = false
        toolTipText = "添加书签"
        preferredSize = Dimension(20, 20)
        maximumSize = Dimension(20, 20)
        addActionListener {
            val url = urlField.text.trim()
            if (url.isNotEmpty() && url != "about:blank") {
                onToggleBookmark(url)
            }
        }
    }

    init {
        urlField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    navigate()
                }
            }
        })

        val goButton = JButton("前往").apply {
            addActionListener { navigate() }
        }

        // 右侧面板：[星标] [前往]
        val rightPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(starButton)
            add(Box.createRigidArea(Dimension(2, 0)))
            add(goButton)
        }

        add(urlField, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)
    }

    fun setUrl(url: String) {
        if (url != urlField.text) {
            urlField.text = url
        }
        updateStarIcon(url)
    }

    fun updateStarIcon(url: String) {
        if (url.isNotEmpty() && url != "about:blank") {
            starButton.icon = if (isUrlBookmarked(url)) WebBrowserIcons.StarFilled else WebBrowserIcons.Star
            starButton.toolTipText = if (isUrlBookmarked(url)) "从书签中移除" else "添加书签"
        } else {
            starButton.icon = WebBrowserIcons.Star
            starButton.toolTipText = "添加书签"
        }
    }

    fun getUrl(): String = urlField.text.trim()

    fun requestFocusOnField() {
        urlField.requestFocus()
        urlField.selectAll()
    }

    private fun navigate() {
        val text = getUrl()
        if (text.isNotEmpty()) {
            onNavigate(text)
        }
    }
}
