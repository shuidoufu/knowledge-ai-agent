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
notes/       → 存放的网页收藏技术文档书签 + 语雀技术文档（处理后，用作 RAG 知识库）
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
29. **防盗链素材站需在搜索结果源头过滤（含 CDN 域名，双层拦截）**：部分站点（如 `nipic.com` 昵图网、`51wendang.com`、`dfic.cn` 图虫、`veer.com`、`quanjing.com`、`vcg.com` 视觉中国、`58pic.com`、`zcool.com.cn`）的图片无法通过任何请求头组合正常下载（403 或返回防盗链占位图，如昵图网返回"昵图网防盗链"占位图）。**关键坑：Bing 返回的图片 URL 通常托管在站点的图片 CDN 域名上（如昵图网的 `ntimg.cn`），只收录主站域名（`nipic.com`）会漏过滤**。黑名单统一维护在 `constant/HotlinkImageConfig.java`（大小写不敏感 contains 匹配，需同时收录主站域名与 CDN 域名，遇到新的防盗链站点/其 CDN 域名追加即可），**双层防线**：①`ImageSearchTool` 搜索结果直接排除；②`ImageProxyService.tryDownload` 下载前拦截（历史消息/漏网 URL 也无法加载）。PDF 侧另有防御：下载的图片像素 <50x50 视为占位图跳过
30. **长文本禁止放 GET query 参数**：URL 编码后超过 Tomcat 默认 `max-http-header-size`（8KB）时，Tomcat 直接返回 400 HTML 错误页（请求根本到不了 Controller，表现为"长文本请求失败、短文本正常"）。**长文本一律走 POST + JSON body**（见 `SpeechController`：文本放 `SpeechRequest.text`）。同类问题排查时先确认 400 响应是 JSON（业务层）还是 HTML（Tomcat 层）
31. **Spring 6 的 MediaType 无 AUDIO_MPEG 常量**：`MediaType.AUDIO_MPEG` / `AUDIO_MPEG_VALUE` 都不存在（编译报"找不到符号"），音频类型用 `MediaType.parseMediaType("audio/mpeg")`
32. **DashScope 语音合成（TTS）接入要点**：`spring-ai-alibaba-starter` 自动注册 `DashScopeSpeechSynthesisModel`（Bean 方法名是**驼峰** `dashScopeSpeechSynthesisModel`，与 chat/embedding 的全小写 `dashscopeXxx` 不同），按类型注入 `SpeechSynthesisModel` 接口即可（当前仅一个实现）。调用链：`SpeechSynthesisPrompt(text, DashScopeSpeechSynthesisOptions(model, voice))` → `call()` → `getResult().getOutput().getAudio()`（ByteBuffer）。**CosyVoice 模型与音色必须匹配**（cosyvoice-v1 用 `longxiaochun`/`longcheng`，v2/v3 音色名带 `_v2`/`_v3` 后缀）；单次合成有长度上限（按 ≤900 字符分段，分段结果 MP3 字节直接拼接可连续播放）；**DashScope WS 合成偶发断连**（`SocketException: 你的主机中的软件中止了一个已建立的连接`），合成调用必须重试（见 `SpeechSynthesisService` 的 `MAX_RETRIES`）；音色/模型建议配置化（`ai.tts.*`）便于切换。**TTS 合成慢（短文本 1-2s，长文本可达 40s+），必须加缓存**：`SpeechSynthesisService` 按**清洗后文本的 MD5** 作 key，合成结果存 `tmp/tts/{md5}.mp3`（文件持久化，重启不丢，命中直接读文件返回，毫秒级）；相同 key 并发请求用 `ConcurrentHashMap` 锁防重复合成；⚠️ **DashScope 额度用尽时 WS 调用不报错而是挂起不响应**（前端表现为"播报无声音 + 120s 超时"），遇到此现象优先检查百炼控制台语音模型额度
33. **前端 Blob URL 必须成对 revoke**：`URL.createObjectURL(blob)` 创建的 URL 在播放结束（`onended`）、出错（`onerror`）、主动停止、播放失败（`play()` reject）四条路径都要 `URL.revokeObjectURL`，否则每次播报泄漏一个 Blob URL。停止/切换播报时统一在 `stopSpeech()` 里处理
34. **DashScope 语音识别（STT）接入要点**：百炼**录音文件识别（离线 paraformer-v2）仅支持公网 URL**（不支持本地文件/Base64），本地音频必须走**实时识别 WS**（`paraformer-realtime-v2`，`dashscope-sdk-java` 的 `com.alibaba.dashscope.audio.asr.recognition.Recognition`，注意 2.18.5 无 `realtimev2` 包）。半双工调用链：`RecognitionParam.builder().model().sampleRate(16000).format("pcm").apiKey()` → `recognition.call(param, callback)` → 逐帧 `sendAudioFrame(ByteBuffer)`（每帧 100ms 音频）→ `stop()`；回调 `isSentenceEnd()` 时取 `sentence.getText()` 拼接。**必须加超时保护**（`CountDownLatch.await(30s)`，WS 挂起教训同 TTS）+ 失败重试 3 次；WAV 需 16kHz 单声道 16bit PCM（前端 Web Audio API 录制 + 手动降采样 + 纯 JS 封装 44 字节 WAV 头，见 `SpeechRecognitionService` / KnowledgeChat.vue 的 `encodeWav`）。前端录音细节：**ScriptProcessor 不要 connect 到 destination（会扬声器回放产生回声）**；`AudioContext` 需 `await resume()`（自动化合成点击不被识别为用户手势时会 suspended 导致采集不触发）
35. **HTML 转 Markdown 工具（HtmlToMarkdownConverter）要点**：①**jsoup 1.19.1 的 `TextNode` 没有 `wholeText()` 方法**，保留原始空白（代码块缩进/换行）必须用 `getWholeText()`（编译报"找不到符号"时先 javap 查 jar 里的实际方法名）；②**递归转行内格式时禁止把元素自身传给下一层**（`appendWrapped(sb, el)` 内再调 `inlineText(el)` 会对 strong/a 等标签无限自环 → StackOverflowError），必须改为递归 `el.childNodes()`（子节点不会命中父级 case）；③**`## 前插 `---` 分割线必须跳过 ``` 代码围栏**，否则代码里的 `## 注释` 会被插入分割线破坏代码并产生错误切片边界，处理时按行扫描维护 `inFence` 状态；④**代码块原文保留**：`<pre><code>` 用 `getWholeText()` 取原始文本，`List<String>` 等泛型/小于号不会被清洗逻辑误删（该工具不执行 DocumentPreprocessor 的 HTML 标签剥离，jsoup 解析后标签不会泄漏进文本节点）；⑤**通用文件名重命名**：浏览器"另存为"的 `index.html` 等用页面 `<title>` 重命名输出（清洗非法字符），否则知识库 status 标签会变成无意义的 "index"；⑥**含第三方依赖的工具类不能裸 `java <文件>.java` 启动**（classpath 只有当前目录），必须走 `mvnw exec:java -Dexec.mainClass=...` 或项目根目录的 `html-to-md.bat/.sh` 脚本
36. **书签模式（html-to-md 解析浏览器收藏夹）要点**：①**Chrome/Edge 的 `Bookmarks` 文件是 JSON 且无扩展名**，书签导出有**三种格式**：JSON（精确名 `Bookmarks` 无扩展名）、**NETSCAPE-Bookmark-file-1 HTML**（按文件头含 `NETSCAPE-Bookmark` 识别）、**Chrome 导出的 `# Bookmarks` Markdown**（按 `.md` 扩展名识别，正则 `\[([^\[\]]*)\]\((https?://[^)\s]+)\)` 提取链接）；②**书签文件识别顺序不能靠"文件名含 bookmarks"**——导出 HTML/Markdown 文件名都含 bookmarks（如 `bookmarks.md`），`isBookmarksJson` 必须收紧为 `.json` 扩展名或**精确名 `Bookmarks`**，否则导出文件会被 Jackson 按 JSON 解析报 `Unexpected character`；③**`exec:java` 的 `-Dexec.args` 按空格分词，路径含空格（如 Chrome 的 `User Data\Default`）必须用引号包裹**（bat/sh 脚本已用 `'%INPUT_DIR%'` 单引号包裹，bat 中单引号是字面字符无转义问题），否则路径被截断报"输入文件或目录不存在"；④**下载细节**：HttpURLConnection 需完整浏览器 UA（部分站点校验 UA）+ 20s 超时 + 重试 2 次；下载存原始字节流，编码检测交给 `Jsoup.parse(file, null)`（自动读 meta）；下载失败只记日志不中断整体流程（**CSDN 等站点偶发/常发 HTTP 521 反爬拦截**——curl 带完整头也绕不过，属站点风控，失败清单打印后可稍后重跑或手动打开另存 HTML 走目录模式）；⑤**下载缓存 `tmp/bookmarks-html/`**（按书签标题命名，重名加序号，已存在的文件跳过下载，重复运行秒级完成）；⑥**并发下载用固定线程池**（Java 17 无虚拟线程，`newFixedThreadPool(4)` + Future.get 收集失败列表）；⑦**书签 JSON 结构**：`roots` 下 bookmark_bar/other/synced 三个根文件夹，节点 `type=url` 有 `url`/`name` 字段，`type=folder` 有 `children` 递归，按 URL 去重 + 过滤 chrome:// 等非 http 链接；⑧**无 alt 的 `<img>` 直接跳过**（点赞/收藏/分享等图标无 alt 只有 URL，输出 `![url](url)` 会成检索噪音），有 alt 的内容图才保留；CSDN 页面噪音类（`.article-info-box` 博主信息、`.toolbox-list` 点赞收藏、`.recommend-box` 推荐阅读等）已收录进 NOISE_SELECTOR；⑨**目录模式必须识别书签 HTML**：用户常把导出的书签 HTML 放进网页收藏目录一起处理，若按普通网页转换会输出一个"书签列表"md（内容只有链接标题，污染知识库）——`processDirectory` 遍历时对每个 `.html/.htm` 先查文件头 `NETSCAPE-Bookmark`，书签文件收集后统一走 `processBookmarksHtml` 书签管线，普通网页才直接转换；⑩**CSDN 等站点 HTTP 521 风控应对**：521 是基于 TLS/请求指纹 + 访问频率的服务端风控，完整 UA/Referer/头组合（curl 实测）均绕不过，**有效手段是携带浏览器 Cookie**（`--cookie "xxx=yyy; ..."` 直接传参或 `--cookie-file <文件>` 从文件读，取法：浏览器 F12 → Network → 右键 Copy as cURL 提取 Cookie 字段），`downloadUrl` 在 cookieHeader 非空时加 `Cookie` 请求头；Cookie 属敏感信息，禁止硬编码在代码里，只经命令行/临时文件传入（tmp 目录已在 .gitignore）；缓存目录 `tmp/bookmarks-html` 已存在的文件跳过下载，若 Cookie 过期导致下载到风控页，需删缓存重跑；**bat/sh 脚本必须透传 `--cookie`/`--cookie-file` 参数**（脚本用 shift 循环解析，`--cookie` 系列参数可出现在任意位置，位置参数 1/2 仍是输入/输出目录，值用单引号包裹避免空格截断）——若脚本只取 `%1` `%2` 两个位置参数，`--cookie-file` 会被当成输出目录，表现为"输出目录变成 --cookie-file"

