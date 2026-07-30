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

## D-007 主页「设为壁纸」（2026-07-30）

第一阶段不展示（避免无 `WallpaperService` 的无效操作）。

## D-008 测试策略（2026-07-30）

- `domain`：JUnit，`DailyTimeRange`、`SelectActiveCollectionsUseCase` 等。
- `data`：Room in-memory，`CollectionDao` CRUD 与级联删除。

## D-009 默认样式与背景（2026-07-30）

新收藏集：`name` 默认「新收藏集」；背景 Solid `#2E3440`；`TextStyleConfig` 文档默认值（白字、32sp、居中）。

## D-010 compileSdk 与 targetSdk（2026-07-30）

产品文档写明 targetSdk 34。AndroidX 依赖要求 **compileSdk 36**，故各模块 `compileSdk = 36`，`targetSdk = 34`。
