# 变更记录（CHANGELOG）

> 按版本号倒序记录。格式：`版本号 / 日期` + 分条变更（新增/修复/优化/重构），与 Git 提交信息一致。

## 0.0.1-SNAPSHOT / 2026-09-16

- 新增 Web 端「知识库文档管理」（仅管理员）：上传（仅 `.md`，≤5MB）/ 列表（模糊搜索、时间与名称排序、分页）/ 查看（预处理正文 + 分块结果）/ 删除与批量删除 / 重新入库
- 新增管理员 `role` 字段（`0` 普通 / `1` 管理员）：登录与 `/auth/me` 返回 `isAdmin`，管理接口在服务层校验（非管理员 403、无 token 401）
- 优化：知识库加载器改为「真实目录优先 + classpath 回退」使运行时上传即时生效；`DocumentPreprocessor` 分割线插入改为幂等
- 修复：排序控件改为与页面风格统一的自定义下拉；分块结果被压成 1px 不显示（flex 子项需 `flex-shrink: 0`）；分块详情改为固定高度 + 内部滚动；详情弹窗请求时序竞态致内容覆盖；重新入库按钮改回"非处理中即可重跑"并为已完成文档加二次确认
- 工程：新增 `.editorconfig` 编辑器约定；`.gitattributes` 行尾规则由 `.gitignore` 移出纳入版本管理（`*.sh` 固定 LF）

## 0.0.1-SNAPSHOT / 2026-08-30

- 重构规则文档体系：CLAUDE.md 精简为规则句 + 文档地图，陷阱详情拆分至 `docs/known-pitfalls.md`，删除废弃 `RULES.md`
- 新增通用规则模板 `docs/rules-template.md`、项目进度文档 `PROGRESS.md` 及模板
- 新增功能测试子智能体 `code-test`、规划子智能体 `planner`（Claude Code 与 Zcode 双端）
- 补充分支策略 / 语义化版本 / CHANGELOG 约定；AGENTS.md 同步为统一规则