37. **Knife4j 4.4.0 的 springdoc 2.3.0 与 Spring Boot 3.5（Spring 6.2）不兼容**：访问 `/api/v3/api-docs` / Swagger 页面报 `NoSuchMethodError: 'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'`（Spring 6.2 移除了单参构造方法）。**修复：knife4j 升到 4.5.0，并在 pom.xml 显式声明 `springdoc-openapi-starter-webmvc-ui` 2.8.9**（knife4j 4.5.0 传递依赖仍是旧 springdoc 2.3.0，必须显式钉版本覆盖，勿误删该显式依赖）。该报错只影响 Swagger 文档接口，登录/业务接口不受影响——排查时先 curl 业务接口（如 `/api/auth/login`）区分是文档问题还是业务问题

38. **前端生产环境 BASE_URL 必须用相对路径（同源）**：`const BASE_URL = import.meta.env.DEV ? '' : 'http://localhost:8123'` 这类写法在生产构建后，外网用户浏览器里的 `localhost` 指向访问者自己的电脑，登录/聊天全部失败（只有部署机本机访问碰巧能用）。**正确写法：`const BASE_URL = ''`**，请求走同源 `/api/...`，由 nginx 反代到后端，任何设备可访问；流式接口用 `new URL(BASE_URL + '/api/...', window.location.origin)` 同样生效。**配套缓存坑**：nginx 对 js/css 设置 `expires 1y` 时，改完前端代码只重启 nginx 没用——浏览器缓存旧文件导致"还是旧页面"，必须 `npm run build` 后**硬刷新（Ctrl+F5）**或清缓存；根治方案：nginx 对 `index.html` 单独加 `Cache-Control: no-cache`（Vite 产物文件名带 hash，index.html 更新后自然拉到新 JS，静态资源仍可长缓存）

