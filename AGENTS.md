# AI-Agent 项目规则

> 你正在处理 `ai-agent` 项目。以下指令**必须**遵守。
>
> **重要：每次完成代码修改后，必须同步更新此规则文件**，把本次修改中发现的重要陷阱、Bean 命名、配置要点等追加到对应章节，确保规则持续累积，避免重复踩坑。

---

## 🎯 项目身份

全栈 AI Agent 平台，基于 **Spring Boot 3.5.10 + Java 17 + Vue 3**，使用 **Spring AI 1.0.0-M6**，**对话模型使用 DeepSeek V4 Flash（通过 OpenAI 兼容接口）**，**向量模型使用阿里云百炼 DashScope（qwen-plus）**，支持 ReAct 模式 Agent、RAG 知识库（MongoDB 向量库）、JWT 鉴权。

---

## 🏗 包结构（代码该放哪）

```
advisor/     → Spring AI Advisor 横切逻辑（日志、提示词优化）
agent/       → Agent 核心（BaseAgent → ReActAgent → ToolCallAgent → Manus）
app/         → 业务应用（KnowledgeApp）
chatmemory/  → 聊天记忆（MongoChatMemory / FileBasedChatMemory）
config/      → 全局配置（CORS, JWT, Auth, McpFallbackConfig）
controller/  → REST 控制器（仅做转发，不含业务逻辑）
demo/invoke/ → AI 调用示例（4种方式）
filter/      → JWT 鉴权过滤器
model/       → 数据模型 / DTO
rag/         → RAG 检索增强（向量存储、文档加载、查询改写）
repository/  → 数据访问层
service/     → 业务逻辑
tool/        → Agent 工具（FileOperationTool、WebSearchTool、ImageSearchTool、WorkflowTool 等）
```

**请求流转**：Vue → Controller → Agent → Advisor链 → LLM → Tool调用 → 响应

---

## 📐 代码生成强制规则

### 通用（Java / Vue 均适用）
- ✅ **遵循项目目录结构**：新增功能按 `🏗 包结构` 章节归类放置（配置类 → config/、工具 → tool/、检索 → rag/、数据模型 → model/ 等）
- ✅ **该拆类就拆类**：功能独立时新建配置类、工具类、DTO 等，禁止把多个职责堆进一个大而全的类；类与文件命名见名知意，便于后续阅读、扩展、维护

### Java
- ✅ 使用 Lombok（`@Slf4j`, `@RequiredArgsConstructor`, `@Data`, `@Builder`）
- ✅ 依赖注入用构造方法（`private final` + `@RequiredArgsConstructor`）
- ✅ Controller 只做转发，不含业务逻辑
- ✅ 所有 public 方法写 Javadoc（纯文本格式，禁止使用 `<p>` `<ul>` `<li>` `<code>` 等 HTML 标签）
- ✅ 日志用 SLF4J 占位符格式：`log.info("action={}", action)`
- ✅ **保留原有注释和注释代码**：修改代码时，未涉及的注释、被注释掉的代码、Javadoc 必须原样保留，不得删除。只改动主题/功能相关的必要部分
- ✅ **注释只写代码的功能逻辑**：禁止在注释/Javadoc 中写"为什么这么改、踩坑原因、修改背景"等详情（踩坑记录统一放本文件"已知陷阱"章节）
- ✅ 异常抛给 `GlobalExceptionHandler` 统一处理
- ❌ 禁止硬编码敏感信息（API Key、密码、JWT Secret）
- ❌ 禁止字段注入 `@Autowired`

### Vue 3
- ✅ 必须使用 `<script setup>` + Composition API
- ✅ 所有 API 调用通过 `src/api/request.js` 的 Axios 实例
- ✅ 样式使用 `scoped`
- ✅ 组件名大驼峰（`KnowledgeChat.vue`）
- ✅ AI 的 Markdown 回复使用 `marked` + `DOMPurify` 渲染
- ✅ 图标统一使用 SVG（禁止使用 emoji 作为 UI 图标 — ❌ `🗑️` `🚀` `⚙️`）

#### Vue 认证状态管理
- ✅ **认证状态必须使用响应式 ref 导出**：在 `src/utils/auth.js` 中导出 `ref`（如 `export const username = ref(...)`），组件直接引用响应式 ref，**禁止**在 `computed` 中直接调用 `getUsername()` 等读取 localStorage 的方法（localStorage 非响应式，缓存值不会随登出/登录更新）
- ✅ `setToken()` / `removeToken()` 必须同步更新 localStorage **和** 对应的 ref 值

