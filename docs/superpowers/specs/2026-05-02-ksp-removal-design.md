# KSP 完全移除方案

## 背景

项目之前引入了 KSP（Kotlin Symbol Processing）用于 Room 注解处理，但在 AGP 9.0 内置 Kotlin 编译器的环境下，KSP 存在兼容性问题（多个提交尝试修复未果）。项目为个人项目，API 需求简单，应选择最稳定的方案。

## 方案

使用 `annotationProcessor`（标准 Java 注解处理）替代 KSP，彻底移除所有 KSP 相关配置。

## 变更清单

| 文件 | 变更 |
|------|------|
| `app/build.gradle.kts` | 移除 `alias(libs.plugins.kotlin.ksp)` 插件引用；`ksp()` → `annotationProcessor()` |
| `build.gradle.kts` | 移除 `alias(libs.plugins.kotlin.ksp) apply false` |
| `gradle/libs.versions.toml` | 移除 `ksp` 版本号；移除 `kotlin-ksp` 插件定义；Kotlin 版本 `2.3.20` → `2.3.10`（原升级仅为兼容 KSP，不再需要） |
| `settings.gradle.kts` | 移除 `gradlePluginPortal()`（原为从 Gradle Plugin Portal 拉取 KSP 而添加） |

## 影响范围

- Room 注解处理由 KSP → `annotationProcessor`，功能保持一致
- Kotlin 版本回退 `2.3.20` → `2.3.10`，回退后会跟随 Kotlin 最新稳定版
- `gradlePluginPortal()` 不再需要，减少仓库解析链长度
- Room 所有现有功能（Entity、DAO、Flow、suspend）完全不受影响

## 理由

1. `annotationProcessor` 是 Room 官方长期以来建议的稳定方案
2. 不需要 KSP 带来的增量编译等 Kotlin 专属优势（个人项目编译频率低）
3. AGP 9.0 对标准 Java 注解处理支持良好
4. Room 2.6.1 的注解处理器成熟稳定
