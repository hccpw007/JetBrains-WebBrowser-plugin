# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ⚠️ 最高优先级规则：对话完成后自动提交

每次对话中完成编译（`./gradlew build` 或 `./gradlew compileKotlin` 通过）后，必须执行以下操作：

1. 运行 `git add -A` 暂存所有文件变更
2. 运行 `git commit -m "{本次修改的内容总结}"` 提交代码
3. commit 信息中的 `{本次修改的内容总结}` 用简短的中文概括本次对话修改了哪些内容、解决了什么问题

此规则覆盖其他所有规则，每次编译通过后必须执行。

## Build & Run Commands

```bash
# Build the plugin
./gradlew build

# Run the IDE with the plugin loaded
./gradlew runIde

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.cpw.browser.*Test"

# Verify plugin compatibility
./gradlew verifyPlugin

# Publish to JetBrains Marketplace
./gradlew publishPlugin

# Clean build artifacts
./gradlew clean
```

## Project Architecture

This is an **IntelliJ Platform Plugin** built with Kotlin + Gradle, targeting IntelliJ IDEA 2026.1.2.

### Tech Stack
- Kotlin 2.2.20
- IntelliJ Platform Gradle Plugin 2.16.0
- IntelliJ IDEA 2026.1.2 (target platform)
- JUnit 4.13.2 (testing)
- Group: `com.cpw.browser`, artifact: `WebBrowser`

### Source Layout
```
src/main/kotlin/          # Production Kotlin sources
src/main/resources/       # Resources
  META-INF/plugin.xml     # Plugin descriptor (id, name, dependencies, extensions)
  META-INF/pluginIcon.svg # Plugin icon
  messages/               # i18n message bundles (.properties)
```

### Key Files
- **plugin.xml**: Defines plugin ID (`com.cpw.browser.WebBrowser`), dependencies (`com.intellij.modules.lsp`, `com.intellij.java`), and extension points (tool windows, listeners, etc.)
- **pluginIcon.svg**: 40x40 plugin icon displayed in IDE Plugin Manager
- **MyToolWindowFactory.kt**: ToolWindow factory — entry point for tool window extensions
- **MyMessageBundle.kt**: i18n helper wrapping `DynamicBundle`

### Extension Points
The plugin declares extensions via `<extensions>` in `plugin.xml`. Current extensions:
- `<toolWindow>` — registers custom tool windows in the IDE sidebar

### Development Workflow
- Run configurations are in `.run/` (Run Plugin, Run Tests, Run Verifications)
- Logs appear in `idea.log` tab during development runs
- Use `./gradlew runIde` to launch a development IDE instance with the plugin
- Plugin depends on `com.intellij.modules.lsp` and `com.intellij.java` — use LSP APIs and Java-specific IntelliJ APIs directly
