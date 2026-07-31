# WallQuote 实施计划

## 状态

| 标记 | 含义 |
|------|------|
| **Phase 1.1 complete** | 事务保存、行 ID、脏检查、BackHandler、RenderSpec |
| **Phase 2 Complete** | WallpaperService + 播放闭环 + 2.1 验收修复 |
| **Phase 3 Complete** | 背景系统收口：Schema 重置、可重试保存、Blur 内存、共享处理 |
| **Phase 4 Feature Complete** | 样式系统、自定义样式、编辑交互、Auto Match、壁纸淡入淡出 |
| **Phase 4.1 Acceptance Fixes Complete** | P4-001..P4-018 全部完成；P4-019 本地回归已补齐（含 `EditorViewModelAutoMatchTest`）；GitHub Actions 绿勾与 API 26/31/34 手工验证仍为 RC1 前置条件 |

## 阶段总览

| 阶段 | 目标 | 验收 |
|------|------|------|
| **1 / 1.1** | 工程骨架 + 可靠性修复 | assemble / tests / lint |
| **2 / 2.1** | 动态壁纸最小闭环 + 验收修复 | 可设壁纸；P0/P1 审查项关闭 |
| **3 / 3.1** | 渐变/图片背景 + 验收收口 | 保存可重试；共享处理；内存有界 |
| **4** | 自定义样式、预设、Auto Match、动画 | 对齐 P1 需求；assemble / tests / lint |

---

## Phase 3.1 任务

- [x] Room 重置为最终 version 1；删除迁移与旧 JSON 别名
- [x] `prepareFormalAsset` + 可重试保存 + `SaveCollectionResult`
- [x] 编辑器 staging / generation / 保存后正式 ID
- [x] asset ID / 路径逃逸 / 字节与像素上限
- [x] 有界 Blur、缓存 Key、OOM、CancellationException、请求去重
- [x] 共享 `BackgroundImageProcessor`（预览与壁纸）
- [x] 共享 LRU + Application `onTrimMemory`
- [x] 失败路径测试 + 文档

---

## Phase 4 任务

### A. 自定义样式 UI + 导航
- [x] `TextStyleConfig` / `TextStyleNormalizer` / `QuoteBlockLayoutCalculator` / `BuiltInTextStylePresets` / `AutoStyleMatcher`
- [x] `CustomStyle` DAO / Repository / UseCases（Observe/Get/Save/Delete/Reorder/CreateFromCollection）
- [x] `CustomStylesListScreen`（观察列表、删除确认「不影响已应用收藏集」、进入编辑/新建）
- [x] `CustomStyleEditorScreen`（名称 + Text/Block/Border/Shadow/Align 分区 + `QuotePreview` 中性底色示例文字 + 保存）
- [x] `WallQuoteNavHost` 路由接入；主页顶栏「自定义样式」入口
- [x] 编辑器「另存为自定义样式」（`CreateStyleFromCollectionUseCase`）；布局模式支持单指拖动与双指旋转
- [x] `EditorScreen.StyleTabContent` 改用 `StyleEditingControls.kt` 的 `StyleSection` / `StyleSectionContent`，删除了自身重复的 `TextStyle*Section` 实现（P4-018）

### B. 主页 UX
- [x] `CollectionCard` 展示名言数量、按 `DailyTimeRange` + 当前 `minuteOfDay` 判断的启用状态
- [x] `ScheduleTimelineGeometry`（domain 纯 Kotlin：全天 / 单段 / 跨零点两段）+ `ScheduleTimelineBar` 可视化
- [x] `SwipeToDismissBox` 滑动显示删除图标；松手仅弹确认对话框，取消复位、确认才调用删除用例（不会滑动阈值即删除）
- [x] 保留「设为壁纸」入口与既有跳转逻辑

