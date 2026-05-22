# WebBrowser

An embedded web browser for IntelliJ IDEA, PyCharm, WebStorm, PhpStorm, and other JetBrains products with multi-tab, bookmark, history, and DevTools support.
适用于 IntelliJ IDEA、PyCharm、WebStorm、PhpStorm 等 JetBrains 产品的内嵌网页浏览器插件，支持多标签页、书签、历史记录和开发者工具。

Embed web pages directly in the IDE with navigation, bookmarks, history, and DevTools support. Powered by JCEF (Java Chromium Embedded Framework).
在 IDE 中直接嵌入网页浏览功能，支持导航、书签、历史记录和开发者工具。基于 JCEF（Java Chromium Embedded Framework）构建。

**Author: Pengwei Chen**

---

## Requirements / 环境要求

### Runtime Requirements / 使用环境要求

- **2025.1 or later** Support for JetBrains products (IntelliJ IDEA, PyCharm, WebStorm, PhpStorm)
- **2025.1 或更新版本** 支持 JetBrains 产品（IntelliJ IDEA、PyCharm、WebStorm、PhpStorm 等）

### Development Requirements / 开发环境要求

- **IntelliJ IDEA** 2025.1+
- **Java** 21+ (JDK)
- **Gradle** (wrapper included / 已包含 wrapper)

---

## Features / 功能特性

### Browser Display / 浏览器显示

- **Tool Window mode** — Browser appears in a docked tool window on the IDE sidebar.
- **工具栏模式** — 浏览器显示在 IDE 侧边栏的停靠工具窗口中。
- **Editor Tab mode** — Browser opens as an editor tab in the main editor area.
- **编辑区模式** — 浏览器以编辑器标签页形式在主编辑区打开。
- **Toggle shortcut** `Alt+Shift+W` — Quickly show or hide the browser.
- **切换快捷键** `Alt+Shift+W` — 快速打开或关闭浏览器。

### Navigation / 导航

- **Back / Forward** — Navigate through session history; buttons auto-disable when no history exists.
- **后退 / 前进** — 在会话历史中导航，按钮在无历史记录时自动禁用。
- **Home** — Navigate to your configured homepage with one click.
- **主页** — 一键导航到您设置的主页。
- **Refresh** — Reload the current page.
- **刷新** — 重新加载当前页面。
- **Smart address bar** — Type URLs or search terms; auto-completes with `https://` for domains, redirects to Google Search for keywords.
- **智能地址栏** — 输入 URL 或搜索词，域名自动补全 `https://`，关键词自动跳转 Google 搜索。

### Tab Management / 标签页管理

- **Multiple tabs** — Up to 20 tabs, each isolated with its own navigation history and zoom level.
- **多标签页** — 最多 20 个标签页，每个标签页拥有独立的导航历史和缩放级别。
- **Chrome-style tab strip** — Custom rendered tabs with active/inactive/hover states, concave bottom for inactive tabs.
- **Chrome 风格标签栏** — 自定义绘制标签，支持活跃/非活跃/悬停状态，非活跃标签底部内凹。
- **Right-click context menu** — Close, Close Others, Close All.
- **右键上下文菜单** — 关闭、关闭其他、关闭所有。
- **New tab button** — Click "+" to create a new tab; optionally opens your homepage automatically.
- **新建标签页** — 点击 "+" 创建新标签页，可选自动打开主页。

### Bookmarks / 书签

- **Add Bookmark** — Bookmark the current page with title and URL.
- **添加书签** — 将当前页面添加到书签，包含标题和网址。
- **Edit / Delete Bookmark** — Edit bookmark name and URL, or delete with confirmation.
- **编辑/删除书签** — 编辑书签名称和网址，或确认删除。
- **Duplicate detection** — Prevents adding the same URL twice.
- **重复检测** — 防止添加相同的 URL。
- **Star icon in address bar** — One-click toggle to add or remove bookmarks.
- **地址栏星标** — 一键切换添加或移除书签。

### Browsing History / 浏览历史

- **Automatic recording** — Pages you visit are automatically recorded after loading completes.
- **自动记录** — 访问过的页面在加载完成后自动记录。
- **Smart grouping** — History entries grouped by Today, Day of Week, and Older.
- **智能分组** — 历史记录按今天、各天（周一到周日）、以前分组显示。
- **Configurable retention** — Set maximum days and maximum number of entries.
- **可配置保留策略** — 设置最大保存天数和最大记录条数。
- **Clear options** — Clear last hour, last 24 hours, or all history.
- **清除选项** — 清除最近 1 小时、24 小时或全部历史。

### Developer Tools / 开发者工具

- **Embedded DevTools** — Chrome DevTools panel embedded directly below the current page (CDP-based).
- **嵌入式 DevTools** — Chrome DevTools 面板直接嵌入当前页面下方（基于 CDP）。
- **Separate window mode** — Option to open DevTools in a popup window.
- **独立窗口模式** — 可选择在新弹出窗口中打开 DevTools。
- **Auto-detects JCEF debugging port** — Scans known JCEF cache directories for the active DevTools port.
- **自动检测 JCEF 调试端口** — 扫描已知 JCEF 缓存目录查找 DevTools 活跃端口。

### Zoom / 缩放

