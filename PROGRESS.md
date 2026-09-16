# 项目进度（PROGRESS）

> 开发/运维过程中持续更新，作为 AI 会话间衔接的"记忆"。每次开发结束必须更新本文件。

## 项目状态

- 当前分支：`knowledge-doc-manage`（自 `test` 拉出的临时分支，仅供"知识库文档管理"需求使用，完成后由用户删除）
- 一句话现状：规则体系、子智能体、进度/版本管理全部落地并已提交推送；P2 自动化测试框架评估后放弃，测试维持手动。Web 端「知识库文档管理」已在 `knowledge-doc-manage` 分支开发完成并通过自测（管理员门控 + 文档上传/删除/列表/查看/搜索 + 状态可视化），**待用户验收**。

## 需求 / 任务清单

| 状态 | 任务 | 说明 / 验收点 |
|------|------|--------------|
| [x] 完成 | Claude Code 上下文窗口放大 | `deepseek-v4-flash[1m]`，/context 显示 1M |
| [x] 完成 | 规则文档体系重构 | CLAUDE.md 精简（467→215 行）、陷阱移入 docs/known-pitfalls.md、删除 RULES.md |
| [x] 完成 | 通用规则模板 | docs/rules-template.md（含 AI 行为红线/命名/依赖/安全/健壮性通用层） |
| [x] 完成 | 功能测试子智能体 | Zcode `code-test.yaml` + Claude Code `code-test.md` |
| [x] 完成 | 项目进度文档 | PROGRESS.md + docs/PROGRESS-template.md |
| [x] 完成 | 新会话验证整套流程 | 违规诱捕自测命中 request.js 陷阱 38（P1）；code-test 子智能体独立复测一致，规则/子智能体确认生效 |
| [x] 完成 | 规划/架构子智能体 | Zcode `planner.yaml` + Claude Code `planner.md` |
| [x] 完成 | CHANGELOG 收尾 | 创建根目录 `CHANGELOG.md` |
| [x] 完成 | remote URL 更新 | 已改为 shuidoufu/knowledge-ai-agent，a9c6e55 推送成功 |
| [x] 完成 | web: 语音识别友好错误提示 | 过短(<0.6s)前端拦截 + 后端 400 带 message + 前端映射中文提示 |
| [x] 完成 | web: 历史对话滚动条与收起按钮重叠 | 收起按钮置于侧边栏右缘外侧(left:260px 不居中)，滚动条贴右侧边框 |
| [x] 完成 | web: 隐藏历史对话中的个人信息组件 | /knowledge 页 user-dock 恒隐藏（临时方案，见后续优化） |

### 本次需求（分支 `knowledge-doc-manage`，仅 Web 端）—— 开发与自测已完成，待验收

| 状态 | 任务 | 说明 / 验收点 |
|------|------|--------------|
| [x] 完成 | 管理员身份 | `User` 加 `role`（0 普通 / 1 管理员，默认 0）；登录与 `/auth/me` 返回 `isAdmin`；6 个管理接口在 service 层查库校验，非管理员 403、无 token 401 |
| [x] 完成 | 文档上传 | 仅 `.md` + 校验（扩展名/大小/UTF-8/文件名净化/重名拒绝）；后台单线程串行执行 预处理 → 落盘 → 原地覆盖为预处理结果 → 分块 → 向量化 |
| [x] 完成 | 文档删除 / 批量删除 | 先删该文档向量再删文件；提示含清理的切片数 |
| [x] 完成 | 文档列表 / 查看 | 名称+文件名 / 大小 / 状态 / 上传时间 / 操作；模糊搜索 + 时间与名称排序 + 分页（每页 10 条）；详情弹窗展示预处理正文与分块结果 |
| [x] 完成 | 状态实时可见 | 预处理中 / 向量化中 / 已完成 / 未入库 / 预处理失败 / 向量化失败 + 失败原因；前端 1.5s 轮询、终态弹提示；未入库与失败可「重新入库」 |
| [x] 完成 | 个人中心入口 | 头像下拉「知识库管理」仅管理员可见；支持返回上一级；非管理员直连 URL 被守卫拦回首页并提示 |
| [x] 完成 | 页面布局 | 按确认结论：不做统计卡、不做分类列；底部「共 N 条 · 第 X/Y 页」分页 |
| [x] 完成 | 自测 | 后端全链路 + 失败路径 + 403/401 + 回归；前端 build + 浏览器实操；独立代码审查 + 独立功能交叉验证（见改动记录） |

