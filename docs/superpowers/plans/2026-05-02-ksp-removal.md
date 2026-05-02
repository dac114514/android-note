# KSP 完全移除 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 彻底清理项目中的所有 KSP 残留，使用 `annotationProcessor` 替代

**Architecture:** 4 个构建文件需要修改，所有变更已在工作树中生效，只需验证、提交、推送

**Tech Stack:** Android Gradle Plugin 9.0, Kotlin 2.3.10, Room 2.6.1

---

### Task 1: 验证所有变更的正确性

**Files:**
- Verify: `app/build.gradle.kts`
- Verify: `build.gradle.kts`
- Verify: `gradle/libs.versions.toml`
- Verify: `settings.gradle.kts`

- [ ] **Step 1: 验证 app/build.gradle.kts 变更**

```diff
-    alias(libs.plugins.kotlin.ksp)
+    (removed)
...
-    ksp(libs.room.compiler)
+    annotationProcessor(libs.room.compiler)
```

确认 KSP 插件已移除，room.compiler 使用 `annotationProcessor`。

- [ ] **Step 2: 验证 build.gradle.kts 变更**

```diff
-    alias(libs.plugins.kotlin.ksp) apply false
+    (removed)
```

确认根目录已无 KSP 插件声明。

- [ ] **Step 3: 验证 libs.versions.toml 变更**

```diff
- ksp = "2.3.6"
+ (removed)
- kotlin = "2.3.20"
+ kotlin = "2.3.10"
...
- kotlin-ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
+ (removed)
```

确认 KSP 版本号和插件定义已移除，Kotlin 版本已降回 2.3.10。

- [ ] **Step 4: 验证 settings.gradle.kts 变更**

```diff
- gradlePluginPortal()
+ (removed)
```

确认已移除 `gradlePluginPortal()`。

- [ ] **Step 5: 验证全局无 KSP 残留**

Run: `grep -r "ksp\|kotlin.ksp\|devtools.ksp" --include="*.kts" --include="*.toml" .`

Expected: 无匹配结果

### Task 2: 提交变更

- [ ] **Step 1: 暂存所有变更**

Run: `git add app/build.gradle.kts build.gradle.kts gradle/libs.versions.toml settings.gradle.kts`

- [ ] **Step 2: 提交**

```bash
git commit -m "$(cat <<'EOF'
fix: completely remove KSP, use annotationProcessor for Room

- Remove KSP plugin from all build files
- Switch Room annotation processing from ksp() to annotationProcessor()
- Revert Kotlin from 2.3.20 to 2.3.10 (upgrade was only for KSP compat)
- Remove gradlePluginPortal() (was added for KSP resolution via GP)
EOF
)"
```

### Task 3: 推送到 GitHub 并监视 CI

- [ ] **Step 1: 推送到远程**

Run: `git push origin main`

- [ ] **Step 2: 等待并监视 CI 构建状态**

Run: `gh run list --repo <owner>/<repo> --limit 1 --json conclusion,status,headBranch`

Expected: 等待 CI 构建完成。成功则结束，失败则拉取日志分析。

- [ ] **Step 3: 输出总结**

CI 成功时输出构建摘要信息。
