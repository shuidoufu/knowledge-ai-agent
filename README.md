# AI Agent 智能代理平台

基于 **Spring Boot 3.5.10 + Java 17 + Vue 3** 的全栈 AI 智能代理平台。**对话模型使用 DeepSeek V4 Flash**，**向量模型使用阿里云百炼 DashScope（qwen-plus）**，支持 ReAct 模式 Agent、RAG 知识库检索、多会话聊天记忆，以及多种工具调用。

---

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| 🤖 **AI 超级智能体** | ReAct 模式 Agent，支持多步推理和工具调用（文件操作、网页搜索、PDF 生成、资源下载等） |
| 🧠 **个人知识助手** | 多源知识融合问答，基于 RAG 检索笔记与收藏，支持流式对话、引用标注和切片展示 |
| 📚 **RAG 知识库 + 引用标注** | MongoDB 向量库（自研 MongoVectorStore，应用层余弦检索），AI 回复标注来源编号 `[1]`、`[2]`，展开查看原文切片 |
| 🔄 **引用持久化** | 引用数据随消息持久化到 MongoDB，刷新页面不丢失 |
| 🧠 **双模型架构** | 对话使用 DeepSeek V4 Flash【@Qualifier("openAiChatModel") ChatModel chatModel】，向量化使用千问 Qwen-Plus（DashScope）【@Qualifier("dashscopeEmbeddingModel") EmbeddingModel】 |
| 💬 **聊天记忆** | MongoDB 持久化聊天记录，支持历史会话管理和知识库检索开关 |
| 🖼 **图片搜索与展示** | 联网图片搜索，统一图片代理服务（ImageProxyService）绕过防盗链，前端聊天内直接显示；硬防盗链素材站黑名单过滤 |
| 📄 **PDF 含图生成** | PDF 支持嵌入图片（Markdown 图片语法），图片下载走图片代理服务（多策略 Referer），统一缩放 |
| 🔗 **工作流引擎** | SPEL 表达式驱动的多步骤工具编排，支持预设工作流（pdf_report、image_album），由 LLM 根据场景自动触发 |
| 🛡 **统一异常降级** | 基于 AOP 注解的统一降级框架，支持重试、缓存、跳过、通知、备选五种降级策略 |
| 🔒 **登录鉴权** | JWT Token 认证，支持注册/登录/密码修改，注册含图片验证码校验；启动时随机生成密钥，重启后旧 token 自动失效 |
| 🎨 **现代化 UI** | 翠绿主题玻璃拟态风格，Lucide 统一图标库，流式 Markdown 渲染，RAG 来源知识卡片，移动端自适应，密码显示/隐藏切换 |
| 🎙 **AI 回复语音播报（TTS）** | 接入百炼 CosyVoice 语音合成，AI 回复一键语音播报，长文本自动分段合成，按内容 MD5 文件缓存（重复内容秒级返回） |
| 🎤 **麦克风语音输入（STT）** | 输入框麦克风按钮录音（Web Audio 降采样编码 WAV），接入百炼 Paraformer 实时识别，转写结果填入输入框，Ctrl+Enter 换行 |

---

## 🏗 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.5.10 | 应用框架 |
| Spring AI | 1.0.0-M6 | AI 模型统一调用（OpenAI 兼容接口 + DashScope） |
| DashScope SDK | 2.18.5 | 阿里云百炼（向量化模型） |
| MongoDB | - | 聊天记忆 + 向量库（自研 MongoVectorStore，集合 `vector_store`） |
| JWT (jjwt) | 0.12.6 | 登录鉴权（启动时随机生成签名密钥） |
| iText Core + font-asian | 9.1.0 | PDF 生成（嵌入微软雅黑中文字体） |
| jsoup | 1.19.1 | 网页抓取 |
| Hutool | 5.8.5 | Java 工具库 |
| Spring AOP | - | 统一降级注解（@Degradable） |
| Knife4j | 4.4.0 | API 文档 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5.x | 前端框架 |
| Vite | 5.4.x | 构建工具 |
| Vue Router | 4.4.x | 路由 |
| Axios | 1.7.x | HTTP 请求 |
| @lucide/vue | - | 图标库（Lucide SVG 图标集） |
| marked + DOMPurify | - | Markdown 安全渲染 |

---

## 🚀 快速开始

### 前置条件

- **Java 17+**（推荐使用 IntelliJ IDEA 自带的 JBR 17）
- **Node.js 18+**
- **MongoDB**（本地运行需安装）

### 1️⃣ 启动后端

```bash
# 直接运行 start-backend.bat（推荐）
start-backend.bat

# 或手动执行
set JAVA_HOME=<你的 Java 17 路径>
cd ai-agent
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="--enable-preview -Dfile.encoding=UTF-8"
```

> 项目使用了 Java 17 预览特性，运行时必须添加 `--enable-preview` 参数。  
> `-Dfile.encoding=UTF-8` 确保中文编码正确。

