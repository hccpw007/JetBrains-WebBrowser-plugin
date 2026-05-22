# CLAUDE.md

本文档为 Claude Code 在此仓库中工作时提供指导。

## ⚠️ 最高优先级规则：对话完成后自动提交

每次对话中完成编译（`./gradlew build` 通过）后，必须执行以下操作：

1. 运行 `git add -A` 暂存所有文件变更
2. 运行 `git commit -m "{本次修改的内容总结}"` 提交代码
3. commit 信息中的 `{本次修改的内容总结}` 用简短的中文概括本次对话修改了哪些内容、解决了什么问题

此规则覆盖其他所有规则，每次编译通过后必须执行。

## 次高优先级规则：代码注释风格，适用于所有 java 代码

- 所有 class 定义上方必须加注释说明类的用途。简短说明用 `//` 单行注释，较长说明允许使用 `/* */` 块注释（这是唯一允许使用块注释的场景）
- 除此之外的其他场景**统一使用 `//` 单行注释**，禁止使用 `/* */` 或 `/** */` 块注释
- 所有函数定义上方用 // 注释说明功能,函数有参数时,参数前需要注释说明参数的用途,返回值前需要注释说明返回值的用途,函数有注解时候,注释应该在注解上方
- 函数内部新定义的变量上方都需要加 `//` 注释说明用途
- 所有常量和变量在定义时都需要在上方加 `//` 注释说明用途,即所有(类型 变量 = 值; 的都需要在上方加`//` 注释说明用途)
- 所有控制流语句（`if` / `else if` / `else` / `switch` / `for` / `while` / `do-while` / `try-catch` / `finally`）必须加注释说明

### 注释位置规则

- `if` 的注释放在 `if` 正上方
- `else` 的注释放在 `else` 右侧同行：`} else { // 条件说明`
- **`//` 注释的上一行如果是同级的代码，注释上方必须空行**（例如两个同级的 `if` 块，第二个 `if` 上方的注释前应有空行）
- 注释连续排列（多条 `//` 相邻）时，第一条注释的上一行如果是同级代码才需空行，后续注释紧随即可

## 对外文档中英双语

修改 `CLAUDE.md`、`README.md`、`CHANGELOG.md`、`plugin.xml` 中的插件描述等对外文档时，必须使用中英双语编写，且**英文在前、换行后为中文**。



# 以下内容用于减少大语言模型常见编码失误的行为准则。可按需与项目专属说明合并使用。

**取舍：** 这些准则偏向谨慎胜过速度。对于简单任务，请结合实际判断。

## 1. 编码前先思考

**不要擅自假设。不要掩盖困惑。把取舍讲清楚。**

实现之前：
- 明确说明你的假设。不确定时就询问。
- 如果存在多种理解，列出来，不要默默选择其中一种。
- 如果有更简单的方案，请直接说明。该提醒时要提醒。
- 如果有不清楚的地方，先停下来。指出困惑点，并提问。

## 2. 简洁优先

**用能解决问题的最少代码。不要做推测性设计。**

- 不实现需求之外的功能。
- 不为只用一次的代码抽象一套框架。
- 不添加未被要求的“灵活性”或“可配置性”。
- 不为不可能发生的场景写错误处理。
- 如果你写了 200 行但 50 行就够，请重写。
- 如果一个代码文件明显过长，请务必合理拆分代码文件。

问自己：“资深工程师会觉得这过度复杂吗？” 如果答案是会，就简化。

## 3. 精准改动

**只改必须改的地方。只清理你自己造成的问题。**

编辑现有代码时：
- 不顺手“改进”相邻代码、注释或格式。
- 不重构没有坏掉的东西。
- 匹配现有风格，即使你会用另一种写法。
- 如果发现无关的废弃代码，提出来，不要删除。

当你的改动产生孤儿代码时：
- 删除由你的改动导致未使用的 import、变量、函数和文件。
- 不删除改动前已经存在的死代码，除非用户明确要求。

检验标准：每一行变更都应该能直接追溯到用户请求。

## 4. 目标驱动执行

**定义成功标准。循环推进直到完成验证。**

把任务转化为可验证的目标：
- “添加校验” → “先为非法输入写测试，再让测试通过”
- “修复 bug” → “先写一个能复现 bug 的测试，再让它通过”
- “重构 X” → “确保重构前后测试都通过”

对于多步骤任务，先给出简短计划：
```
1. [步骤] → 验证：[检查项]
2. [步骤] → 验证：[检查项]
3. [步骤] → 验证：[检查项]
```

