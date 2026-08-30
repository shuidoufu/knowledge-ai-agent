# 项目进度（PROGRESS）

> 开发/运维过程中持续更新，作为 AI 会话间衔接的"记忆"。每次开发结束必须更新本文件。

## 项目状态

- 当前分支：`knowledge-miniprogram`
- 一句话现状：规则体系验证流程已闭环（违规诱捕自测 + code-test 子智能体实跑双向确认规则生效，捕获 1 个 P1：request.js 生产 BASE_URL 命中陷阱 38），待处理 P1 后提交文档重构改动。

## 需求 / 任务清单

| 状态 | 任务 | 说明 / 验收点 |
|------|------|--------------|
| [x] 完成 | Claude Code 上下文窗口放大 | `deepseek-v4-flash[1m]`，/context 显示 1M |
| [x] 完成 | 规则文档体系重构 | CLAUDE.md 精简（467→215 行）、陷阱移入 docs/known-pitfalls.md、删除 RULES.md |
| [x] 完成 | 通用规则模板 | docs/rules-template.md（含 AI 行为红线/命名/依赖/安全/健壮性通用层） |
| [x] 完成 | 功能测试子智能体 | Zcode `code-test.yaml` + Claude Code `code-test.md` |
| [x] 完成 | 项目进度文档 | PROGRESS.md + docs/PROGRESS-template.md |
| [x] 完成 | 新会话验证整套流程 | 违规诱捕自测命中 request.js 陷阱 38（P1）；code-test 子智能体独立复测一致，规则/子智能体确认生效 |
| [ ] 待办 | 规划/架构子智能体 | Zcode planner（新项目/大需求用，P1） |

## 本次改动记录（最新在前）

| 日期 | 改动 | 涉及文件/模块 | 是否已测/已审 |
|------|------|--------------|--------------|
| 2026/8/30 | 验证流程闭环：违规诱捕自测 + code-test 子智能体实跑 | request.js（捕获 P1：生产 BASE_URL 命中陷阱 38）、PROGRESS.md | 是（前端构建 / 后端接口 11 项 / 消费方回归通过；浏览器 UI 无 headless 跳过） |
| 2026/8/30 | 规则文档体系重构 + 通用模板 + 测试子智能体 | CLAUDE.md、AGENTS.md、docs/known-pitfalls.md、docs/rules-template.md、docs/PROGRESS-template.md、PROGRESS.md、RULES.md(删)、~/.claude/agents/code-test.md、~/.zcode/agents/code-test.yaml | 文档类改动，结构已验证 |
| 2026/8/30 | Claude Code 上下文窗口放大 + 子代理模型修复 | ~/.claude/settings.json | 是 |

## 风险 / 遗留问题

- 🔴 P1（已决策）：request.js 生产 BASE_URL = `http://localhost:8123` 命中陷阱 38；用户决定保留作本地配置，**提交时排除该文件（不入库）**
- AGENTS_BAK.md 为未跟踪备份文件，按提交规则（只提交本次改动文件）不应入库
- AGENTS.md 与 CLAUDE.md 需保持一致（本次已同步，后续改规则两文件一起改）
- 全部改动未提交（CLAUDE.md / AGENTS.md / RULES.md 删除 / request.js / docs 四个新文件）
- 无自动化测试框架（JUnit/vitest），测试靠手动 curl/页面，待建（P2）
- code-test 子智能体已实跑验证生效（本次）；浏览器级 UI 交互无 headless 环境未覆盖

## 下一步

1. ✅ P1 已决策：request.js 保留 localhost:8123 作本地配置，提交时排除该文件
2. 处理 AGENTS_BAK.md 备份文件（不入库）
3. 提交文档重构 + 规则体系改动（精确 `git add`，排除 request.js / AGENTS_BAK.md）
4. 用户评估规则体系后，进入新的功能开发或运维任务
