# 设计决策记录

## D-001 模块划分（2026-07-30）

- `:app` — Activity、Navigation、ViewModel、Compose UI、Hilt 入口。
- `:domain` — 纯 Kotlin 模型、仓库接口、用例；不依赖 Android Framework。
- `:data` — Room、Repository 实现、JSON 转换；仅依赖 `:domain`。
- `:core:preview` — 预览用渲染状态与 Compose 绘制（依赖 `:domain`）；后续壁纸 Canvas 渲染可复用规格映射。

依赖方向：`app` → `domain`, `data`, `core:preview`；`data` → `domain`；`core:preview` → `domain`。`data` 不依赖 `app` 或 `core:preview`。

## D-002 数据库 version 1 即最终结构（2026-07-30）

不实现历史 v1→v7 迁移。单版本 Schema 包含 `collections`、`collection_text_lines`、`custom_styles`。`textStyleData` 与 `backgroundData` 使用 kotlinx.serialization JSON。

## D-003 收藏集名称（2026-07-30）

采纳 ER 图：`collections.name` NOT NULL，Domain `Collection` 含 `name`。列表卡片展示名称。

## D-004 第一阶段背景能力（2026-07-30）

`BackgroundSpec` 密封类保留 Solid/Gradient/Photo 序列化形状；UI 与壁纸预览仅启用 **Solid**。Gradient/Photo 的编辑与加载属 P1。

## D-005 第一阶段时间 UI（2026-07-30）

不实现环形时间选择器。使用开始/结束「时:分」选择与 30 分钟对齐可选；逻辑仍使用 `DailyTimeRange`。

## D-006 字体（2026-07-30）

不下载外部字体。使用 Android 系统 `Typeface`（Serif、SansSerif、Monospace、Default/Cursive）。不打包 `smiley_sans_oblique` 直至有明确许可证资源。

## D-007 主页「设为壁纸」（2026-07-30，Phase 2 更新）

Phase 1 不展示。Phase 2 起展示「设为壁纸」：优先 `ACTION_CHANGE_LIVE_WALLPAPER`，回退 `ACTION_LIVE_WALLPAPER_CHOOSER`；失败时明确提示。无收藏集时先确认再进入系统选择器。

## D-008 测试策略（2026-07-30）

- `domain`：JUnit，`DailyTimeRange`、`SelectActiveCollectionsUseCase` 等。
- `data`：Room in-memory，`CollectionDao` CRUD 与级联删除。

## D-009 默认样式与背景（2026-07-30）

新收藏集：`name` 默认「新收藏集」；背景 Solid `#2E3440`；`TextStyleConfig` 文档默认值（白字、32sp、居中）。

## D-010 compileSdk 与 targetSdk（2026-07-30）

产品文档写明 targetSdk 34。AndroidX 依赖要求 **compileSdk 36**，故各模块 `compileSdk = 36`，`targetSdk = 34`。

## D-011 Phase 1.1 数据与身份（2026-07-30）

- 收藏集与名言行通过 `CollectionDao.saveCollectionWithLines` **单事务**保存；父表使用 `@Insert` / `@Update`，禁止 `@Insert(REPLACE)`。
- Domain 使用 `QuoteLine(id, text, displayOrder)`；更新时保留已有行 ID，删除仅针对从编辑集中移除的行。
- 壁纸游标预留 `PlaybackCursor(collectionId, quoteLineId)`。

## D-012 共享渲染规格（2026-07-30）

- **`QuoteRenderInput` / `QuoteTransform`** 放在 `:domain`（纯 Kotlin，归一化中心 0–1）。
- `:core:preview` 仅负责 Compose 绘制；Canvas 壁纸阶段依赖 `:domain` 规格，不反向依赖 Compose。

## D-013 编辑器脏状态（2026-07-30）

- 使用 `EditorDraft` 快照；新建与编辑已有收藏集共用 `isDirty` 规则。
- 系统返回与顶栏返回均走 `requestClose()`；`BackHandler` 拦截手势/返回键。

## D-014 权威文档（2026-07-30）

- 实现规格：`REQUIREMENTS.md`、`DECISIONS.md`、`IMPLEMENTATION_PLAN.md`。
- `docs/archive/LEGACY_SOFTWARE_DESIGN.md` 仅为考古参考，不要求兼容旧迁移或反编译结构。

## D-015 壁纸推进与 SCREEN_ON（2026-07-30）

不注册 `SCREEN_ON` 广播。仅由 `onVisibilityChanged(true)` 在隐藏 ≥30s 时推进一次。若将来注入 `WallpaperEvent.ScreenOn`，也只同步重绘、不调用 `advance()`，避免与可见性回调重复推进。

## D-016 空壁纸提示（2026-07-30）

无收藏集或无有效游标时：绘制默认纯色 `#2E3440`；无收藏集时显示短提示「打开「壁上言」添加内容」。当前时间无有效收藏集时不回退到无效集合（计划语义优先）。

## D-017 Phase 2 模块边界（2026-07-30）

播放决策在 `:domain`（`PlaybackController` / `PlaybackReconciler` / `ScheduleBoundaryCalculator`）。`WallpaperCoordinator` 单 Channel 事件循环；`CanvasWallpaperRenderer` 只绘制。不新增 Gradle 模块。`QuoteLayoutCalculator` 为 Compose 与 Canvas 共享布局规则。
