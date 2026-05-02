# Development Rules

## 1. Build Restrictions

- **DO NOT** run any Gradle command locally
- Builds, linting, formatting checks — everything runs on **GitHub Actions CI**

## 2. Workflow

### Daily Development
- Write code on local `main` branch, commits stay local

### During Development
- I judge when verification is needed (e.g., after completing a logical unit of work) and push for review on my own initiative
- `git push origin main:master` — push current code to remote `master`
- Trigger `review.yml` — CI runs checks (lint, spotless) on the `master` branch
- Fix issues if any, then continue

### Development Complete
1. `git push origin main` — push final code to remote `main`
2. `build.yml` auto-triggers to build the APK
3. Monitor CI status:
   - **Failed**: read logs, analyze, suggest fixes
   - **Success**: report summary (time, result)

## 3. Notes

- No extra branches, no merge operations
- Every change must pass CI verification
- No unauthorized Gradle commands

---

## Coding Guidelines

### Think Before Coding
- State assumptions, ask if uncertain
- Surface tradeoffs when multiple approaches exist
- Push back when something can be simplified

### Keep It Simple
- Solve only what was asked, no extra features
- No abstractions for single-use code
- Don't handle impossible errors

### Surgical Changes
- Touch only what's necessary
- Match existing style, don't refactor working code
- Clean up orphans your changes created (unused imports/variables)

### Goal-Driven
- Define verification criteria, iterate until met
- For multi-step tasks, state a brief plan first
