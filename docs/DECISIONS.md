# 设计决策记录

## D-001 模块划分（2026-07-30）

- `:app` — Activity、Navigation、ViewModel、Compose UI、Hilt 入口。
- `:domain` — 纯 Kotlin 模型、仓库接口、用例；不依赖 Android Framework。
- `:data` — Room、Repository 实现、JSON 转换；仅依赖 `:domain`。
- `:core:preview` — 预览用渲染状态与 Compose 绘制（依赖 `:domain`）；后续壁纸 Canvas 渲染可复用规格映射。

依赖方向：`app` → `domain`, `data`, `core:preview`；`data` → `domain`；`core:preview` → `domain`。`data` 不依赖 `app` 或 `core:preview`。

## D-002 数据库 version 1 即最终结构（2026-07-30）

不实现历史 v1→v7 迁移。单版本 Schema 包含 `collections`、`collection_text_lines`。`textStyleData` 与 `backgroundData` 使用 kotlinx.serialization JSON。（`custom_styles` 表随自定义样式库一起于 2026-07-31 删除，见 D-027；「最近样式」改用 DataStore Preferences 而非 Room 表。）

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

## D-021 自定义样式：拷贝值而非外键（2026-07-31）— **已废弃，见 D-027**

- ~~`custom_styles` 表与 `CustomTextStyle` 仅作为**样式模板库**：`CustomStylesListScreen` / `CustomStyleEditorScreen` 增删改查独立于收藏集。~~
- ~~收藏集应用某个自定义样式时，**拷贝** `TextStyleConfig` 值写入 `collections.textStyleData`；不存储 `customStyleId` 外键。之后编辑收藏集样式或删除/修改该自定义样式**互不影响**（`CustomStylesListScreen` 删除确认文案明确提示「不影响已应用的收藏集」）。~~
- ~~`CreateStyleFromCollectionUseCase` 反向从收藏集当前样式创建新的自定义样式模板，同样是值拷贝。~~
- ~~校验与归一化统一在 `SaveCustomStyleUseCase` 内完成：去空格后名称非空、名称去重（按需排除自身 id）、`TextStyleNormalizer.validate` + `normalize`。~~
- 独立的自定义样式库（列表页 + 编辑器 + CRUD）已整体移除：详见 D-027。

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

## D-024 自定义样式名称唯一性：`normalizedName` 列（2026-07-31，P4-014）— **已废弃，见 D-027**

- ~~`custom_styles` 新增 `normalizedName: String` 列（写入时为 `name.trim().lowercase(Locale.ROOT)`），唯一索引建在 `normalizedName` 上；`name` 列本身改为非唯一，仅用于展示原始大小写。~~
- ~~应用尚未发布，Room 仍是 **version 1**：直接修改 `CustomStyleEntity` + ksp 重新生成 `data/schemas/.../1.json`，不新增迁移（沿用 D-020 的 `fallbackToDestructiveMigration`）。~~
- ~~`CustomStyleDao.countByNormalizedName` 与 `CustomStyleRepositoryImpl.existsName` 均按 `normalizedName` 比较，替代此前不一致的 `lower(name)` SQL 表达式判断 + 大小写敏感索引的组合。~~
- ~~`CustomStyleDao.reorder(orderedIds)` 改为 Kotlin 接口的 `@Transaction` 默认方法（内部循环调用 `updateSortOrder`），保证批量重排是单一事务；`CustomStyleRepositoryImpl.reorder` 直接委托给它，不再自行循环。~~
- `custom_styles` 表已随整个自定义样式库一起删除：详见 D-027。

## D-025 `BackgroundImageLoader.load()` 诊断改为按调用传入（2026-07-31，P4-017）

- `DefaultBackgroundImageLoader` 是 `@Singleton`（与共享 Bitmap 缓存/处理器绑定），但診断（`WallpaperDiagnostics`）是每个壁纸 Engine 实例私有的；此前用可变 `var diagnostics` 字段由 `WallQuoteWallpaperService.onCreate` 赋值，多个 Engine（或 Engine + `HomeViewModel` 缩略图加载）共享同一 Loader 时会互相覆盖。
- 改为 `load()` 增加 `diagnostics: WallpaperDiagnostics? = null` 形参，由调用方（`WallpaperCoordinator` 持有自己的 `diagnostics`）逐次传入；不需要诊断的调用方（如 `HomeViewModel` 缩略图）使用默认 `null`。移除了 Loader 上的可变字段。

## D-026 编辑器变换（Transform）统一裁剪路径（2026-07-31，P4-013 跟进）

