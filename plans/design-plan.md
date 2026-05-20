# WebBrowser 插件详细设计方案

## Context

这是一个 IntelliJ IDEA 浏览器插件项目，目标是在 IDE 中嵌入一个类 VS Code Simple Browser 的功能完整的浏览器。当前项目只有一个最小化的工具窗口实现（随机数按钮）。需要将其改造为具备地址栏、导航、书签、多标签页和开发者工具的完整浏览器插件。

**技术基础**：利用 IntelliJ IDEA 2026.1.2 内置的 JCEF（Java Chromium Embedded Framework），通过 `com.intellij.ui.jcef.JBCefBrowser` 实现浏览器内核嵌入，无需额外依赖。

---

## 1. 整体架构

### 模块划分

```
com.cpw.browser/
  BrowserToolWindowFactory.kt     # 工具窗口工厂（替代 MyToolWindowFactory）
  MyMessageBundle.kt              # i18n 消息包（扩展）
  toolwindow/
    BrowserToolWindowPanel.kt     # 顶层主面板（地址栏 + 工具栏 + 标签页 + 书签侧栏）
    BrowserTabPanel.kt            # 单个标签页（封装 JBCefBrowser）
  browser/
    BrowserTabManager.kt          # 标签页集合管理（创建/关闭/切换）
  ui/
    AddressBar.kt                 # 地址栏输入组件
    BrowserToolbar.kt             # 导航工具栏
    BookmarkSidebar.kt            # 书签侧边栏面板
  bookmark/
    Bookmark.kt                   # 书签数据类
    BookmarkPersistentState.kt    # 书签持久化（PersistentStateComponent）
  action/
    GoBackAction.kt               # 后退
    GoForwardAction.kt            # 前进
    RefreshAction.kt              # 刷新
    OpenDevToolsAction.kt         # 打开开发者工具
    AddBookmarkAction.kt          # 添加书签
    RemoveBookmarkAction.kt       # 删除书签
    NavigateToAction.kt           # 地址栏导航
    NewTabAction.kt               # 新建标签页
    CloseTabAction.kt             # 关闭标签页
```

### 依赖关系

```
BrowserToolWindowFactory
  └── BrowserToolWindowPanel（顶层容器）
        ├── BrowserToolbar（ActionToolbar）
        │     └── 各 AnAction → 通过 tabManager 操作当前标签页
        ├── AddressBar（JBTextField + 导航）
        ├── BookmarkSidebar（JBList + 书签 CRUD）
        │     └── BookmarkPersistentState（持久化）
        └── BrowserTabManager（标签页生命周期）
              └── BrowserTabPanel × N
                    └── JBCefBrowser（CEF 浏览器内核，每个标签页 1 个）
```

---

## 2. 核心类设计

### 2.1 BrowserToolWindowFactory

- **路径**: `src/main/kotlin/com/cpw/browser/BrowserToolWindowFactory.kt`
- **继承**: `ToolWindowFactory`
- **职责**: 创建 `BrowserToolWindowPanel`，注册为工具窗口内容。在 `disposeToolWindowContent` 中释放所有 JCEF 资源。

```kotlin
class BrowserToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project): Boolean = true
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow)
    // 存储 panel 引用以便在 dispose 时释放
}
```

### 2.2 BrowserToolWindowPanel

- **路径**: `src/main/kotlin/com/cpw/browser/toolwindow/BrowserToolWindowPanel.kt`
- **类型**: `JBPanel<JBPanel<*>>`（顶层 Swing 容器）
- **核心字段**:
  - `tabManager: BrowserTabManager` — 标签页管理器
  - `addressBar: AddressBar` — 地址栏
  - `toolbar: BrowserToolbar` — 工具栏
  - `bookmarkSidebar: BookmarkSidebar` — 书签侧栏
  - `tabPane: JBTabbedPane` — 标签页容器

- **布局（从上到下）**：
```
┌──────────────────────────────────────────────────────┐
│ [←] [→] [↻] [DevTools] [☆+] [NewTab] [CloseTab]    │  <- ActionToolbar
├──────────────────────────────────────────────────────┤
│ [https://example.com                        ] [Go]   │  <- 地址栏
├────────────────────┬─────────────────────────────────┤
│ 书签侧栏           │  JBTabbedPane                   │
│ ┌────────────────┐ │  ┌─────────────────────────────┐│
│ │ GitHub         │ │  │                             ││
│ │ Google         │ │  │   JBCefBrowser 内容区        ││
│ │ StackOverflow  │ │  │                             ││
│ └────────────────┘ │  └─────────────────────────────┘│
├────────────────────┴─────────────────────────────────┤
│ 状态栏：Loading... / Done                             │
└──────────────────────────────────────────────────────┘
```

