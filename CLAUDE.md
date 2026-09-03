# AI-Agent 项目规则

> 约束性规则文档。AI 在 `ai-agent` 项目里生成代码必须遵守，违反视为不合格。
> **维护（活系统）**：代码改动先交付验证，**用户确认无误后**再统一同步文档——规则/要点追加到 CLAUDE.md 对应章节；踩坑详情追加到 `docs/known-pitfalls.md` 并在索引补行；`README.md` / `docs/*.md` 中涉及本次改动的部分同步更新。避免每次小改动频繁动文档，未确认前不更新。

---

## 🎯 项目身份

全栈 AI Agent 平台：Spring Boot 3.5.10 + Java 17（必须 `--enable-preview`）+ Vue 3，Spring AI 1.0.0-M6。
对话模型 **DeepSeek V4 Flash**（OpenAI 兼容接口）；向量模型 **千问 Qwen-Plus**（DashScope，1536 维）；RAG 用自研 MongoDB 向量库；JWT 鉴权。
三端：**Web**（frontend/）· **微信小程序**（miniprogram/，分支 knowledge-miniprogram）· **安卓壳**（android/）。

---

## 🏗 目录结构（代码该放哪）

| 包 | 职责 |
|---|---|
| `advisor/` | Advisor 横切逻辑（日志、提示词优化） |
| `agent/` | Agent 核心（BaseAgent→ReActAgent→ToolCallAgent→Manus） |
| `app/` · `chatmemory/` | 业务应用（KnowledgeApp）· 聊天记忆 |
| `config/` · `filter/` | 全局配置（CORS/JWT/Auth/McpFallbackConfig）· JWT 过滤器 |
| `controller/` | REST 控制器（**只做转发，不含业务**） |
| `model/` · `repository/` | 数据模型/DTO · 数据访问层 |
| `rag/` | 向量存储/文档加载/查询改写 |
| `service/` · `tool/` | 业务逻辑 · Agent 工具 |
| `notes/` · `docs/` | 知识库素材（处理后作 RAG）· 文档 |

新增功能按职责归类：配置→config/，工具→tool/，检索→rag/，DTO→model/。

---

## 📚 文档地图（按需读取）

> 动手前先 Read 对应文档获取最新知识，禁止凭记忆猜。

| 场景 | 读什么 |
|------|--------|
| 项目总览 / 快速开始 / API / 构建部署 / 运维 | `README.md` |
| 安卓 APP 构建、安装、部署 | `docs/android-app.md` |
| 微信小程序开发、真机调试 | `docs/miniprogram.md` |
| 踩坑详情（索引编号对应） | `docs/known-pitfalls.md` |
| 通用规则模板（新建项目起步） | `docs/rules-template.md` |
| 项目进度（每次开发后更新） | `PROGRESS.md` |
| 项目进度模板（新项目起步） | `docs/PROGRESS-template.md` |

---

## 📐 代码生成强制规则

