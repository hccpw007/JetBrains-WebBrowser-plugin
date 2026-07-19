# 横向书签栏（Bookmark Bar）设计文档

**日期**：2026-07-19
**状态**：待实现

## 1. 目标

在浏览器地址栏下方新增一个类似 Chrome 的横向书签栏，提供书签的快速访问入口；同时在设置页面增加"是否总是显示书签栏"选项（默认开启）。

## 2. 需求决策汇总

| 决策点 | 选择 |
|--------|------|
| 与现有左侧书签侧边栏的关系 | 并存（侧边栏保留，仍管理书签+历史） |
| 设置"总是显示书签栏"关闭后行为 | 隐藏，右侧工具栏新增切换按钮可临时显示/隐藏 |
| 书签溢出处理 | 右侧 ">>" 按钮弹出下拉菜单显示溢出书签 |
| "总是显示书签栏"默认值 | true（默认显示） |
| 切换按钮是否写回设置 | 否，仅临时改变可见性，下次打开按设置初始值 |

## 3. 架构与组件

### 3.1 新建组件 `ui/BookmarkBar.java`

继承 `JBPanel<BookmarkBar>`，`BorderLayout`：

- **CENTER**：`visiblePanel`（`BoxLayout.X_AXIS`）--放置当前可用宽度内能放下的书签按钮
- **EAST**：`overflowButton`（">>" 文字 `JButton`）--仅当存在溢出书签时可见，点击弹 `JBPopup`

**书签按钮**：`JButton`，显示书签标题，固定最大宽度（160px），超出用 "..." 截断
- 左键：调用 `onBookmarkSelected(bookmark)` 在当前标签页导航
- 右键：弹出菜单（编辑… / 删除）

**构造参数**：
```java
public BookmarkBar(Consumer<Bookmark> onBookmarkSelected,
                   Runnable onBookmarkChanged)  // 编辑/删除后触发外部刷新（侧边栏、地址栏星标）
```

### 3.2 图标资源

已生成：
- `src/main/resources/icons/bookmark-bar.svg`（浅色主题 `#272636`）
- `src/main/resources/icons/bookmark-bar_dark.svg`（深色主题 `#ffffff`）

设计意图：顶部一条横边（书签栏顶边）+ 下方三个竖条（书签项）。

`WebBrowserIcons.java` 新增常量：
```java
public static final Icon BOOKMARK_BAR = load("bookmark-bar");
```

### 3.3 布局挂载（`BrowserToolWindowPanel`）

`topSection`（`BoxLayout.Y_AXIS`）新增 `bookmarkBar` 于 `navAddressBar` 之后：
```
topSection:
  - tabStripPanel      (编辑区模式隐藏)
  - navAddressBar      (地址栏行)
  - bookmarkBar        ← 新增，贯穿顶部全宽
```

编辑区模式下 `tabStripPanel` 隐藏，但 `navAddressBar` 与 `bookmarkBar` 照常显示（依附地址栏而非 tab 栏）。

## 4. 设置项

### 4.1 `BrowserSettingsState`

`State` 内类新增字段（照现有 `editorNewTabOnClick` 模式）：
```java
private boolean alwaysShowBookmarkBar = true;
```
配套 getter/setter，外层 `BrowserSettingsState` 转发 getter/setter。

**不依赖**显示位置（toolbar/editor 模式都生效），设置页中始终可见，不挂显隐联动。

### 4.2 `BrowserSettingsPage`

- 新增 `JBCheckBox alwaysShowBookmarkBarCheckBox`，文案键 `settings.bookmark.bar.always.show`
- 位置：放在 `editorNewTabOnClickCheckBox`（Row 11）之后，作为新 Row 12；原签名 footer 顺延至 Row 13
- `isModified` / `apply` / `reset` / `refreshLabels` 照现有模式补齐
- `apply()` 中若该设置变更，调用 `BrowserSettingsChangeNotifier.notifySettingsChanged()`

## 5. 切换按钮

新增 `PanelActions.ToggleBookmarkBar`（仿 `ToggleBookmarkSidebar`）：
- 图标 `WebBrowserIcons.BOOKMARK_BAR`
- 放在 `rightGroup` 中 `ToggleBookmarkSidebar` 附近
- 行为：`bookmarkBar.setVisible(!bookmarkBar.isVisible())` + `revalidate/repaint`
- **不写回设置**（临时切换语义）
- tooltip 键 `action.toggle.bookmark.bar`

与 `ToggleBookmarkSidebar`（控制左侧栏）是两个独立按钮。

## 6. 状态同步与通知机制

### 6.1 新建 `BrowserSettingsChangeNotifier`

照搬 `TranslationUtil` 的列表监听模式，轻量实现：
```java
public final class BrowserSettingsChangeNotifier {
    public interface SettingsChangeListener { void onSettingsChanged(); }
    private static final List<SettingsChangeListener> listeners = new ArrayList<>();
    public static void addListener(SettingsChangeListener l) { listeners.add(l); }
    public static void removeListener(SettingsChangeListener l) { listeners.remove(l); }
    public static void notifySettingsChanged() { for (SettingsChangeListener l : listeners) l.onSettingsChanged(); }
}
```

### 6.2 触发路径

