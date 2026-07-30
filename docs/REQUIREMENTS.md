# WallQuote 需求分级

> 依据 `docs/archive/LEGACY_SOFTWARE_DESIGN.md`（考古）整理。类名与模块名仅作职责参考，不要求复刻原实现。

## P0 首版需求

### 产品与平台

- Android 动态壁纸应用「壁上言」（WallQuote），包名 `com.example.wallquote`。
- minSdk 26，targetSdk 34，**compileSdk 36**（依赖库要求；运行时目标仍为 34），versionCode 1，versionName 1.0。

### 架构与质量

- Clean Architecture 依赖边界（UI → Domain ← Data；壁纸/预览核心可共享 Domain 模型）。
- MVVM + Jetpack Compose + Material 3。
- Hilt 依赖注入。
- Room 数据库从 **version 1** 起，采用最终版表结构（无历史迁移）。
- Domain 与数据库层单元测试；每阶段可构建、可测试。

### 数据与 CRUD（第一阶段）

- 收藏集（collections）：创建、读取、更新、删除；排序字段 `sortOrder`。
- 名言行（collection_text_lines）：按收藏集增删改查，`displayOrder`。
- 持久化字段覆盖：名称、日时间范围、背景规格（第一阶段仅使用纯色）、文本样式 JSON、偏移/旋转、文本行内容。

### UI（第一阶段）

- Compose 主页：收藏集列表、新建、进入编辑、删除（含确认）。
- 基础编辑器：预览区、时间/背景/内容/样式分区（可用简化控件，非占位 UI）。
- 纯色背景选择与预览。
- 基础文字样式：颜色、字号、粗体/斜体、水平对齐（系统字体族：Serif/SansSerif/Monospace 等，不引入未知许可证字体资源）。
- 可复用的预览渲染状态（Compose 预览与后续壁纸渲染共享同一套状态/规格映射）。

### 领域逻辑（第一阶段范围）

- `DailyTimeRange`：全天、跨午夜、`contains(minuteOfDay)`。
- 观察有序收藏集列表（Flow）。
- 按时间筛选激活收藏集（为后续壁纸做准备，第一阶段可测、可不接壁纸服务）。

### 动态壁纸（P0 产品目标，分阶段交付）

- 作为 Android `WallpaperService` 运行、轮播、屏幕事件、淡入淡出等属 **后续阶段**；第一阶段 **不实现** 完整轮播与 `MyWallpaperService` 行为（见实施计划阶段划分）。

---

## P1 增强需求

- ~~渐变背景（`BackgroundSpec.Gradient`）。~~ **Phase 3 已实现**
- ~~自定义图片背景（`BackgroundSpec.Photo`）、Coil 加载、dim/blur。~~ **Phase 3 已实现**（私有 assetId，非外部 URI）
- 预设背景与默认名言首次启动种子数据。
- ~~预设文字样式网格与用户自定义样式 CRUD（`custom_styles` 表 UI）。~~ **Phase 4 已实现**
- ~~自定义样式全参数编辑器（边框、阴影、文字背景块）。~~ **Phase 4 已实现**（竖排未覆盖，见下）
- 编辑器内拖动/旋转手势变换（归一化 `QuoteTransform`）。
- ~~环形时间选择器（30 分钟步长）。~~ **Phase 4 已实现**（`CircularTimePicker`）
- ~~主页卡片时间轴可视化、SwipeToDismiss。~~ **Phase 4 已实现**
- ~~「设为壁纸」入口与壁纸选择器集成。~~ **Phase 2 已实现**
- ~~`AutoStyleMatcher` / `BackgroundSampler`。~~ **Phase 4 已实现**
- ~~壁纸淡入淡出动画（`FadeTransitionController`）。~~ **Phase 4 已实现**（`WallpaperTransitionDriver`，288ms）
- 竖排文字样式（自定义样式编辑器暂未覆盖）。
- `FontResolver` 含应用内打包字体（若许可证明确）。
- Palette KTX 色彩提取（`BackgroundSampler` 目前用自研采样，未引入 Palette 库）。
- 存储权限（API 33+ `READ_MEDIA_IMAGES` 等）。

---

## Deferred 暂缓需求

- 原应用 v1→v7 数据库迁移链（不存在旧安装用户，不实现）。
- 反编译时代的类名/包结构一比一复刻。
- `TextStyleConverter` 对 `org.json.JSONObject` 的遗留解码回退（可选：仅在新库用 kotlinx.serialization，损坏 JSON 回退默认值）。
- `ThreadLocal<Paint>` 等微优化（实现壁纸引擎时再引入）。
- `MultiInstanceInvalidationService` 多进程 Room（单进程应用可暂缓，待壁纸进程策略确定再加）。

---

## Open Questions 待确定事项

| ID | 问题 | 第一阶段决策（见 `DECISIONS.md`） |
|----|------|----------------------------------|
| OQ-1 | 收藏集 `name` 在 ER 图中有字段，但 `CollectionConfig` 示例无 `name` | 采用 ER：`collections.name` 必填，Domain 模型含 `name` |
| OQ-2 | 新收藏集无 `collectionId` 时返回是否弹保存对话框 | 第一阶段：显式保存按钮 + 返回时若有未保存更改则确认 |
| OQ-3 | 默认背景「暗夜紫 `#2E3440`」与编辑器初始状态 | 新收藏集默认 Solid `#2E3440` |
| OQ-4 | `custom_styles` 表是否在第一阶段建表但不提供 UI | 是：Schema v1 含表，UI/CRUD 延至 P1 |
| OQ-5 | 渐变/图片在 Schema 中如何存 | `backgroundType` + `backgroundData` JSON，与 sealed `BackgroundSpec` 一致；阶段一只读写 Solid |
| OQ-6 | 主页「设为壁纸」是否在阶段一 | 否：无完整壁纸服务时不提供无效按钮 |
| OQ-7 | 时间编辑 UI 无环形选择器时的交互 | 阶段一：开始/结束时间用 Material 时间输入或滑块（分钟精度），仍持久化 `startMinuteOfDay`/`endMinuteOfDay` |