### C. 壁纸淡入淡出（288ms）
- [x] `TransitionDriver` 契约 + `WallpaperTransitionDriver`（Handler 驱动，144ms 渐隐切换 + 144ms 渐显）
- [x] `WallpaperRenderSpec.transitionTextAlpha` 字段
- [x] `WallpaperCoordinator` 接入：仅长隐藏推进路径播放动画；Room 同步/日程边界/尺寸变化/时间时区/首次创建均只同步不动画
- [x] 取消规则：Surface 销毁（不采纳）、隐藏（采纳目标）、收藏集变化（采纳目标后立即同步重绘）、日程边界（采纳目标后仅同步）、新推进请求（采纳目标后重新开始）
- [x] 图片目标等待 300ms 加载/缓存命中，超时先以占位播放动画，动画完成后由常规后台加载流程补齐
- [x] `WallpaperCoordinatorTest`：新增同步路径不触发动画、推进触发动画并可完成、四类取消场景（隐藏/销毁/数据变化/日程边界）共 6 个测试，全部通过

### D. 测试与文档
- [x] Domain 测试：`CircularTimeGeometryTest`、`AutoStyleMatcherTest`、`QuoteBlockLayoutCalculatorTest`、`TextStyleNormalizerTest`、`ScheduleTimelineGeometryTest`
- [x] `DECISIONS.md`：D-021（自定义样式拷贝值非外键）、D-022（淡入淡出规则）、D-023（`TextStyleConfig` 最终形状）
- [x] `REQUIREMENTS.md` P1 复核（见下方勾选）；`Palette KTX` 与「竖排文字」仍为未实现项，保留在 P1 列表

### Phase 4 关闭的 P1 需求项
- [x] 预设文字样式网格与用户自定义样式 CRUD（`custom_styles` 表 UI）
- [x] 自定义样式全参数编辑器（边框、阴影、文字背景块）— 竖排文字样式未覆盖（P1 遗留项，见下方列表）
- [x] 环形时间选择器（`CircularTimePicker`，30 分钟步长）
- [x] 主页卡片时间轴可视化、`SwipeToDismiss`
- [x] `AutoStyleMatcher` / `BackgroundSampler`
- [x] 壁纸淡入淡出动画（`WallpaperTransitionDriver`，288ms）
- [ ] `Palette KTX` 色彩提取（`BackgroundSampler` 目前用自研平均色/对比度采样，未引入 Palette 库）
- [ ] 竖排文字样式
- [ ] `FontResolver` 应用内打包字体（无明确许可证，仍 deferred）
- [ ] API 33+ 存储权限（Photo 背景走内部 assetId，本阶段无需申请）

---

## Phase 4.1 任务（验收修复，不新增产品功能）

Phase 4.1 不引入新功能，只修复 Phase 4 验收中发现的功能错误、状态管理问题、Compose/Canvas 不一致，并补齐自动化测试。条目按发现顺序编号 P4-001..P4-019；全部 P0/P1/P2 条目已完成，剩余仅 P4-019 的测试覆盖广度为「部分完成」。

