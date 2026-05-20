package com.cpw.browser

import com.intellij.ui.jcef.JBCefAppRequiredArgumentsProvider

/**
 * 在 JCEF 初始化时传递 --remote-debugging-port=0 参数，
 * 使 JCEF 启用远程调试端口并生成 DevToolsActivePort 文件，
 * 供嵌入式 DevTools 通过 CDP 连接使用。
 */
internal class JcefArgsProvider : JBCefAppRequiredArgumentsProvider {

    override val options: List<String> = listOf("--remote-debugging-port=0")
}
