# WallQuote 实施计划

## 状态

| 标记 | 含义 |
|------|------|
| **Phase 1 scaffold complete** | 多模块、Schema、基础 UI/CRUD 已落地 |
| **Phase 1.1 complete** | 事务保存、行 ID、脏检查、BackHandler、RenderSpec |
| **Phase 2（当前）** | 动态壁纸最小可用闭环 |
| **Phase 2 acceptance** | Service + 推进/调度/同步 + 测试/CI 通过 |

## 阶段总览

| 阶段 | 目标 | 验收 |
|------|------|------|
| **1 / 1.1** | 工程骨架 + 可靠性修复 | assemble / domain+data tests / lint |
| **2** | `WallpaperService`、Canvas、轮播、计划边界 | 可设为壁纸并稳定显示/推进 |
| 3 | 渐变/图片背景、权限、Coil | 背景类型全开 |
| 4 | 自定义样式、预设、Auto Match、动画 | 对齐 P1 需求 |

---

## Phase 2 任务

- [x] Domain：`PlaybackController` / `Reconciler` / `ActiveCollectionSelector` / `ScheduleBoundaryCalculator`
- [x] `WallpaperCoordinator` 单线程事件循环（Channel）
- [x] `CanvasWallpaperRenderer` + `QuoteLayoutCalculator`
- [x] `WallQuoteWallpaperService` + Manifest + `wallpaper.xml`
- [x] 主页「设为壁纸」
- [x] Domain / Coordinator / Renderer 测试
- [x] 不注册 SCREEN_ON 广播（见 D-015）
- [x] 文档决策更新

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
