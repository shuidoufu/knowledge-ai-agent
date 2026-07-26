# 📋 AI-Agent 项目开发规则

> 本文件定义了 AI-Agent 项目的开发规范和约定，所有团队成员应遵守。
> 项目版本：0.0.1-SNAPSHOT | Java 17 + Spring Boot 3.5.10 + Vue 3

---

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术栈约定](#2-技术栈约定)
- [3. 目录结构与职责](#3-目录结构与职责)
- [4. 编码规范（后端）](#4-编码规范后端)
- [5. 编码规范（前端）](#5-编码规范前端)
- [6. 命名规范](#6-命名规范)
- [7. API 规范](#7-api-规范)
- [8. 数据库规范](#8-数据库规范)
- [9. 安全规范](#9-安全规范)
- [10. Agent 开发规范](#10-agent-开发规范)
- [11. Git 工作流](#11-git-工作流)
- [12. 日志规范](#12-日志规范)
- [13. 错误处理规范](#13-错误处理规范)
- [14. 测试规范](#14-测试规范)
- [15. 配置管理](#15-配置管理)
- [16. AI / LLM 交互规范](#16-ai--llm-交互规范)

---

## 1. 项目概述

**AI-Agent** 是一个基于 Spring AI + DashScope（阿里云百炼）的智能代理平台，支持：
- **多模式 AI 调用**：Spring AI、原生 SDK、HTTP 直连、LangChain4J 四种方式
- **Manus 风格 Agent**：ReAct 模式，支持工具调用（Tool Calling）
- **RAG 知识库**：基于 PostgreSQL + PGVector 的向量检索增强生成
- **个人知识助手（KnowledgeApp）**：基于 RAG 的知识问答应用，整合笔记与收藏辅助回忆
- **聊天记忆**：支持 MongoDB 持久化和文件两种存储方式
- **JWT 登录鉴权**：前后端分离认证

---

## 2. 技术栈约定

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.5.10 | 应用框架 |
| Spring AI | 1.0.0-M6 | AI 集成框架 |
| Maven | - | 构建工具 |
| MongoDB | - | 聊天记忆存储 |
| PostgreSQL + PGVector | - | 向量数据库 |
| DashScope SDK | 2.18.5 | 阿里云百炼 |
| LangChain4J | 1.0.0-beta2 | LangChain Java 版 |
| JWT (jjwt) | 0.12.6 | 登录鉴权 |
| iText Core | 9.1.0 | PDF 生成 |
| jsoup | 1.19.1 | 网页抓取 |
| Hutool | 5.8.5 | 工具类库 |
| Knife4j | 4.4.0 | API 文档 |
| Lombok | 1.18.36 | 代码精简 |
| Kryo | 5.6.2 | 序列化 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.x | 前端框架 |
| Vite | 5.4.x | 构建工具 |
| Vue Router | 4.4.x | 路由 |
| Axios | 1.7.x | HTTP 请求 |
| marked + DOMPurify | - | Markdown 渲染 |

---

## 3. 目录结构与职责

```
ai-agent/
├── src/main/java/com/example/aiagent/
│   ├── advisor/           # Spring AI Advisor（切面增强逻辑）
│   ├── agent/             # Agent 核心实现（Manus, ReAct, ToolCall）
│   ├── app/               # 业务应用层（如 KnowledgeApp）
│   ├── chatmemory/        # 聊天记忆持久化实现
│   ├── config/            # 全局配置类（CORS, JWT, Auth）
│   ├── constant/          # 常量定义
│   ├── controller/        # REST 控制器 + 全局异常处理
│   ├── demo/invoke/       # AI 调用示例（多种方式）
│   ├── filter/            # 过滤器（如登录鉴权）
│   ├── model/             # 数据模型 / DTO
│   ├── rag/               # RAG 相关（向量存储、文档加载、查询重写）
│   ├── repository/        # 数据访问层
│   ├── service/           # 业务逻辑层
│   └── tool/              # Agent 工具定义
├── frontend/
│   └── src/
│       ├── api/           # API 请求封装
│       ├── router/        # 前端路由
│       ├── utils/         # 工具函数
│       └── views/         # 页面组件
└── application*.yml       # 多环境配置
```

### 分层职责

```
Controller → Service → Repository/Agent → External(LLM/DB)
    ↓           ↑
   DTO        Model
```

- **Controller**：仅做参数校验和请求分发，不包含业务逻辑
- **Service**：业务编排，调用多个组件完成功能
- **Agent**：AI Agent 核心，管理对话状态和工具调用
- **Advisor**：拦截对话流程，做日志、上下文增强等横切关注点
- **RAG**：知识检索增强，包含向量存储配置、文档加载、查询改写
- **Tool**：Agent 可调用的外部工具（如文件操作）

---

## 4. 编码规范（后端）

### 4.1 基础规则

- **缩进**：4 空格，禁止使用 Tab
- **编码**：UTF-8
- **行宽**：不超过 120 字符
- **文件末尾**：保留一个空行
- **Lombok**：优先使用 `@Slf4j`、`@Data`、`@Builder`、`@AllArgsConstructor` 等注解减少样板代码

### 4.2 类规范

```java
/**
 * 类功能描述（一句话说明）
 *
 * <p>详细说明（可选）
 */
@Slf4j
@Service  // 或 @Component, @Configuration 等
public class XxxService {
    // 顺序：常量 → 依赖注入 → 构造方法 → 公有方法 → 私有方法
}
```

- 每个 public 方法必须有 Javadoc 注释
- 类名使用大驼峰（PascalCase）
- 所有类必须有清晰的职责，单一职责原则（SRP）

### 4.3 方法规范

```java
/**
 * 方法功能描述
 *
 * @param paramName 参数说明
 * @return 返回值说明
 * @throws XxxException 异常说明
 */
public ReturnType methodName(String paramName) {
    // 参数校验在前
    // 业务逻辑在后
}
```

- 方法体不超过 50 行（超出考虑拆分）
- 避免过深的嵌套（超过 3 层考虑提取方法）

### 4.4 依赖注入

- **推荐**：使用构造方法注入（`@RequiredArgsConstructor` + `final` 字段）
- **不推荐**：`@Autowired` 字段注入（Spring Boot 3.x 仍支持但已不建议）

```java
@RequiredArgsConstructor  // Lombok 自动生成构造方法
@Service
public class UserService {
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
}
```

### 4.5 异常处理

- 业务异常继承 `RuntimeException`，使用全局异常处理器（`GlobalExceptionHandler`）统一处理
- Controller 层不做 try-catch，抛给全局异常处理器
- Service 层捕获并转换业务异常

---

## 5. 编码规范（前端）

### 5.1 基础规则

- **缩进**：2 空格
- **编码**：UTF-8
- **引号**：优先使用单引号
- **分号**：必须使用分号
- **组件**：使用 Vue 3 Composition API (`<script setup>`)

### 5.2 Vue 组件规范

```vue
<template>
  <!-- 模板部分 -->
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { apiMethod } from '@/api/request'

// 响应式状态
const data = ref(null)
const loading = ref(false)

// 生命周期
onMounted(() => {
  fetchData()
})

// 方法
const fetchData = async () => {
  loading.value = true
  try {
    data.value = await apiMethod()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 作用域样式 */
</style>
```

### 5.3 API 调用

- 统一使用 `src/api/request.js` 中封装的 Axios 实例
- 每个页面/模块的 API 调用在对应 view 中直接进行（小项目约定）
- 错误处理在调用处完成，至少要有用户可见的提示

---

## 6. 命名规范

### Java
| 类别 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰 | `UserService`, `KnowledgeApp` |
| 方法名 | 小驼峰 | `findById()`, `processMessage()` |
| 变量名 | 小驼峰 | `userName`, `chatHistory` |
| 常量 | 全大写+下划线 | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| 包名 | 全小写 | `com.example.aiagent.service` |
| 枚举 | 大驼峰类 + 大写常量 | `Status.ACTIVE` |

### Vue / JavaScript
| 类别 | 规范 | 示例 |
|------|------|------|
| 组件名 | 大驼峰 | `KnowledgeChat.vue`, `ManusChat.vue` |
| 变量 | 小驼峰 | `userInfo`, `chatMessages` |
| 方法 | 小驼峰 | `fetchData()`, `handleSubmit()` |
| 常量 | 全大写+下划线 | `API_BASE_URL` |

### 命名原则
- **见名知意**：名称要能准确表达用途
- **避免缩写**：除非是广泛接受的缩写（如 `info`, `config`, `util`, `dto`）
- **布尔类型**：使用 `is`, `has`, `can` 前缀（如 `isActive`, `hasPermission`）

---

## 7. API 规范

### 7.1 基础配置

- **Context Path**：`/api`
- **端口**：`8123`
- **文档**：Knife4j / Swagger → `http://localhost:8123/api/swagger-ui.html`

### 7.2 接口设计

```
GET    /api/xxx          查询列表
GET    /api/xxx/{id}     查询详情
POST   /api/xxx          创建
PUT    /api/xxx/{id}     更新
DELETE /api/xxx/{id}     删除
```

### 7.3 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

### 7.4 AI 相关接口

```
POST   /api/ai/chat            对话接口（通用）
POST   /api/ai/agent/chat      Agent 对话（Manus 模式）
GET    /api/ai/history         获取聊天历史
GET    /api/ai/knowledge/chat/sse        知识助手 SSE 流式对话
GET    /api/ai/knowledge/chat/stream     知识助手纯文本流式对话（推荐，无 SSE 包装）
GET    /api/ai/knowledge/chat/sync       知识助手同步对话
GET    /api/ai/knowledge/chat/history    历史会话列表
GET    /api/ai/knowledge/chat/history/{chatId}  历史会话详情
PUT    /api/ai/knowledge/chat/history/{chatId}/title  更新会话标题
DELETE /api/ai/knowledge/chat/history/{chatId}        删除会话
```

> **SSE 流注意事项**：`/sse` 端点的 `data:` + `\n\n` 包装与 AI token 中的换行符冲突，
> 推荐使用 `/stream` 端点（`TEXT_PLAIN`，裸文本流），前端无需 SSE 解析，直接 `onChunk(raw)`。

### 7.5 认证接口

```
POST   /api/auth/login      登录（返回 JWT Token）
POST   /api/auth/change-pwd 修改密码
```

---

## 8. 数据库规范

### MongoDB（聊天记忆）
- Database：`chat_memory_db`
- 集合命名：使用下划线命名法，如 `chat_sessions`, `chat_messages`
- 索引：高频查询字段建立索引

### PostgreSQL + PGVector（向量存储）
- 向量维度：1536（对应 DashScope 向量模型）
- 索引类型：HNSW
- 距离类型：COSINE_DISTANCE
- 表结构由 Spring AI PGVector Store 自动管理

---

## 9. 安全规范

### 9.1 JWT 鉴权
- Token 通过 `Authorization: Bearer <token>` 请求头传递
- 默认过期时间：7 天（通过 `app.jwt.expiration-seconds` 配置）
- 密钥通过 `app.jwt.secret` 配置（生产环境使用环境变量）
- `AuthFilter` 拦截除 `/auth/login` 外的所有接口

### 9.2 密码管理
- 前端密码修改接口：`POST /api/auth/change-pwd`
- 生产环境密码/密钥禁止硬编码，使用环境变量覆盖

### 9.3 CORS 配置
- `CorsConfig` 中配置允许的前端地址
- 开发阶段可放行本地开发服务器地址

### 9.4 生产安全检查清单
- [ ] 修改默认演示账号/密码
- [ ] 替换 JWT Secret（使用强随机字符串）
- [ ] 替换 DashScope API Key
- [ ] 数据库连接串使用环境变量
- [ ] 关闭 Swagger/Knife4j 文档（或限制访问）

---

## 10. Agent 开发规范

### 10.1 Agent 类层次

```
BaseAgent (抽象基类)
  ├── ReActAgent (思考-行动-观察循环)
  │     └── ToolCallAgent (工具调用实现)
  └── Manus (通用助手 Agent)
```

### 10.2 开发新 Agent 的步骤

1. 继承 `BaseAgent` 或已有子类
2. 重写 `processMessage()` 或 `execute()` 方法
3. 注册需要的 Tool（通过 `ToolCallAgent` 的工具注册机制）
4. 配置 Advisor（如有需要）
5. 在 Controller 中添加对应的 API 端点

### 10.3 Advisor 使用

- `MyLoggerAdvisor`：记录对话日志，默认启用
- `ReReadingAdvisor`：优化提示词，增加上下文理解
- 自定义 Advisor 应放在 `advisor/` 包中，实现 `AroundAdvisor` 接口

### 10.4 Tool 开发规范

- 每个 Tool 放在 `tool/` 包下
- 实现 `ToolCallback` 或使用 `@Tool` 注解标记
- Tool 应该有明确的描述（description），帮助 LLM 理解何时调用
- Tool 的名称使用小驼峰命名

### 10.5 ChatMemory 规范

- 优先使用 `MongoChatMemory`（生产环境）
- `FileBasedChatMemory` 用于本地开发/测试
- 自定义 Memory 需实现对应的 ChatMemory 接口

---

## 11. Git 工作流

### 11.1 分支策略

- `main`：稳定发布分支，只接受 PR 合并
- `dev`：开发分支，日常开发的基础分支
- `feature/*`：功能开发分支（从 dev 拉取）
- `fix/*`：Bug 修复分支
- `release/*`：发布准备分支

### 11.2 Commit Message 规范

```
<type>(<scope>): <subject>

<body>
```

**类型**：
- `feat`：新功能
- `fix`：Bug 修复
- `refactor`：重构
- `docs`：文档
- `style`：格式（不影响代码运行）
- `test`：测试
- `chore`：构建/工具

**示例**：
```
feat(agent): add Manus tool calling support

- Implement ToolCallAgent with callback mechanism
- Add file operation tool
```

### 11.3 开发流程

1. 从 `dev` 拉取最新代码
2. 创建 `feat/xxx` 或 `fix/xxx` 分支
3. 开发完成后提交 PR 到 `dev`
4. Code Review 通过后合并

### 11.4 .gitignore 规则

项目 `.gitignore` 已排除以下内容：

| 类别 | 排除项 |
|------|--------|
| 构建产物 | `target/`, `*.class`, `*.jar`, `*.war` |
| 依赖 | `node_modules/`, `frontend/node_modules/` |
| IDE | `.idea/`, `*.iml`, `.vscode/` |
| 敏感配置 | `application.yml`, `application-*.yml`（含 API Key / 密码） |
| 运行时数据 | `chat_memory/`, `.mongo-data/`, `tmp/` |
| AI 技能 | `.zcode/`, `.claude/` |
| 日志 | `*.log`, `build.log`, `compile.log` |
| 临时文件 | `nul` |

---

## 12. 日志规范

### 12.1 日志框架

使用 `@Slf4j`（Lombok） + SLF4J + Logback（Spring Boot 默认）

### 12.2 日志级别

| 级别 | 使用场景 |
|------|---------|
| `ERROR` | 系统异常、请求失败、需人工介入的问题 |
| `WARN` | 不影响运行但需关注的异常情况 |
| `INFO` | 关键流程节点（启动、请求进入、操作完成） |
| `DEBUG` | 开发调试信息（不带到生产环境） |
| `TRACE` | 细粒度跟踪（极少使用） |

### 12.3 日志内容要求

- **包含上下文**：用户 ID、会话 ID、请求 ID 等
- **禁止敏感信息**：密码、Token、API Key 禁止记录
- **结构化**：`log.info("action={}, userId={}, result={}", action, userId, result)`

---

## 13. 错误处理规范

### 13.1 全局异常处理

所有异常统一由 `GlobalExceptionHandler` 处理，返回统一格式的 JSON。

### 13.2 业务异常

```java
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;
    // ...
}
```

### 13.3 AI 调用异常

AI 调用可能因网络、限流、API Key 等问题失败，调用方应：
1. 设置合理的超时时间
2. 实现重试机制（有限次）
3. 友好地向用户提示错误
4. 记录详细日志以便排查

---

## 14. 测试规范

### 14.1 单元测试

- 框架：Spring Boot Test + JUnit（默认）
- Service 层和工具类必须编写单元测试
- Controller 层编写 Web 层集成测试（`@WebMvcTest`）
- 测试文件放在 `src/test/java/` 对应包下

### 14.2 测试命名

```
XxxServiceTest
XxxControllerTest
```

### 14.3 测试方法命名

```
方法名_测试场景_期望结果
示例：findById_用户存在_返回用户信息()
示例：processMessage_空消息_抛出异常()
```

---

## 15. 配置管理

### 15.1 多环境配置

| 文件 | 用途 | 是否提交 Git |
|------|------|-------------|
| `application.yml` | 公共配置（含 API Key/密码，已 .gitignore） | ❌ 否 |
| `prompt.yaml` | AI 系统提示词 | ✅ 是 |
| `application-local.yml` | 本地开发环境 | ❌ 否 |
| `application-prod.yml` | 生产环境 | ❌ 否 |

### 15.2 敏感信息管理

- **禁止**：在配置文件中硬编码密码、API Key、密钥
- **开发**：使用 `application-local.yml`，不提交到 Git
- **生产**：通过环境变量覆盖（`SPRING_DATASOURCE_PASSWORD`、`SPRING_AI_DASHSCOPE_API_KEY` 等）

### 15.3 当前配置密钥清单

以下配置在生产环境必须使用环境变量替换：
| 配置项 | 环境变量 |
|--------|---------|
| `spring.data.mongodb.uri` | `SPRING_DATA_MONGODB_URI` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` |
| `spring.ai.dashscope.api-key` | `SPRING_AI_DASHSCOPE_API_KEY` |
| `app.jwt.secret` | `APP_JWT_SECRET` |

---

## 16. AI / LLM 交互规范

### 16.1 模型选择

- 默认模型：`qwen-plus`（通过 `spring.ai.dashscope.chat.options.model` 配置）
- 大多数通用对话场景使用 `qwen-plus`
- 复杂推理任务可切换至 `qwen-max`

### 16.2 Prompt 工程

- 系统提示词（System Prompt）放在常量类或配置文件中，避免硬编码
- 对话消息使用 Spring AI 的 `Message` 体系（`SystemMessage`, `UserMessage`, `AssistantMessage`）
- 需要结构化输出时，使用 `jsonschema-generator` 定义输出 Schema

### 16.3 RAG 规范

- 知识文档放在 `src/main/resources/document/` 下
- 目前支持的格式：Markdown
- 文档加载器在 `rag/KnowledgeAppDocumentLoader.java` 中定义
- 查询分析使用 `rag/QueryRewriter.java`（一次 LLM 调用同时完成"是否需要检索"判断 + 查询改写）
- 向量存储配置在 `rag/` 包中的 Config 类

### 16.4 AI 调用方式选择

项目支持四种 AI 调用方式（位于 `demo/invoke/` 包）：
| 方式 | 适用场景 |
|------|---------|
| **Spring AI (`SpringAiInvoke`)** | 主推方式，功能最完善 |
| **DashScope SDK (`SdkAiInvoke`)** | 需要阿里云原生能力时 |
| **LangChain4J (`LangChainAiInvoke`)** | 需要 LangChain 生态功能时 |
| **HTTP 直连 (`HttpAiInvoke`)** | 快速测试/调试 |

---

## 附录 A：常用命令

```bash
# 后端启动（须设置 Java 17 + --enable-preview）
set JAVA_HOME=<Java 17 路径>
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="--enable-preview"

# 后端构建
mvnw.cmd clean package -DskipTests

# 前端启动
cd frontend && npm run dev

# 前端构建
cd frontend && npm run build
```

> **注意**：项目使用了 Java 17 预览特性（`patterns in switch`），
> 编译（`pom.xml` 中 `maven-compiler-plugin`）和运行（`spring-boot-maven-plugin`）均需配置 `--enable-preview`。
> IntelliJ IDEA 需设置 `.iml` 文件的 `LANGUAGE_LEVEL="JDK_17_PREVIEW"`。

## 附录 B：IDE 配置

- **VS Code**：项目已包含 `.vscode/extensions.json` 和 `.vscode/settings.json`
- **IntelliJ IDEA**：推荐插件
  - Lombok
  - Spring Assistant
  - Vue.js
  - Maven Helper
- **IDEA 预览特性设置**：
  - `File → Settings → Build → Compiler → Java Compiler` → `Additional command line parameters` 填入 `--enable-preview`
  - 或确保 `ai-agent.iml` 中 `LANGUAGE_LEVEL="JDK_17_PREVIEW"`

---

> **维护人**：项目团队 | **最后更新**：2026-07-03
>
> 本文档应随项目演进持续更新，所有团队成员的 PR 涉及新增功能或架构变更时，应同步更新本文档。
