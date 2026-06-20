# 编辑页：键盘与格式工具栏间距问题

## 现象

在 iOS 风格笔记编辑页中，软键盘弹起时出现两类布局问题：

1. **文本被顶没**：键盘弹出后，标题和正文移出可视区域，编辑区只剩空白。
2. **工具栏与键盘之间有缝隙**：格式工具栏（加粗、列表等）悬在键盘上方，中间露出一条米色背景空隙；多次修复后缝隙逐渐变小，但仍有残留。

## 环境

- Jetpack Compose + Material 3 `Scaffold`
- `MainActivity` 使用 `enableEdgeToEdge()`
- `AndroidManifest`：`android:windowSoftInputMode="adjustNothing"`
- 外层 [`Navigation.kt`](../app/src/main/java/com/andriod/reader/ui/Navigation.kt) 有全局 `Scaffold` + `NavHost`
- 内层 [`EditorScreen.kt`](../app/src/main/java/com/andriod/reader/ui/editor/EditorScreen.kt) 有独立 `Scaffold` + 格式工具栏

## 根因

问题来自 **WindowInsets 被重复或错误地应用**，而不是工具栏组件本身。

| 阶段 | 错误做法 | 后果 |
|------|----------|------|
| 初版 | 内容区与 `Scaffold.bottomBar` 各加 `imePadding()` | 键盘高度被计算两次，可视区域几乎为 0 |
| 二版 | 父 `Box` 加 `imePadding()`，工具栏再叠加定位 | 父容器已上移，工具栏二次偏移，出现米色缝隙 |
| 三版 | `Column` 内放工具栏 + 单次 `imePadding()` | 缝隙缩小，但 `Scaffold` 默认 bottom inset 仍把工具栏抬在导航栏上方 |
| **最终** | 去掉导航栏 bottom inset，按 IME 精确贴底 | 工具栏紧贴键盘，无缝隙 |

具体原因：

1. **`adjustResize` 与 `imePadding()` 冲突**  
   `enableEdgeToEdge()` 下应使用 `adjustNothing`，由 Compose 的 `WindowInsets.ime` 处理键盘，避免窗口缩放与 inset 叠加。

2. **`imePadding()` 多处使用**  
   同一层级链路上只需一处消费 IME inset；内容区和工具栏应共用同一套底部偏移量，而不是各自 `imePadding()`。

3. **`Scaffold` 默认 `contentWindowInsets`**  
   默认包含 `safeDrawing`（含底部导航栏）。编辑页内容 `padding` 会多出一段导航栏高度，工具栏停在导航栏上方，键盘在其上，形成缝隙。

4. **外层 `NavHost` 的 padding**  
   全局 `Scaffold` 在编辑路由仍可能传入底部 padding，进一步抬高编辑区域。

## 最终方案

### 1. Manifest / Activity

```xml
android:windowSoftInputMode="adjustNothing"
```

配合 `enableEdgeToEdge()`，不在系统层做 `adjustResize`。

### 2. 外层导航（编辑页零 padding）

编辑路由下，`NavHost` 不再应用外层 `Scaffold` 的 padding：

```kotlin
val navPadding = if (isEditor) {
    PaddingValues()
} else {
    padding
}
```

### 3. 编辑页 Scaffold（仅保留状态栏 inset）

```kotlin
Scaffold(
    contentWindowInsets = WindowInsets.statusBars,
    // ...
)
```

避免把底部导航栏高度算进内容 `padding`。

### 4. 工具栏贴键盘：显式 IME 偏移

不用 `imePadding()` 包裹整块布局，改为读取 IME 高度并分别作用于内容与工具栏：

```kotlin
val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

Box(Modifier.fillMaxSize().padding(padding)) {
    // 正文：底部留出 IME + 工具栏高度
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = if (showToolbar) imeBottom + ToolbarHeight else imeBottom)
            .verticalScroll(...)
    ) { /* 标题、正文 */ }

    // 工具栏：从屏幕底边上移 imeBottom，贴在键盘顶沿
    if (showToolbar) {
        EditorFormattingToolbar(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = imeBottom)
        )
    }
}
```

要点：

- **一个** `imeBottom` 值，两处使用（内容留白 + 工具栏定位）
- 工具栏相对 **屏幕底边** 定位，而不是相对已 inset 的父容器底边
- 工具栏放在 `Box` 底部 overlay，不放在带 `imePadding()` 的 `Column` 里

## 相关文件

- [`app/src/main/AndroidManifest.xml`](../app/src/main/AndroidManifest.xml)
- [`app/src/main/java/com/andriod/reader/MainActivity.kt`](../app/src/main/java/com/andriod/reader/MainActivity.kt)
- [`app/src/main/java/com/andriod/reader/ui/Navigation.kt`](../app/src/main/java/com/andriod/reader/ui/Navigation.kt)
- [`app/src/main/java/com/andriod/reader/ui/editor/EditorScreen.kt`](../app/src/main/java/com/andriod/reader/ui/editor/EditorScreen.kt)
- [`app/src/main/java/com/andriod/reader/ui/editor/EditorFormattingToolbar.kt`](../app/src/main/java/com/andriod/reader/ui/editor/EditorFormattingToolbar.kt)

## 调试建议

若类似问题复现，按顺序检查：

1. Manifest 是否为 `adjustNothing`（edge-to-edge + Compose）
2. 链路上有几处 `imePadding()` / `navigationBarsPadding()`（超过一处易重叠）
3. `Scaffold` 的 `contentWindowInsets` 是否把 `navigationBars` 算进 bottom padding
4. 嵌套 `Scaffold`（外层 App + 内层 Editor）是否重复施加 bottom inset
5. 工具栏应对齐 **IME bottom**，而非已扣除 inset 的父布局底边

## 备注

- 输入法自带工具栏（如 Gboard 顶部一行）属于 IME 界面，与应用工具栏不同；若缝隙颜色为输入法主题色，需按具体 IME 单独评估。
- 编辑时仍显示 Markdown 源码，非所见即所得；格式按钮插入的是 Markdown 符号。