- `EditorViewModel.updateTransformRequested(rawTransform, provisionalTextHeightPx, density)` 是手势拖拽与「调整布局」滑块共用的唯一变换更新入口，内部调用 `QuoteBlockLayoutCalculator.clampTransform`。此前仅手势路径做旋转包围盒裁剪，滑块路径直接调用 `updateTransform`，只受 Slider 自身 `0..1` / `-180..180` 值域限制，旋转后的文字块仍可被拖出大半屏幕。
- 视口尺寸通过 `EditorUiState.previewViewportWidthPx/HeightPx`（由 `EditorScreen` 的 `Modifier.onSizeChanged` 报告给 `EditorViewModel.setPreviewViewportSize`）传递，属于纯 UI 布局状态，不计入 `EditorDraft`/脏检查。
- 视口尚未测量时（宽高为 0）回退为仅 `QuoteTransformNormalizer.normalize`，不做裁剪。

## D-027 移除自定义样式库，改为 DataStore 记忆「最近样式」（2026-07-31，取代 D-021 / D-024）

- **动机**：独立的自定义样式列表/编辑器页（`CustomStylesListScreen`、`CustomStyleEditorScreen` 及其 ViewModel）与其 CRUD 是一套与收藏集编辑器平行的样式管理体系，但用户已经可以在收藏集编辑器内直接编辑样式并实时预览；独立样式库只增加维护成本（DAO、Repository、UseCases、导航路由、两块 UI、专项测试），没有对应的产品价值，故整体删除。
- **删除范围**：`ui/style/CustomStylesListScreen.kt`、`CustomStylesListViewModel.kt`、`CustomStyleEditorScreen.kt`、`CustomStyleEditorViewModel.kt`；`domain/model/CustomTextStyle.kt`、`domain/repository/CustomStyleRepository.kt`、`domain/usecase/CustomStyleUseCases.kt`（`ObserveCustomStylesUseCase`/`GetCustomStyleUseCase`/`SaveCustomStyleUseCase`/`DeleteCustomStyleUseCase`/`ReorderCustomStylesUseCase`/`CreateStyleFromCollectionUseCase`）；`data/local/CustomStyleDao.kt`、`CustomStyleEntity`（`Entities.kt`）、`CustomStyleRepositoryImpl.kt`；对应测试 `CustomStyleDaoTest`、`CustomStyleEditorViewModelTest`；`WallQuoteNavHost` 的 `CUSTOM_STYLES` / `CUSTOM_STYLE_EDITOR` 路由；`HomeScreen`/`EditorScreen` 的入口按钮与「另存为」「管理」「应用自定义样式」UI。`StyleEditingControls.kt`（Text/Block/Border/Shadow 分区滑块，收藏集编辑器与已删除的自定义样式编辑器共用）保留，因为收藏集编辑器仍需要它。
- **Room**：`AppDatabase` 的 `entities` 只剩 `CollectionEntity` + `CollectionTextLineEntity`，移除 `customStyleDao()`；应用尚未发布，Schema 仍是 **version 1**，直接改 `@Database` 注解 + ksp 重新生成 `data/schemas/.../1.json`（不再含 `custom_styles` 表），沿用 D-020 的 `fallbackToDestructiveMigration`。
- **替代方案 —「最近使用的文字样式」**：新增 `domain.repository.RecentTextStyleRepository`（`get(): TextStyleConfig?` / `save(style)` / `clear()`），由 `data.repository.DataStorePreferencesRecentTextStyleRepository` 实现，用 Jetpack DataStore Preferences（`androidx.datastore:datastore-preferences`）持久化单个 `recent_text_style_json` 字符串键，复用既有 `JsonMappers`/`kotlinx.serialization`（`TextStyleSanitizer` + `TextStyleConfig` 的 `@Serializable`）编解码，格式与 `collections.textStyleData` 一致。JSON 损坏时 `get()` 返回 `null` 而非静默回退默认值。
- **收藏集行为**：
  - 新建收藏集：初始样式取 `recent.get() ?: TextStyleConfig()`（`EditorViewModel.buildNewEditorState`），即继承最近一次成功保存的样式；没有历史记录则用内置默认值。
  - 编辑既有收藏集：始终使用该收藏集自身持久化的 `textStyle`，**永不**被「最近样式」覆盖（`loadExisting` 路径不读取 `RecentTextStyleRepository`）。
  - 保存成功后：`saveAndFinish()` 用 `runCatching { recentTextStyleRepository.save(config.textStyle) }` 更新「最近样式」；DataStore 写入失败不影响已经提交成功的收藏集保存流程（best-effort，失败静默吞掉）。
  - 用户主动放弃（`discardAndFinish`）或返回时选择放弃修改，都不调用 `save`，「最近样式」保持不变。
