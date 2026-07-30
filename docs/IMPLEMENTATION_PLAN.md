# WallQuote 实施计划

## 状态

| 标记 | 含义 |
|------|------|
| **Phase 1.1 complete** | 事务保存、行 ID、脏检查、BackHandler、RenderSpec |
| **Phase 2 Complete** | WallpaperService + 播放闭环 + 2.1 验收修复 |
| **Phase 3（当前）** | 渐变 / 图片背景、私有资产、Dim/Blur、异步加载 |
| **Phase 3 Complete** | 背景闭环验收通过后方可标记 |

## 阶段总览

| 阶段 | 目标 | 验收 |
|------|------|------|
| **1 / 1.1** | 工程骨架 + 可靠性修复 | assemble / tests / lint |
| **2 / 2.1** | 动态壁纸最小闭环 + 验收修复 | 可设壁纸；P0/P1 审查项关闭 |
| **3** | 渐变/图片背景、Photo Picker、Coil 预览 | 背景类型全开 |
| 4 | 自定义样式、预设、Auto Match、动画 | 对齐 P1 需求 |

---

## Phase 3 任务

- [x] `BackgroundSpec.Photo.assetId` + `PhotoScaleMode` + 背景校验
- [x] `GradientGeometryCalculator` / `CenterCropCalculator`
- [x] `BackgroundAssetStore`（staging / commit / discard / orphan cleanup）
- [x] Photo Picker（无广泛媒体权限）
- [x] 渐变编辑器 + Compose/Canvas 渲染
- [x] 异步 `BackgroundImageLoader` + LRU + token 校验
- [x] 图片编辑 UI + Dim + blur（StackBlur）
- [x] 保存/删除/放弃时的资产生命周期
- [x] 测试 + 文档

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

## SDK 版本（权威）

- minSdk **26**
- compileSdk **36**
- targetSdk **34**