强成功标准能让你独立循环推进。弱标准（“让它能用”）会导致不断澄清。

---

**这些准则生效的迹象：** diff 中无关改动更少，因过度复杂导致的返工更少，澄清问题出现在实现之前而不是犯错之后。


# 构建与运行命令

```bash
# 构建插件
./gradlew build

# 启动 IDE 并加载插件
./gradlew runIde

# 运行所有测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "com.cpw.browser.*Test"

# 验证插件兼容性
./gradlew verifyPlugin

# 发布到 JetBrains Marketplace
./gradlew publishPlugin

# 清理构建产物
./gradlew clean
```

# 项目架构

这是一个 **IntelliJ Platform 插件**，使用 **纯 Java + Gradle** 构建，目标平台为 IntelliJ IDEA 2026.1.2。

## 技术栈
- Java（无 Kotlin 依赖）
- IntelliJ Platform Gradle Plugin 2.16.0
- IntelliJ IDEA 2026.1.2（目标平台）
- JUnit 4.13.2（测试）
- JCEF（Java Chromium Embedded Framework，用于内嵌浏览器）
- Group: `com.cpw.browser`, artifact: `WebBrowser`

## 源码结构
```
src/main/java/                 # 生产 Java 源码
  com/cpw/browser/
    JcefArgsProvider.java          # JCEF 启动参数（remote-debugging-port）
    MyMessageBundle.java           # 国际化消息工具类
    PluginFirstRunActivity.java    # 首次运行弹窗提示重启
    WebBrowserIcons.java           # 图标常量（UPPER_SNAKE_CASE 命名）
    action/                        # Action 类（ToggleBrowser、导航、书签、DevTools）
    bookmark/                      # 书签数据模型、持久化（BookmarkPersistentState）
    editor/                        # 文件编辑器集成（BrowserFileEditor、BrowserFileType）
    history/                       # 浏览历史数据模型、持久化（BrowsingHistoryState）
    settings/                      # 插件设置页面与持久化（BrowserSettingsPage、BrowserSettingsState）
    toolwindow/                    # 工具窗口主面板（BrowserToolWindowPanel）、标签页（BrowserTabPanel/Manager）、工厂
    ui/                            # UI 组件（地址栏 AddressBar、ChromeTab、书签侧边栏 BookmarkSidebar）
src/main/resources/          # 资源文件
  META-INF/plugin.xml        # 插件描述符（ID、名称、依赖、扩展点）
  META-INF/pluginIcon.svg    # 插件图标
  icons/                     # SVG 图标（英文命名，支持 xxx_dark.svg 深色主题）
  messages/                  # i18n 消息包（.properties）
```

## 关键文件
- **plugin.xml**: 定义插件 ID（`com.cpw.browser.WebBrowser`）、依赖（`com.intellij.modules.platform`）和扩展点（toolWindow、applicationService、fileEditorProvider 等）
- **BrowserToolWindowPanel.java**: 工具窗口主面板，包含标签页管理器、地址栏、书签侧边栏、缩放提示等核心 UI
- **BrowserTabPanel.java**: 单个浏览器标签页，封装 JBCefBrowser，管理导航历史、缩放、DevTools
- **BrowserTabManager.java**: 多标签页管理，支持创建/关闭/切换标签页，管理回调事件
- **BookmarkSidebar.java**: 书签与历史记录侧边栏，包含分段切换器（Element Plus 风格）
- **AddressBar.java**: 地址栏，支持 URL 输入、书签星标、加载进度显示

## 扩展点
插件通过 `<extensions>` 在 `plugin.xml` 中声明以下扩展：
- `<toolWindow>` — 注册浏览器工具窗口（id: WebBrowser，左侧）
- `<applicationService>` — 书签持久化、设置持久化、浏览历史持久化
- `<applicationConfigurable>` — 插件设置页面
- `<fileEditorProvider>` — 文件编辑器集成（`.webbrowser` 文件类型）
- `<fileType>` — 自定义文件类型
- `<editorTabTitleProvider>` — 编辑器标签页标题提供者

## 开发工作流
- 运行配置在 `.run/` 目录下（Run Plugin、Run Tests、Run Verifications）
- 日志在开发运行的 `idea.log` 标签页中查看
- 使用 `./gradlew runIde` 启动开发 IDE 实例
- 插件依赖 `com.intellij.modules.platform`，使用 JCEF API 内嵌浏览器
- **本项目已从 Kotlin 完全迁移为纯 Java，禁止引入新的 Kotlin 依赖或源文件**