### 通用
- ✅ **先读文档再动手**：开发/运维前，先 Read「📚 文档地图」对应文档获取最新知识，禁止凭记忆猜
- ✅ **拿不准就问**：需求理解、技术方案、代码行为有疑问，必须向用户提出，禁止自行猜测
- ✅ **遵循目录结构**；**该拆类就拆类**，禁止把多职责堆进一个大而全的类
- ✅ **保留无关的原有注释/注释代码**，只改动主题相关的必要部分
- ✅ **文档随改动同步（活系统，确认后统一更新）**：新增/修改/修复功能后，**等用户验收确认无误**再一次性同步涉及文档——README.md 与 docs/*.md 的相关部分、踩坑详情（追加到 `docs/known-pitfalls.md` 并在索引补行）、文档地图描述若过期一并修正；用户确认前不更新文档

### Java
- ✅ Lombok（`@Slf4j` / `@RequiredArgsConstructor` / `@Data` / `@Builder`）；依赖注入用构造方法（`private final`）
- ✅ Controller 只做转发；public 方法写 Javadoc（纯文本，禁 `<p>`/`<ul>`/`<code>` 等 HTML 标签）
- ✅ 日志用 SLF4J 占位符：`log.info("action={}", action)`
- ✅ 异常抛给 `GlobalExceptionHandler` 统一处理
- ✅ 注释只写功能逻辑，禁止写"为什么这么改/踩坑原因/背景"（踩坑详情进 `docs/known-pitfalls.md`）
- ❌ 硬编码敏感信息（API Key、密码、JWT Secret）；字段注入 `@Autowired`

### Vue 3
- ✅ `<script setup>` + Composition API；样式 `scoped`；组件名大驼峰
- ✅ API 调用统一走 `src/api/request.js`；AI 的 Markdown 回复用 `marked` + `DOMPurify` 渲染
- ✅ 图标统一 SVG（@lucide/vue），**禁止 emoji 作 UI 图标**
- ✅ 认证状态：从 `auth.js` 导入响应式 ref，禁止在 computed 里直接读 localStorage/`getUsername()`；`setToken`/`removeToken` 同步更新 localStorage 与 ref
- ✅ 密码框必须有显示/隐藏切换（眼睛 SVG，`position:absolute`，`tabindex="-1"`）；表单校验失败用 shake 动画 + 红色提示
- ✅ blur/click 竞态：操作按钮用 `@mousedown.prevent.stop`，blur 回调加编辑状态守卫
- ✅ 跨组件状态用 `provide`/`inject`（禁止 Pinia/Vuex）
- ✅ CSS 变量定义在 `App.vue` `:root`（`--bg-*` / `--text-*` / `--border-*` / `--shadow-*`）
- ✅ 返回按钮胶囊 `border-radius:999px` + SVG 箭头 + 玻璃拟态；对话头像 40×40 等大（margin-top 对齐首行文字）；侧边栏 260px、历史项右 padding ≥72px

---

## 🎨 前端 UI/UX 硬约束

**任何 UI/UX 改动前必须先加载 `/ui-ux-pro-max` 技能**获取设计规范。

- ✅ 可点击元素 `cursor-pointer` + hover 反馈（150-300ms 过渡）
- ✅ 触控目标 ≥44×44px；文本对比度 ≥4.5:1；focus 状态可见
- ✅ 动效用 `transform`/`opacity`（非 width/height）；响应式断点 375/768/1024/1440px
- ✅ 卡面/弹窗优先玻璃拟态（`backdrop-filter: blur` + 半透明 + 柔和阴影）
- 配色：主 `#10B981` · 辅 `#34D399` · 强调 `#F59E0B` · 背景 `#ECFDF5` · 文字 `#064E3B`
- 字体：正文 Plus Jakarta Sans · 装饰标题 Playfair Display · 书法 Ma Shan Zheng
- 图标：`@lucide/vue`，24×24 viewBox / 1.5px 描宽 / 圆角端点

---

## ⚠️ 已知陷阱（索引）

> 编号 + 一句话摘要。写代码遇相似报错/场景先看编号命中，再查 **`docs/known-pitfalls.md`** 详情。

### 后端陷阱（Spring AI / Spring Boot / Java）
1. **PGVector 二选一**：pgvector-store 手动/自动构件不可同时使用
2. **JWT Secret 长度**：HS256 需 ≥256 bits
3. **iText 中文 PDF 字体**：优先用系统字体 `C:/Windows/Fonts/msyh.ttc,0`（iText 9 仅支持 2 参 createFont）
4. **LLM 调用限流**：调用方必须做重试与降级
5. **MCP 客户端默认禁用**：勿删改 `McpFallbackConfig.java`
9. **DashScope Bean 名全小写**：`dashscopeChatModel` / `dashscopeEmbeddingModel`（非驼峰）
10. **@Resource 字段名大小写敏感**：字段名必须与 bean 名完全一致
11. **多模型共存用 @Qualifier 区分**：对话→openAiChatModel，向量化→dashscopeEmbeddingModel
12. **YAML 缩进严格**：禁止混用 Tab/空格，注意多 profile 覆盖
13. **embedding 需独立配置**：DeepSeek embedding 可能 404，保留 DashScope 专门向量化
14. **@Tool 方法名不能重载**：同名重载启动报错，用多参数方法替代
15. **ToolRegistration 循环依赖**：WorkflowEngine 本地 new（推荐）或 @Lazy
16. **MongoDB 社区版无 Atlas Search**：用自研 `MongoVectorStore`，勿引入 atlas-store 构件
17. **DashScope embedding 单次上限 25 条**：自定义按条数分批的 BatchingStrategy
18. **Spring AI M6 Document 不可变**：builder 重建；VectorStore 实现 4 个方法
19. **向量库幂等增量加载**：documentId=文件名#内容MD5，upsert 防重复向量化
20. **标题/metadata 不参与向量化**：标题须拼入正文再入库（检索不准头号原因）
21. **MarkdownDocumentReader 按标题切片**：勿重复实现补分割线逻辑
22. **status 分类标签语义化**：用 extractTopic 去序号前缀，勿取文件名中间字符
23. **中文长文本相似度偏低**：similarityThreshold 经验值 0.5（0.6 会漏召回）
24. **ChatClient.user(String) 走模板渲染**：prompt 禁止出现 `{}` 字面量
25. **LLM 不调预设工具 / 误走 RAG**：工具描述强引导 + 单一判据 + 阈值兜底
26. **WorkflowEngine SPEL**：变量须带 `#` 前缀；工具输出无法属性访问，需注册 SPEL 函数
27. **tool.call() 返回值被 JSON 序列化**：需 readValue 还原（normalizeToolOutput）
28. **图片下载必须走 ImageProxyService**：防盗链需完整浏览器请求头
29. **防盗链素材站源头过滤**：双层防线（搜索排除+下载前拦截），含主站+CDN 域名
30. **长文本禁止放 GET query**：>8KB 触发 Tomcat 400，一律 POST + JSON body
31. **Spring 6 无 AUDIO_MPEG 常量**：用 MediaType.parseMediaType("audio/mpeg")
32. **DashScope TTS 接入要点**：Bean 驼峰命名；模型/音色须匹配；必须重试 + 缓存（tmp/tts）
34. **DashScope STT 接入要点**：离线识别仅公网 URL；本地走实时 WS；需超时保护 + 重试
37. **Knife4j 4.5.0 + springdoc 2.8.9**：Spring Boot 3.5 需显式钉版本，勿误删
40. **AI 误把 tmp/file 当知识库**：prompt.yml 明确"工作缓存非知识库"，禁止主动 listFiles
45. **历史会话按 updatedAt 排序**：null 回退 createdAt 且排最后，勿在 MongoDB 层 Sort
46. **RAG 引用门控**：仅当回复含 [n] 标注才下发 references

### 前端陷阱（Vue Web）
6. **localStorage 非响应式**：必须用 auth.js 响应式 ref，禁止 computed 里读 getUsername()
7. **blur 与 click 竞态**：操作按钮用 @mousedown.prevent.stop，blur 回调加 editingChatId 守卫
8. **CSS absolute 容器塌陷**：优先 flex + 负边距替代绝对定位
33. **Blob URL 必须成对 revoke**：onended/onerror/主动停止/play reject 四路径
38. **生产 BASE_URL 用相对路径**：`''` + 同源 /api（nginx 反代），index.html 加 no-cache
39. **语音播报按钮已隐藏**：v-if="false"，脚本与样式原样保留勿删
41. **移动端适配要点**：16px 防 iOS 放大 / 100dvh / 抽屉侧边栏 / safe-area / ≥44px 触控
42. **flex 子项 min-width:auto 挤出屏幕**：容器加 min-width:0 + 换行布局
43. **/g 正则 lastIndex 残留**：使用前必须重置 lastIndex=0
44. **AI 下载地址需前端链接化**：linkify.js 链接化 /api/ 根相对路径，跳过 pre/code
49. **语音识别(STT)错误提示**：录音过短(<0.6s)前端拦截；后端 400 响应带 message；前端优先取 response.data.message，再按状态码/网络/超时映射中文提示
50. **历史对话滚动条与收起按钮重叠**：收起按钮置于侧边栏右缘外侧（left:260px 不居中），滚动条保持贴右侧边框
51. **user-dock 与历史对话重叠**：/knowledge 页 showDock 恒 false，暂时隐藏个人信息组件（布局待后续优化）