- **Zoom In / Zoom Out** — Adjust zoom level by 5% increments.
- **放大 / 缩小** — 以 5% 步进调整缩放级别。
- **Toast notification** — Shows current zoom percentage for 1 second then auto-hides.
- **缩放提示** — 显示当前缩放百分比，1 秒后自动消失。

### Bookmark Sidebar / 书签侧边栏

- **Dual-tab sidebar** — Segmented toggle between Bookmarks and History (Element-Plus style).
- **双标签侧边栏** — Element Plus 风格分段切换器，在书签和历史之间切换。
- **Bookmark operations** — Click to navigate, pencil icon to edit, "x" to delete.
- **书签操作** — 点击导航，编辑图标修改，"x" 删除。
- **History operations** — Click to navigate, "x" to remove a single entry, "Clear" link for batch operations.
- **历史操作** — 点击导航，"x" 删除单条，"清空"链接批量操作。
- **Collapsible** — Toggle visibility via the toolbar button.
- **可折叠** — 通过工具栏按钮显示或隐藏。

### Auto-Refresh / 定时刷新

- **Configurable interval** — Automatically refresh a page at a user-specified interval (in seconds).
- **可配置间隔** — 按用户指定的间隔（秒）自动刷新页面。
- **Confirmation dialogs** — Confirms before enabling and asks before stopping.
- **确认对话框** — 开启时确认，停止时再次确认。

### Cache Management / 缓存管理

- **Clear Cache** — Clears `localStorage`, `sessionStorage`, `caches` and cookies via JavaScript, then reloads the page.
- **清空缓存** — 通过 JavaScript 清除 `localStorage`、`sessionStorage`、`caches` 和 Cookie，然后重新加载页面。

### Open in System Browser / 系统浏览器打开

- **One-click** — Opens the current page URL in your operating system's default browser.
- **一键打开** — 在操作系统默认浏览器中打开当前页面 URL。

### Internationalization / 多语言支持

- **7 languages supported** — Simplified Chinese, English, Japanese, Korean, French, German, plus IDE default.
- **支持 7 种语言** — 简体中文、英语、日语、韩语、法语、德语，以及跟随 IDE 默认。
- **Instant switching** — Language takes effect immediately; no IDE restart required.
- **即时切换** — 切换语言后立即生效，无需重启 IDE。

### Settings / 设置

- **Customizable homepage** — Set any URL as your homepage.
- **可定制主页** — 设置任意 URL 作为主页。
- **New tab behavior** — Choose whether to open the homepage on new tabs.
- **新标签页行为** — 选择新标签页是否打开主页。
- **History retention** — Configure max days and max entries.
- **历史保留策略** — 配置最大天数和最大条目数。
- **DevTools mode** — Choose embedded or separate window.
- **DevTools 模式** — 选择嵌入或独立窗口。
- **Display position** — Choose tool window or editor tab.
- **显示位置** — 选择工具栏或编辑区。
- **Language** — Switch UI language on the fly.
- **语言** — 随时切换界面语言。

### Keyboard Shortcuts / 键盘快捷键

---

## Installation / 安装

### From JetBrains Marketplace / 从 Marketplace 安装

Search for **WebBrowser** in **Settings/Preferences → Plugins** and install it.
在 **设置/偏好设置 → 插件** 中搜索 **WebBrowser** 并安装。

Restart the IDE if prompted.
如果提示，请重启 IDE。

### Build from source / 从源码构建

```bash
git clone https://github.com/hccpw007/JetBrains-WebBrowser-plugin.git
cd WebBrowser
./gradlew build
```

The built plugin JAR will be at `build/libs/WebBrowser-*.jar`.
构建产物位于 `build/libs/WebBrowser-*.jar`。

---

## Development / 开发

```bash
# Run IDE with the plugin / 启动 IDE 并加载插件
./gradlew runIde

# Run tests / 运行测试
./gradlew test

# Build / 构建
./gradlew build

# Verify plugin compatibility / 验证插件兼容性
./gradlew verifyPlugin

# Publish to JetBrains Marketplace / 发布到 Marketplace
./gradlew publishPlugin
```

### Project Structure / 项目结构

```
src/main/java/com/cpw/browser/
├── action/                    # Action classes (navigation, bookmarks, panel)
├── bookmark/                  # Bookmark data model and persistence
├── editor/                    # Editor tab integration
├── history/                   # Browsing history data model and persistence
├── settings/                  # Settings page and state persistence
├── toolwindow/                # Tool window, tab manager, embedded DevTools
├── ui/                        # UI components (AddressBar, ChromeTab, etc.)
├── util/                      # Utilities (i18n, URL normalization)
├── WebBrowserIcons.java       # Icon constants
├── JcefArgsProvider.java      # JCEF startup arguments
├── MyMessageBundle.java       # i18n utility
└── PluginFirstRunActivity.java # First-run onboarding dialog
```

---

## Tech Stack / 技术栈

- **Java** (pure Java, no Kotlin dependencies) / 纯 Java，无 Kotlin 依赖
- **IntelliJ Platform Plugin SDK** (Gradle)
- **JCEF** (Java Chromium Embedded Framework)

---

## License / 许可证

This project is open source under the MIT License.
本项目基于 MIT 许可证开源。
