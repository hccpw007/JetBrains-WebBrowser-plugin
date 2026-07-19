# 编辑区多 tab 独立浏览器实例设计

## Background / 背景

Current editor mode: clicking the top icon toggles a single temporary `.webbrowser` file; all editor instances share one `BrowserToolWindowPanel` (singleton via `BrowserProjectService.getEditorPanel()`), whose internal Chrome-style tab strip manages multiple browser tabs.

当前编辑区模式：点击顶部 icon toggle 一个临时 `.webbrowser` 文件，所有编辑器实例共享一个 `BrowserToolWindowPanel`（通过 `BrowserProjectService.getEditorPanel()` 单例），面板内部的 Chrome 风格 tab 栏管理多个浏览器标签页。

## Goal / 目标

- Edit-area mode optionally opens one native IDEA editor tab per icon click (each an independent browser instance).
- Hide the plugin's internal tab strip in this new mode (keep address bar, navigation, bookmark sidebar, status bar).
- Tool-window mode is unchanged.
- Add a setting so the user controls whether to enable this behavior.

- 编辑区模式可选：每次点击 icon 新建一个 IDEA 原生编辑区 tab（每个 tab 是独立浏览器实例）。
- 新模式下隐藏插件内部 tab 栏（保留地址栏、导航、书签侧边栏、状态栏）。
- 工具窗口模式完全不变。
- 新增设置项让用户控制是否启用此行为。

## Requirements / 需求

1. New setting `editorNewTabOnClick` (boolean, default `false`):
   - Shown in settings page only when `displayPosition = "editor"`.
   - `true`: each icon click creates a new independent IDEA editor tab (new behavior).
   - `false`: keep current behavior (toggle a single shared tab).
   - Default `false` per the user's statement "if the user does not choose... keep current behavior".

- 新增设置项 `editorNewTabOnClick`（boolean，默认 `false`）：
   - 仅在 `displayPosition = "editor"` 时显示。
   - `true`：每次点击 icon 新建独立 IDEA tab（新行为）。
   - `false`：维持现状（toggle 单个共享 tab）。
   - 默认 `false`，符合用户描述"如果用户没有选择...维持现在这个方式"。

2. When `editorNewTabOnClick = true`:
   - Each icon click creates a `LightVirtualFile` (FileType = `BrowserFileType.INSTANCE`) and calls `FileEditorManager.openFile`.
   - Each `BrowserFileEditor` constructs its own `BrowserToolWindowPanel(project, editorMode=true)`.
   - `editorMode=true` hides `tabStripPanel` + `addTabButton`; still creates one internal tab (reuses all address-bar/navigation/bookmark/zoom logic).
   - Closing the IDEA tab calls `BrowserFileEditor.dispose()` -> `browserPanel.dispose()` -> releases JCEF.

- 当 `editorNewTabOnClick = true`：
   - 每次点击 icon 创建 `LightVirtualFile`（FileType = `BrowserFileType.INSTANCE`），调用 `FileEditorManager.openFile`。
   - 每个 `BrowserFileEditor` 构造自己的 `BrowserToolWindowPanel(project, editorMode=true)`。
   - `editorMode=true` 隐藏 `tabStripPanel` + `addTabButton`；内部仍创建一个 tab（复用全部地址栏/导航/书签/缩放逻辑）。
   - 关闭 IDEA tab 时 `BrowserFileEditor.dispose()` -> `browserPanel.dispose()` -> 释放 JCEF。

3. When `editorNewTabOnClick = false`: keep current behavior unchanged (shared singleton panel, toggle semantics).

- 当 `editorNewTabOnClick = false`：维持现有行为不变（共享单例面板，toggle 语义）。

4. Tool-window mode (`displayPosition = "toolbar"`): completely unchanged.

- 工具窗口模式（`displayPosition = "toolbar"`）：完全不变。

## Design / 设计

### Data Flow / 数据流

