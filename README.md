# WebBrowser

A fully-featured Chrome-like web browser plugin for JetBrains IDEs.  
一款功能完整类似 Chrome 的浏览器插件。

---

## Features / 功能特性

- **Any URL** — Enter any URL to browse directly.  
  **任意网址** — 输入任意网址即可直接浏览。
- **Auto-Refresh** — Auto-reload pages at configurable intervals.  
  **自动刷新** — 按可配置间隔自动重新加载页面。
- **Search Engine** — Search directly when input is not a URL.  
  **搜索引擎** — 输入内容非网址时，可直接使用搜索引擎。
- **DevTools** — Embedded split-pane or separate window mode.  
  **开发者工具** — 支持嵌入分屏或独立窗口模式。
- **Multi-Tab** — Create, close, and switch between tabs.  
  **多标签页** — 创建、关闭和切换标签页。
- **Bookmarks** — Manage bookmarks.  
  **书签管理** — 管理书签。
- **History** — Browsing history records.  
  **浏览历史** — 浏览历史记录。
- **Zoom Controls** — Zoom in/out/reset.  
  **缩放控制** — 放大/缩小/重置。
- **Dual Display** — Sidebar tool window or editor tab.  
  **双模式显示** — 支持侧边栏工具窗口和编辑区标签页两种模式。
- **Internationalization** — Supports 7 languages with instant switching.  
  **国际化** — 支持 7 种语言（英文、简体中文、日文、韩文、法文、德文），即时切换。
- **More** — Other customizable settings.  
  **其他** — 更多可自定义设置。

## Requirements / 环境要求

- **JetBrains IDE** 2025.3 or later  
  **JetBrains IDE** 2025.3 或更新版本

## Installation / 安装

Search for **WebBrowser** in **Settings/Preferences → Plugins** and install it.  
在 **设置/偏好设置 → 插件** 中搜索 **WebBrowser** 并安装。

Or build from source / 或从源码构建：

```bash
git clone https://github.com/hccpw007/JetBrains-WebBrowser-plugin.git
cd JetBrains-WebBrowser-plugin
./gradlew build
```

## Tech Stack / 技术栈

- **Java** (pure Java, no Kotlin dependencies)
- **IntelliJ Platform Plugin SDK**
- **JCEF** (Java Chromium Embedded Framework)

## License / 许可证

MIT License