#### Vue 表单交互模式
- ✅ **密码输入框**必须提供显示/隐藏切换按钮（眼睛 SVG 图标：睁开眼 `👁` = 显示明文，睁眼+斜线 = 隐藏）。按钮位于输入框右侧，`position: absolute`，`tabindex="-1"`
- ✅ **表单校验失败**时使用 shake 动画（`animation: shake 0.5s ease`）+ 红色错误提示文字
- ✅ **按钮与输入框冲突场景**（blur 先于 click 触发）：操作按钮用 `@mousedown.prevent.stop`，blur 回调加编辑状态守卫（详见陷阱 7）

#### Vue 跨组件状态共享
- ✅ 使用 **`provide` / `inject`** 模式共享跨组件状态（如侧边栏折叠状态），禁止引入 Pinia/Vuex 等额外状态库

#### Vue 组件样式规范
- ✅ **CSS 自定义属性**在 `App.vue` 的 `:root` 中定义，命名统一：`--bg-xxx` / `--text-xxx` / `--border-xxx` / `--shadow-xxx`
- ✅ **返回按钮**统一使用胶囊形（`border-radius: 999px`）+ SVG 箭头图标 + 玻璃拟态背景
- ✅ **对话头像**：用户头像与 AI 头像大小一致（40×40px），使用 `margin-top` 对齐气泡第一行文字中心（约 10px）
- ✅ **侧边栏**：宽度固定 260px，历史项右侧 padding 留足空间（≥72px）给操作按钮组

## 🖼 图片识别规则

**收到图片（本地路径/URL）或看图意图时，必须调用 `/view-image` 技能**（智谱 GLM-4.6V-Flash 免费视觉模型），禁止凭文件名猜测图片内容。
调用方式：`node "C:\Users\陶逸峰\.zcode\skills\view-image\scripts\glm-vision.js" "<图片路径或URL>" --prompt "分析要求"`；OCR 场景 prompt 写"请完整提取图片中的文字，保留原有格式"。

## 🎨 前端 UI/UX 设计规则

**在进行任何前端 UI/UX 优化、样式修改、组件设计时，必须自动调用 `/ui-ux-pro-max` 技能**，以此获取设计系统的推荐和规范。

### 调用流程
1. 收到前端 UI/UX 相关的需求 → **必须先加载 Skill `ui-ux-pro-max`**
2. 根据需求关键词运行设计系统生成器获取推荐配色/字体/风格
3. 将推荐方案融入实际实现

### UI/UX 核心约束
- ✅ **交互反馈**：所有可点击元素加 `cursor-pointer`，hover 状态有视觉反馈（颜色/阴影过渡 150-300ms）
- ✅ **触控目标**：最小 44×44px
- ✅ **玻璃拟态风格**：卡面和弹窗优先使用 `backdrop-filter: blur()` + 半透明背景 + 柔和阴影
- ✅ **无障碍**：文本对比度 ≥4.5:1，focus 状态可见
- ✅ **动效**：微交互 150-300ms，使用 `transform`/`opacity` 而非 `width`/`height`
- ✅ **响应式**：支持 375px / 768px / 1024px / 1440px 断点

### 配色体系（当前项目）
```
主色     → #10B981（Emerald 翠绿）
辅助色   → #34D399
强调色   → #F59E0B（Amber 琥珀 — 高亮/知识引用标注）
背景色   → #ECFDF5
文字色   → #064E3B
边框色   → rgba(255,255,255,0.3)
阴影色   → rgba(16,185,129,0.06~0.15)
```

### 图标库
- 使用 `@lucide/vue` 作为图标库（替代手写内联 SVG）
- 所有图标基于 24×24 viewBox、1.5px 描宽、圆角端点，视觉统一
- 安装命令：`npm install @lucide/vue`

### 排版字体
- 标题/正文：`Plus Jakarta Sans`
- 装饰标题：`Playfair Display`（衬线）
- 诗意/书法文字：`Ma Shan Zheng`（马善政楷书）

### 生产安全检查清单
- [ ] 修改默认演示账号 admin/admin
- [ ] 替换 JWT Secret（强随机 ≥256 bits）
- [ ] 替换 DashScope API Key
- [ ] 数据库密码使用环境变量
- [ ] 关闭或限制 Knife4j/Swagger 文档