39. **语音播报按钮已隐藏（KnowledgeChat.vue，演示需要）**：前端「播报」按钮通过 `v-if="false"` 隐藏，TTS 脚本逻辑（`toggleSpeech`/`speechAudio`/`stopSpeech`）与 `.speech-btn` 样式**原样保留**（隐藏后为不可达死代码，符合"保留原有代码"规则，勿误删）；恢复方法：把按钮的 `v-if="false"` 还原为 `v-if="msg.content && !(loading && i === messages.length - 1)"`

40. **AI 会把 tmp/file 工作目录误当成"知识库"**：`FileOperationTool` 的 `listFiles`/`readFile` 根目录是 `{user.dir}/tmp/file/`（工作缓存目录，非知识库），已注册进 Agent 工具链。`prompt.yml` 的"用户引导"章节指示"输入模糊时主动查看用户知识领域"，DeepSeek 收到"？"等模糊输入后会**主动调用 `listFiles` 扫描 tmp/file**，把里面的缓存文件（如测试写入的 `编程导航.txt`、手动放置的 `shanghai_date_plan.txt`）当作"知识库"向用户汇报；文件删除后 AI 又会说"知识库是空的"——**此时向量数据库从未被查询**（`QueryRewriter` 把"？"判为不需要检索，走无 RAG 的 `doChatByStream`）。排查此类"AI 看到不该看的数据"问题时：先查 `chat_memory` 集合对应会话确认是工具调用还是 RAG 引用（`references` 字段），再直连 MongoDB 查 `vector_store` 集合 `metadata.filename` 确认向量库实际内容。**修复方案**：`prompt.yml` 明确"tmp/file 是文件工作缓存目录，不是知识库，仅在用户明确要求时调用文件工具，禁止主动 listFiles 扫描或把其中文件当作知识库介绍"；"用户引导"章节禁止输出"你的知识库是空的"之类表述，未命中检索时直接引导可问方向（联网搜索/资料整理/网页解读/文件管理等）