- **UI**：样式 Tab 保留内置预设网格（`BuiltInTextStylePresets`，视觉卡片）与 Auto Match；新增两个 `FilterChip`：「使用最近样式」（`EditorViewModel.applyRecentStyle`，读取当前 `RecentTextStyleRepository.get()` 并整体应用到当前编辑草稿）与「恢复默认样式」（`EditorViewModel.applyDefaultStyle`，应用 `TextStyleConfig()`）。两者都只影响当前编辑草稿的 `textStyle`，不会立即写回 DataStore（写回仍只发生在保存成功时）。**Phase 5 起编辑器 Style UI 已改为全屏 Draft + MRU 行，见 D-028；本段 FilterChip 描述作废，DataStore 契约以 D-028 为准。**
- **依赖**：`gradle/libs.versions.toml` 新增 `datastorePreferences = "1.1.7"` 与 `androidx-datastore-preferences` 库坐标；`data/build.gradle.kts` 增加 `implementation(libs.androidx.datastore.preferences)`。

## D-028 Phase 5 编辑器壳层与首页（2026-08-15）

- **范围**：只改 UI/交互。不改 domain 模型、`CollectionRepository`、`BackgroundAssetStore`、`QuotePreview`/`CanvasWallpaperRenderer` 绘制语义、`WallpaperCoordinator`、时间数学、背景图流水线、Auto Match domain。
- **编辑器壳层**：`QuotePreview` 全屏；顶部关闭；`selectedPanel: EditorPanel?`。未展开时只显示 `EditorBottomDock`（时间/背景/内容/样式）；展开时只显示 `EditorBottomPanel`（含 Handle + `EditorPanel.title`），Dock 隐藏。二者互斥，避免 Panel（约屏高 58%）再叠一层 Dock 把 Preview 多吃一块。Style 不是 Bottom Panel：进入后 `startStyleDraft()`，全屏 Draft（× 丢弃 / ✓ 提交）。
- **Preview 手势**：删除 `layoutAdjustEnabled` 模式；Preview 始终 `detectTransformGestures`（拖动/旋转）。
- **Style Draft**：`styleDraft.workingStyle` 只驱动 Preview，不改正式 `textStyle`；✓ 才写入正式样式并 `RecentTextStyleRepository.save`；× 丢弃。最近使用是最多 6 条 MRU（自动去重、最近排前），无名字、无 CRUD、无 + NEW。DataStore 键 `recent_text_styles_json`，兼容旧单条 `recent_text_style_json` 并迁移。
- **首页**：横向 Collection Card；溢出菜单重命名走 `RenameCollectionUseCase`（读原 Collection、只改 name、upsert），不改 Schema。收藏集名称仍在 DB，编辑器 chrome 不展示；默认名「新收藏集」。归档仍 deferred。
- **状态**：Feature Complete / Acceptance Fixes Pending；验收修复见 Phase 5.1 与 D-029、D-030。

## D-029 Auto Match 以 Draft working style 为 baseline（2026-08-15，P5-001）

- `EditorUiState.autoMatchBaselineStyle` = `styleDraft?.workingStyle ?: textStyle`。`requestAutoMatch()` 用该值采样/建议，完成时 stale 检查比较的也是它，而不是正式 `textStyle`。
- `previewTextStyle` 仍优先 `autoMatchSuggestion`，因此过期 suggestion 会盖住正在编辑的 working style，甚至在离开 Style 后泄漏到主编辑器 Preview。
- 因此 `startStyleDraft`、`updateWorkingStyle`、`applyWorkingStyle`、`confirmStyleDraft`、`discardStyleDraft`、`updateTextStyle`、`applyTextStyle` 以及背景变更路径都必须 `invalidateAutoMatch()`（bump `autoMatchGeneration` 并清 suggestion/loading）。
- `confirmAutoMatch` 在存在 draft 时只写 `workingStyle`，正式 `textStyle` 仍等 ✓。

## D-030 统一 ColorPickerRow（2026-08-15，P5-003）

- 公共颜色行不再只有固定 Hex 色块。`ColorPickerRow` 布局为 `[取消?] [色盘] | [预设…]`；色盘打开 HSV + Hex `ColorPickerDialog`（不引入第三方取色库）。
- 语义：
  - Text：不能取消文字颜色，只提供色盘与预设。
  - Border 取消：`blockBorderWidthDp = 0`、`blockBorderColorHex = null`。
  - Shadow 取消：`shadowAlpha = 0`。
  - Block 取消：`blockColorHex = null`、`blockAlpha = 0`。
  - Custom Background 纯色/渐变：无取消（必须有颜色），仍可走色盘。

