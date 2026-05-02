# Development Rules

## 1. Build Restrictions

- **DO NOT** run any local Gradle commands that generate APKs
- APKs must be built via **GitHub Actions CI** only

## 2. Workflow

- Develop directly on the local `main` branch
- After review, `git push origin main` to GitHub
- Listening CI build status:
  - **Failed** — fetch logs, analyze error, propose fix
  - **Success** — output summary (time, result)

---

# Coding Guidelines

## Think Before Coding
- State assumptions, ask if uncertain
- Surface tradeoffs when multiple approaches exist
- Push back when something can be simplified

## Keep It Simple
- Solve only what was asked, no extra features
- No abstractions for single-use code
- Don't handle impossible errors

## Surgical Changes
- Touch only what's necessary
- Match existing style, don't refactor working code
- Clean up orphans your changes created (unused imports/variables)

## Goal-Driven
- Define verification criteria, iterate until met
- For multi-step tasks, state a brief plan first
