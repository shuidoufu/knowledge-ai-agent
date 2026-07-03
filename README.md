# AI Agent 智能代理平台

基于 **Spring Boot 3.5.10 + Java 17 + Vue 3** 的全栈 AI 智能代理平台。集成了阿里云百炼 DashScope 大模型，支持 ReAct 模式 Agent、RAG 知识库检索、多会话聊天记忆，以及多种工具调用。

---

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| 🤖 **AI 超级智能体** | ReAct 模式 Agent，支持多步推理和工具调用（文件操作、网页抓取、PDF 生成等） |
| 💕 **AI 恋爱大师** | 基于 RAG 知识库的情感问答，支持流式对话和历史记录 |
| 🧠 **多模型支持** | Spring AI + DashScope SDK + LangChain4J + HTTP 直连，四种调用方式 |
| 📚 **RAG 知识库** | PostgreSQL + PGVector 向量存储，支持文档检索增强生成 |
| 💬 **聊天记忆** | MongoDB 持久化聊天记录，支持历史会话管理 |
| 🔐 **登录鉴权** | JWT Token 认证，支持注册/登录/密码修改 |
| 🎨 **现代化 UI** | 浅色玻璃拟态风格，流式 Markdown 渲染，移动端自适应 |

---

## 🏗 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.5.10 | 应用框架 |
| Spring AI | 1.0.0-M6 | AI 模型统一调用 |
| DashScope SDK | 2.18.5 | 阿里云百炼 |
| MongoDB | - | 聊天记忆存储 |
| PostgreSQL + PGVector | - | 向量检索 |
| JWT (jjwt) | 0.12.6 | 登录鉴权 |
| iText Core | 9.1.0 | PDF 生成 |
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
- **PostgreSQL + pgvector 插件**（可选，用于 RAG 功能）

### 1️⃣ 启动后端

```bash
set JAVA_HOME=<你的 Java 17 路径>
cd ai-agent
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="--enable-preview"
```

> 项目使用了 Java 17 预览特性，运行时必须添加 `--enable-preview` 参数。

### 2️⃣ 启动前端

```bash
cd frontend
npm install
npm run dev
```

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
│   ├── advisor/           # Spring AI Advisor（日志、提示词优化）
│   ├── agent/             # Agent 核心（BaseAgent → ReActAgent → ToolCallAgent → Manus）
│   ├── app/               # 业务应用（LoveApp）
│   ├── chatmemory/        # 聊天记忆（MongoDB / 文件）
│   ├── config/            # 全局配置（CORS, JWT, MCP 后备）
│   ├── controller/        # REST 接口
│   ├── demo/invoke/       # AI 调用示例（4种方式）
│   ├── filter/            # JWT 鉴权过滤器
│   ├── model/             # 数据模型
│   ├── rag/               # RAG 检索增强（向量存储、文档加载、查询重写）
│   ├── repository/        # 数据访问层
│   ├── service/           # 业务逻辑
│   └── tool/              # Agent 工具（文件操作、网页搜索、PDF 生成等）
├── frontend/
│   └── src/
│       ├── api/           # API 请求封装
│       ├── router/        # 路由配置
│       ├── utils/         # 工具函数
│       └── views/         # 页面（登录、首页、恋爱大师、超级智能体）
├── src/main/resources/
│   ├── application.yml    # 主配置（需自行填入 API Key）
│   ├── prompt.yaml        # AI 系统提示词
│   └── document/          # RAG 知识文档
└── pom.xml                # Maven 依赖
```

---

## 🔗 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/change-password` | 修改密码 |
| GET | `/api/ai/love_app/chat/sse` | 恋爱大师流式对话 |
| GET | `/api/ai/love_app/chat/stream` | 恋爱大师纯文本流式对话 |
| GET | `/api/ai/love_app/chat/sync` | 恋爱大师同步对话 |
| GET | `/api/ai/love_app/chat/history` | 历史会话列表 |
| GET | `/api/ai/love_app/chat/history/{chatId}` | 历史会话详情 |
| PUT | `/api/ai/love_app/chat/history/{chatId}/title` | 更新会话标题 |
| DELETE | `/api/ai/love_app/chat/history/{chatId}` | 删除会话 |
| GET | `/api/ai/manus/chat` | 超级智能体对话 |

---

## 🧪 开发说明

- **Java 预览特性**：编译和运行时均需 `--enable-preview`
- **MCP 客户端**：默认禁用，如需启用请配置 `mcp-servers.json`
- **向量库**：PGVector 维度 1536，HNSW 索引，余弦距离
- **前端 UI**：使用 `/ui-ux-pro-max` 技能获取设计规范

---

## 📄 协议

MIT License
