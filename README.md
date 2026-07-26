# AI Agent 智能代理平台

基于 **Spring Boot 3.5.10 + Java 17 + Vue 3** 的全栈 AI 智能代理平台。**对话模型使用 DeepSeek V4 Flash**，**向量模型使用阿里云百炼 DashScope（qwen-plus）**，支持 ReAct 模式 Agent、RAG 知识库检索、多会话聊天记忆，以及多种工具调用。

---

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| 🤖 **AI 超级智能体** | ReAct 模式 Agent，支持多步推理和工具调用（文件操作、网页搜索、PDF 生成、资源下载等） |
| 💕 **AI 恋爱大师** | 基于 RAG 知识库的情感问答，支持流式对话、历史记录、引用标注和切片展示 |
| 📚 **RAG 知识库 + 引用标注** | SimpleVectorStore 内存向量库，AI 回复标注来源编号 `[1]`、`[2]`，展开查看原文切片 |
| 🔄 **引用持久化** | 引用数据随消息持久化到 MongoDB，刷新页面不丢失 |
| 🧠 **双模型架构** | 对话使用 DeepSeek V4 Flash【@Qualifier("openAiChatModel") ChatModel chatModel】，向量化使用千问 Qwen-Plus（DashScope）【ChatModel dashScopeChatModel】 |
| 💬 **聊天记忆** | MongoDB 持久化聊天记录，支持历史会话管理和知识库检索开关 |
| 🖼 **图片搜索与展示** | 联网图片搜索，后端代理加载绕过防盗链，前端聊天内直接显示，无白边 |
| 📄 **PDF 含图生成** | PDF 支持嵌入图片（Markdown 图片语法），多策略下载原图，统一缩放 |
| 🔒 **登录鉴权** | JWT Token 认证，支持注册/登录/密码修改，注册含图片验证码校验；启动时随机生成密钥，重启后旧 token 自动失效 |
| 🎨 **现代化 UI** | 浅色玻璃拟态风格，流式 Markdown 渲染，移动端自适应，密码显示/隐藏切换 |

---

## 🏗 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.5.10 | 应用框架 |
| Spring AI | 1.0.0-M6 | AI 模型统一调用（OpenAI 兼容接口 + DashScope） |
| DashScope SDK | 2.18.5 | 阿里云百炼（向量化模型） |
| MongoDB | - | 聊天记忆存储 |
| JWT (jjwt) | 0.12.6 | 登录鉴权（启动时随机生成签名密钥） |
| iText Core + font-asian | 9.1.0 | PDF 生成（嵌入微软雅黑中文字体） |
| jsoup | 1.19.1 | 网页抓取 |
| Hutool | 5.8.5 | Java 工具库 |
| Knife4j | 4.4.0 | API 文档 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5.x | 前端框架 |
| Vite | 5.4.x | 构建工具 |
| Vue Router | 4.4.x | 路由 |
| Axios | 1.7.x | HTTP 请求 |
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

### 2️⃣ 启动虚拟机前后端
1. 后端进行jar打包，上传至虚拟机中
2. 前端进行npm run build，将dist文件上传至虚拟机中


### 3️⃣ 访问

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
│   ├── app/               # 业务应用（LoveApp，含 RAG 流式对话）
│   ├── chatmemory/        # 聊天记忆（MongoDB / 文件）
│   ├── config/            # 全局配置（CORS, JWT, MCP 后备）
│   ├── controller/        # REST 接口
│   ├── filter/            # JWT 鉴权过滤器
│   ├── model/             # 数据模型（含 RAG 引用标注字段）
│   ├── rag/               # RAG 检索增强（本地/PG 向量存储、文档加载、查询重写、元信息增强），PgVectorStoreConfig（基于 PGVector 向量数据库）
│   ├── repository/        # 数据访问层
│   ├── service/           # 业务逻辑（含 AuthService、CaptchaService 验证码服务、UserService）
│   └── tool/              # Agent 工具（文件操作、PDF生成、百度联网搜索、图片搜索等）
├── frontend/
│   └── src/
│       ├── api/           # API 请求封装（含 RAG 流式接口）
│       ├── router/        # 路由配置
│       ├── utils/         # 工具函数
│       └── views/         # 页面（登录、首页、恋爱大师、超级智能体）
├── src/main/resources/
│   ├── application.yml    # 主配置（需自行填入 API Key）
│   ├── prompt.yaml        # AI 系统提示词
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
| GET | `/api/ai/love_app/chat/stream` | 恋爱大师纯文本流式对话 |
| GET | `/api/ai/love_app/chat/rag/stream` | 恋爱大师 RAG 流式对话（含引用标注） |
| GET | `/api/ai/love_app/chat/sse` | 恋爱大师 SSE 流式对话 |
| GET | `/api/ai/love_app/chat/sync` | 恋爱大师同步对话 |
| GET | `/api/ai/love_app/chat/history` | 历史会话列表 |
| GET | `/api/ai/love_app/chat/history/{chatId}` | 历史会话详情（含引用数据） |
| PUT | `/api/ai/love_app/chat/history/{chatId}/title` | 更新会话标题 |
| DELETE | `/api/ai/love_app/chat/history/{chatId}` | 删除会话 |
| GET | `/api/ai/manus/chat` | 超级智能体对话 |
| GET | `/api/files/**` | 静态文件服务（访问 `tmp/` 下的 PDF、图片等） |
| GET | `/api/image-proxy?url=` | 图片代理下载（绕过防盗链） |

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
| `doTerminate` | Agent 任务结束信号 |

### 📖 RAG 引用标注流程

```
用户发送消息（知识库检索开关开启）
  ↓
GET /api/ai/love_app/chat/rag/stream
  ↓
后端：查询重写 → QuestionAnswerAdvisor（向量检索）
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
AI 搜索图片 → ImageSearchTool 获取原图 URL（murl）
  ↓
前端显示 → 后端 ImageProxyController 代理加载（绕过防盗链）
  ↓
PDF 生成 → 后端多策略下载（不同 Referer 重试），iText 嵌入
```

- 图片来源：Bing Image Search 原始来源 URL，**无 CDN 白边填充**
- 防盗链：后端代理下载，携带浏览器完整请求头
- 图片下载到 `tmp/download/`，PDF 生成后保留供后续使用

---

## 🧪 开发说明

- **Java 预览特性**：编译和运行时均需 `--enable-preview`
- **中文编码**：JVM 启动参数需添加 `-Dfile.encoding=UTF-8`，否则 Markdown 文档加载会出现中文乱码
- **JWT 密钥**：每次启动随机生成，重启后旧 token 自动失效，无需重新登录配置
- **MCP 客户端**：默认禁用，如需启用请配置 `mcp-servers.json`
- **向量库**：SimpleVectorStore（内存向量库），启动时从 `classpath:document/*.md` 加载知识文档
- **前端 UI**：使用 `/ui-ux-pro-max` 技能获取设计规范

---

## 虚拟机相关配置说明
1. 修改前端nginx.conf文件中的server_name为虚拟机IP地址。
2. 修改后端application.yml文件中的数据库连接地址为虚拟机IP地址。（MongoDB数据库等）
3. 修改前端中requests.js文件中的BASE_URL地址为虚拟机IP地址。

---

## 📄 协议

MIT License
