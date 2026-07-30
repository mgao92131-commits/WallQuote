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

## D-018 Phase 2.1 验收修复（2026-07-30）

- 编辑器未保存行使用**负数** `clientKey`，已保存行使用正数 DB id，避免碰撞。
- Room **version 2**：`offsetX/Y` 重命名为 `centerXFraction/centerYFraction`，迁移时一律重置为 `0.5`（Phase 1 的 `0` 表示居中，不能按分数语义直读）。
- Compose 预览使用与 Canvas 相同的**固定文字宽度**（`surfaceWidth × 0.84`）。
- Canvas 字号使用 `density * fontScale`（与 Compose `sp` 一致，避免废弃的 `scaledDensity` 字段）。
- 隐藏时长用 `elapsedRealtime`；日程边界用墙钟秒级精度；监听 `TIME_CHANGED` / `TIMEZONE_CHANGED` / `DATE_CHANGED`。
- `@Update` 校验影响行数；名言行 UPDATE 带 `collectionId` 归属条件。
- `WallpaperCoordinator.close()` 同步销毁，Service 不再依赖异步 Destroy 事件。

## D-019 Phase 3 背景资产（2026-07-30）

- `BackgroundSpec.Photo` 使用内部 `assetId`，**不**持久化外部 `content://` URI；不申请 `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`。
- 正式文件在 `files/backgrounds/`；编辑临时文件在 `cache/background_staging/`。
- 本阶段**不**建 `background_assets` 表；元数据由文件系统读取。
- 渐变角度：0° 左→右，顺时针增加；Compose / Canvas 共用 `GradientGeometryCalculator`。
- Center Crop / Dim 共用计算器；Dim 不进缓存 Key；Blur（0–25dp）在解码后异步处理并纳入缓存 Key。
- 壁纸图片经 `BackgroundImageLoader` 异步加载，`BackgroundLoadToken` 校验防过期覆盖。

## D-020 Phase 3.1 未发布 Schema 与保存协议（2026-07-30）

- 应用尚未正式发布。Room 最终 Schema 为 **version 1**（字段直接为 `centerXFraction` / `centerYFraction`）；删除开发期迁移与旧 JSON 字段别名。
- 开发阶段修改 Schema 后需卸载 APK 或清除应用数据；首个正式版本发布后才维护真实迁移。当前使用 `fallbackToDestructiveMigration`。
- 图片保存：`prepareFormalAsset()` 生成正式文件且**保留 staging** → Room 成功 → 删除 staging → 尽力删除旧正式文件。Room 失败则删除新正式文件并保留 staging，可直接重试。
- 旧资产删除失败只记诊断，不回滚已成功的 Room 写入，也不删除新正式文件。
- 正式 `assetId` 格式 `bg_[0-9a-f]{32}`；staging `draft_[uuid]`；解析时校验 canonical path。
- 编辑器与壁纸共用 `BackgroundImageProcessor`；共享 LRU；`onTrimMemory` 真正 trim/evict。

## D-021 自定义样式：拷贝值而非外键（2026-07-31）

- `custom_styles` 表与 `CustomTextStyle` 仅作为**样式模板库**：`CustomStylesListScreen` / `CustomStyleEditorScreen` 增删改查独立于收藏集。
- 收藏集应用某个自定义样式时，**拷贝** `TextStyleConfig` 值写入 `collections.textStyleData`；不存储 `customStyleId` 外键。之后编辑收藏集样式或删除/修改该自定义样式**互不影响**（`CustomStylesListScreen` 删除确认文案明确提示「不影响已应用的收藏集」）。
- `CreateStyleFromCollectionUseCase` 反向从收藏集当前样式创建新的自定义样式模板，同样是值拷贝。
- 校验与归一化统一在 `SaveCustomStyleUseCase` 内完成：去空格后名称非空、名称去重（按需排除自身 id）、`TextStyleNormalizer.validate` + `normalize`。

## D-022 壁纸推进淡入淡出（288ms）规则（2026-07-31）

- `WallpaperTransitionDriver`：0–144ms 目标透明度 alpha 1→0（渐隐旧内容），144ms 时刻切换后台渲染目标为新游标，144–288ms alpha 0→1（渐显新内容）；帧回调节流 ~16ms。
- **仅**在“推进”路径播放动画：`onVisibilityChanged` 命中隐藏 ≥30s 阈值的 advance。Room 数据同步（`syncOnly`）、日程边界（`ScheduleBoundaryReached`）、Surface 尺寸变化、时间/时区变化、首次创建 **均不**触发动画，只做同步 `sync` + 立即重绘。
- 过渡期间渲染使用 `WallpaperRenderSpec.transitionTextAlpha` 叠乘文字透明度；旧背景保持显示直到过渡中点（144ms）才切到目标游标的背景。
- 取消规则：
  - Surface 销毁：取消动画，不采纳目标（`adoptTarget = false`），因为即将整体重建。
  - 隐藏（`visible = false`）：取消动画但**采纳目标游标**的背景状态，保持指针在目标处，避免下次可见时重新决定。
  - 收藏集数据变化：取消动画（采纳目标）后立即基于新数据 `sync` + 重绘，不残留旧过渡状态。
  - 新的推进请求（理论上的连续 advance）：取消当前动画（采纳目标）后从当前指针重新开始一次新的 288ms 过渡。
  - 日程边界：取消动画（采纳目标）后仅 `sync`，不 `advance`。
- 目标为图片背景时，过渡开始前等待最多 300ms 尝试命中/解码缓存；超时则先以占位（沿用当前渲染）播放动画，动画完成后交由常规后台加载流程补齐图片。
- 测试使用 `FakeTransitionDriver`（记录 start/cancel 调用次数，由测试手动驱动帧），避免在 JVM 单元测试中触碰真实 `Handler`/`Looper`。

## D-023 `TextStyleConfig` 最终形状（2026-07-31）

`TextStyleConfig` 冻结为以下分组字段（`domain/model/TextStyleConfig.kt`），编辑器与自定义样式编辑器共用同一模型，不再变更结构：

- 文字：`colorHex`、`textAlpha`、`textSizeSp`、`fontFamily`（`SystemFontFamily` 系统字体枚举）、`isBold`、`isItalic`、`horizontalAlignment`、`letterSpacingEm`、`lineHeightMultiplier`。
- 文字背景块：`blockColorHex`（null 表示不显示）、`blockAlpha`、`blockPaddingDp`、`blockCornerRadiusDp`。
- 边框：`blockBorderWidthDp`、`blockBorderColorHex`、`blockBorderAlpha`（复用文字背景块的圆角）。
- 阴影：`shadowRadiusDp`、`shadowDistanceDp`、`shadowAngleDegrees`（0°=右，顺时针增加）、`shadowColorHex`、`shadowAlpha`。
- 不含竖排文字字段（P1 「竖排」需求本阶段未实现，见 REQUIREMENTS 遗留项）。
- `TextStyleNormalizer` 负责越界裁剪、非有限值回退默认值、透明度归一化到 `[0,1]`；`AutoStyleMatcher` 只调整颜色/阴影/背景块相关字段，不触碰字号、字重、对齐、字距、行高。
