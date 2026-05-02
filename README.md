<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="日程" width="120" height="120" style="border-radius: 24px;">
  <h1 align="center">日程 — Schedule</h1>
  <p align="center">
    基于 Jetpack Compose + Material Design 3 构建的 Android 日程管理应用
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin&logoColor=white">
    <img src="https://img.shields.io/badge/Compose-BOM_2026.01.01-4285F4?logo=jetpackcompose&logoColor=white">
    <img src="https://img.shields.io/badge/minSdk-24-brightgreen">
    <img src="https://img.shields.io/badge/targetSdk-35-brightgreen">
    <img src="https://img.shields.io/badge/Material_3-Yes-0061FF">
    <img src="https://img.shields.io/badge/license-MIT-blue">
  </p>
</div>

---

## 功能

| | 功能 | 说明 |
|---|---|---|
| 📅 | **日视图** | 查看每日日程列表，支持完成状态标记与进度统计 |
| 📆 | **月视图** | 月历概览，标记有日程的日期，支持按日筛选 |
| 🔍 | **智能搜索** | 跨月搜索所有日程，结果悬浮展示，点击直达对应日期 |
| 🤖 | **AI 分析** | 集成 DeepSeek API，自动分析月度/每日日程并提出改进建议 |
| 🏷️ | **分类管理** | 自定义颜色标签，轻松分类与筛选日程 |
| 🌙 | **深色模式** | 支持深色/浅色主题切换 |
| ⏰ | **日程编辑** | 标题、日期、时间、全天事件、分类、地点、备注一应俱全 |

## 技术栈

### 架构

```
MVVM — Room + ViewModel + StateFlow + Navigation Compose
```

| 层次 | 技术 |
|---|---|
| **UI** | Jetpack Compose + Material Design 3 |
| **状态管理** | ViewModel + StateFlow + `combine` 变换 |
| **导航** | Navigation Compose |
| **持久化** | Room 数据库 (ScheduleEntity / CategoryEntity / RepeatRule) |
| **AI 集成** | DeepSeek API (HttpURLConnection) |
| **构建** | Gradle Version Catalog + Kotlin DSL |

### 依赖

| 库 | 用途 |
|---|---|
| `Compose BOM 2026.01.01` | Compose UI 版本管理 |
| `Material 3` | Material Design 3 组件库 |
| `Navigation Compose 2.8.5` | 声明式导航 |
| `Lifecycle ViewModel Compose 2.8.7` | ViewModel 集成 |
| `Activity Compose 1.8.0` | Compose + Activity 桥接 |
| `Material Icons Extended 1.7.8` | 扩展图标库 |

## 截图

<!-- TODO: 添加应用截图 -->

## 快速开始

```bash
# 克隆项目
git clone https://github.com/dac114514/android-note.git

# 使用 Android Studio 打开项目
# 等待 Gradle 同步完成

# 构建 APK（通过 GitHub Actions）
git push origin main
# CI 自动构建：https://github.com/dac114514/android-note/actions
```

## 构建

APK 由 **GitHub Actions CI** 自动构建。推送至 `main` 分支后，在 [Actions 页面](https://github.com/dac114514/android-note/actions) 查看构建结果。

## 项目结构

```
app/
├── src/main/
│   ├── java/com/faster/note/
│   │   ├── data/
│   │   │   ├── ai/           # DeepSeek AI 集成
│   │   │   ├── db/entity/    # Room 实体
│   │   │   ├── local/        # 本地存储
│   │   │   └── repository/   # 数据仓库
│   │   └── ui/
│   │       ├── about/        # 关于页面
│   │       ├── components/   # 公共组件
│   │       ├── day/          # 日视图
│   │       ├── month/        # 月视图
│   │       ├── navigation/   # 导航配置
│   │       ├── settings/     # 设置页面
│   │       └── theme/        # Material 3 主题
│   └── res/
│       ├── drawable/         # 矢量图标
│       ├── mipmap*/          # 应用图标
│       └── values/           # 资源文件
```

## AI 分析

应用集成 **DeepSeek API**，可对日程进行智能分析：

- **月分析** — 时间分配概况、完成情况、发现问题、改进建议
- **日分析** — 每日日程总结与优化建议

在 **设置** 页面配置 API Key 后即可使用。

---

<div align="center">
  Made with ❤️ by dac114514
</div>
