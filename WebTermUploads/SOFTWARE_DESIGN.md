# WallQuote（壁上言）软件设计文档

> 版本: 1.0 | 日期: 2024 | 基于反编译源码分析重构

---

## 目录

1. [项目概述](#1-项目概述)
2. [系统架构](#2-系统架构)
3. [模块设计](#3-模块设计)
4. [数据库设计](#4-数据库设计)
5. [UI 设计](#5-ui-设计)
6. [核心流程](#6-核心流程)
7. [壁纸渲染引擎](#7-壁纸渲染引擎)
8. [技术选型与决策](#8-技术选型与决策)

---

## 1. 项目概述

### 1.1 产品定位

**WallQuote（壁上言）** 是一款 Android 动态壁纸应用。用户可以在手机壁纸上轮播显示中英文名言警句，支持自定义文字样式、背景图片/颜色/渐变，以及按时间计划自动切换收藏集。

### 1.2 核心功能

| 功能模块 | 描述 |
|----------|------|
| **收藏集管理** | 创建、编辑、删除名言收藏集，每个收藏集包含多行文本 |
| **时间调度** | 设置收藏集在一天内的活跃时间段，系统自动切换 |
| **视觉定制** | 文字样式（颜色、字体、大小、对齐、阴影、边框）、背景（纯色/渐变/图片） |
| **预设样式** | 内置多套预设文字样式，支持用户创建自定义样式 |
| **动态壁纸** | 作为 Android 原生动态壁纸运行，支持屏幕亮起时推进和淡入淡出动画 |
| **实时预览** | 编辑器中实时预览壁纸效果，支持拖动和旋转 |

### 1.3 技术参数

| 项目 | 值 |
|------|-----|
| 包名 | `com.example.wallquote` |
| 应用名 | 壁上言 |
| 最低 SDK | Android 8.0 (API 26) |
| 目标 SDK | Android 14 (API 34) |
| 编译 SDK | 34 |
| 版本 | 1.0 (versionCode=1) |

---

## 2. 系统架构

### 2.1 分层架构

应用采用 **Clean Architecture + MVVM** 多层架构：

```
┌─────────────────────────────────────────────────────────────┐
│ UI Layer (Jetpack Compose)                                  │
│ ├── Home Screen (收藏集列表)                                 │
│ ├── Editor Screen (编辑器)                                   │
│ └── Custom Style Editor (自定义样式)                         │
├─────────────────────────────────────────────────────────────┤
│ ViewModel Layer                                             │
│ ├── HomeViewModel (StateFlow<List<CollectionConfig>>)       │
│ ├── CollectionEditorViewModel (StateFlow<EditorUiState>)    │
│ └── CustomStyleEditorViewModel                              │
├─────────────────────────────────────────────────────────────┤
│ Domain Layer (Use Cases)                                    │
│ ├── ObserveOrderedCollectionsUseCase                        │
│ ├── SelectActiveCollectionsUseCase                          │
│ ├── AutoStyleMatcher                                        │
│ └── BackgroundSampler                                       │
├─────────────────────────────────────────────────────────────┤
│ Data Layer (Repository + Room + Model)                      │
│ ├── CollectionRepository / CustomStyleRepository            │
│ ├── Room DAOs / Entities / Converters                       │
│ └── Domain Models (@Serializable)                           │
├─────────────────────────────────────────────────────────────┤
│ Wallpaper Service Layer                                     │
│ ├── WallpaperSessionController (轮播逻辑)                    │
│ ├── WallpaperRenderer (Canvas渲染)                          │
│ ├── FadeTransitionController (过渡动画)                      │
│ └── WallpaperTriggerObserver (屏幕事件)                      │
├─────────────────────────────────────────────────────────────┤
│ Core Renderer                                               │
│ ├── QuoteTextRenderer / FontResolver / QuoteRenderSpec      │
│ └── ColorParsers / BackgroundPreview                        │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 依赖注入

使用 **Dagger Hilt** 进行依赖注入，主要组件：

- `@HiltAndroidApp MyApplication` — 应用级组件
- `@AndroidEntryPoint MainActivity` — Activity 级组件
- `@HiltViewModel` — ViewModel 注入（Home, Editor, CustomStyle）
- `@Module @InstallIn(SingletonComponent) DatabaseModule` — 数据库依赖
- `@Singleton` 作用域：`AppDatabase`, `DefaultCollectionInitializer`, `CustomStyleRepository`

---

## 3. 模块设计

### 3.1 领域模型

#### CollectionConfig（收藏集配置）

```kotlin
data class CollectionConfig(
    val id: Long = 0,
    val schedule: DailyTimeRange,     // 时间调度
    val background: BackgroundSpec,   // 背景规格
    val texts: List<String>,          // 文本行列表
    val textStyle: TextStyleConfig,   // 文本样式
    val offsetX: Float = 0f,          // X偏移
    val offsetY: Float = 0f,          // Y偏移
    val rotation: Float = 0f,         // 旋转
    val sortOrder: Int = 0            // 排序顺序
)
```

#### BackgroundSpec（背景规格 - 密封类）

```
BackgroundSpec
├── Solid(colorHex: String)
├── Gradient(startColorHex, endColorHex, angle)
└── Photo(uri: String, dimAmount=0, blurRadius=0)
```

#### DailyTimeRange（时间范围）

```kotlin
data class DailyTimeRange(
    val startMinuteOfDay: Int,  // 0-1439
    val endMinuteOfDay: Int     // 0-1439
)
```

- `isAllDay()`: startMinute == endMinute
- `isOvernight()`: startMinute > endMinute（跨午夜）
- `contains(minuteOfDay)`: 判断指定分钟是否在范围内

#### TextStyleConfig（文本样式 - 23个参数）

```kotlin
@Serializable
data class TextStyleConfig(
    // 文本颜色
    val colorHex: String = "#FFFFFF",
    val textAlpha: Float = 1f,
    // 字体
    val textSizeSp: Float = 32f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val fontFamilyName: String = "Serif",
    // 对齐
    val alignment: Int = 1,           // 0左/1中/2右
    val verticalAlignment: Int = 1,   // 0上/1中/2下
    val isVerticalText: Boolean = false,
    // 间距
    val letterSpacingSp: Float = 0f,
    val lineHeightMultiplier: Float = 1.3f,
    // 文字背景
    val bgColorHex: String? = null,
    val bgAlpha: Float = 0.5f,
    val cornerRadius: Float = 0f,
    val padding: Float = 0f,
    // 边框
    val borderWidth: Float = 0f,
    val borderColorHex: String? = null,
    val borderAlpha: Float = 1f,
    // 阴影
    val shadowRadius: Float = 0f,
    val shadowDistance: Float = 8f,
    val shadowAngle: Float = 90f,
    val shadowAlpha: Float = 1f,
    val shadowColorHex: String = "#80000000"
)
```

### 3.2 用例层

| 用例 | 输入 | 输出 | 职责 |
|------|------|------|------|
| `ObserveOrderedCollectionsUseCase` | — | `Flow<List<CollectionConfig>>` | 观察所有收藏集（排序后） |
| `SelectActiveCollectionsUseCase` | `List<CollectionConfig>`, `minuteOfDay` | `List<CollectionConfig>` | 按时间筛选激活的收藏集 |
| `AutoStyleMatcher` | `TextStyleConfig`, `BackgroundSample` | `TextStyleConfig` | 根据背景自动适配文本样式 |
| `BackgroundSampler` | `BackgroundSpec` | `BackgroundSample` | 从背景提取色彩样本 |

### 3.3 编辑器状态管理

#### 编辑动作（EditorAction - 密封接口，21个变体）

```
Initialize | StartMinuteChanged | EndMinuteChanged |
BackgroundSelected | PhotoPicked | PreviewTextIndexChanged |
AddText | DeleteText | UpdateText | TextColorChanged |
AlignmentChanged | ApplyPreset | AutoMatch | Transform |
ToggleTab | CloseSheet | CloseRequested | SaveAndExit |
ConfirmSaveNewCollection | ConfirmDiscardNewCollection |
DismissSaveNewCollectionDialog
```

#### 编辑器 UI 状态（EditorUiState - 14个字段）

```kotlin
data class EditorUiState(
    val collectionId: Long?,
    val startMinute: Int,    // 开始时间
    val endMinute: Int,      // 结束时间
    val backgroundSpec: BackgroundSpec,
    val texts: List<EditorTextEntry>,
    val textStyle: TextStyleConfig,
    val offsetX: Float, val offsetY: Float, val rotation: Float,
    val sortOrder: Int,
    val selectedTab: EditorTab?,       // 当前标签页（null=关闭面板）
    val previewTextIndex: Int,
    val showSaveNewCollectionDialog: Boolean,
    val customStyles: List<CustomStyleEntity>
)
```

---

## 4. 数据库设计

### 4.1 ER 图

```
┌──────────────────────────┐       ┌───────────────────────────┐
│     collections           │       │  collection_text_lines    │
├──────────────────────────┤       ├───────────────────────────┤
│ id (PK, AUTO)            │──1:N──│ id (PK, AUTO)             │
│ name                     │       │ collectionId (FK, INDEX)   │
│ startMinuteOfDay         │       │ text                      │
│ endMinuteOfDay           │       │ displayOrder              │
│ backgroundType           │       └───────────────────────────┘
│ backgroundData           │           FK → collections.id
│ textStyleData (JSON)     │           ON DELETE CASCADE
│ offsetX                  │
│ offsetY                  │       ┌───────────────────────────┐
│ rotation                 │       │     custom_styles         │
│ sortOrder                │       ├───────────────────────────┤
└──────────────────────────┘       │ id (PK, AUTO)             │
                                    │ name                     │
                                    │ textStyleData (JSON)     │
                                    └───────────────────────────┘
```

### 4.2 数据库版本演进

| 版本 | 迁移内容 |
|------|----------|
| v1→v2 | 重构表结构：`text_groups`/`text_lines` → `collections`/`collection_text_lines` |
| v2→v3 | 新增 `custom_styles` 表（独立样式列） |
| v3→v4 | `custom_styles` 添加 `isItalic` 列 |
| v4→v5 | 无结构变化（空迁移） |
| v5→v6 | `custom_styles` 添加 7 列（alpha、对齐、边框、阴影等） |
| v6→v7 | 重大重构：`custom_styles` 的 20+ 独立列合并为 JSON `textStyleData` 列 |

### 4.3 类型转换器

`TextStyleConverter` 负责 `TextStyleConfig` 与 JSON 字符串的双向转换：

- **编码**: 使用 `kotlinx.serialization.Json` 序列化
- **解码防御**: 先尝试 `kotlinx.serialization`；失败时回退到 `org.json.JSONObject` 手动解析；完全失败返回全默认值对象

---

## 5. UI 设计

### 5.1 屏幕导航

```
MainActivity
  └── WallQuoteNavHost
        ├── "home" → HomeScreen
        │     ├── [新建按钮] → "collection_editor?collectionId=" (新收藏集)
        │     └── [卡片点击] → "collection_editor?collectionId={id}" (编辑)
        ├── "collection_editor?collectionId={collectionId}" → EditorScreen
        │     └── [自定义样式按钮] → "custom_style_editor"
        └── "custom_style_editor" → CustomStyleEditor
```

### 5.2 主页（HomeScreen）

```
┌─────────────────────────────────┐
│  TopAppBar                      │
│   [新建] [设为壁纸]               │
├─────────────────────────────────┤
│  LazyColumn (SwipeToDismiss)    │
│  ┌───────────────────────────┐  │
│  │ CollectionCard            │  │
│  │ ┌─────────────────────┐   │  │
│  │ │ 背景预览              │   │  │
│  │ ├─────────────────────┤   │  │
│  │ │ 收藏集标题            │   │  │
│  │ │ 时间轴 [====●=====]  │   │  │
│  │ │ 文本预览行1           │   │  │
│  │ │ 文本预览行2           │   │  │
│  │ │ 文本预览行3           │   │  │
│  │ └─────────────────────┘   │  │
│  └───────────────────────────┘  │
│  左滑 → 删除确认对话框            │
└─────────────────────────────────┘
```

### 5.3 编辑器（CollectionEditorScreen）

```
┌─────────────────────────────────┐
│  预览区域（全屏）                  │
│  ┌─────────────────────────────┐│
│  │  背景 + 文字实时预览          ││
│  │  （支持拖动和旋转手势）        ││
│  │                         [×] ││
│  └─────────────────────────────┘│
│                                 │
│  ┌─ Time ─ Background ─ Content ─ Style ─┐  (标签栏)
│  │─────────────────────────────────────────│
│  │  ModalBottomSheet（高度自适应）          │
│  │  根据选中标签显示对应内容                 │
│  │  TIME(82%): 环形时间选择器              │
│  │  BG(58%):   背景预设+自定义图片          │
│  │  CONTENT(56%): 文本编辑列表             │
│  │  STYLE(36%): 预设样式+颜色+对齐         │
│  └─────────────────────────────────────────│
└─────────────────────────────────┘
```

#### 时间标签页（CircularTimeRangePicker）
- 24小时环形 Canvas 选择器
- 可拖拽的起始(S)和结束(E)手柄
- 步长 30 分钟
- 显示 "HH:MM - HH:MM" 格式

#### 内容标签页（ContentTabContent）
- LazyColumn 文本条目列表
- SwipeToDismiss 左滑删除
- 点击条目设为预览状态（高亮边框）
- TextField 实时编辑
- "+" 添加按钮

#### 样式标签页（StyleTabContent）
- 内置预设样式网格（带图标）
- 用户自定义样式列表
- 颜色调色板点选
- 左/中/右对齐按钮
- "Auto Match" 自动匹配按钮

### 5.4 自定义样式编辑器

```
┌─────────────────────────────────────┐
│  TopAppBar: "自定义样式"  [保存]      │
├─────────────────────────────────────┤
│  预览区域 (200dp)                    │
│  ┌─────────────────────────────────┐│
│  │   壁纸效果预览                    ││
│  └─────────────────────────────────┘│
├─────────────────────────────────────┤
│  ┌ Text ─ Border ─ Shadow ─ BG ─ Align ┐│
│  ├───────────────────────────────────────┤│
│  │  文本: 字体选择 / 粗体 / 斜体 / 字号 / ││
│  │        透明度 / 颜色                  ││
│  │                                       ││
│  │  描边: 宽度 / 颜色 / 透明度            ││
│  │                                       ││
│  │  阴影: 半径 / 距离 / 角度 / 颜色 /     ││
│  │        透明度                         ││
│  │                                       ││
│  │  背景: 颜色 / 透明度 / 圆角 / 内边距   ││
│  │                                       ││
│  │  对齐: 水平对齐 / 垂直对齐 / 竖排文本   ││
│  └───────────────────────────────────────┘│
├─────────────────────────────────────┤
│  样式名称: [____________]            │
│  [取消]                    [保存]     │
└─────────────────────────────────────┘
```

---

## 6. 核心流程

### 6.1 收藏集生命周期

```
创建收藏集
    │
    ├── 主页 [新建] → EditorScreen (collectionId = null)
    │       │
    │       ├── 编辑文本内容（添加/删除行）
    │       ├── 设置时间范围（环形选择器）
    │       ├── 选择背景（预设/自定义）
    │       ├── 调整样式（预设/自定义/AutoMatch）
    │       └── 拖动/旋转调整位置
    │       │
    │       ├── [返回] → 显示保存确认对话框
    │       │       ├── [保存] → 持久化到 Room → 返回主页
    │       │       └── [放弃] → 直接返回主页
    │       └── [×关闭] → EditorEvent.Finish → 返回主页
    │
    └── 主页 [卡片点击] → EditorScreen (collectionId = id)
            └── 从 Room 加载数据 → 填充 EditorUiState → 编辑保存
```

### 6.2 壁纸轮播流程

```
用户设置壁纸 → 系统绑定 MyWallpaperService
    │
    ├── onCreate → collectLatest(ObserveOrderedCollectionsUseCase)
    │       └── 收藏集变化 → replaceCollections → syncAndRender
    │
    ├── onVisibilityChanged(visible=true)
    │       ├── 不可见 > 30秒 → advanceAndRender（推进）
    │       └── 不可见 ≤ 30秒 → syncAndRender（仅同步）
    │
    ├── TriggerObserver → SCREEN_ON → advanceAndRender
    │
    └── syncAndRender 流程:
            ├── 获取当前分钟
            ├── sessionController.sync(minuteOfDay)
            │       ├── selectActiveCollections 筛选
            │       └── 确定当前 CollectionConfig + textIndex
            ├── loadBitmapIfNeeded (仅 Photo 背景)
            ├── renderCurrent → renderer.render(holder, state, bitmap, alpha)
            │       ├── drawBackground (Solid/Gradient/Photo)
            │       └── drawQuote → QuoteTextRenderer.drawText
            └── recycle old bitmap

        advanceAndRender 流程（推进到下一个文本/收藏集）:
            ├── sessionController.advance(minuteOfDay)
            │       ├── 无激活集合 → 清空
            │       ├── textIndex+1（当前集合还有文本）
            │       └── 轮转到下一个集合（当前集合文本已播完）
            ├── 判断 animate 标志
            └── [animate=true] → FadeTransitionController.runTransition
                    ├── 分步渐变 (12步, 每步24ms)
                    ├── onFrame: 更新 textAlpha → renderCurrent
                    └── onComplete: 更新最终状态
```

### 6.3 自动样式匹配流程

```
AutoStyleMatcher.match(currentStyle, targetPreset, backgroundSample)
    ├── 计算背景亮度 (luminance)
    ├── 判断暗色/亮色壁纸 (阈值 0.45)
    ├── 背景色 → HSL 分析
    ├── 有 bgColor:
    │       ├── 暗壁纸: 亮背景 + 暗文字
    │       └── 亮壁纸: 暗背景 + 白文字
    └── 无 bgColor:
            ├── 暗壁纸: 白文字 + 半透明黑阴影
            └── 亮壁纸: 深色文字 + 浅色阴影
```

---

## 7. 壁纸渲染引擎

### 7.1 渲染管线

```
WallpaperRenderer.render(holder, state, bitmap, textAlpha)
    │
    ├── holder.lockCanvas() → Canvas
    ├── drawBackground(canvas, background, bitmap)
    │       ├── Solid:  canvas.drawColor(parseAndroidColor(hex))
    │       ├── Gradient:  canvas.drawRect + LinearGradient shader
    │       │       └── 角度→方向向量→起始/结束色→shader
    │       └── Photo:  缩放 bitmap 居中 + dimAmount 叠加层
    │               └── bitmap=null → 黑色兜底
    ├── drawQuote(canvas, state, textAlpha)
    │       └── QuoteTextRenderer.drawText(...)
    │               ├── TextStyleConfig → QuoteRenderSpec（映射）
    │               ├── 配置 TextPaint（颜色/字号/字体/样式）
    │               ├── StaticLayout（自动换行）
    │               ├── 计算位置（对齐+padding+偏移+旋转）
    │               ├── 绘制背景矩形（圆角+边框）
    │               └── staticLayout.draw(canvas)
    └── holder.unlockCanvasAndPost(canvas)
```

### 7.2 字体系统

`FontResolver` 支持 5 种字体：

| 字体名 | Android Typeface | Compose FontFamily |
|--------|------------------|-------------------|
| `smiley_sans_oblique` | 从 R.font 加载 | — |
| `serif` | `Typeface.SERIF` | `FontFamily.Serif` |
| `sansserif` | `Typeface.SANS_SERIF` | `FontFamily.SansSerif` |
| `monospace` | `Typeface.MONOSPACE` | `FontFamily.Monospace` |
| `cursive` | `Typeface.DEFAULT` | `FontFamily.Cursive` |

---

## 8. 技术选型与决策

### 8.1 技术栈总览

| 分类 | 技术 | 版本 | 选型理由 |
|------|------|------|----------|
| 语言 | Kotlin | 1.9.0 | Android 官方推荐 |
| 构建 | Gradle | 8.7 | Kotlin DSL |
| UI | Jetpack Compose + Material 3 | 1.6.0 BOM | 声明式 UI，现代化开发体验 |
| 导航 | Navigation Compose | 2.7.7 | 类型安全路由 |
| DI | Dagger Hilt | 2.48 | Google 官方推荐 |
| 数据库 | Room | 2.6.1 | SQLite ORM，Flow 支持 |
| 序列化 | Kotlinx Serialization | 1.6.0 | Kotlin 原生，编译期安全 |
| 协程 | Kotlinx Coroutines | — | 异步编程标准 |
| 图片加载 | Coil | 2.5.0 | Compose 原生集成 |
| 调色板 | Palette KTX | 1.0.0 | Material 色彩提取 |

### 8.2 关键设计决策

1. **Sealed Class 用于模型多态**: `BackgroundSpec` 使用 Kotlin sealed class + `@SerialName` 实现对 Solid/Gradient/Photo 的序列化

2. **StateFlow + SharedFlow 模式**: ViewModel 使用 `MutableStateFlow` 管理持续性 UI 状态，`MutableSharedFlow` 发送一次性事件（如 Finish），实现 Clean UI 模式

3. **ThreadLocal<Paint> 优化**: `QuoteTextRenderer` 使用 ThreadLocal 复用 Paint 对象，避免频繁创建，提升渲染性能

4. **Mutex 渲染互斥**: 壁纸引擎使用 `Mutex` 串行化渲染操作，防止并发绘制

5. **颜色值防御式解析**: `ColorParsersKt` 和 `TextStyleConverter` 都有 try-catch 退路，确保单个字段损坏不会导致整体失败

6. **数据库渐进式迁移**: 从 v1 到 v7 的 6 次迁移保留了完整的 Schema 演进历史，v6→v7 从多列转为 JSON 列是重要架构优化

7. **Reacttive 数据流**: `ObserveOrderedCollectionsUseCase` 使主页和壁纸服务共享同一数据源，Room Flow 自动推送变化

### 8.3 权限设计

| 权限 | 用途 | 保护级别 |
|------|------|----------|
| `android.permission.READ_EXTERNAL_STORAGE` | 读取外部存储图片 | dangerous |
| `android.permission.READ_MEDIA_IMAGES` | 读取媒体图片 (API 33+) | dangerous |
| `android.permission.BIND_WALLPAPER` | 壁纸服务绑定 | signature |
| `com.example.wallquote.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | 内部广播保护 | signature |

### 8.4 服务组件

| 组件 | 类型 | 说明 |
|------|------|------|
| `com.example.wallquote.MainActivity` | Activity | 应用入口，launcher |
| `com.example.wallquote.service.MyWallpaperService` | WallpaperService | 动态壁纸核心 |
| `androidx.room.MultiInstanceInvalidationService` | Service | Room 多进程失效通知 |
| `androidx.startup.InitializationProvider` | ContentProvider | 启动初始化链 |

---

## 附录

### A. 预设背景选项

| 名称 | 类型 | 参数 |
|------|------|------|
| 纯黑 | Solid | `#000000` |
| 极光蓝 | Gradient | `#1A73E8` → `#4FC3F7` |
| 日出橙 | Gradient | `#FF6F00` → `#FFD54F` |
| 紫罗兰 | Gradient | `#7B1FA2` → `#CE93D8` |
| 暗夜紫 | Solid | `#2E3440`（默认） |

### B. 默认名言（首次启动）

1. Focus on the present.
2. Stay Hungry, Stay Foolish.
3. Discipline creates freedom.
4. Reduce anxiety. Increase action.