### 2️⃣ 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 3️⃣ 启动虚拟机前后端
1. 后端进行jar打包，上传至虚拟机中
2. 前端进行npm run build，将dist文件上传至虚拟机中


### 4️⃣ 访问

| 服务 | 地址 |
|------|------|
| 前端页面 | `http://localhost:5173` |
| 后端 API | `http://localhost:8123/api` |
| API 文档 | `http://localhost:8123/api/swagger-ui.html` |

### 默认账号

```
用户名：admin
密码：  admin
```

---

## 📁 项目结构

```
ai-agent/
├── src/main/java/com/example/aiagent/
│   ├── advisor/           # Spring AI Advisor（日志、提示词优化、RAG 引用捕获）
│   ├── agent/             # Agent 核心（BaseAgent → ReActAgent → ToolCallAgent → Manus）
│   ├── app/               # 业务应用（KnowledgeApp，含 RAG 流式对话）
│   ├── chatmemory/        # 聊天记忆（MongoDB / 文件）
│   ├── config/            # 全局配置（CORS, JWT, MCP 后备）
│   ├── controller/        # REST 接口
│   ├── degradation/       # 统一降级框架（@Degradable 注解 + AOP 切面，五种降级策略）
│   ├── filter/            # JWT 鉴权过滤器
│   ├── model/             # 数据模型（含 RAG 引用标注字段）
│   ├── rag/               # RAG 检索增强（MongoDB 向量库 MongoVectorStore、文档加载、查询重写、检索优化），MongoVectorStoreConfig（基于 MongoDB）
│   ├── repository/        # 数据访问层
│   ├── service/           # 业务逻辑（AuthService、CaptchaService、ImageProxyService 图片代理服务）
│   └── tool/              # Agent 工具（文件操作、PDF生成、图片搜索、工作流 WorkflowEngine/WorkflowTool 等）
├── frontend/
│   └── src/
│       ├── api/           # API 请求封装（含 RAG 流式接口）
│       ├── router/        # 路由配置
│       ├── utils/         # 工具函数
│       └── views/         # 页面（登录、首页、个人知识助手、超级智能体）
├── src/main/resources/
│   ├── application.yml    # 主配置（需自行填入 API Key）
│   ├── prompt.yml        # AI 系统提示词
│   └── document/          # RAG 知识文档（Markdown 格式）
└── pom.xml                # Maven 依赖
```

---

## 🔗 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/register` | 注册（需图片验证码） |
| POST | `/api/auth/change-password` | 修改密码 |
| GET | `/api/auth/captcha` | 获取图片验证码 |
| GET | `/api/ai/knowledge/chat/stream` | 知识助手纯文本流式对话 |
| GET | `/api/ai/knowledge/chat/rag/stream` | 知识助手 RAG 流式对话（含引用标注） |
| GET | `/api/ai/knowledge/chat/sse` | 知识助手 SSE 流式对话 |
| GET | `/api/ai/knowledge/chat/sync` | 知识助手同步对话 |
| GET | `/api/ai/knowledge/chat/history` | 历史会话列表 |
| GET | `/api/ai/knowledge/chat/history/{chatId}` | 历史会话详情（含引用数据） |
| PUT | `/api/ai/knowledge/chat/history/{chatId}/title` | 更新会话标题 |
| DELETE | `/api/ai/knowledge/chat/history/{chatId}` | 删除会话 |
| GET | `/api/ai/manus/chat` | 超级智能体对话 |
| GET | `/api/files/**` | 静态文件服务（访问 `tmp/` 下的 PDF、图片等） |
| GET | `/api/image-proxy?url=` | 图片代理下载（绕过防盗链，黑名单站点拦截） |
| POST | `/api/speech/tts` | 文本转语音（CosyVoice，返回 MP3，内容 MD5 缓存） |
| POST | `/api/speech/stt` | 语音转文字（Paraformer，上传 WAV 返回识别文本） |

### 🛠 Agent 工具列表

| 工具 | 说明 |
|------|------|
| `searchWeb` | 联网搜索（百度 AppBuilder） |
| `searchImages` | 联网图片搜索（Bing Images），返回原图 URL，无白边 |
| `scrapeWebPage` | 网页内容抓取 |
| `downloadResource` | 从 URL 下载资源到本地 |
| `generatePDF` | 生成 PDF 文档，支持文字、标题、图片嵌入 |
| `listPdfFiles` / `deletePdfFile` | PDF 文件管理 |
| `FileOperationTool` | 文件读写、目录操作 |
| `executeWorkflow` | 执行预设工作流（pdf_report、image_album），自动编排多工具按序执行 |
| `doTerminate` | Agent 任务结束信号 |

### 📖 RAG 引用标注流程