### 前端问题修复（2026/9/16，同分支 `knowledge-doc-manage`）

| 状态 | 问题现象 | 根因 | 处理方式 |
|------|----------|------|----------|
| [x] 完成 | 排序下拉窗口 UI 与页面主体风格不符（原生控件默认外观） | 用的是原生 `<select class="sort-select">`，未做外观定制（无 `appearance:none`、无自定义箭头），浏览器默认渲染风格与页面玻璃拟态不一致 | 改为自定义下拉：触发按钮对齐搜索框视觉（44px/圆角 12px/聚焦绿光晕），面板 Teleport 到 body + fixed 定位 + 玻璃拟态；点击外部、Esc、Tab、页面滚动均关闭 |
| [x] 完成 | 查看文档内容时「分块结果」显示异常——内容过多不显示、全挤在一页 | `.chunk-list` 是 `display:flex; flex-direction:column` 容器，而 `.chunk-item` 自带 `overflow:hidden`；按 Flexbox 规范，子项 `overflow` 非 `visible` 时自动最小尺寸为 **0**，于是分块项被压缩、内容被裁切 | 分块项加 `flex-shrink: 0` 消除根因；卡片高度固定、只展示部分内容（2 行预览），详情由点击展开 |
| [x] 完成 | 分块详情显示不全，且「有的有滚动条、有的没有」 | 与上一条同源（被压扁裁切，所以看不到滚动条）；叠加 `.chunk-text` 只设 `max-height:320px`，形成"外层列表挤压 + 内层各自滚动"的双层滚动：短内容不滚、长内容滚 | 展开的分块固定高度（320px）+ 内部滚动，并补齐与 `.pane-body` 一致的 6px 细滚动条样式，使滚动表现统一 |

- 呈现方式（用户确认）：**不用弹窗**——分块折叠式，每个分块最右侧空心三角箭头（Lucide `TriangleRight`，展开时旋转 90°）指示开合，点击展开显示详情，展开的分块大小固定、超长内容内部滚动，其余分块依次往下排。
- 验收点：下拉开合/选中/点外部关闭/Esc 关闭、面板不被裁剪；分块卡片高度一致、列表可滚动；展开后高度固定、超长内容内部滚动、再点收起；搜索/排序/分页/批量/上传/删除/重新入库与左侧「预处理后正文」栏不回归。
- 改动范围：仅前端（`frontend/src/views/KnowledgeDocuments.vue`）+ 本文件；后端经核查对分块正文无截断、无数量上限，不动。
- 已实测（浏览器）：76 分块文档下每张卡片高度一致（87px）、零裁切，连 9 字的最短分块也是 87px；展开后卡片 363px＝头部 41px + 正文固定 320px，3124 字分块内部滚动（scrollHeight 1171）、短分块不滚，箭头旋转 90°、`aria-expanded` 正确；下拉面板 `position:fixed`、Teleport 到 body、`z-index:3000`、玻璃拟态生效、与触发按钮对齐且不出视口，选中/点外部/Esc/滚动均关闭；375px 宽度下不溢出、卡片仍统一 87px。

## 后续优化（待办）

> 已确认但暂不做的优化，集中登记，后续需求按项处理。