1. **设置变更**：`BrowserSettingsPage.apply()` -> `notifySettingsChanged()` -> `BrowserToolWindowPanel` 回调 -> `bookmarkBar.setVisible(state.isAlwaysShowBookmarkBar())` + revalidate/repaint
2. **手动切换**：`ToggleBookmarkBar` action -> `bookmarkBar.setVisible(!visible)`（不写回设置）
3. **书签增删**：`BrowserToolWindowPanel.toggleBookmark(url)` 中增删后调用 `bookmarkBar.refreshBookmarks()`（与 `bookmarkSidebar.refreshBookmarks()` 并列）

### 6.3 语言变更刷新

`TranslationUtil.addListener` 回调中新增 `bookmarkBar.refreshLabels()` 调用。

## 7. 溢出逻辑（`BookmarkBar` 内部）

- 监听 `visiblePanel` 的 `componentResized` 事件
- 计算可用宽度（`bookmarkBar.getWidth() - overflowButtonReservedWidth`）
- 逐个累计书签按钮 `getPreferredSize().width`，放不下则移入溢出列表
- 溢出列表非空：显示 ">>" 按钮，点击弹 `JBPopup`（`JBList<Bookmark>` + 渲染器，复用 `BookmarkListCellRenderer` 风格）
- 溢出列表空：隐藏 ">>" 按钮
- 溢出菜单项左键 = 导航；右键 = 编辑/删除（与主栏一致）

## 8. 右键菜单（书签按钮）

复用现有 `BookmarkSidebar.showBookmarkEditDialog` 与删除确认逻辑：
- 编辑…：弹编辑对话框，调用 `BookmarkPersistentState.updateBookmark()`
- 删除：`Messages.showYesNoDialog` 确认后 `removeBookmark()`
- 操作后：BookmarkBar 内部调用 `refreshBookmarks()` 更新自身按钮；再调用 `onBookmarkChanged.run()` 通知外部刷新侧边栏与地址栏星标

> 注：地址栏星标触发的增删走 `BrowserToolWindowPanel.toggleBookmark(url)`，已在该方法中直接刷新 `bookmarkBar`；本节仅指 BookmarkBar 右键菜单触发的编辑/删除。

## 9. 国际化键

7 个语言包文件（`MyMessageBundle*.properties`）各新增：
- `settings.bookmark.bar.always.show` -- "总是显示书签栏"
- `action.toggle.bookmark.bar` -- "显示/隐藏书签栏"
- `bookmark.bar.overflow` -- "更多书签"（">>" tooltip）

各语言翻译：
| key | zh | en | ja | ko | fr | de |
|-----|----|----|----|----|----|----|
| settings.bookmark.bar.always.show | 总是显示书签栏 | Always show bookmark bar | 常にブックマークバーを表示 | 항상 북마크 바 표시 | Toujours afficher la barre de favoris | Lesezeichenleiste immer anzeigen |
| action.toggle.bookmark.bar | 显示/隐藏书签栏 | Show/Hide bookmark bar | ブックマークバーの表示切替 | 북마크 바 표시 전환 | Afficher/Masquer la barre de favoris | Lesezeichenleiste ein-/ausblenden |
| bookmark.bar.overflow | 更多书签 | More bookmarks | 他のブックマーク | 더 많은 북마크 | Plus de favoris | Weitere Lesezeichen |

## 10. 涉及文件清单

**新增**：
- `src/main/java/com/cpw/browser/ui/BookmarkBar.java`
- `src/main/java/com/cpw/browser/util/BrowserSettingsChangeNotifier.java`
- `src/main/resources/icons/bookmark-bar.svg`（已生成）
- `src/main/resources/icons/bookmark-bar_dark.svg`（已生成）

**修改**：
- `src/main/java/com/cpw/browser/WebBrowserIcons.java`（新增 `BOOKMARK_BAR` 常量）
- `src/main/java/com/cpw/browser/settings/BrowserSettingsState.java`（新增 `alwaysShowBookmarkBar` 字段及 getter/setter）
- `src/main/java/com/cpw/browser/settings/BrowserSettingsPage.java`（新增 checkbox、行号调整、isModified/apply/reset/refreshLabels）
- `src/main/java/com/cpw/browser/action/PanelActions.java`（新增 `ToggleBookmarkBar` 内部类）
- `src/main/java/com/cpw/browser/toolwindow/BrowserToolWindowPanel.java`（挂载 bookmarkBar、初始可见性、刷新回调、toggleBookmark 联动、监听注册）
- `src/main/resources/messages/MyMessageBundle.properties` + 6 个语言变体（新增 3 个键）

## 11. 错误处理

- 书签数据模型已校验（`Bookmark` 字段防空），无新增风险
- 溢出 `JBPopup` 在无溢出时不显示
- 切换按钮在书签栏已 destroy 时不触发（`BrowserToolWindowPanel.dispose` 时移除监听）

## 12. 测试策略

- **单元测试**：`BookmarkBar` 的溢出计算纯逻辑可提取为静态方法 `computeVisibleCount(int availableWidth, List<Integer> widths)`，断言可见数与溢出列表
- **手动验证**（`runIde`）：
  1. 默认状态下地址栏下方显示书签栏
  2. 设置关闭"总是显示"-> 书签栏隐藏；切换按钮临时显示
  3. 书签超过栏宽 -> ">>" 按钮出现，点击弹出菜单
  4. 书签按钮左键导航、右键编辑/删除
  5. 编辑区模式下书签栏正常显示
  6. 语言切换后文案刷新