41. **移动端适配要点（前端）**：
    - **iOS Safari 聚焦输入框自动放大**：输入控件 font-size < 16px 时聚焦会自动 zoom。聊天页 `textarea` 原为 `0.95rem`（15.2px），必须在 `@media (max-width: 768px)` 下提升到 `1rem`；登录/改密输入框已是 1rem 不受影响。**不要用 `user-scalable=no` 禁缩放**（无障碍要求），靠 16px 字号根治
    - **移动端浏览器地址栏高度变化**：`height: 100%` 在 iOS Safari/Android Chrome 上可能高于可视区导致布局跳动。`.app` 需 `height: 100%; height: 100dvh;`（dvh 不支持的旧浏览器自动回退 100%），聊天页内部滚动才稳定
    - **KnowledgeChat 移动端侧边栏是抽屉（off-canvas）**：`@media (max-width: 768px)` 下 `transform: translateX(-100%)` 滑入，此时 `.toggle-sidebar-btn`（边缘切换按钮）是 `display: none`——**必须另加汉堡按钮**（`.sidebar-menu-btn`，桌面 `display:none`、移动端显示，放 header-left 最左）+ 遮罩层 `.sidebar-backdrop`（`position:absolute; inset:0; z-index:15`，低于 sidebar 的 20）点击关闭，否则手机上历史会话无法打开
    - **固定定位元素遮挡**：App.vue 的 `.user-dock`（左下角）在移动端聊天页会压住底部输入区、`.brand-logo`（右上角）会压住页面头部——移动端聊天页（`/knowledge`、`/manus`）两者都要隐藏（`showDock`/`hideBrand` computed 判断 `window.innerWidth <= 768` 且路由为聊天页），用户回首页仍可访问账号
    - **刘海屏/底部横条**：`viewport-fit=cover` + 固定定位元素用 `env(safe-area-inset-*)`（`.user-dock` 的 bottom、`.brand-logo` 的 top、聊天页 `.input-area` 的 `padding-bottom`）
    - **移动端发送按钮**：聊天页 send-btn 原 `padding: 0 28px` 带"发送"文字太宽，移动端改为 44×44 纯图标（模板文字包 `<span class="btn-text">`，移动端 `display:none`）
    - **Home 应用卡片**：`≤480px` 时改为横向布局（图标左 + 文字中 + 箭头右，`flex-direction: row`），比纵向堆叠更省空间；触控目标 ≥44px
    - 移动端检测统一用 `window.innerWidth <= 768` + `resize` 监听（App.vue / KnowledgeChat.vue 各维护 `isMobile` ref，组件卸载时移除监听），避免各组件硬编码 `window.innerWidth` 判断