```
用户发送消息（知识库检索开关开启）
  ↓
GET /api/ai/knowledge/chat/rag/stream
  ↓
后端：QueryRewriter 智能判断（是否需要检索，单一判据：问题是否与个人笔记相关）
  ├── 无需检索 → 直接走普通流式对话
  ├── 需要检索 → 查询改写 → QuestionAnswerAdvisor（向量检索，相似度阈值 0.5）
  ├── DocCaptureAdvisor（捕获检索到的文档切片）
  ├── 注入引用标注指令 → AI 用 [1]、[2] 标注来源
  ├── 流式输出 AI 回复
  └── 流结束后追加 <!--RAG_REFS--> + JSON 引用数据
      ↓
前端解析分隔符
  ├── 提取引用数据 → 显示可折叠的引用卡片
  ├── 纯文本内容 → Markdown 渲染
  └── 引用持久化到 MongoDB（刷新页面不丢失）
```

### 🖼 图片处理流程

```
AI 搜索图片 → ImageSearchTool 获取原图 URL（murl，黑名单过滤防盗链素材站）
  ↓
前端显示 → ImageProxyController 代理加载（ImageProxyService 单次尝试，完整浏览器请求头）
  ↓
PDF 生成 → ImageProxyService 多策略下载（Bing Referer → 无 → 域名自身），iText 嵌入
```

- 图片来源：Bing Image Search 原始来源 URL，**无 CDN 白边填充**
- 防盗链：统一走 `ImageProxyService` 下载（携带完整浏览器请求头 `sec-ch-ua`、`sec-fetch-*` 等）
- 硬防盗链站点（昵图网、道客巴巴、图虫、视觉中国等）在搜索源头黑名单过滤
- 图片下载到 `tmp/download/`，PDF 生成后保留供后续使用

### 🔗 工作流引擎

```
用户请求匹配预设场景（如"搜索图片并生成PDF"）
  ↓
LLM 判断命中工作流 → 调用 executeWorkflow("image_album", query)
  ↓
WorkflowEngine 按序执行：
  Step1: searchImages(query)                         → ctx.imageUrls
  Step2: generatePDF(fileName, query + 图片列表)      → ctx.pdfFile
  ↓
返回结果给 LLM → LLM 告知用户
```

- 步骤参数用 `{表达式}` 占位符模板（SPEL 变量自动补 `#` 前缀），参数以 JSON 对象形式调用多参数工具
- 工具输出经 JSON 还原后作为步骤变量，通过注册的 SPEL 函数提取字段（`toImages` 提取图片 URL 转 Markdown、`extractUrl` 取首个 URL）
- 预置流程：`pdf_report`（搜索→图片搜索→抓取→生成PDF）、`image_album`（搜图→生成PDF）
- 不匹配预设场景时由 LLM 自行编排工具调用

### 🛡 统一异常降级

基于 `@Degradable` 注解 + AOP 切面，工具调用异常时自动执行降级策略：

| 策略 | 行为 | 适用场景 |
|------|------|---------|
| `NOTIFY_USER`（默认） | 捕获异常，返回用户提示 | 所有工具通用兜底 |
| `RETRY` | 自动重试 N 次（默认 3 次） | 网络波动等临时故障 |
| `USE_CACHE` | 返回上次调用成功的结果 | 搜索结果等幂等操作 |
| `SKIP` | 返回 null，跳过此工具 | 非关键路径的辅助工具 |
| `USE_ALTERNATIVE` | 提示已切换备选方案 | 有备用服务可切换的场景 |

使用示例：
```java
@Tool(description = "联网搜索")
@Degradable(strategy = FallbackStrategy.RETRY, maxRetries = 2)
public String searchWeb(String query) { ... }
```

---

## 🧪 开发说明

- **Java 预览特性**：编译和运行时均需 `--enable-preview`
- **中文编码**：JVM 启动参数需添加 `-Dfile.encoding=UTF-8`，否则 Markdown 文档加载会出现中文乱码
- **JWT 密钥**：每次启动随机生成，重启后旧 token 自动失效，无需重新登录配置
- **MCP 客户端**：默认禁用，如需启用请配置 `mcp-servers.json`
- **向量库**：MongoDB（自研 `MongoVectorStore`，集合 `vector_store`，应用层余弦检索），通过 `conditionProperty.ai.bean-type` 与内存向量库（SimpleVectorStore）互斥切换，启动时从 `classpath:document/*.md` 幂等增量加载知识文档
- **前端 UI**：使用 `/ui-ux-pro-max` 技能获取设计规范

---

## 虚拟机相关配置说明
1. 修改前端nginx.conf文件中的server_name为虚拟机IP地址。
2. 修改后端application.yml文件中的数据库连接地址为虚拟机IP地址。（MongoDB数据库等）
3. 修改前端中requests.js文件中的BASE_URL地址为虚拟机IP地址。

---

## 📄 协议

MIT License
