# 横向书签栏 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在地址栏下方新增 Chrome 风格横向书签栏，右侧工具栏切换按钮控制显隐并写回持久化。

**Architecture:** 新建 `BookmarkBar` 组件横向排列书签按钮，溢出书签通过 ">>" 下拉菜单展示；`ToggleBookmarkBar` action 切换显隐并写回 `BrowserSettingsState.alwaysShowBookmarkBar`；`BrowserToolWindowPanel` 挂载于地址栏下方。

**Tech Stack:** 纯 Java、IntelliJ Platform、JCEF、JUnit 4

## Global Constraints

- 纯 Java，禁止 Kotlin
- 注释规范：class 上方注释说明用途；函数上方 `//` 注释含参数/返回值说明（注解上方写注释）；变量定义上方 `//` 说明用途；控制流加注释；`//` 注释上一行若为同级代码须空行
- 构建命令：`./gradlew build`
- 编译通过后必须 `git add -A` + `git commit -m "中文总结"`

---

### Task 1: BrowserSettingsState 新增持久化字段

**Files:**
- Modify: `src/main/java/com/cpw/browser/settings/BrowserSettingsState.java`

**Interfaces:**
- Produces: `BrowserSettingsState.isAlwaysShowBookmarkBar()` / `setAlwaysShowBookmarkBar(boolean)`

- [ ] **Step 1: 在 `State` 内类新增字段与访问器**

在 `State` 类的 `searchEngine` 字段后新增：
```java
        // 是否显示书签栏（持久化，不暴露设置页 UI）
        private boolean alwaysShowBookmarkBar = true;
```
在 `State` 类 `setSearchEngine` 后新增：
```java
        // 是否显示书签栏
        public boolean isAlwaysShowBookmarkBar() {
            return alwaysShowBookmarkBar;
        }

        // 设置是否显示书签栏
        public void setAlwaysShowBookmarkBar(boolean alwaysShowBookmarkBar) {
            this.alwaysShowBookmarkBar = alwaysShowBookmarkBar;
        }
```

- [ ] **Step 2: 外层 `BrowserSettingsState` 转发 getter/setter**

在 `setSearchEngine` 后新增：
```java
    // 是否显示书签栏
    public boolean isAlwaysShowBookmarkBar() {
        return state.isAlwaysShowBookmarkBar();
    }

    // 设置是否显示书签栏
    public void setAlwaysShowBookmarkBar(boolean alwaysShowBookmarkBar) {
        state.setAlwaysShowBookmarkBar(alwaysShowBookmarkBar);
    }
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: BrowserSettingsState 新增 alwaysShowBookmarkBar 持久化字段"
```

---

### Task 2: 国际化键

**Files:**
- Modify: 7 个 `src/main/resources/messages/MyMessageBundle*.properties`

**Interfaces:** Produces i18n keys `action.toggle.bookmark.bar`、`bookmark.bar.overflow`

- [ ] **Step 1: 默认包新增 2 键**

在 `MyMessageBundle.properties` 末尾新增：
```properties
action.toggle.bookmark.bar=Show/Hide bookmark bar
bookmark.bar.overflow=More bookmarks
```

- [ ] **Step 2: 各语言包新增 2 键**

`_zh`:
```properties
action.toggle.bookmark.bar=显示/隐藏书签栏
bookmark.bar.overflow=更多书签
```
`_ja`:
```properties
action.toggle.bookmark.bar=ブックマークバーの表示切替
bookmark.bar.overflow=他のブックマーク
```
`_ko`:
```properties
action.toggle.bookmark.bar=북마크 바 표시 전환
bookmark.bar.overflow=더 많은 북마크
```
`_fr`:
```properties
action.toggle.bookmark.bar=Afficher/Masquer la barre de favoris
bookmark.bar.overflow=Plus de favoris
```
`_de`:
```properties
action.toggle.bookmark.bar=Lesezeichenleiste ein-/ausblenden
bookmark.bar.overflow=Weitere Lesezeichen
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: 新增书签栏切换与溢出菜单国际化键"
```

---

### Task 3: WebBrowserIcons 注册 BOOKMARK_BAR

**Files:**
- Modify: `src/main/java/com/cpw/browser/WebBrowserIcons.java`

**Interfaces:** Produces `WebBrowserIcons.BOOKMARK_BAR`

- [ ] **Step 1: 新增常量**