42. **flex 子项默认 `min-width: auto` 会把内容挤出屏幕（聊天页"内容被裁切/挤出"头号原因）**：`KnowledgeChat` 的 `.chat-container { flex-grow: 1 }` 是 `.chat-layout`（row flex）的子项，未设 `min-width: 0` 时其 min-content 宽度由输入区决定（textarea 固有宽度 `cols` 约 190px + RAG 工具栏 + 麦克风 44 + 发送 44 + gap），在窄屏（如 375px）下容器被撑到 443px，外层 `.chat-layout { overflow: hidden }` 把右侧裁掉——**右对齐的用户气泡（`align-self: flex-end`）被切出屏幕**，表现为"用户提问和 AI 回复不在同一画面、内容被挤出"。桌面宽度充裕不触发，只在移动端暴露。**修复**：①`.chat-container` 加 `min-width: 0`；②移动端 `.input-area { flex-wrap: wrap }` + `.input-toolbar { flex: 1 1 100% }` 让 RAG 工具栏独占一行，避免和 textarea/按钮挤一行；③`.bubble-content` 加 `min-width: 0` + `overflow-wrap: anywhere`；④Markdown 表格原本无样式会撑破气泡，必须加 `.markdown-body table { display: block; overflow-x: auto }`（气泡内横向滚动）。排查手段：CDP `Emulation.setDeviceMetricsOverride` + `getBoundingClientRect()` 对比容器宽与视口宽（本机 Edge/Chrome headless 可用 `--remote-debugging-port` + Node 内置 WebSocket 驱动）