---

## ⚠️ 已知陷阱（避免踩坑）

1. **PGVector 二选一**：`spring-ai-pgvector-store`（手动）与 `spring-ai-starter-vector-store-pgvector`（自动）不可同时使用
2. **JWT Secret 长度**：HS256 需要 ≥256 bits
3. **iText 中文 PDF 字体**：`font-asian` 模块的 `STSongStd-Light` 虽然能加载但**不会嵌入 PDF**，导致用户设备无此字体时显示乱码。**修复方案：** 优先使用 Windows 系统字体路径（`C:/Windows/Fonts/msyh.ttc,0`），iText 9 的 `PdfFontFactory.createFont(String, String)` 会自动嵌入系统字体到 PDF 中，文件约 170KB。注意：
    - TTC 文件需加 `,0` 索引后缀（如 `msyh.ttc,0`）
    - iText 9 不支持 `PdfFontFactory.createFont(String, String, boolean)` 3 参数重载，只能用 2 参数版本
    - 禁止在 pom.xml 中声明多个 `font-asian` 版本，否则导致字体注册冲突
4. **LLM 调用限流**：调用方必须做好重试和降级
5. **MCP 客户端**：默认禁用（`spring.ai.mcp.client.enabled: false`）。如需启用，需配置有效的 `mcp-servers.json`。禁用时，由 `config/McpFallbackConfig.java` 提供空的 `ToolCallbackProvider` 替代 Bean，保证 `@Resource` 注入不失败。**不要删除或修改 `McpFallbackConfig.java`，否则启动会失败。**
6. **localStorage 非响应式**：Vue `computed` 或 `watch` 中直接读取 `localStorage.getItem()` 不会追踪变化，必须通过 ref 代理。所有认证相关组件必须从 `auth.js` 导入响应式 ref 而非调用 `getUsername()`
7. **blur 与 click 竞态**：输入框聚焦时点击外部按钮，`@blur` 先于 `@click` 触发。如果 blur 处理了保存并清空状态，click 可能会错误地重新进入编辑模式。修复方案：操作按钮使用 `@mousedown.prevent.stop`，blur 回调加 `editingChatId` 守卫
8. **CSS position: absolute 容器塌陷**：绝对定位元素不占用父容器空间。若父容器无显式宽度，子元素宽度可能为 0（如胶囊 bar 使用 `position: absolute; left: 24px; right: 0` 但父容器仅 48px 宽时，bar 实际只有 24px）。优先使用 flex + 负边距方案替代绝对定位
9. **Spring AI Alibaba Bean 命名全是小写 dashscope**：自动注册的 Bean 名称为 `dashscopeChatModel`、`dashscopeEmbeddingModel`（**全小写 dashscope**，不是驼峰 `dashScope`）。使用 `@Qualifier` 时必须拼写准确
10. **@Resource 按字段名匹配，大小写敏感**：`@Resource private ChatModel dashScopeChatModel;`（大写 S）不会匹配 bean 名 `dashscopeChatModel`（小写 s），会回退到按类型匹配。如果类型有多个候选则报错。**字段名必须与 bean 名完全一致**
11. **多模型共存时必须用 @Qualifier 区分**：当 `spring-ai-alibaba-starter`（千问）和 `spring-ai-openai-spring-boot-starter`（DeepSeek）同时存在时，会有 2 个 `ChatModel` Bean 和 2 个 `EmbeddingModel` Bean。每个注入点都必须明确指定用哪个：
    - 对话 → `@Qualifier("openAiChatModel")`（DeepSeek）
    - 向量化 → `@Qualifier("dashscopeEmbeddingModel")`（千问）
    - 查询重写 → `@Qualifier("openAiChatModel")`（DeepSeek）