在 `SHOW_BOOKMARK` 常量后新增：
```java
    // 显示/隐藏书签栏图标
    public static final Icon BOOKMARK_BAR = load("bookmark-bar");
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: WebBrowserIcons 注册 BOOKMARK_BAR 图标常量"
```

---

### Task 4: BookmarkBar 组件 + 溢出计算测试

**Files:**
- Create: `src/main/java/com/cpw/browser/ui/BookmarkBar.java`
- Create: `src/test/java/com/cpw/browser/ui/BookmarkBarTest.java`

**Interfaces:**
- Consumes: `Bookmark`、`BookmarkPersistentState`、`BookmarkSidebar.showBookmarkEditDialog`、`BookmarkListCellRenderer`、`TranslationUtil`
- Produces: `BookmarkBar(Consumer<Bookmark>, Runnable)`、`refreshBookmarks()`、`refreshLabels()`、`computeVisibleCount(int, List<Integer>)`

- [ ] **Step 1: 写溢出计算单元测试**

`src/test/java/com/cpw/browser/ui/BookmarkBarTest.java`:
```java
package com.cpw.browser.ui;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;

// BookmarkBar 溢出计算逻辑单元测试
public class BookmarkBarTest {

    // 所有书签都能放下时返回全部数量
    @Test
    public void allFitWhenWidthSufficient() {
        List<Integer> widths = Arrays.asList(100, 100, 100);
        assertEquals(3, BookmarkBar.computeVisibleCount(400, widths));
    }

    // 宽度不足时返回可放下的数量
    @Test
    public void stopsAtCapacity() {
        List<Integer> widths = Arrays.asList(100, 100, 100);
        assertEquals(2, BookmarkBar.computeVisibleCount(250, widths));
    }

    // 可用宽度为 0 时返回 0
    @Test
    public void emptyWhenNoWidth() {
        List<Integer> widths = Arrays.asList(100, 100);
        assertEquals(0, BookmarkBar.computeVisibleCount(0, widths));
    }

    // 空列表返回 0
    @Test
    public void emptyListReturnsZero() {
        assertEquals(0, BookmarkBar.computeVisibleCount(400, Collections.emptyList()));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew test --tests "com.cpw.browser.ui.BookmarkBarTest"`
Expected: 编译失败（BookmarkBar 不存在）

- [ ] **Step 3: 实现 BookmarkBar 完整组件**