43. **带 /g 标志的正则 lastIndex 跨调用残留（前端工具函数隐蔽 bug）**：模块级 `const R = /.../g` 的 `.test()`/`.exec()` 会推进 `lastIndex`，**多次调用同一函数时上次的 lastIndex 会残留到下次**——若上次匹配位置超过本次字符串长度，`.test()` 直接返回 false，函数静默跳过处理（表现为"同样的输入，有时转换有时不转换"）。**修复：每次使用前必须 `R.lastIndex = 0` 重置，或在函数入口统一重置**。本项目 `frontend/src/utils/linkify.js` 即因此踩坑（连续转换多个消息时部分 URL 不链接化）。排查手段：CDP 页面内逐步执行正则观察 lastIndex

44. **聊天消息中 AI 返回的下载地址（`/api/files/pdf/xxx.pdf`）必须前端链接化**：`PDFGenerationTool` 返回根相对路径（无站点前缀），用户无法直接访问。**修复方案（前端 `src/utils/linkify.js` + `renderMarkdown`）**：在 `DOMPurify.sanitize` 之后对 HTML 文本节点做链接化——匹配 `https?://`、`www.`（补 `https://`）、`/api/` 根相对路径（点击时浏览器自动基于当前站点解析，生产环境经 nginx 同源反代同样生效），跳过 `pre`/`code`/已有 `a` 内的文本；`/api/` 前缀限制避免误转普通斜杠文本（如"1/2"）；中文文件名正常保留，但地址尾部粘连中文（如 `a.pdf。下载吧`）需按"扩展名/路径段后跟中文"启发式裁剪。不要尝试在后端拼绝对 URL（后端无法可靠知道公网域名/IP）。配套 `prompt.yml` 已改为引导 AI 输出"点击链接即可下载"，后端重启后生效

45. **历史会话排序必须用 `updatedAt`（修改时间）而非 `createdAt`**：`chat_memory` 文档含 `createdAt`/`updatedAt` 两个字段；`MongoChatMemory.add()` 每次追加消息都会刷新 `updatedAt`（含用户消息和 AI 回复），所以对话后修改时间天然会更新。**历史列表（`AiController.getKnowledgeAppHistory`）必须按 `updatedAt` DESC 排序**（最新对话置顶），旧数据 `updatedAt` 为 null 时回退 `createdAt` 且排最后（用 Java `Comparator` + `nullsLast`，勿在 MongoDB 层直接 `Sort.by(updatedAt)`，null 顺序不可控）。配套：`ChatHistoryDTO` 需暴露 `updatedAt`；前端历史时间显示用 `chat.updatedAt || chat.createdAt`；`KnowledgeApp` 两个流式聊天方法在 flux `.doOnComplete` 时调用 `touchUpdatedAt(chatId)` 兜底刷新修改时间（RAG 引用持久化时同步刷新）

46. **RAG 引用标注必须门控在"AI 实际引用了知识库"（引用门控）**：检索（`QuestionAnswerAdvisor` 相似度阈值 0.5）返回的切片**不等于**被 AI 使用——AI 可能明确回答"知识库中没有检索到数据"却仍附上 3 篇不相关切片引用，用户视角即为"没检索到还显示引用"。**修复（`KnowledgeApp.doChatByStreamWithRag`）**：用 `.doOnNext` 累积流式回复全文，流结束后仅当 `docs 非空 且 回复包含引用标注 [n]`（`containsCitation`，正则 `\[\d{1,2}\]`，限制 1-2 位数字避免误匹配年份如 [2026]）时才追加 `<!--RAG_REFS-->` 并持久化 references，否则 `Flux.empty()` 不下发（前端 `references.length > 0` 自然隐藏）。曾尝试过"传 `hasUsefulRefs` 标识"方案，但标识语义若仍是"docs 非空"就与 references 非空等价、解决不了问题，已还原；**判定"有用"的唯一可靠信号是 AI 回复中的 [n] 标注**（系统提示词已有引用规范引导 AI 引用时标注 [1]、[2]）。副作用：AI 用了知识库却不标 [n] 时引用会隐藏，属可接受权衡

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