| ID | 摘要 | 状态 |
|----|------|------|
| P4-001 | 双指旋转角度被 `Math.toDegrees` 二次转换错误放大约 57.3 倍；提取纯函数 `QuoteGestureTransformer` | ✅ 完成（domain 纯函数 + `EditorScreen.kt` 手势回调已接入 `QuoteGestureTransformer.apply`） |
| P4-002 | 自定义样式编辑器返回/系统返回会静默丢失未保存修改，缺少脏状态与确认对话框 | ✅ 完成（`CustomStyleEditorViewModel.isDirty` + `ShowUnsavedDialog` 事件，`CustomStyleEditorScreen` 接入确认对话框） |
| P4-003 | Auto Match 采样未应用图片 `dimAmount`，与实际显示效果不符 | ✅ 完成（`DefaultBackgroundSampler.samplePhoto` 对采样像素应用 `dimAmount`；见 `BackgroundSamplerTest`） |
| P4-004 | Auto Match 异步采样结果可能在背景已变化后覆盖新背景的建议（竞态） | ✅ 完成（`EditorViewModel.autoMatchGeneration` + `autoMatchKey()` 双重校验，过期结果丢弃） |
| P4-005 | Auto Match「预览」与「应用」状态语义错误，未点击应用也可能被保存 | ✅ 完成（`EditorUiState.previewTextStyle` 仅供预览；`confirmAutoMatch`/`undoAutoMatch` 才改写正式 `textStyle`） |
| P4-006 | 对比度计算使用 `contrastWithBlack`（纯黑）而非实际文字色 `#1A1A1A` | ✅ domain 完成（`AutoStyleMatcher`） |
| P4-007 | `TextStyleNormalizer.validate()` 先 normalize 再校验，非法颜色被静默修复而非报错 | ✅ domain 完成（新增 `TextStyleValidator` / `TextStyleSanitizer`） |
| P4-008 | Canvas 边框绘制与背景块填充状态耦合不一致（Compose vs Canvas 独立性不同） | ✅ 完成（`QuotePreview`/`CanvasWallpaperRenderer` 的 `hasBorder` 判断独立于 `hasBlock`） |
| P4-009 | Cursive 字体在 Compose 预览与 Canvas 壁纸中映射不一致 | ✅ 完成（`CanvasWallpaperRenderer` 改用 `Typeface.create("cursive", ...)`，与 Compose `FontFamily.Cursive` 对齐） |
| P4-010 | 内置样式预设仅文字标签，无颜色/字体等视觉预览 | ✅ 完成（`EditorScreen.PresetStyleCard` 用真实 `QuotePreview` 渲染每个预设） |
| P4-011 | 主页图片收藏集缩略图显示黑底 | ✅ 完成（`HomeViewModel` 经共享 `BackgroundImageLoader` 加载缩略图，`CollectionCardUiModel.thumbnailBitmap`/`thumbnailLoadFailed`） |
| P4-012 | 壁纸动画中点切换时可能先闪现新文字再变回透明 | ✅ 完成（`WallpaperCoordinator.transitionShowTarget`/`transitionTextAlpha` 严格按过渡中点切换渲染目标） |
| P4-013 | `clampTransform` 基于未旋转矩形计算可见范围，未考虑文字旋转后的实际包围盒 | ✅ 完成（domain：`QuoteBlockLayoutCalculator` 旋转 AABB；app 跟进：`EditorViewModel.updateTransformRequested` 统一手势与滑块两条路径都走裁剪，见 D-026） |
| P4-014 | 自定义样式名称唯一性判断（`lower(name)`）与数据库默认大小写敏感索引不一致；重排非事务 | ✅ 完成（`normalizedName` 唯一索引 + `CustomStyleDao.reorder` `@Transaction`，见 D-024） |
| P4-015 | 自定义样式编辑器分区标签横向 `FilterChip` 误用 `verticalScroll` | ✅ 完成（改为 `LazyRow` 横向滚动） |
| P4-016 | 全天时间状态下起止手柄重叠，距离相等时规则固定选中 Start | ✅ 完成（`CircularTimeGeometry.resolveDragHandle` 按 `lastSelected` 交替选中） |
| P4-017 | Singleton 图片 Loader 的可变诊断字段被多个 Wallpaper Engine 实例覆盖 | ✅ 完成（`load()` 新增 `diagnostics` 形参，见 D-025） |
| P4-018 | 编辑器保留两套重复样式控件实现（`EditorScreen` 自有 vs `StyleEditingControls.kt`） | ✅ 完成（`EditorScreen.StyleTabContent` 改用 `StyleSection`/`StyleSectionContent`，删除重复实现） |
| P4-019 | Phase 4 测试覆盖不足：Canvas/DAO/ViewModel/未保存退出等缺少专项测试 | ✅ 本地完成：含 `EditorViewModelAutoMatchTest`（预览/确认/撤销/背景竞态/手动编辑失效）、`CustomStyleEditorViewModelTest`、`CustomStyleDaoTest`、`BackgroundSamplerTest`、`WallpaperTransitionDriverTest`、`QuoteGestureTransformerTest`、`TextStyleValidatorTest` 等；Compose Golden / connectedAndroidTest / CI 绿勾仍待 RC1 实机与远程验收 |

---

## 构建与验证命令

```bash
cd /Users/gao/Desktop/WallQuote
./gradlew :app:assembleDebug \
  :domain:test \
  :data:testDebugUnitTest \
  :app:testDebugUnitTest \
  :app:lintDebug
```

开发期修改 Room Schema 后：卸载应用或清除应用数据（见 D-020）。

## SDK 版本（权威）

- minSdk **26**
- compileSdk **36**
- targetSdk **34**