New mode (`editorNewTabOnClick = true`):
新模式（`editorNewTabOnClick = true`）：
```
icon click -> ToggleBrowserAction
  -> LightVirtualFile("Web Browser N.webbrowser", BrowserFileType.INSTANCE)
  -> FileEditorManager.openFile(vfile, true)
  -> BrowserFileEditorProvider.createEditor
     -> new BrowserFileEditor(project, vfile)
        -> new BrowserToolWindowPanel(project, editorMode=true)
  -> IDEA editor area adds a native tab
  -> BrowserEditorTabTitleProvider supplies page title
```

Close / 关闭：
```
IDEA tab X -> BrowserFileEditor.dispose() -> browserPanel.dispose() -> JCEF released
```

### Component Changes / 组件改动

1. **`BrowserSettingsState`**: add `boolean editorNewTabOnClick = false` with getter/setter.
2. **`BrowserSettingsPage`**: add a `JBCheckBox` below the display-position combo. Bind an `ItemListener` on `displayPositionCombo` to show/hide the checkbox (visible only when editor selected). Update `isModified`/`apply`/`reset`/`refreshLabels`.
3. **`ToggleBrowserAction`**: in editor mode, branch on `editorNewTabOnClick`:
   - `true` -> `createNewEditorTab(project)`: create `LightVirtualFile` (with a per-project incrementing counter for unique names) + `openFile`. No toggle-off.
   - `false` -> existing `toggleEditorMode(project)` unchanged.
4. **`BrowserFileEditor`**: branch on `editorNewTabOnClick`:
   - `true` -> `new BrowserToolWindowPanel(project, true)`; `dispose()` calls `browserPanel.dispose()`.
   - `false` -> existing shared `BrowserProjectService.getEditorPanel()`; `dispose()` does not release (shared, as today).