`src/main/java/com/cpw/browser/ui/BookmarkBar.java`:
```java
// 横向书签栏组件，类似 Chrome 书签栏，在地址栏下方横向展示书签按钮
package com.cpw.browser.ui;

import com.cpw.browser.bookmark.Bookmark;
import com.cpw.browser.bookmark.BookmarkPersistentState;
import com.cpw.browser.util.TranslationUtil;
import com.intellij.openapi.ui.JBPopup;
import com.intellij.openapi.ui.JBPopupFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// 横向书签栏，横向排列书签按钮，溢出书签通过 ">>" 按钮以下拉菜单展示
public class BookmarkBar extends JBPanel<BookmarkBar> {

    // 书签按钮最大宽度（像素）
    private static final int MAX_BUTTON_WIDTH = 160;
    // 书签按钮左右内边距
    private static final int BUTTON_PADDING = 12;
    // 溢出按钮预留宽度
    private static final int OVERFLOW_BUTTON_WIDTH = 36;

    // 可见书签按钮容器
    private final JPanel visiblePanel;
    // 溢出按钮
    private final JButton overflowButton;
    // 当前所有书签
    private List<Bookmark> bookmarks = new ArrayList<>();
    // 书签选中回调
    private final Consumer<Bookmark> onBookmarkSelected;
    // 书签变更回调（编辑/删除后通知外部刷新）
    private final Runnable onBookmarkChanged;
    // 溢出弹窗
    private JBPopup overflowPopup;

    // 构造横向书签栏
    // onBookmarkSelected 为点击书签导航回调
    // onBookmarkChanged 为编辑/删除后外部刷新回调
    public BookmarkBar(Consumer<Bookmark> onBookmarkSelected, Runnable onBookmarkChanged) {
        super(new BorderLayout());
        this.onBookmarkSelected = onBookmarkSelected;
        this.onBookmarkChanged = onBookmarkChanged;

        // 可见书签按钮容器，横向排列
        visiblePanel = new JPanel();
        visiblePanel.setLayout(new BoxLayout(visiblePanel, BoxLayout.X_AXIS));
        visiblePanel.setOpaque(false);

        // 溢出按钮，点击弹出溢出书签列表
        overflowButton = new JButton("»");
        overflowButton.setBorderPainted(false);
        overflowButton.setContentAreaFilled(false);
        overflowButton.setFocusPainted(false);
        overflowButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overflowButton.setToolTipText(TranslationUtil.getText("bookmark.bar.overflow"));
        overflowButton.setVisible(false);
        overflowButton.setPreferredSize(new Dimension(OVERFLOW_BUTTON_WIDTH, 28));
        overflowButton.addActionListener(e -> showOverflowPopup());

        // 监听尺寸变化重新布局
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayout();
            }
        });

        add(visiblePanel, BorderLayout.CENTER);
        add(overflowButton, BorderLayout.EAST);

        refreshBookmarks();
    }

    // 刷新书签列表并重新布局
    public void refreshBookmarks() {
        bookmarks = BookmarkPersistentState.getInstance().getBookmarks();
        relayout();
    }

    // 语言切换后刷新文本
    public void refreshLabels() {
        overflowButton.setToolTipText(TranslationUtil.getText("bookmark.bar.overflow"));
        relayout();
    }

    // 计算给定可用宽度和书签按钮宽度列表下，能放下的书签数量
    // availableWidth 为可用宽度
    // buttonWidths 为各书签按钮宽度
    // 返回可放下的书签数量
    static int computeVisibleCount(int availableWidth, List<Integer> buttonWidths) {
        int used = 0;
        for (int i = 0; i < buttonWidths.size(); i++) {
            int w = buttonWidths.get(i);
            // 累计宽度超过可用宽度则返回当前索引
            if (used + w > availableWidth) {
                return i;
            }
            used += w;
        }
        return buttonWidths.size();
    }

    // 重新布局书签按钮
    private void relayout() {
        visiblePanel.removeAll();
        // 计算每个书签按钮的偏好宽度
        List<Integer> widths = new ArrayList<>();
        for (Bookmark b : bookmarks) {
            widths.add(measureButtonWidth(b.getTitle()));
        }
        // 可用宽度（预留溢出按钮宽度，确保溢出时可显示）
        int available = Math.max(0, getWidth() - OVERFLOW_BUTTON_WIDTH);
        int visibleCount = computeVisibleCount(available, widths);

        // 添加可见书签按钮
        for (int i = 0; i < visibleCount; i++) {
            visiblePanel.add(createBookmarkButton(bookmarks.get(i)));
        }
        // 有溢出时显示溢出按钮
        boolean hasOverflow = visibleCount < bookmarks.size();
        overflowButton.setVisible(hasOverflow);

        visiblePanel.revalidate();
        visiblePanel.repaint();
    }

    // 测量书签标题对应的按钮宽度
    private int measureButtonWidth(String title) {
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(title);
        return Math.min(MAX_BUTTON_WIDTH, textWidth + BUTTON_PADDING * 2);
    }

    // 截断标题以适配最大宽度，超出加省略号
    private String truncateTitle(String title, FontMetrics fm) {
        // 文本宽度未超则原样返回
        if (fm.stringWidth(title) <= MAX_BUTTON_WIDTH - BUTTON_PADDING * 2) {
            return title;
        }
        // 逐字缩减直到适配
        for (int i = title.length() - 1; i > 0; i--) {
            String truncated = title.substring(0, i) + "…";
            // 适配则返回
            if (fm.stringWidth(truncated) <= MAX_BUTTON_WIDTH - BUTTON_PADDING * 2) {
                return truncated;
            }
        }
        return "…";
    }

    // 创建单个书签按钮
    // bookmark 为书签数据
    // 返回配置好的书签按钮
    private JButton createBookmarkButton(Bookmark bookmark) {
        FontMetrics fm = getFontMetrics(getFont());
        // 截断后的显示文本
        String display = truncateTitle(bookmark.getTitle(), fm);
        JButton button = new JButton(display);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(MAX_BUTTON_WIDTH, 28));
        button.setPreferredSize(new Dimension(measureButtonWidth(bookmark.getTitle()), 28));
        button.setToolTipText(bookmark.getTitle() + " - " + bookmark.getUrl());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 左键导航
                if (e.getButton() == MouseEvent.BUTTON1) {
                    onBookmarkSelected.accept(bookmark);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // 右键弹出菜单（Windows/Linux 在 press 触发）
                if (e.isPopupTrigger()) {
                    showBookmarkContextMenu(bookmark, e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // 右键弹出菜单（Mac 在 release 触发）
                if (e.isPopupTrigger()) {
                    showBookmarkContextMenu(bookmark, e);
                }
            }
        });
        return button;
    }

    // 显示书签右键菜单
    private void showBookmarkContextMenu(Bookmark bookmark, MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        // 编辑菜单项
        JMenuItem editItem = new JMenuItem(TranslationUtil.getText("bookmark.edit.tooltip"));
        editItem.addActionListener(ev -> editBookmark(bookmark));
        popup.add(editItem);
        // 删除菜单项
        JMenuItem deleteItem = new JMenuItem(TranslationUtil.getText("bookmark.delete.tooltip"));
        deleteItem.addActionListener(ev -> deleteBookmark(bookmark));
        popup.add(deleteItem);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    // 编辑书签
    private void editBookmark(Bookmark bookmark) {
        // 弹出编辑对话框
        String[] result = BookmarkSidebar.showBookmarkEditDialog(bookmark.getTitle(), bookmark.getUrl());
        // 用户确认则更新书签
        if (result != null) {
            BookmarkPersistentState.getInstance().updateBookmark(bookmark.getUrl(), result[0], result[1]);
            refreshBookmarks();
            // 通知外部刷新
            if (onBookmarkChanged != null) {
                onBookmarkChanged.run();
            }
        }
    }

    // 删除书签
    private void deleteBookmark(Bookmark bookmark) {
        // 确认删除
        int confirm = Messages.showYesNoDialog(
                TranslationUtil.getText("bookmark.delete.confirm", bookmark.getTitle()),
                TranslationUtil.getText("bookmark.delete.title"),
                null
        );
        // 用户确认则删除
        if (confirm == Messages.YES) {
            BookmarkPersistentState.getInstance().removeBookmark(bookmark.getUrl());
            refreshBookmarks();
            // 通知外部刷新
            if (onBookmarkChanged != null) {
                onBookmarkChanged.run();
            }
        }
    }

    // 显示溢出书签弹窗
    private void showOverflowPopup() {
        // 可见按钮数量
        int visibleCount = visiblePanel.getComponentCount();
        // 收集溢出书签
        List<Bookmark> overflow = new ArrayList<>();
        for (int i = visibleCount; i < bookmarks.size(); i++) {
            overflow.add(bookmarks.get(i));
        }
        // 无溢出则返回
        if (overflow.isEmpty()) {
            return;
        }

        Bookmark[] arr = overflow.toArray(new Bookmark[0]);
        JBList<Bookmark> list = new JBList<>(arr);
        list.setCellRenderer(new BookmarkListCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 左键点击导航
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    onBookmarkSelected.accept(arr[idx]);
                    // 关闭弹窗
                    if (overflowPopup != null) {
                        overflowPopup.closeOk(null);
                    }
                }
            }
        });

        overflowPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(list, list)
                .setRequestFocus(true)
                .createPopup();
        overflowPopup.showUnderneathOf(overflowButton);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew test --tests "com.cpw.browser.ui.BookmarkBarTest"`