12. **YAML 配置缩进严格**：YAML 对缩进极其敏感，修改 `application.yml` 等配置文件时必须使用正确层级，禁止混用 Tab 和空格。多 profile（local/prod）可能覆盖主配置
13. **切换模型时 embedding 需独立配置**：DeepSeek 的 embedding API 可能不可用（返回 404），需要保留千问的 DashScope 配置专门用于向量化。`spring.ai.openai.embedding.options.model` 可能需要单独指定
14. **@Tool 方法名不能重载（overload）**：Spring AI 的 `@Tool` 注解使用 Java 方法名作为工具注册名。同名的重载方法会导致启动时报错 `Multiple tools with the same name found in sources`。同一个类中每个 `@Tool` 方法必须有不同的方法名。需要用多参数方法替代重载，AI 模型会通过 `@ToolParam` 的描述决定传参
15. **ToolRegistration 循环依赖**：`ToolRegistration`（`@Configuration`）中通过 `@Resource` 注入 `WorkflowEngine`，而 `WorkflowEngine` 需要注入 `ToolCallback[]`（来自 `ToolRegistration.allTools()`），形成循环依赖。**修复方案**：打破循环有两种方式：
    - 方式一（推荐）：`WorkflowEngine` 不做 Spring Bean，在 `ToolRegistration.allTools()` 方法内本地 new 出来，先构建基础工具数组，再传入 `WorkflowEngine` 构造函数
    - 方式二：在 `WorkflowEngine` 的 `@Resource` 字段上加 `@Lazy`，延迟注入直到实际使用时才解析
16. **MongoDB 社区版不支持 Atlas Search**：`$vectorSearch` / `createSearchIndexes` 仅限 MongoDB Enterprise/Atlas（社区版 `--enableSearch` 报 `Unrecognized option`）。**项目已自研 `rag/MongoVectorStore.java`**（文档+向量存 MongoDB 集合 `vector_store`，应用层余弦检索 + filter 评估）。官方构件 `spring-ai-mongodb-atlas-store` 不要引入。`enableSearch`/`searchIndexManagement` 必须配置在 mongod.cfg **顶层**（与 `net:` 同级），放在 `net:` 子级会导致 mongod 启动失败
17. **DashScope embedding API 单次上限 25 条**：批量向量化（`embeddingModel.embed(docs, options, batchingStrategy)`）时默认 `TokenCountBatchingStrategy` 按 token 数分批（切片 token 少时单批可超 25 条），报 `The input texts limit 25`。修复：自定义按条数分批的 `BatchingStrategy`（每批 ≤20 条），见 `MongoVectorStoreConfig.DashScopeBatchingStrategy`
18. **Spring AI M6 的 Document 不可变**：没有 `setId()` 方法，需要 `Document.builder().id(x).text(x).metadata(x).build()` 重建。M6 的 `VectorStore` 接口需实现：`add(List<Document>)`、`delete(List<String>)`、`delete(Filter.Expression)`、`similaritySearch(SearchRequest)`；`Filter.Expression` 为 `(ExpressionType, Operand left, Operand right)` 结构，`Group` 操作数需解包
19. **向量库持久化的幂等增量加载**：为文档切片生成稳定 documentId（`文件名#内容MD5`），写入前查询集合已有 `_id` 过滤，避免重启重复调用 embedding API；`mongoTemplate.save` 为 upsert 语义（重复 id 覆盖）。DashScope 实际输出向量维度 **1536**（非配置项，索引/检索由 `EmbeddingModel.dimensions()` 决定）
20. **标题/metadata 不参与向量化（检索不准的头号原因）**：`MarkdownDocumentReader` 把标题放入 metadata 的 `title` 字段，而 embedding 仅基于正文（metadata 默认不参与）。**搜标题搜不到内容的修复：把标题拼入切片正文开头再入库**（见 `KnowledgeAppDocumentLoader.parseDocument`）。同理，`KeywordMetadataEnricher` 生成的关键词存 metadata 对检索无效，必须拼入正文或用于查询扩展
21. **MarkdownDocumentReader 按标题自动切片**：M6 的 reader 即使无 `---` 分割线也会按标题（##/###）切分，不要重复实现"补充分割线"逻辑；`---` 与标题都会产生切片边界。生成切片自带 `title`/`category` metadata（无需额外解析标题）
22. **status 等分类标签需语义化**：旧规则取文件名中间字符（`"1. JAVA.md"→"AV"`）无意义。改用文件主题（去序号前缀：`extractTopic` 正则 `^\\d+\\.\\s*`），便于按分类过滤检索
23. **中文长文本余弦相似度普遍偏低**：`QuestionAnswerAdvisor` 的 `similarityThreshold` 经验值 0.5（0.6 会过滤掉本应命中的相关切片，与遗留 `KnowledgeAppRagCustomAdvisorFactory` 阈值一致）
24. **ChatClient.user(String) 会走 PromptTemplate 模板渲染**：prompt 中出现 `{` `}` 字面量（如 JSON 示例 `{"needsRetrieval": true}`）会报 `The template string is not valid`。**修复：prompt 中禁止出现 `{}` 字面量，用文字描述 JSON 格式**。注意：M6 的 `ChatClientRequestSpec` 只有 `user(String)/user(Resource)/user(Consumer<PromptUserSpec>)`，没有 `user(Message)` 重载
25. **LLM 不调用预设工具 + 任务型请求误走 RAG**：①工具 @Tool 描述需强引导（"多步骤任务必须使用本工具，不要分别调用其他工具"），工作流调用引导写在 `prompt.yml` 系统提示词的"工具使用"章节；②`QueryRewriter` 判定用"单一判据"（问题是否可能与用户笔记/收藏相关）而非场景清单（场景无限），任务型请求（搜索图片、生成PDF、下载等）判为不需要检索；③即使预判错误也有 `QuestionAnswerAdvisor.similarityThreshold` 兜底（检索不到就不注入上下文），预判错误代价仅是毫秒级向量检索；④改写查询必须保留任务步骤，不得删减（如"搜索图片并生成PDF"不能改写成只剩"搜索图片"）
26. **WorkflowEngine SPEL 细节**：SPEL 变量引用必须带 `#` 前缀（`{query}` 要解析为 `#query`，裸 `query` 报 EL1007E）；工具输出是纯文本字符串无法属性访问（`{result.urls[0]}` 无效），需注册 SPEL 函数提取（如 `toImages` 提取 URL 转 Markdown 图片、`extractUrl` 取首个 URL）；多参数工具调用需构建 JSON 对象 `{"参数名": "值"}` 传给 `tool.call(String)`
27. **tool.call() 返回值会被 JSON 序列化**：Spring AI 的 `DefaultToolCallResultConverter` 把工具返回值 `JsonParser.toJson(result)`——String 值变成**带首尾引号和转义的 JSON 字符串字面量**（真实换行变成字面量 `\n`、引号变 `\"`）。工作流步骤间传值时必须用 `objectMapper.readValue(output, String.class)` 还原（`normalizeToolOutput`），否则后续基于行结构/正则的处理全部失效
28. **图片下载必须走 ImageProxyService**：Bing 等 CDN 防盗链校验需要完整浏览器请求头（`sec-ch-ua`、`sec-fetch-*` 等），仅 UA+Referer 会被拒（返回 403/防盗链占位图）。`service/ImageProxyService.java` 统一提供下载（完整请求头 + 多策略 Referer），`ImageProxyController`（前端展示，单次尝试 `fetchOnce`）与 `PDFGenerationTool`（PDF 插图，多策略 `fetch`）共用，禁止各自实现下载逻辑
29. **防盗链素材站需在搜索结果源头过滤**：部分站点（如 `nipic.com` 昵图网、`51wendang.com`、`dfic.cn` 图虫、`veer.com`、`quanjing.com`、`vcg.com` 视觉中国、`58pic.com`、`zcool.com.cn`）的图片无法通过任何请求头组合正常下载（403 或返回防盗链占位图，如昵图网返回"昵图网防盗链"占位图）。**在 `ImageSearchTool.BLOCKED_IMAGE_HOSTS` 黑名单过滤（搜索结果直接排除），遇到新的防盗链站点追加域名即可**。PDF 侧另有防御：下载的图片像素 <50x50 视为占位图跳过

