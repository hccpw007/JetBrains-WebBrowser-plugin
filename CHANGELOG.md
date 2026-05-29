<!-- Keep a Changelog guide -> https://keepachangelog.com -->

## [1.0.2] - 2026-05-29

### Fixed

- Fix bookmark and history delete icons not clickable, increase right padding to avoid scrollbar covering.
  <br>修复书签和历史记录删除图标点击不到的问题，增加右侧内边距避免被滚动条遮挡。

## [1.0.1] - 2026-05-25

### Added

- URL auto-completion: show history and bookmark suggestions while typing in the address bar, navigate via arrow keys + Enter or mouse click.
<br>URL 自动提示：在地址栏输入时根据浏览历史和书签提供建议，支持上下键 + 回车或鼠标点击导航。
- Search bar displays first suggestion as the current input content, auto-selects for quick Enter navigation.
<br>提示框第一行固定为当前输入内容，默认选中方便回车快速导航。

### Fixed

- Keep all tabs when closing and reopening the browser in editor mode.
<br>编辑器模式下关闭再打开浏览器插件时保留所有标签页
- Fix toggle button not working after IDE restart / session restore.
<br>重启 IDE 恢复会话后浏览器切换按钮无效的问题
- Auto-hide sidebar button in editor mode on IDE startup.
<br>工具窗口侧边栏按钮在编辑器模式下启动时自动隐藏
- Fix GitHub URL and README `cd` directory name description errors.
<br>修复 README 中 GitHub URL 和 cd 目录名描述错误

### Changed

- Update plugin icon to globe style design.
<br>更新插件图标为地球样式
- Rename system browser icon from `google` to `system-browser`.
<br>将系统浏览器图标由 google 改名为 system-browser
- Bump target IntelliJ platform to 2025.3.
<br>更新目标 IntelliJ 平台版本到 2025.3

### Removed

- Remove first-run restart prompt (`PluginFirstRunActivity`), as IDE handles restart natively after plugin install.
<br>移除首次运行重启提示，IDE 在插件安装后会自动处理

## [1.0.0] - 2026-05-22

### Added

#### Core Browser Engine
- JCEF (Java Chromium Embedded Framework) powered browser engine with CDP (Chrome DevTools Protocol) support. 基于 JCEF 的内嵌浏览器引擎，支持 CDP 协议
- Remote debugging on auto-assigned port (`--remote-debugging-port=0`). 自动分配端口的远程调试支持
- Custom JCEF startup arguments for secure WebSocket connections. 自定义 JCEF 启动参数，确保 WebSocket 连接安全

#### Display Modes
- Tool window mode: browser docks as a sidebar tool window (left anchor), toggleable via `Alt+Shift+W`. 工具窗口模式：浏览器停靠在左侧，通过 `Alt+Shift+W` 切换
- Editor tab mode: browser opens as a file editor tab in the main editor area using `.webbrowser` temp file. 编辑器标签页模式：浏览器在主编辑区以 `.webbrowser` 临时文件形式打开
- Mode preference configurable in settings (toolbar / editor). 模式偏好可在设置中配置（工具栏 / 编辑器）

#### Navigation Actions
- GoBack: navigate backward in session history (disabled at oldest entry). 后退：在会话历史中向后导航
- GoForward: navigate forward in session history (disabled at newest entry). 前进：在会话历史中向前导航
- GoHome: navigate to configurable homepage URL. 主页：导航到可配置的主页 URL
- Refresh: reload current page. 刷新：重新加载当前页面
- NewTab: create a new browser tab. 新标签页：创建新的浏览器标签页
- OpenDevTools: open Chrome DevTools for the current page. 开发者工具：打开当前页面的 Chrome DevTools