Expected: 4 个测试通过

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "feat: 新增 BookmarkBar 横向书签栏组件与溢出计算逻辑"
```

---

### Task 5: ToggleBookmarkBar action

**Files:**
- Modify: `src/main/java/com/cpw/browser/action/PanelActions.java`

**Interfaces:**
- Consumes: `BookmarkBar`、`WebBrowserIcons.BOOKMARK_BAR`、`BrowserSettingsState`、`TranslationUtil`
- Produces: `PanelActions.ToggleBookmarkBar(BookmarkBar)`

- [ ] **Step 1: 新增 import**

在 PanelActions.java 顶部 import 区新增：
```java
import com.cpw.browser.settings.BrowserSettingsState;
import com.cpw.browser.ui.BookmarkBar;
```

- [ ] **Step 2: 新增 ToggleBookmarkBar 内部类**

在 `ToggleBookmarkSidebar` 类后新增：
```java
    // 书签栏显示/隐藏切换，写回持久化
    public static class ToggleBookmarkBar extends AnAction implements DumbAware {

        private final BookmarkBar bookmarkBar;

        public ToggleBookmarkBar(BookmarkBar bookmarkBar) {
            super(TranslationUtil.getText("action.toggle.bookmark.bar"), TranslationUtil.getText("action.toggle.bookmark.bar"), WebBrowserIcons.BOOKMARK_BAR);
            this.bookmarkBar = bookmarkBar;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(TranslationUtil.getText("action.toggle.bookmark.bar"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            // 切换可见性
            boolean visible = !bookmarkBar.isVisible();
            bookmarkBar.setVisible(visible);
            // 写回持久化
            BrowserSettingsState.getInstance().setAlwaysShowBookmarkBar(visible);
            bookmarkBar.revalidate();
            bookmarkBar.repaint();
        }
    }
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: 新增 ToggleBookmarkBar 切换按钮 action"
```

---

### Task 6: BrowserToolWindowPanel 集成

**Files:**
- Modify: `src/main/java/com/cpw/browser/toolwindow/BrowserToolWindowPanel.java`

**Interfaces:**
- Consumes: `BookmarkBar`、`PanelActions.ToggleBookmarkBar`、`BrowserSettingsState`
- Produces: 集成后的浏览器面板（地址栏下方书签栏）

- [ ] **Step 1: 新增 import**

在文件顶部 import 区新增：
```java
import com.cpw.browser.ui.BookmarkBar;
```

- [ ] **Step 2: 新增字段**

在 `bookmarkSidebar` 字段后新增：
```java
    // 横向书签栏
    private final BookmarkBar bookmarkBar;
```

- [ ] **Step 3: 初始化 bookmarkBar**

在构造方法中 `bookmarkSidebar` 初始化块之后（`bookmarkSidebar.setMaximumSize(...)` 之后）新增：
```java
        // ---- 初始化横向书签栏 ----
        bookmarkBar = new BookmarkBar(
                bookmark -> onBookmarkSelected(bookmark),
                () -> {
                    // 书签变更后刷新侧边栏和地址栏星标
                    bookmarkSidebar.refreshBookmarks();
                    BrowserTabPanel tab = tabManager.getActiveTab();
                    // 有活跃标签页则刷新地址栏星标
                    if (tab != null) {
                        addressBar.updateStarIcon(tab.getCurrentUrl());
                    }
                }
        );
        // 按持久化值初始化可见性
        bookmarkBar.setVisible(BrowserSettingsState.getInstance().isAlwaysShowBookmarkBar());
```

- [ ] **Step 4: 挂载到 topSection**

将 `topSection` 组装处修改为：
```java
        // 顶部区域：[标签页栏] [地址栏] [书签栏]
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(tabStripPanel);
        topSection.add(navAddressBar);
        topSection.add(bookmarkBar);
```

- [ ] **Step 5: rightGroup 添加切换按钮**

在 `rightGroup.add(new PanelActions.ToggleBookmarkSidebar(...))` 后新增：
```java
        rightGroup.add(new PanelActions.ToggleBookmarkBar(bookmarkBar));
```

- [ ] **Step 6: toggleBookmark 联动刷新**

在 `toggleBookmark` 方法的删除分支（`bookmarkSidebar.refreshBookmarks();` 后）新增：
```java
            bookmarkBar.refreshBookmarks();
```
在添加分支（`bookmarkSidebar.refreshBookmarks();` 后）新增：
```java
            bookmarkBar.refreshBookmarks();
```

- [ ] **Step 7: 语言监听联动刷新**

在 `TranslationUtil.addListener` 回调中 `bookmarkSidebar.refreshLabels();` 后新增：
```java
            bookmarkBar.refreshLabels();
```

- [ ] **Step 8: 提交**

```bash
git add -A && git commit -m "feat: BrowserToolWindowPanel 集成横向书签栏"
```

---

### Task 7: 构建验证

**Files:** 无（仅构建）

- [ ] **Step 1: 运行构建**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 若构建失败则修复后再次构建**

修复编译错误，重新 `./gradlew build` 直到 BUILD SUCCESSFUL。

- [ ] **Step 3: 提交（如有修复）**

```bash
git add -A && git commit -m "fix: 修复书签栏集成构建问题"
```

- [ ] **Step 4: 手动验证（runIde）**

Run: `./gradlew runIde`
验证：
1. 默认状态下地址栏下方显示横向书签栏
2. 点击右侧书签栏切换按钮 -> 隐藏，重启 IDE 仍隐藏（持久化）
3. 添加足够多书签 -> "»" 按钮出现，点击弹出菜单
4. 书签按钮左键导航、右键编辑/删除
5. 编辑区模式下书签栏正常显示
6. 语言切换后文案刷新