- **URL 规范化逻辑**：
```kotlin
private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) return trimmed
    if (trimmed.contains('.') && !trimmed.contains(' ')) return "https://$trimmed"
    return "https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}"
}
```

### 2.3 BrowserTabPanel

- **路径**: `src/main/kotlin/com/cpw/browser/toolwindow/BrowserTabPanel.kt`
- **职责**: 封装单个 `JBCefBrowser` 实例，管理导航历史
- **核心字段**:
  - `browser: JBCefBrowser` — 浏览器内核实例
  - `component: JComponent` — `browser.component`（AWT 重量级组件）
  - `navigationHistory: ArrayDeque<String>` — URL 历史栈
  - `currentHistoryIndex: Int` — 当前历史位置
  - `currentUrl: String` — 当前 URL
  - `pageTitle: String` — 页面标题（用于标签页名称）
  - `isLoading: Boolean` — 加载状态

- **核心方法**:
  - `navigate(url: String)` — 加载 URL，推入历史栈
  - `goBack()` / `goForward()` — 调用 `browser.goBack()` / `browser.goForward()`
  - `refresh()` — 重新加载当前页
  - `canGoBack(): Boolean` — `currentHistoryIndex > 0`
  - `canGoForward(): Boolean` — `currentHistoryIndex < navigationHistory.size - 1`
  - `openDevTools()` — `browser.openDevtools()`
  - `dispose()` — 在 EDT 上调用 `browser.dispose()`

### 2.4 BrowserTabManager

- **路径**: `src/main/kotlin/com/cpw/browser/browser/BrowserTabManager.kt`
- **职责**: 管理标签页集合和当前活动标签页
- **核心字段**:
  - `tabs: MutableList<BrowserTabPanel>` — 所有标签页
  - `activeTabIndex: Int` — 当前活动标签页索引
  - `activeTab: BrowserTabPanel?` — 当前活动标签页
  - `onTabChange: ((BrowserTabPanel?) -> Unit)?` — 标签切换回调

- **核心方法**:
  - `createTab(initialUrl: String): BrowserTabPanel` — 创建新标签页
  - `closeTab(index: Int): Boolean` — 关闭指定标签页（释放 JCEF 资源）
  - `switchToTab(index: Int)` — 切换到指定标签页

### 2.5 AddressBar

- **路径**: `src/main/kotlin/com/cpw/browser/ui/AddressBar.kt`
- **组件**: 水平排列的 `JBTextField` + "前往" `JButton`
- **交互**: Enter 键或点击按钮触发 `onNavigate(url)` 回调
- **方法**: `setUrl(url)`, `getUrl(): String`, `requestFocus()`

### 2.6 BrowserToolbar

- **路径**: `src/main/kotlin/com/cpw/browser/ui/BrowserToolbar.kt`
- **实现**: 使用 `ActionManager.createActionToolbar()` + `DefaultActionGroup` 创建 IntelliJ 原生工具栏
- **按钮列表**: 后退、前进、分隔线、刷新、分隔线、DevTools、添加书签、分隔线、新建标签页、关闭标签页
- **按钮启用/禁用**: 通过 `AnAction.update()` 根据 `tabManager.activeTab` 状态动态控制

### 2.7 BookmarkSidebar

- **路径**: `src/main/kotlin/com/cpw/browser/ui/BookmarkSidebar.kt`
- **组件**: `JBList<Bookmark>` + `CollectionListModel`，可折叠
- **交互**: 单击跳转到书签 URL，右键删除书签
- **数据源**: `BookmarkPersistentState.getInstance().getBookmarks()`

### 2.8 Bookmark（数据类）