- **用户管理功能（给用户授予管理员）**：`role` 字段目前只能手工维护（本次需求未做管理界面）。后续需求：新增用户管理页，可给指定用户授予/撤销管理员；届时移除本文件与 README 里的手工改库说明，并在页面入参处做取值校验。
  当前手工维护方式（本机未安装 mongosh，可用 MongoDB Compass 执行，或用 `NumberInt` 明确 Int32）：
  `db.users.updateOne({username:"admin"},{$set:{role:NumberInt(1)}})`
  取值约定：`0` 普通用户 · `1` 管理员（`role` 为 null 或非 1 一律视为普通用户）。
- **web 历史对话页与知识库管理页个人信息入口重构**：为修复与历史列表/批量操作条重叠，`/knowledge` 与 `/knowledge-documents` 两页暂时隐藏左下角 user-dock（个人信息组件）。副作用是这两页无法再从该组件进入"修改密码/退出登录"（知识库管理页可经「返回」回首页使用）。后续需重新设计个人信息入口（如侧边栏底部并入个人卡片、或放头部菜单），需同时考虑桌面端与移动端一致性，改完移除 `showDock` 中对这两个路径的临时隐藏分支
- **manus 超级智能体**：后续慢慢实现该功能
- **知识库管理页遗留问题（2026/9/16 独立审查发现，存量）**：
  ① **应用外壳在较矮视口下不滚动**：`html, body { height:100% }` + `html, body { overflow-x:hidden }`（后者按 CSS 规范把 `overflow-y` 计算成 `auto`，body 成为滚动容器），配合 `.main-content { flex:1; min-height:0 }`（`overflow:visible`），实测 1280×720 下 `document.scrollingElement.scrollHeight === innerHeight === 720`、设 `scrollTop` 无效，列表第 10 行与底部分页不可达。需单独排查外壳滚动方案（影响面覆盖全部页面，不在本页内改）。
  ② ~~**批量操作条与左下角 user-dock 重叠**：视口宽约 769–850px 时 dock（`z-index:999`，`left:20px; bottom:20px`）压住批量条"取消"按钮文字并抢走点击。~~ **已于 2026/9/16 修复**：按用户要求直接在 `/knowledge-documents` 隐藏 user-dock（见改动记录）；`App.vue` 中 `provide('chatBatchMode')` 目前仍无人消费，属既有无效机制，未动。
  ③ ~~**`canReindex` 与注释不符**：注释写"未入库与失败状态可重新入库"，实现为 `!isRunning(doc)`，导致已完成文档也显示「重新入库」按钮。~~ **2026/9/16 二次修正**：先按"白名单（未入库/失败）"收紧，实测后用户反馈**日常用的重跑入口被一起收掉了**（"重新入库的按钮怎么没了"）——说明该按钮既是失败恢复也是主动重跑入口。最终方案：`canReindex` 恢复为 `!isRunning(doc)`（预处理中/向量化中不可点），并对**已完成**文档增加二次确认弹窗（重跑先删旧切片，失败会掉到「失败」态）；未入库与失败无切片可丢，直接重跑不确认。
  ④ ~~**`openDetail` 无请求时序保护**：先点 A→关闭→点 B 时，A 的响应后到会覆盖 B 的内容，失败分支还会关掉刚打开的 B 弹窗。~~ **已于 2026/9/16 修复**：`openDetail` 引入递增请求序号，仅采纳最新请求的响应；`closeDetail` 使进行中请求失效。已用"延迟首个请求造成乱序"的方式验证（见改动记录）。
  ⑤ **存量触控目标 <44px**：`.row-btn` 36×36（≤768px 才升 44）、`.pager-btn` 40×40、`.icon-btn` 40×40、`.check-circle` 22×22，属项目既有模式，本次未引入新风格。


## 本次改动记录（最新在前）