5. **`BrowserToolWindowPanel`**: add `boolean editorMode` constructor param. When `true`: `tabStripPanel.setVisible(false)`, skip adding `addTabButton`, still call `tabManager.createTab()`. When `false`: current behavior. Update both callers (editor `true`/`false`, tool-window factory `false`).
6. **`BrowserProjectService`**: KEEP (still needed by `false` mode's shared singleton panel). Do not remove.
7. **i18n**: add `settings.editor.new.tab.on.click` to all 6 message bundles (`MyMessageBundle.properties` default/en + `_zh`/`_ja`/`_ko`/`_fr`/`_de`). The settings UI offers 7 language options (including "follow system"), backed by these 6 property files.

- 1. **`BrowserSettingsState`**：新增 `boolean editorNewTabOnClick = false` 及 getter/setter。
- 2. **`BrowserSettingsPage`**：在显示位置下拉框下方新增 `JBCheckBox`。给 `displayPositionCombo` 绑定 `ItemListener`，仅当选中 editor 时显示该 checkbox。更新 `isModified`/`apply`/`reset`/`refreshLabels`。
- 3. **`ToggleBrowserAction`**：editor 模式下按 `editorNewTabOnClick` 分流：
   - `true` -> `createNewEditorTab(project)`：创建 `LightVirtualFile`（用项目级递增计数器保证文件名唯一）+ `openFile`。不再 toggle 关闭。
   - `false` -> 现有 `toggleEditorMode(project)` 不变。
- 4. **`BrowserFileEditor`**：按 `editorNewTabOnClick` 分流：
   - `true` -> `new BrowserToolWindowPanel(project, true)`；`dispose()` 调用 `browserPanel.dispose()`。
   - `false` -> 现有共享 `BrowserProjectService.getEditorPanel()`；`dispose()` 不释放（共享，维持现状）。
- 5. **`BrowserToolWindowPanel`**：新增 `boolean editorMode` 构造参数。`true`：`tabStripPanel.setVisible(false)`，不添加 `addTabButton`，仍调用 `tabManager.createTab()`。`false`：现有行为。更新两处调用（editor `true`/`false`、工具窗口工厂 `false`）。
- 6. **`BrowserProjectService`**：保留（`false` 模式的共享单例面板仍需要）。不移除。
- 7. **国际化**：6 个消息包文件（`MyMessageBundle.properties` 默认/en + `_zh`/`_ja`/`_ko`/`_fr`/`_de`）新增 `settings.editor.new.tab.on.click`。设置 UI 提供 7 种语言选项（含"跟随系统"），由这 6 个 properties 文件支撑。

### Resource Release / 资源释放

`true` mode: `BrowserFileEditor.dispose()` -> `BrowserToolWindowPanel.dispose()` -> `tabManager.disposeAll()` -> each `BrowserTabPanel.dispose()` -> `JBCefBrowser.dispose()`. Each IDEA tab close releases its own JCEF (addresses the JCEF-leak class fixed in commit 644a4ce).

`true` 模式：`BrowserFileEditor.dispose()` -> `BrowserToolWindowPanel.dispose()` -> `tabManager.disposeAll()` -> 每个 `BrowserTabPanel.dispose()` -> `JBCefBrowser.dispose()`。每个 IDEA tab 关闭即释放对应 JCEF（呼应提交 644a4ce 修复的 JCEF 泄漏类问题）。

`false` mode: unchanged (shared panel released by `BrowserProjectService.dispose()` at project close).

`false` 模式：不变（共享面板由 `BrowserProjectService.dispose()` 在项目关闭时释放）。

### Edge Cases / 边界情况

- Multi-project: each project's `FileEditorManager` is independent; `LightVirtualFile` instances are separate objects, no conflict.
- `displayPosition` editor -> toolbar: `toggleToolbarMode` already iterates and closes all `BrowserFileEditor`s, adapts to multi-tab automatically.
- `LightVirtualFile` lifecycle: in-memory, GC-collected after editor close, no disk residue.
- New-tab default URL: reuse existing `BrowserTabManager.createTab()` logic (`about:blank` + `openHomeOnNewTab` setting).
- Toggling `editorNewTabOnClick` in settings: takes effect on next icon click; existing already-open tabs keep their mode until closed.

- 多项目：各 project 的 `FileEditorManager` 独立，`LightVirtualFile` 是独立对象，无冲突。
- `displayPosition` editor -> toolbar：`toggleToolbarMode` 已遍历关闭所有 `BrowserFileEditor`，自动适配多 tab。
- `LightVirtualFile` 生命周期：内存对象，editor 关闭后 GC 回收，无磁盘残留。
- 新建 tab 默认 URL：沿用现有 `BrowserTabManager.createTab()` 逻辑（`about:blank` + `openHomeOnNewTab` 设置）。
- 设置中切换 `editorNewTabOnClick`：下次点击 icon 生效；已打开的 tab 维持各自模式直到关闭。

## Verification / 验证

Manual via `runIde`:
- `editorNewTabOnClick = true`: click icon multiple times -> each click opens an independent editor tab; tabs browse different URLs independently; closing one tab releases JCEF (no leak warnings in `idea.log`).
- `editorNewTabOnClick = false`: icon toggles a single shared tab as before.
- Settings page: checkbox appears only when editor selected; hidden for toolbar.
- Switch `displayPosition` editor -> toolbar: all editor tabs close, tool-window behavior unchanged.
- Tool-window mode: internal multi-tab behavior unchanged.

通过 `runIde` 手动验证：
- `editorNewTabOnClick = true`：多次点击 icon -> 每次新建独立编辑区 tab；各 tab 独立浏览不同 URL；关闭单个 tab 释放 JCEF（`idea.log` 无泄漏告警）。
- `editorNewTabOnClick = false`：icon 仍 toggle 单个共享 tab，行为同升级前。
- 设置页：checkbox 仅在选中 editor 时显示；toolbar 时隐藏。
- 切换 `displayPosition` editor -> toolbar：所有编辑器 tab 关闭，工具窗口行为不变。
- 工具窗口模式：内部多 tab 行为不变。

## Out of Scope / 不在范围内

- Persisting/restore of editor tabs across IDE restart (tabs are ephemeral).
- Migrating the existing shared-panel `false` mode.
- Refactoring `BrowserToolWindowPanel` to not use `tabManager` in editor mode (kept to minimize changes).

- IDE 重启后编辑器 tab 的持久化/恢复（tab 是临时的）。
- 迁移现有共享面板的 `false` 模式。
- 在编辑区模式下重构 `BrowserToolWindowPanel` 使其不使用 `tabManager`（保留以最小化改动）。
