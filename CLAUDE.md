# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
