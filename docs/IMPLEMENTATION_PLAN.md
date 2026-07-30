# WallQuote 实施计划

## 阶段总览

| 阶段 | 目标 | 验收 |
|------|------|------|
| **1（当前）** | 工程骨架、Schema v1、收藏集/名言 CRUD、主页+基础编辑器、纯色+基础样式、预览状态、单元测试 | `./gradlew assembleDebug test lint` 通过 |
| 2 | `WallpaperService`、Canvas 渲染、轮播与屏幕触发（无淡入淡出可先硬切） | 可设为壁纸并轮播 |
| 3 | 渐变/图片背景、权限、Coil | 背景类型全开 |
| 4 | 自定义样式、预设、Auto Match、动画与高级编辑器 | 对齐设计文档 P1 |

---

## 阶段 1 任务清单

### 1. Gradle 与模块

- [x] 根 `settings.gradle.kts`、`libs.versions.toml`
- [x] 模块：`app`, `domain`, `data`, `core:preview`
- [x] minSdk 26, compile/target 34, Hilt, Room, Compose BOM, Navigation, Serialization

### 2. Domain

- [x] `BackgroundSpec`, `TextStyleConfig`, `DailyTimeRange`, `CollectionConfig`
- [x] `CollectionRepository` 接口
- [x] `ObserveOrderedCollectionsUseCase`, `SelectActiveCollectionsUseCase`
- [x] CRUD 用例：`SaveCollectionUseCase`, `DeleteCollectionUseCase`, `GetCollectionUseCase`

### 3. Data

- [x] Entities, DAOs, `AppDatabase` version 1
- [x] Type converters / JSON for background & text style
- [x] `CollectionRepositoryImpl`

### 4. Core Preview

- [x] `WallpaperPreviewState`（背景 + 文本 + 样式 + 变换）
- [x] Compose `QuotePreview` 绘制纯色与基础文本

### 5. App UI

- [x] `MainActivity`, Hilt `Application`, NavHost
- [x] `HomeScreen` + `HomeViewModel`（列表、删除、导航）
- [x] `EditorScreen` + `EditorViewModel`（加载/保存、文本行、纯色、基础样式、简化时间）

### 6. 测试

- [x] `DailyTimeRangeTest`, `SelectActiveCollectionsUseCaseTest`
- [x] `CollectionDaoTest`（in-memory Room）

### 7. 文档

- [x] `REQUIREMENTS.md`, `DECISIONS.md`, 本文件

---

## 阶段 1 明确不做

- 自定义图片背景、渐变 UI、Auto Match、淡入淡出、竖排文字、背景模糊
- 环形时间选择器、完整动态壁纸轮播、`MyWallpaperService`
- 无效「设为壁纸」按钮、无实现的配置项

---

## 构建与验证命令

```bash
cd /Users/gao/Desktop/WallQuote
./gradlew :app:assembleDebug :domain:test :data:testDebugUnitTest :app:lintDebug
```