```kotlin
data class Bookmark(
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2.9 BookmarkPersistentState

- **路径**: `src/main/kotlin/com/cpw/browser/bookmark/BookmarkPersistentState.kt`
- **实现**: `PersistentStateComponent<BookmarkPersistentState.State>`
- **存储**: application 级别，自动持久化到 `<IDE config>/options/WebBrowser.xml`
- **方法**: `addBookmark()`, `removeBookmark()`, `getBookmarks()`, `contains()`
- **获取实例**: `ApplicationManager.getApplication().getService(BookmarkPersistentState::class.java)`

---

## 3. JCEF 关键技术要点

### 3.1 JBCefBrowser 创建

```kotlin
import com.intellij.ui.jcef.JBCefBrowserBuilder

val browser = JBCefBrowserBuilder()
    .setUrl("about:blank")
    .build()
val browserComponent: JComponent = browser.component
```

### 3.2 加载状态监听

```kotlin
browser.jbCefClient.addLoadHandler(object : JBCefLoadHandler {
    override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
        if (frame.isMain) {
            // 更新地址栏、页面标题、推送历史记录
        }
    }
    override fun onLoadingStateChange(browser: CefBrowser, isLoading: Boolean, ...) {
        // 更新加载指示器
    }
    override fun onLoadError(browser: CefBrowser, frame: CefFrame, errorCode: ..., errorText: String, failedUrl: String) {
        // 显示错误页面
    }
}, browser.cefBrowser)
```

### 3.3 生命周期管理

- **创建**: 每个标签页创建时实例化一个 `JBCefBrowser`
- **销毁**: `browser.dispose()` **必须在 EDT 上调用**，否则会导致死锁
- **工具窗口关闭**: 在 `disposeToolWindowContent()` 中释放所有浏览器实例
- **测试模式**: `ApplicationManager.getApplication().isUnitTestMode` 为 true 时跳过 JCEF 创建

### 3.4 DevTools

```kotlin
browser.openDevtools() // 打开独立的 Chrome DevTools 窗口
```

### 3.5 注意事项

- JCEF 初始化是异步的，不要立即调用 `loadURL()`
- 浏览器组件是 AWT 重量级组件，不要将轻量级组件覆盖在其上方
- JCEF 在 IDEA 2026.1.2 中默认可用，无需额外配置

---

## 4. plugin.xml 变更

```xml
<idea-plugin>
    <id>com.cpw.browser.WebBrowser</id>
    <name>WebBrowser</name>
    <vendor url="https://www.yourcompany.com">YourCompany</vendor>

    <description><![CDATA[
        A Simple Browser for IntelliJ IDEA, inspired by VS Code Simple Browser.
        Embed web pages directly in the IDE with navigation, bookmarks, and DevTools support.
    ]]></description>

    <depends>com.intellij.modules.platform</depends>

    <resource-bundle>messages.MyMessageBundle</resource-bundle>

    <extensions defaultExtensionNs="com.intellij">
        <toolWindow
            id="WebBrowser"
            factoryClass="com.cpw.browser.BrowserToolWindowFactory"
            icon="AllIcons.Toolwindows.ToolWindowPalette"
            anchor="bottom"
            secondary="false"/>

        <applicationService
            serviceImplementation="com.cpw.browser.bookmark.BookmarkPersistentState"/>
    </extensions>

    <actions>
        <action id="WebBrowser.FocusAddressBar"
                class="com.cpw.browser.action.NavigateToAction"
                text="Focus Address Bar">
            <keyboard-shortcut keymap="$default" first-keystroke="control L"/>
        </action>
        <action id="WebBrowser.OpenDevTools"
                class="com.cpw.browser.action.OpenDevToolsAction"
                text="Open DevTools">
            <keyboard-shortcut keymap="$default" first-keystroke="control shift I"/>
        </action>
    </actions>