#### Panel Actions
- ZoomIn: increase zoom by 5% with toast notification. 放大：缩放增加 5%，显示 Toast 提示
- ZoomOut: decrease zoom by 5% with toast notification. 缩小：缩放减少 5%，显示 Toast 提示
- ZoomReset: reset zoom to 100% with toast notification. 重置缩放：恢复缩放到 100%，显示 Toast 提示
- ToggleBookmarkSidebar: show/hide bookmark and history sidebar. 书签侧边栏：显示/隐藏书签和历史记录侧边栏
- OpenInSystemBrowser: open current URL in the OS default browser. 系统浏览器：在操作系统默认浏览器中打开当前 URL
- AutoRefresh: toggle auto-refresh with user-specified interval (in seconds). 自动刷新：按用户指定的间隔（秒）自动刷新页面
- ClearCache: clear localStorage, sessionStorage, caches API, and cookies via JavaScript execution, then reload. 清除缓存：通过 JavaScript 清除 localStorage、sessionStorage、caches API 和 cookies 后重新加载
- Settings: open WebBrowser settings dialog. 设置：打开 WebBrowser 设置对话框
- MoreMenu: popup menu containing Settings, DevTools, AutoRefresh, ClearCache actions. 更多菜单：包含设置、开发者工具、自动刷新、清除缓存的弹出菜单

#### Bookmark Management
- Add bookmark for current page URL, with duplicate detection warning. 添加书签：为当前页面 URL 添加书签，支持重复检测
- Remove bookmark from sidebar. 删除书签：从侧边栏删除书签
- Inline edit/delete in bookmark list (edit dialog with Name/URL fields). 内联编辑/删除：书签列表中直接编辑（名称/URL 对话框）或删除
- Star toggle in address bar for quick bookmark add/remove. 地址栏星标：快速添加/删除书签
- Persistent storage via `BookmarkPersistentState` with XML serialization. 持久化存储：通过 `BookmarkPersistentState` 进行 XML 序列化
- Bookmark data model: title, URL, creation timestamp. 书签数据模型：标题、URL、创建时间戳

#### Browsing History
- Automatic history recording with same-day same-URL deduplication. 自动记录浏览历史，同日内同 URL 自动去重
- Smart trimming by max days, max count, and 10,000-entry hard cap. 智能裁剪：按最大天数、最大条数和 10000 条硬上限自动清理
- History sidebar with grouped view: Today, Monday-Sunday (this week), Older. 历史记录侧边栏：按今天、本周（周一至周日）、更早分组展示
- Contextual timestamp formatting (HH:mm for today, "Mon HH:mm" for this week, "MM-dd HH:mm" for older). 上下文时间戳格式化
- Quick delete single history entry, clear by time range (Last hour / 24 hours / All). 单条删除或按时间范围清除（最近 1 小时 / 24 小时 / 全部）
- Persistent storage via `BrowsingHistoryState` with XML serialization. 持久化存储：通过 `BrowsingHistoryState` 进行 XML 序列化

#### Address Bar
- URL input field with JBTextField, custom rounded border (focused/unfocused). URL 输入框：JBTextField，自定义圆角边框（聚焦/非聚焦状态）
- Smart URL display: `about:blank` shown as empty string. 智能 URL 显示：`about:blank` 显示为空字符串
- Enter-to-navigate with KeyAdapter listening for VK_ENTER. 回车导航：KeyAdapter 监听回车键
- Star bookmark toggle layered inside the text field. 书签星标：嵌入在输入框内的书签切换按钮
- Focus-on-demand via `requestFocusOnField()` for `Ctrl+L` shortcut. 聚焦支持：通过 `requestFocusOnField()` 配合 `Ctrl+L` 快捷键

