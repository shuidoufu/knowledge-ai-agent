# 变更记录（CHANGELOG）

> 按版本号倒序记录。格式：`版本号 / 日期` + 分条变更（新增/修复/优化/重构），与 Git 提交信息一致。

## 0.0.1-SNAPSHOT / 2026-08-30

- 重构规则文档体系：CLAUDE.md 精简为规则句 + 文档地图，陷阱详情拆分至 `docs/known-pitfalls.md`，删除废弃 `RULES.md`
- 新增通用规则模板 `docs/rules-template.md`、项目进度文档 `PROGRESS.md` 及模板
- 新增功能测试子智能体 `code-test`、规划子智能体 `planner`（Claude Code 与 Zcode 双端）
- 补充分支策略 / 语义化版本 / CHANGELOG 约定；AGENTS.md 同步为统一规则