</idea-plugin>
```

**关键变更**：
- 工具窗口 ID: `"MyToolWindow"` → `"WebBrowser"`
- 工厂类: `MyToolWindowFactory` → `BrowserToolWindowFactory`
- 依赖: `com.intellij.modules.lsp` → `com.intellij.modules.platform`（JCEF 不需要 LSP）
- 新增 `<applicationService>` 用于书签持久化
- 新增 `<actions>` 键盘快捷键

---

## 5. Gradle 依赖

`build.gradle.kts` 无需额外添加依赖。`JBCefBrowser` 及其相关类属于 IntelliJ Platform 核心（`com.intellij.ui.jcef` 包），平台依赖已自动包含。

---

## 6. 实施步骤

### Phase 1 — 核心浏览器（预估 1-2 天）

1. 重命名 `MyToolWindowFactory.kt` → `BrowserToolWindowFactory.kt`
2. 创建 `BrowserToolWindowPanel`，直接嵌入单个 `JBCefBrowser`
3. 创建 `AddressBar`（JBTextField + Enter 导航）
4. 更新 `plugin.xml`，验证浏览器能正常加载网页
5. 确认 JCEF 在开发环境中正常工作

### Phase 2 — 导航功能（预估 1 天）

6. 创建 `BrowserToolbar` + `GoBackAction`, `GoForwardAction`, `RefreshAction`
7. 在 `BrowserTabPanel` 中实现导航历史追踪（`ArrayDeque<String>` + index）
8. 通过 `JBCefLoadHandler` 回调同步 URL 和标题
9. 工具栏按钮启用/禁用状态联动

### Phase 3 — 书签功能（预估 1 天）

10. 创建 `Bookmark` 数据类和 `BookmarkPersistentState`
11. 创建 `BookmarkSidebar`（JBList + 可折叠面板）
12. 创建 `AddBookmarkAction` / `RemoveBookmarkAction`
13. 书签点击触发导航，侧栏可通过工具栏按钮切换显隐

### Phase 4 — 多标签页（预估 1 天）

14. 创建 `BrowserTabManager` 和 `BrowserTabPanel` 类
15. 在布局中集成 `JBTabbedPane`
16. 创建 `NewTabAction` / `CloseTabAction`
17. 标签切换时同步地址栏和工具栏状态

### Phase 5 — DevTools 和收尾（预估 1 天）

18. 实现 `OpenDevToolsAction`
19. 添加状态栏（加载状态、悬停链接 URL）
20. 注册键盘快捷键（Ctrl+L 聚焦地址栏，Ctrl+Shift+I 打开 DevTools）
21. 扩展 `MyMessageBundle.properties` i18n key
22. 处理边界情况：JCEF 不可用降级、空 URL 处理、资源释放

---

## 7. 文件变更清单

| 操作 | 文件 |
|------|------|
| 重命名/修改 | `MyToolWindowFactory.kt` → `BrowserToolWindowFactory.kt` |
| 修改 | `MyMessageBundle.kt` — 扩展 i18n key |
| 修改 | `MyMessageBundle.properties` — 添加翻译 |
| 修改 | `plugin.xml` — 更新工具窗口 ID、添加 service 和 actions |
| **新建** | `toolwindow/BrowserToolWindowPanel.kt` |
| **新建** | `toolwindow/BrowserTabPanel.kt` |
| **新建** | `browser/BrowserTabManager.kt` |
| **新建** | `ui/AddressBar.kt` |
| **新建** | `ui/BrowserToolbar.kt` |
| **新建** | `ui/BookmarkSidebar.kt` |
| **新建** | `bookmark/Bookmark.kt` |
| **新建** | `bookmark/BookmarkPersistentState.kt` |
| **新建** | `action/GoBackAction.kt` |
| **新建** | `action/GoForwardAction.kt` |
| **新建** | `action/RefreshAction.kt` |
| **新建** | `action/OpenDevToolsAction.kt` |
| **新建** | `action/AddBookmarkAction.kt` |
| **新建** | `action/RemoveBookmarkAction.kt` |
| **新建** | `action/NavigateToAction.kt` |
| **新建** | `action/NewTabAction.kt` |
| **新建** | `action/CloseTabAction.kt` |

**合计: 2 个文件修改, 1 个文件重命名, 16 个新文件创建**

---

## 8. 验证方案

### 构建验证
```bash
./gradlew build        # 编译通过，无错误
./gradlew test         # 测试通过
./gradlew verifyPlugin # 插件兼容性验证通过
```

### 功能验证（通过 `./gradlew runIde` 启动开发环境 IDE）
1. 打开 WebBrowser 工具窗口
2. 在地址栏输入 `https://example.com`，确认页面正确渲染
3. 点击页面内链接，确认地址栏 URL 同步更新
4. 点击"添加书签"，确认书签出现在侧栏
5. 点击书签，确认跳转到对应页面
6. 点击后退/前进/刷新按钮，确认功能正常
7. 新建标签页，在不同标签页间切换
8. 点击 DevTools 按钮，确认 Chrome DevTools 窗口打开
9. 关闭工具窗口再重新打开，确认之前的状态恢复
10. 重启 IDE，确认书签数据持久化保留
