package com.cpw.browser.bookmark

data class Bookmark(
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)