| 日期 | 改动 | 涉及文件/模块 | 是否已测/已审 |
|------|------|--------------|--------------|
| 2026/9/16 | **按用户反馈恢复「重新入库」入口 + 二次确认**：先前的收紧把管理员日常用的重跑入口一起收掉了（用户反馈"重新入库的按钮怎么没了"），故 ① `canReindex` 从白名单（仅未入库/失败）恢复为 `!isRunning(doc)`，预处理中/向量化中不可点、其余状态均可重跑；② 新增 `confirmReindexDoc` 状态与 `askReindex`/`confirmReindex` 函数 + 确认弹窗（复用既有 `.modal-content` 与 `.modal-btn.confirm.green`，文案含文件名与待删切片数），**已完成**文档点重跑先确认（重跑先删旧切片，失败会从「已完成」掉到「失败」），未入库/失败无切片可丢则直接重跑 | `frontend/src/views/KnowledgeDocuments.vue`、`AGENTS.md`（陷阱 55 重写）、`docs/known-pitfalls.md`（详情 55 重写）、`CHANGELOG.md`、`PROGRESS.md` | 是（前端 build 通过 + 格式核查；浏览器实测：10 行「已完成」显示 10 个重新入库按钮；点按钮弹确认框、文案为"将先删除已有 2 个切片"；**取消** → 0 次 reindex 请求、状态仍「已完成」；**确认重跑** → 1 次 `POST .../reindex`、状态转「预处理中」、toast 正确，轮询后回到「已完成」，接口复查 `COMPLETED` + `chunkCount: 2`（对切片最少的一篇 Git 文档做了真实重跑，幂等））；**副作用已处理**：该文档受版本管理，重跑会原地覆盖磁盘文件（本次多出 2 个尾部空行），已 git checkout 还原，工作区无残留；该行为已记入陷阱 56 |
| 2026/9/16 | **遗留问题 ③④ 修复 + `.gitattributes` 纳入版本管理**：① `canReindex` 由 `!isRunning(doc)` 收紧为白名单 `REINDEXABLE_STATUSES`（未入库 / 预处理失败 / 向量化失败），已完成文档不再显示「重新入库」按钮（**该收紧随后按用户实际使用反馈恢复，见上一条改动记录**）；② `openDetail` 增加递增请求序号 `detailRequestId`，仅采纳最新请求的响应，`closeDetail` 使进行中请求失效，修掉"先点 A → 关闭 → 点 B 时，A 的响应后到覆盖 B 内容 / A 失败时关掉 B 弹窗"的时序竞态；③ `.gitignore` 移除 `.gitattributes`，该文件（`/mvnw text eol=lf`、`*.cmd text eol=crlf`、`*.sh text eol=lf`）改为纳入版本管理，行尾规则从此对所有克隆生效 | `frontend/src/views/KnowledgeDocuments.vue`、`.gitignore`、`.gitattributes`（转为受版本管理）、`PROGRESS.md` | 是（前端 build 通过；③ 浏览器实测：10 行全部"已完成"时「重新入库」按钮 0 个、查看内容按钮完好；④ 用"延迟首个 `/content` 请求 2.5s"制造真实乱序，**反证**——撤掉守卫时最终显示 A（A 响应 5276ms 晚于 B 的 3906ms）、恢复守卫后显示 B，失败路径另测：A 请求报错时 B 弹窗仍在、内容为 B、无误报提示） |
| 2026/9/16 | **知识库管理页收尾项（按用户确认执行）**：① `/knowledge-documents` 隐藏左下角 user-dock——`App.vue` 的 `showDock` 增加该路径，修掉 769–850px 视口下 dock（`z-index:999`）压住批量操作条"取消"按钮文字并抢走点击；② `script/html-to-md.sh`、`script/preprocess-docs.sh` 由 CRLF 转 LF，消除 Git Bash 下 `bad interpreter: /bin/bash^M` 隐患（HEAD 中本就存 LF，故工作区转换后 `git diff` 为空、无需提交）；③ `.gitattributes` 补 `*.sh text eol=lf`，否则 `core.autocrlf=true` 会在下次检出时把脚本改回 CRLF；④ `AGENTS.md`「命名与代码风格」增加一行指向根目录 `.editorconfig`，并写明不引入 Spotless/Prettier | `frontend/src/App.vue`、`script/html-to-md.sh`、`script/preprocess-docs.sh`、`.gitattributes`（当时本机未跟踪且在 `.gitignore` 中，随后已按用户确认移出并纳入版本管理）、`AGENTS.md`、`PROGRESS.md` | 是（前端 build 通过；浏览器实测 800px 视口下批量条"取消"按钮 `elementFromPoint` 命中按钮自身、dock `opacity:0` + `pointer-events:none`；`/` 首页 dock 仍正常显示，`/knowledge`、`/knowledge-documents` 隐藏；两个脚本行尾复核 CRLF=0） |
| 2026/9/16 | **知识库管理页前端三处问题修复**（同分支 `knowledge-doc-manage`）：①排序下拉由原生 `<select class="sort-select">` 改为自定义下拉（触发按钮对齐搜索框视觉、`ChevronDown` 展开旋转，面板 `<Teleport to="body">` + `position:fixed` + 玻璃拟态 `blur(20px)` + `z-index:3000`，按触发按钮视口坐标定位、下方空间不足向上翻转、面板最小宽度对齐触发按钮；点击外部/Esc/Tab/页面滚动关闭；补 `aria-haspopup`/`aria-controls`/`role=listbox`/`aria-activedescendant` + 选项 `tabindex="-1"`，↑↓ 移动高亮、Enter 选中、选中后焦点交还触发按钮）；②③分块结果"挤在一页/内容被裁切、滚动条时有时无"：根因是 `.chunk-list` 为 flex 列容器且 `.chunk-item` 自带 `overflow:hidden`，按 Flexbox 规范子项自动最小尺寸为 0 → 被压扁裁切（实测 76 个分块项各 `offsetHeight:1px` 而 `scrollHeight:44px`），叠加 `.chunk-text` 仅 `max-height:320px` 形成双层滚动；修复为 `.chunk-item{flex-shrink:0}` + 折叠态卡片固定高度（头行 + 2 行预览，`-webkit-line-clamp:2` + `min-height:2.9em` 预留两行）+ 展开态正文固定 `height:320px` 内部滚动（补齐 6px 细滚动条样式），最右侧 Lucide `TriangleRight` 空心三角指示开合（展开 rotate(90deg)），展开后 `scrollIntoView` 保证可见；**顺带修一处本文件既有缺陷**：`watch(sort)` 原来在非首页时会重复发一次列表请求（应与 `reloadFromFirstPage()` 一致），实测由 2 次降为 1 次 | `frontend/src/views/KnowledgeDocuments.vue`、`PROGRESS.md` | 是（`npm run build` 通过；浏览器实测：修复前复现 76×1px、修复后 76 张卡片统一 87px 且零裁切、连 9 字最短分块也 87px；展开 363px＝头 41px+正文 320px，3124 字内部滚动、短分块不滚、箭头旋转与 `aria-expanded` 正确；下拉面板 fixed/Teleport/z-index/玻璃拟态/对齐/不出视口、选中/外部点击/Esc/滚动关闭均通过；排序请求次数由 2 降为 1、焦点回归触发按钮；375px 无横向溢出且卡片仍 87px；回归搜索/分页/批量/上传/删除确认/左侧正文栏通过；独立代码审查无 P0/P1，已采纳其 P2/P3 建议） |
| 2026/9/13 | **知识库文档管理（Web 端）落地**：①管理员 `role` 字段与门控（登录/me 返回 isAdmin，管理接口 service 层查库校验，非管理员 403）；②文档上传（仅 .md + 校验，后台异步三阶段流水线：预处理→原地覆盖→分块→向量化）；③列表（模糊搜索 / 时间与名称排序 / 分页）、状态实时轮询（预处理中/向量化中/已完成/未入库/失败+原因）、详情弹窗（预处理正文 + 分块结果）；④删除 / 批量删除（先删向量再删文件）与「重新入库」；⑤头像下拉「知识库管理」入口（仅管理员，页面可返回上一级，非管理员直连被拦截）；⑥加载器改为「源码目录优先 + classpath 回退」使运行时上传立即生效，并抽出稳定 id 与切片解析复用；⑦`DocumentPreprocessor` 分割线插入改为幂等（重跑不再累积 `---`）；⑧修复自测发现的缺陷：子目录 `yuque-sync/` 文档无法查看/删除/重入库、详情接口缺 `chunkCount`、重名判断大小写敏感、失败记录残留锁死文件名、前端勾选未随筛选/排序清理、轮询重叠、上传文件重选不生效；⑨格式化核查（机械化扫描本功能全部改动文件）：修复 `KnowledgeAppDocumentLoader.extractTopic` 方法签名与首句代码粘连、`KnowledgeDocumentService` 3 处超长行，补 `model/User` 文件末尾换行，`.editorconfig` 自身行尾统一为 CRLF；发现并补上一处检查盲区——未跟踪目录（`bootstrap/`）最初未被扫描覆盖，改为展开未跟踪目录后重扫；经与 HEAD 逐条比对，`App.vue`/`Login.vue` 的制表符与字体 `@import` 长行、`AiController` 长行、`DocumentPreprocessor` 行尾空格均属存量问题，未顺手改动以免污染本功能 diff | 新增 `constant/UserRole`、`model/DocumentStatus`、`model/KnowledgeDocumentDTO`/`DetailDTO`/`PageDTO`、`service/KnowledgeDocumentService`、`controller/KnowledgeDocumentController`、`config/KnowledgeDocumentTaskConfig`、`bootstrap/AdminAccountChecker`、`frontend/src/views/KnowledgeDocuments.vue`；改 `model/User`、`service/UserService`、`service/AuthService`、`repository/UserRepository`、`controller/AuthController`、`controller/AiController`、`controller/GlobalExceptionHandler`、`rag/KnowledgeAppDocumentLoader`、`rag/MongoVectorStoreConfig`、`rag/DocumentPreprocessor`、`application.yml`、`utils/auth.js`、`api/request.js`、`router/index.js`、`App.vue`、`views/Login.vue`、`AGENTS.md`（目录结构章节）、`PROGRESS.md`、新增根目录 `.editorconfig`（编辑器约定：UTF-8 / CRLF / Java 4 空格 / 前端与配置 2 空格 / Kotlin 4 空格 / 末行换行 / 去行尾空格，并按用户要求**不引入** Spotless、Prettier 等强制校验）；**包结构调整（按用户要求）**：`KnowledgeDocumentService` 与既有 `YuqueDocumentSyncService` 从 `rag/` 迁入 `service/`（分层优先），`AdminAccountChecker` 从 `config/` 迁入新建的 `bootstrap/`，`UserRole` 从 `model/` 迁入 `constant/`；为跨包复用把 `DocumentPreprocessor.processContent` 与 `KnowledgeAppDocumentLoader.extractTopic` 放开为 public | 是（后端 curl 全链路 + 失败路径 + 403/401 + 登录/历史/RAG 回归全部通过；前端 build 通过 + 浏览器实操；独立代码审查无 P0/P1 并已修 P2；独立功能交叉验证 10 项全通过，23 篇内置文档 MD5/切片数与测试前一致；包迁移后重新编译 + 启动自检 + 接口自检通过） |
| 2026/9/1 | README 新增「📸 界面预览」章节：Web / 安卓 APP / 微信小程序三端各 2 张运行截图（docs/screenshots/，英文命名） | README.md、docs/screenshots/（6 张图）、PROGRESS.md | 文档类改动，路径已验证 |
| 2026/9/1 | web 端交互优化修正：收起按钮移到侧边栏右缘外侧（left:260px 不居中），滚动条恢复贴右侧边框（撤销 .history-list margin-right:18px 让位方案，避免滚动条离边框太远）。陷阱 50 修复方案同步 | KnowledgeChat.vue、AGENTS.md、CLAUDE.md、docs/known-pitfalls.md、PROGRESS.md | 未测试（用户声明无需测试） |
| 2026/9/1 | web 端交互问题修复：①语音识别友好提示（前端过短拦截<0.6s + sttErrorMessage 映射 + 后端 400 附 message）；②历史对话滚动条与收起按钮重叠（.history-list 预留右侧 margin 18px）；③隐藏 /knowledge 页个人信息组件（showDock 恒 false，待优化）。文档同步 49/50/51 | KnowledgeChat.vue、App.vue、SpeechController.java、AGENTS.md、docs/known-pitfalls.md、PROGRESS.md | 前端 build 通过；后端 compile 通过（页面交互未 headless 验证） |
| 2026/8/30 | 验证流程闭环：违规诱捕自测 + code-test 子智能体实跑 | request.js（捕获 P1：生产 BASE_URL 命中陷阱 38）、PROGRESS.md | 是（前端构建 / 后端接口 11 项 / 消费方回归通过；浏览器 UI 无 headless 跳过） |
| 2026/8/30 | 规则文档体系重构 + 通用模板 + 测试子智能体 | CLAUDE.md、AGENTS.md、docs/known-pitfalls.md、docs/rules-template.md、docs/PROGRESS-template.md、PROGRESS.md、RULES.md(删)、~/.claude/agents/code-test.md、~/.zcode/agents/code-test.yaml | 文档类改动，结构已验证 |
| 2026/8/30 | Claude Code 上下文窗口放大 + 子代理模型修复 | ~/.claude/settings.json | 是 |