# 网页收藏 HTML 转知识库 Markdown 文档（默认输出到 document 目录）
html-to-md.bat D:\网页收藏目录
# 或 html-to-md.sh /path/to/html-notes

# 原始 Markdown 笔记清洗（默认输出到 document/yuque-sync）
preprocess-docs.bat D:\笔记目录

# 构建
./mvnw clean package -DskipTests
```

---

## 🔧 Git 提交规则

**提交代码时：只提交本次改动的代码文件，提交信息按"日期 + 分条编号"格式。**

### 提交原则

- ✅ **只提交改动代码**：必须精确 `git add <文件路径>`（可一次加多个文件），**禁止** `git add .` / `git add -A` / `git add *`（会把构建产物、缓存、测试临时文件全部提交）
- ✅ **提交前先检查**：`git status` 查看改动列表，确认只包含本次改动的文件
- ✅ **排除以下文件**（无论 git status 是否显示，一律不提交）：
  - 构建产物：`frontend/dist/`（npm run build 输出，不是源码）
  - 缓存目录：`**/__pycache__/`、`.idea/` 等
  - 本地敏感配置：`application.yml`、`application-local.yml`、`application-prod.yml`（含 API Key 明文，已在 .gitignore）
  - 测试临时文件：测试生成的 mp3、日志、临时脚本等
- ✅ **删除文件**：`git rm <文件>` 或用 `git add <已删除的文件路径>`（Git 会记录删除）
- ✅ **提交后推送**：`git push` 推送到当前分支（如 `knowledge`），提交前 `git pull` 同步远端

### 提交信息格式

```
2026/8/10：
1. PDF生成时，图片代理相关服务优化，防止出现防盗链图片（屏蔽网站）；
2. 查询重写优化，闲聊问题不调用RAG；
3. 新增工作流引擎功能
```

规则要点：

- 第一行：`YYYY/M/D：`（日期不补零，如 `2026/8/10`，后跟中文全角冒号 `：`）
- 每条改动一行，编号 `1.` `2.` `3.`，每条以中文分号 `；` 结尾（最后一条可省略）
- 描述格式：`名词 + 动词` 的短语（如"新增工作流引擎功能"、"查询重写优化"），一句话说清改了什么
- 一个功能多文件改动 → 合并为一条；多个独立功能 → 分条列出

### 提交命令示例（Git Bash）

```bash
# 1. 精确添加本次改动文件
git add src/main/java/com/example/aiagent/service/SpeechSynthesisService.java \
        src/main/java/com/example/aiagent/controller/SpeechController.java \
        frontend/src/views/KnowledgeChat.vue

# 2. 提交（多行信息用 heredoc，避免引号转义问题）
git commit -F - <<'EOF'
2026/8/11：
1. AI回复支持语音播报，接入百炼CosyVoice语音合成（TTS）；
2. 新增语音合成接口与重试降级，长文本自动分段
EOF

# 3. 推送
git push
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
7. **测试文件必须明显标注 + 清理测试数据**：
   - **测试用临时文件/目录统一放在 `tmp/test-*`**（如 `tmp/test-html`、`tmp/test-out`），命名带 `test` 标识，禁止与真实数据目录（`tmp/download`、`tmp/pdf`、`tmp/tts` 等）混放
   - **测试产物文件名加 `_test` 后缀或 `测试` 字样标注**（如 `test-知识库转换.html`），并在测试开始时明确告知用户"以下文件为测试临时文件，测试后删除"，方便用户区分
   - 测试完成后**仅删除本次测试新产生的文件**，**禁止使用 `rm -rf` 或通配符清空整个目录**（如 `rm -f tmp/pdf/*.pdf`），以免误删用户之前保存的文件。应精确删除已知的测试文件名（`tmp/test-*` 目录删除前先 `ls` 确认内容）
   - **测试文件不得进入知识库/提交**：测试产物禁止输出到 `src/main/resources/document/`（用临时输出目录），若测试写入知识库目录必须测试后精确删除并复查
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
