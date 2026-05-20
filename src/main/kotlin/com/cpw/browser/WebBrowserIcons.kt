package com.cpw.browser

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object WebBrowserIcons {
    val Back = load("back")
    val Forward = load("forward")
    val Refresh = load("refresh")
    val Home = load("home")
    val DevTools = load("devtools")
    val BookmarkAdd = load("bookmark-add")
    val BookmarkRemove = load("bookmark-remove")
    val NewTab = load("new-tab")
    val ToolWindow = load("toolwindow")
    val Star = load("star")
    val StarFilled = load("star-filled")
    val ShowBookmark = load("显示书签")
    val ZoomIn = load("放大镜")
    val ZoomOut = load("缩小镜")
    val Google = load("谷歌")

    private fun load(name: String): Icon =
        IconLoader.getIcon("/icons/$name.svg", javaClass)
}