### 脚本工具陷阱（html-to-md / 文档处理）
35. **HtmlToMarkdownConverter 要点**：getWholeText / 递归子节点防自环 / 跳过代码围栏 / 保留原文
36. **书签模式要点**：三种格式识别 / isBookmarksJson 收紧 / 路径引号包裹 / Cookie 绕 521

### 移动端陷阱（安卓 APP + 微信小程序）
47. **安卓 WebView 壳要点**：onCreateWindow 拦截 target=_blank / 三条下载路径 / 录音 HTTPS+双层权限 / 菜单 Teleport 防裁剪
48. **微信小程序要点**：真实 AppID / es6:false / 预览链语法降级 / 图片本地化下载 / 录音参数 / 流式纯文本渲染

---

## 🔑 关键配置

- 端口 `8123` · Context Path `/api` · Swagger `/api/swagger-ui.html` · 默认账号 admin/admin
- 对话：DeepSeek V4 Flash（`spring.ai.openai.*`）；向量：qwen-plus（`spring.ai.dashscope.*`，1536 维）
- 向量库：MongoDB（`MongoVectorStore`，集合 `vector_store`；`conditionProperty.ai.bean-type` 可切内存库）
- ADVISOR 链：MyLoggerAdvisor → ReReadingAdvisor → RAG Advisors
- MCP 客户端默认禁用（`McpFallbackConfig` 兜底，勿删）；JVM 必须 `--enable-preview`

---

## 📦 快速命令