## 风险 / 遗留问题

- 🔴 **打包成 jar 部署后无法在线管理知识库文档**：`app.knowledge.document-dir` 默认指向源码目录 `src/main/resources/document`，jar 内该目录只读 → 上传与删除会失败（列表、查看仍正常）。要支持线上管理，把该配置指向可写目录即可，无需改代码。注意该配置写在 `src/main/resources/application.yml`（该文件被 `.gitignore` 忽略、不入库），代码里 `@Value` 已带同名默认值，因此换机器也能正常工作
- ⚠️ 管理页「已入库」切片数来自 MongoDB 向量集合；若把 `conditionProperty.ai.bean-type` 切到 `memoryVectorStore`，状态会全部显示「未入库」
- ⚠️ 主目录与 `yuque-sync/` 若出现同名 `.md`（只能由手工放置产生，上传会被重名校验拒绝），列表会出现两行同名、操作只作用于主目录那份。彻底修需把文档标识升级为「相对路径」，本次记为已知限制
- ⚠️ 上传的文档会被预处理**就地覆盖**（去 HTML 内联标签 + `##` 前插分割线），上传弹窗已提示。注意 `DocumentPreprocessor` 的 HTML 标签正则会吞掉尖括号内容（如 `List<String>` → `List`），这是既有离线流水线的同一行为，本次未改其语义（如需改进属另一需求）
- ⚠️ 任务状态存内存：后端重启后「失败原因」丢失，降级为「未入库」（文件与向量不受影响，点「重新入库」可恢复）
- ⚠️ 本机未安装 mongosh，管理员授权目前只能用 MongoDB Compass 或自备脚本执行（命令见「后续优化（待办）」）
- AGENTS_BAK.md 为未跟踪备份文件，按提交规则（只提交本次改动文件）不应入库
- AGENTS.md 与 CLAUDE.md 需保持一致（本次改动尚未同步规则文档，按规则待用户验收后统一同步）
- 自动化测试框架（JUnit/vitest）已决定不引入（P2 评估后放弃），测试维持手动 curl/页面
- MCP 服务暂不配置（后续可能接数据库 MCP，待定）
- ⚠️ 用 `local` profile 启动时，`spring.ai.mcp.client.stdio` 会用 `npx` 拉起高德地图 MCP server，启动偶发超时导致启动失败（本次自测期间遇到一次，改用默认 profile 即不触发）；该配置文件里 API Key 仍是占位值 `改成你的 API Key`
- code-test 子智能体已实跑验证生效（本次）；浏览器级 UI 交互无 headless 环境未覆盖
- ⚠️ 本次 web 端 3 处修复已做前端 build / 后端 compile 验证，但**页面级交互未 headless 验证**（录音需真实麦克风、滚动条重叠需窄屏视觉确认），建议用户本地 `npm run dev` 复核
- ⚠️ /knowledge 页个人信息组件(user-dock)已临时隐藏，副作用为桌面端该页暂无"修改密码/退出登录"入口，见「后续优化」