#### Multi-Tab Management
- Create, close, and switch between up to 20 browser tabs. 创建、关闭和切换最多 20 个浏览器标签页
- Auto-create fallback: when last tab is closed, creates a new blank tab. 自动创建兜底：关闭最后一个标签页时自动创建新空白页
- Smart active tab index adjustment on close/switch. 智能活跃标签索引调整：关闭/切换时自动修正索引
- New tab behavior: optionally auto-navigate to homepage. 新标签页行为：可选自动导航到主页
- JavaScript popup interceptor redirects popups into new tabs. JavaScript 弹窗拦截：将弹窗重定向到新标签页
- Zoom delegation: zoomIn/Out/Reset on active tab via CSS `document.body.style.zoom`. 缩放委派：通过 CSS `document.body.style.zoom` 控制当前标签页缩放

#### Chrome-Style Tab UI
- Custom painted tab strip with blue active/inactive state colors. 自定义绘制的标签条，蓝色高亮当前标签
- Chrome-style rounded top corners (CR=10) with concave bottom indentation (BC=4). Chrome 风格圆角顶部和凹形底部
- Close button ("x") per tab with tooltip. 每个标签页的关闭按钮及工具提示
- Right-click context menu: Close, Close Others, Close All. 右键上下文菜单：关闭、关闭其他、关闭全部
- Size constraints: min 70px, max 240px, 26px height. 尺寸约束：最小 70px，最大 240px，高度 26px
- Title truncation: 20 chars with "..." via `getTabTitle()`. 标题截断：20 字符 + "..."

#### Bookmark & History Sidebar
- Dual-tab segmented toggle: Element-Plus style with "Bookmarks" and "History" tabs. 双标签分段切换器：Element Plus 风格的书签和历史标签
- JBList with CollectionListModel for bookmarks, custom renderer with edit/delete buttons. 书签列表：JBList + CollectionListModel，自定义渲染器含编辑/删除按钮
- JBList with HistoryEntriesModel for history, custom renderer with date-grouped headers. 历史记录列表：JBList + HistoryEntriesModel，自定义渲染器含日期分组标题
- CardLayout switching between bookmark and history panels. CardLayout 切换书签和历史面板
- Click-to-navigate on entries, inline delete buttons. 点击条目导航，内联删除按钮
- Bookmark edit dialog with Name/URL fields via JOptionPane. 书签编辑对话框：名称/URL 字段

#### DevTools Integration
- CDP-based DevTools connecting to JCEF remote debugging port. 基于 CDP 协议的开发者工具，连接 JCEF 远程调试端口
- Dual port discovery: API-based (`getRemoteDebuggingPort()`) and filesystem-based (`DevToolsActivePort` file). 双端口发现策略：API 和文件系统
- Local DevTools frontend: loads `http://127.0.0.1:{port}/devtools/inspector.html` (avoids CDN). 本地 DevTools 前端：从本地端口加载（避免 CDN 依赖）
- Screencast mode auto-fix: injects JavaScript to disable screencast on load. Screencast 模式自动修复：注入 JavaScript 在加载时关闭 screencast
- Embedded mode: splits browser content panel vertically with JSplitPane (70/30 weight). 嵌入模式：通过 JSplitPane 垂直分割浏览器内容面板（70/30 比例）
- Separate window mode option in settings. 独立窗口模式选项

#### Zoom System
- CSS `document.body.style.zoom` JavaScript approach (avoids JCEF HostZoomMap cross-tab interference). 基于 CSS zoom 的缩放方案（避免 JCEF HostZoomMap 跨标签干扰）
- 5% zoom step increment/decrement. 5% 缩放步进
- Visual toast notification with zoom percentage, auto-hides after 1 second. 缩放百分比 Toast 提示，1 秒后自动隐藏
- Zoom persists across page refreshes (reapplied on load complete). 缩放跨刷新保持（加载完成后重新应用）
- Zoom reset to 100% with toast "Zoom reset to 100%". 重置到 100%，提示"缩放已重置"