```bash
script\start-backend.bat                    # 后端启动（或 mvnw spring-boot:run 加 --enable-preview）
cd frontend && npm run dev                  # 前端
./mvnw clean package -DskipTests            # 构建
script\html-to-md.bat <网页收藏目录>         # 网页收藏 → 知识库 md
script\preprocess-docs.bat <笔记目录>        # 原始笔记清洗
cd android && gradlew.bat assembleDebug     # 安卓 APK（需 JDK17+SDK，见 docs/android-app.md）
cd miniprogram && npm install && npm run dev:mp-weixin   # 小程序编译
```

---

## 🔧 Git 提交规则

- ✅ 只**精确 `git add <文件>`**；禁止 `git add .` / `-A` / `*`
- ✅ 提交前 `git status` 核对改动列表；提交后 `git push`（先 `git pull`）
- ✅ 一律排除：`frontend/dist/`、`**/__pycache__/`、`.idea/`、`application*.yml`（含密钥）、测试临时文件
- ✅ 提交信息格式：首行 `YYYY/M/D：`（不补零、全角冒号）；每条一行 `1. 名词+动词；`；多行信息用 heredoc

---

## 🔖 分支 / 版本 / 变更记录

### 分支策略
- `master` 为稳定主干，只接受已测试的功能合并
- 功能/修复从 `master` 拉分支，命名见名知意（如 `knowledge-miniprogram`）
- 完成流程：自测 + 审查 → 合并回 `master` → 删除功能分支

### 版本号（SemVer）
- 格式 `主版本.次版本.修订号`：主版本=不兼容变更，次版本=向后兼容新功能，修订号=向后兼容修复
- Maven 项目版本在 `pom.xml` 维护；安卓版本在 `app/build.gradle.kts`（`versionCode` 整数递增 / `versionName` 如 `1.0.0`）

### 变更记录（CHANGELOG）
- 根目录 `CHANGELOG.md`，按版本号倒序记录
- 每版本条目：`版本号 / 日期` + 分条变更（新增/修复/优化/重构），与 Git 提交信息格式一致

---

## 🧪 自测试规则

**每次代码修改后必须自测（用户声明"不需要测试"除外）。**

- **后端**：编译 → 启动 → 新改接口 curl 验证 → 查日志 → 回归登录/聊天/历史 → 关闭端口
- **前端**：`npm run build` 无错 + 页面交互验证
- **边界**：空数据/错误输入/极端值要有友好提示
- **🚨 功能回归红线**：改动前梳理关联点（共用组件/请求层/工具函数/事件链/样式/跳转链路）；改动后逐一回归所有受影响功能——最隐蔽风险是"改一个功能悄悄弄坏另一个"
- **测试卫生**：测试文件统一 `tmp/test-*` 且命名带 test 标识；清理只删本次产生文件，**禁止 `rm -rf`/通配符清目录**；测试产物不得进知识库/提交
- **独立功能测试**：开发后调用功能测试子智能体（Zcode / Claude Code 的 `code-test`）交叉验证，避免"自己写自己测"的确认偏差
- **Code Review**：自测通过后调用代码审查（Claude Code `/code-review` 或 Zcode `code-review` 子智能体）审查（缺陷 + 规则符合性），按 P0-P3 修复并告知用户
- **进度更新（PROGRESS.md）**：开始新需求前，先写入根目录 `PROGRESS.md` 的「需求/任务清单」；开发/运维结束后更新任务状态、本次改动、下一步、风险，保持会话间衔接

> 编译通过 ≠ 能运行：Bean 冲突、配置问题、工具名重复只在启动时暴露。

---

## 📋 AI 输出前自检清单

- [ ] 包路径 `com.example.aiagent.*`？`@Slf4j`？构造注入？无硬编码？异常走 `GlobalExceptionHandler`？
- [ ] Agent 新增 Tool 已注册？涉及 MCP 改动时 `McpFallbackConfig` 是否需同步？
- [ ] UI/UX 改动前已加载 `/ui-ux-pro-max`？SVG 替代 emoji？密码框有切换？auth.js 响应式 ref？blur 竞态已处理？
- [ ] 文件放对目录？Vue 组件 `scoped`？
- [ ] 已完成自测 + 回归受影响功能 + Code Review？
- [ ] 新发现已写入：规则→CLAUDE.md 对应章节，踩坑详情→docs/known-pitfalls.md
- [ ] 本次改动的相关文档（README.md / docs/*.md / 文档地图描述）已同步更新？
- [ ] 项目进度文档 `PROGRESS.md` 已更新？
- [ ] 涉及版本变更时，`CHANGELOG.md` / 版本号已更新？
