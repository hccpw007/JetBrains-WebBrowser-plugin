package com.cpw.browser.ui

import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JPanel

class AddressBar(
    private val onNavigate: (String) -> Unit
) : JPanel(BorderLayout()) {

    val urlField = JBTextField()

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

        add(urlField, BorderLayout.CENTER)
        add(goButton, BorderLayout.EAST)
    }

    fun setUrl(url: String) {
        if (url != urlField.text) {
            urlField.text = url
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