## 下一步

1. ⏳ **等待用户验收**「知识库文档管理」（分支 `knowledge-doc-manage`；启动：`script\start-backend.bat` + `cd frontend && npm run dev`，管理员入口在主页左下角头像下拉）
2. 验收后：合并回 `master` 并删除 `knowledge-doc-manage` 分支
3. 规则文档同步：**已完成（2026/9/16）**——`AGENTS.md`「目录结构」章节（补 `bootstrap/`/`constant/`/`degradation/`/`demo/` 四个包 + "分层优先" + "缺包就新建"）；「已知陷阱」索引新增 52–56（flex 子项压扁、浮层 Teleport+fixed、请求时序竞态、可操作状态白名单、知识库目录写入限制）并扩写 51（覆盖两页 dock 隐藏）；「命名与代码风格」补 `.editorconfig` 与 `.gitattributes` 两条；`docs/known-pitfalls.md` 同步详情 52–56 与 51；`README.md`「知识库文档维护」补 Web 管理页说明与 jar 部署限制；`CHANGELOG.md` 补 `0.0.1-SNAPSHOT / 2026-09-16` 条目
4. 提交注意：`frontend/src/api/request.js` 现已改回 `BASE_URL = ''`（陷阱 38 不再存在），且本次功能的新增接口都在该文件里，**需随本次改动一起入库**（原「排除 request.js」的旧决策已失效，请确认）
5. 提交注意：工作区里 `.agents/skills/ui-ux-pro-max/**` 的 28 项删除与本需求无关，按规则逐个 `git add <文件>`，不要带进来
6. **待办：存量空白/行尾统一（用户已同意）**——单独一个 `chore: 统一空白与行尾` 提交，清掉 `.editorconfig` 落地前留下的差异（`App.vue`/`Login.vue`/`KnowledgeChat.vue`/`ManusChat.vue`/`ChangePassword.vue` 的制表符缩进、`frontend/src/main.js`/`vite.config.js`/`application.yml`/`AiAgentApplication.java` 的 LF 行尾、`DocumentPreprocessor` 的行尾空格）。**必须在功能提交之后单独做**，否则会与功能 diff 混在同一批改动里，无法拆成两个提交
7. **`.gitattributes` 已按用户确认改为纳入版本管理**：`.gitignore` 中的该行已移除，文件现为可提交状态（内容：`/mvnw text eol=lf`、`*.cmd text eol=crlf`、`*.sh text eol=lf`），提交时需 `git add .gitattributes` 与 `.gitignore` 一起入库，否则行尾规则仍只在本机生效
8. 提交说明（已核实）：`src/main/resources/application.yml` 被 `.gitignore` 第 32 行忽略、未纳入版本管理，**故本次新增的配置项 `app.knowledge.document-dir` 不会入库**——代码里带了默认值 `src/main/resources/document`，其他环境不受影响；该配置项的含义已写进 README
9. 提交说明（已核实）：`src/main/resources/document/` 整个目录被 `.gitignore` 第 26 行忽略，**运行时上传的文档不会出现在 git 工作区**（仓库里那 23 篇是忽略规则之前就已入库的跟踪文件，不受影响）
10. 后续需求：用户管理功能（给用户授予管理员，见「后续优化（待办）」）
