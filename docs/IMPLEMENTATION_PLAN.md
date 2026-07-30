# WallQuote 实施计划

## 状态

| 标记 | 含义 |
|------|------|
| **Phase 1.1 complete** | 事务保存、行 ID、脏检查、BackHandler、RenderSpec |
| **Phase 2 Complete** | WallpaperService + 播放闭环 + 2.1 验收修复 |
| **Phase 3 Complete** | 背景系统收口：Schema 重置、可重试保存、Blur 内存、共享处理 |
| **Phase 4 Complete / RC1** | 样式系统、自定义样式、编辑交互、Auto Match、壁纸淡入淡出 |

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
- [ ] Gap：`EditorScreen` 仍保留自身 `StyleTabContent`，未切换到抽取出的 `StyleEditingControls.kt`（两者字段等价，非阻塞）

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
- [x] 自定义样式全参数编辑器（边框、阴影、文字背景块）— 竖排未覆盖，见 Gap
- [x] 环形时间选择器（`CircularTimePicker`，30 分钟步长）
- [x] 主页卡片时间轴可视化、`SwipeToDismiss`
- [x] `AutoStyleMatcher` / `BackgroundSampler`
- [x] 壁纸淡入淡出动画（`WallpaperTransitionDriver`，288ms）
- [ ] `Palette KTX` 色彩提取（`BackgroundSampler` 目前用自研平均色/对比度采样，未引入 Palette 库）
- [ ] 竖排文字样式
- [ ] `FontResolver` 应用内打包字体（无明确许可证，仍 deferred）
- [ ] API 33+ 存储权限（Photo 背景走内部 assetId，本阶段无需申请）

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