---

## 🔑 关键配置

- 后端端口：`8123` | Context Path：`/api`
- **对话模型**：DeepSeek V4 Flash（通过 `spring.ai.openai.*` 配置）
- **向量模型**：千问 Qwen-Plus（通过 `spring.ai.dashscope.*` 配置，输出 1536 维）
- **向量库**：MongoDB（`rag/MongoVectorStore.java` 自研实现，集合 `vector_store`，应用层余弦检索；配置 `spring.ai.vectorstore.mongodb.*`）
- **向量库切换**：`conditionProperty.ai.bean-type` 条件化加载（`mongoVectorStore`=MongoDB / `memoryVectorStore`=内存 SimpleVectorStore，二者互斥，参照 `MyChatClientConfig` 的 `@ConditionalOnProperty` 模式）
- Swagger：`http://localhost:8123/api/swagger-ui.html`
- 默认登录：`admin / admin`
- ADVISOR 链：`MyLoggerAdvisor` → `ReReadingAdvisor` → RAG Advisors
- MCP 客户端：已禁用，由 `McpFallbackConfig` 提供后备注入
- **JVM 参数**：必须启用 `--enable-preview`（项目使用了 Java 17 预览特性）

---

## 📦 快速命令

```bash
# 后端启动（本地，推荐使用脚本）
start-backend.bat

# 或手动执行（需设置 Java 17 + --enable-preview）
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="--enable-preview"

# 前端启动
cd frontend && npm run dev

# 构建
./mvnw clean package -DskipTests
```