#### Settings / Configurable Page
- Language selector: Default (IDE), 简体中文, English, 日本語, 한국어, Français, Deutsch — instant switch, no restart needed. 语言选择器：即时切换，无需重启
- Homepage URL configuration with free-text field. 主页 URL 配置：自由文本输入
- "Open homepage on new tab" checkbox toggle. "新标签页打开主页"复选框
- Default search engine: Google, Bing, DuckDuckGo, Baidu. 默认搜索引擎：Google、Bing、DuckDuckGo、Baidu
- History retention: max days and max entries fields. 历史记录保留：最大天数和最大条数字段
- DevTools mode: embedded (below page) or separate window. 开发者工具模式：嵌入当前页面或独立窗口
- Display position: toolbar (tool window) or editor area (editor tab). 显示位置：工具栏（工具窗口）或编辑区（编辑器标签页）
- Developer signature: "Developer: Pengwei Chen" in bottom-right corner. 开发者签名

#### Internationalization (i18n)
- 7 languages supported: English, Simplified Chinese, Japanese, Korean, French, German, plus IDE default. 支持 7 种语言
- ~120 message keys covering all UI labels, action texts, tooltips, dialogs. 约 120 个消息键覆盖所有 UI
- Live language switching: `TranslationUtil` notifies all components on language change. 即时语言切换：所有组件即时刷新
- `MessageFormat` parameter substitution support (e.g., `{0}` for zoom percentage). MessageFormat 参数替换支持

#### URL Processing
- Protocol auto-completion: recognizes valid protocol URLs, adds `https://` for domain-like inputs. 协议自动补全：识别有效协议 URL，为域名类输入自动添加 `https://`
- Search engine fallback: non-URL inputs become search queries via configured engine. 搜索引擎兜底：非 URL 输入转为搜索引擎查询
- URL encoding for search query parameters. 搜索查询参数的 URL 编码

#### Editor Integration
- Custom `.webbrowser` file type with dedicated icon. 自定义 `.webbrowser` 文件类型及专属图标
- FileEditorProvider (`webbrowser-editor`) with `HIDE_DEFAULT_EDITOR` policy. 文件编辑器提供者，隐藏默认编辑器
- Editor tab title truncation to 15 chars + "..." via `EditorTabTitleProvider`. 编辑器标签页标题截断 15 字符 + "..."
- Title sync from browser page to VirtualFile user data. 标题从浏览器页面同步到 VirtualFile 用户数据

#### Keyboard Shortcuts
- `Alt+Shift+W`: Toggle browser open/close. 切换浏览器打开/关闭
- `Ctrl+L`: Focus address bar and select all text. 聚焦地址栏并全选文本
- `Ctrl+Shift+I`: Open/close DevTools. 打开/关闭开发者工具

#### First-Run Experience
- First run detection via `PropertiesComponent`. 首次运行检测
- Dialog: "WebBrowser plugin installed. IDE restart required. Restart now?" 弹窗提示："插件已安装，需要重启 IDE"
- "Restart Now" and "Later" buttons with `Application.restart()` support. "立即重启"和"稍后"按钮

#### Icon Assets
- 39 SVG icons with light and dark theme variants (`xxx_dark.svg`). 39 个 SVG 图标，包含浅色和深色主题变体
- Navigation icons: back, forward, refresh, home, new-tab. 导航图标
- Zoom icons: zoom-in, zoom-out, zoom-reset. 缩放图标
- Bookmark icons: star (unfilled/filled), bookmark-add, bookmark-remove, show-bookmark. 书签图标
- DevTools icon, More menu icon, Google (system browser) icon. 开发者工具、更多菜单、系统浏览器图标
- Tool window stripe icon, plugin icon, file type icon. 工具窗口、插件、文件类型图标
- Centralized icon constants in `WebBrowserIcons.java`. 集中式图标常量管理

### Supported
- IntelliJ IDEA 2025.1 and above. 支持 IntelliJ IDEA 2025.1 及以上版本
- Java 21+ runtime. Java 21+ 运行时环境
- Windows, macOS, Linux cross-platform. 跨平台支持 Windows、macOS、Linux