---

## 🧪 自测试规则

**每次完成代码修改后，必须进行自测试，除非用户明确说明"不需要测试"。**

### 测试范围

| 类型 | 覆盖内容 | 检查点 |
|------|---------|--------|
| **后端 API** | 新增或修改的接口 | 返回 HTTP 200/201、数据结构正确、异常场景返回合理错误码 |
| **后端编译** | 整个项目 | `mvnw compile` 无错误、`--enable-preview` 生效 |
| **后端启动** | 整个项目 | `start-backend.bat` 或 `mvnw spring-boot:run` 启动成功，**编译通过不代表能运行**（Bean 冲突、配置问题、工具名重复等只在启动时暴露） |
| **前端编译** | 整个前端 | `npm run build` 无错误、Vite 开发服务器无异常 |
| **前端功能** | 新增或修改的页面/组件 | 页面正常渲染、交互逻辑正确、无控制台报错 |
| **边界场景** | 空数据、错误输入、极端值 | 空列表显示友好提示、表单校验正常工作 |

### 测试流程

**后端重要功能（新增 Tool、修改核心逻辑等）必须进行严格测试：**

1. 启动后端（`start-backend.bat`）和前端（`npm run dev`）
2. **对新增/修改的 API 用 curl 或前端页面调用验证**，确认响应结果符合预期，而不是只看编译通过
3. 检查前后端日志是否有异常报错
4. 确认核心功能不受影响（登录、聊天、历史管理）
5. 测试完成后告知用户测试结果
6. 测试完毕后，关闭所启动的端口
7. **清理测试数据**：仅删除本次测试新产生的文件，**禁止使用 `rm -rf` 或通配符清空整个目录**（如 `rm -f tmp/pdf/*.pdf`），以免误删用户之前保存的文件。应精确删除已知的测试文件名
8. **代码审查**：每次代码修改完成且自测通过后，**必须调用 Code Review 子智能体**（`review-agent` 技能）审查本次改动：检查代码缺陷（正确性、安全性、明显错误等）+ 对照本文件"代码生成强制规则"的符合性（注释只写功能逻辑、保留原有注释、Lombok、构造注入、Javadoc 格式等），按 P0-P3 输出发现并修复后告知用户

> 注意：**编译通过不等于程序能运行**。Bean 冲突、配置问题、工具名重复、API 调用失败等缺陷只会在启动和实际运行时暴露。严格测试 = 编译 → 启动 → 接口调用 → 验证响应 → 清理数据。

---

## 📋 AI 输出前自检清单

- [ ] 包路径是 `com.example.aiagent.xxx`？
- [ ] 加了 `@Slf4j`？
- [ ] 用构造方法注入？
- [ ] 敏感信息没硬编码？
- [ ] 异常会被 `GlobalExceptionHandler` 捕获？
- [ ] Agent 新增 Tool 已注册？
- [ ] 涉及 MCP 相关的改动时，`McpFallbackConfig.java` 是否需要同步更新？
- [ ] 前端 UI/UX 改动前是否已加载 `/ui-ux-pro-max` 技能？
- [ ] 收到图片/看图需求时是否已调用 `/view-image` 技能？
- [ ] SVG 图标是否替代了 emoji？
- [ ] 密码输入框是否加了显示/隐藏切换？
- [ ] 认证相关组件是否从 `auth.js` 导入响应式 ref 而非调用 `getUsername()`？
- [ ] 编辑/操作按钮是否在必要时使用了 `@mousedown.prevent.stop` 避免 blur 竞态？
- [ ] Vue 组件用了 `scoped`？
- [ ] 文件放在正确的包/目录？
- [ ] **已完成自测试？**（后端编译+启动、前端编译+功能验证、核心接口测试）
- [ ] **是否已调用 Code Review 子智能体审查本次改动？**（代码缺陷 + AGENTS.md 规则符合性）
- [ ] **本次修改是否有需要写入规则的新发现？**（Bean 命名、配置要点、踩坑记录等 → 追加到对应章节）
