# 🤖 AI 助手项目系统提示词

> 本文件是 AI 编码助手（Cursor / Windsurf / GitHub Copilot / ZCode 等）的项目级系统提示词。
> AI 在为此项目生成代码、分析或建议时，应遵循以下指令。
>
> **🔔 ZCode 用户**：项目中已包含 `.zcode/skills/ai-agent-rules/SKILL.md` 技能文件，
> 你可以在对话中说 **"请加载 /ai-agent-rules 技能"** 来让 AI 读取精简版规则。

---

## 一、项目身份认知

你正在协助的是一个 **全栈 AI Agent 平台**，项目名为 `ai-agent`。

**一句话定位**：基于 Spring AI + 阿里云百炼 DashScope 的智能代理平台，支持 ReAct 模式 Agent、RAG 知识库检索、多会话聊天记忆，并提供 Vue 3 前端交互界面。

**核心用户场景**：
1. 用户通过 Web 界面与 AI Agent 对话
2. Agent 可调用工具（如文件操作）完成任务
3. LoveApp 场景下，Agent 从向量知识库检索情感内容辅助回答
4. Manus 模式下，Agent 进行多步推理和工具调用

---

## 二、技术栈总览（AI 必须熟知）

### 后端
```
Java 17 + Spring Boot 3.5.10 + Maven
Spring AI 1.0.0-M6 → 大模型统一调用层（默认模型：qwen-plus）
DashScope SDK 2.18.5 → 阿里云百炼原生调用
LangChain4J 1.0.0-beta2 → LangChain 生态
MongoDB → 聊天记忆持久化
PostgreSQL + PGVector 0.7.x → 向量存储（HNSW索引，1536维，余弦距离）
JWT (jjwt 0.12.6) → 登录鉴权
iText Core 9.1.0 → PDF 生成
jsoup 1.19.1 → 网页抓取
Hutool 5.8.5 → Java 工具库
Knife4j 4.4.0 → API 文档（路径：/api/swagger-ui.html）
Lombok 1.18.36 → 代码生成（@Slf4j, @Data, @Builder, @RequiredArgsConstructor）
Kryo 5.6.2 → 序列化
```

### 前端
```
Vue 3.5 + Composition API + <script setup>
Vite 5.4 → 构建工具
Vue Router 4.4 → 前端路由
Axios 1.7 → HTTP 请求（baseURL 已统一配置）
marked + DOMPurify → Markdown 渲染（用于展示 AI 回复）
```

### 部署相关
```
后端端口：8123 | Context Path: /api
JWT 默认账号：admin / admin（生产环境必须修改）
多环境配置：application.yml（公共） + application-local.yml（本地） + application-prod.yml（生产）
```

---

## 三、架构理解（AI 必须知道的关键图景）

### 3.1 包结构语义

```
advisor/        ← Spring AI AroundAdvisor，拦截对话流做横切增强
  ├── MyLoggerAdvisor        → 记录对话日志
  └── ReReadingAdvisor       → 提示词优化（重读上下文）
agent/          ← Agent 核心体系
  ├── BaseAgent              → 抽象基类
  ├── ReActAgent             → 思考(Thought)→行动(Action)→观察(Observation) 循环
  ├── ToolCallAgent          → 支持工具调用的 Agent
  └── Manus                  → 通用助手 Agent，继承 ToolCallAgent
  └── model/AgentState       → Agent 状态枚举
app/            ← 面向场景的应用层
  └── LoveApp                → 恋爱助手应用
chatmemory/     ← 聊天记忆实现
  ├── MongoChatMemory        → 生产环境：MongoDB 持久化
  └── FileBasedChatMemory    → 开发测试：文件存储
config/         ← 全局配置
  ├── CorsConfig             → 跨域配置
  ├── AuthFilterConfig       → 鉴权过滤器注册
  └── JwtProperties          → JWT 配置属性绑定
constant/       ← 常量定义
  └── FileConstant           → 文件相关常量
controller/     ← REST 控制器
  ├── AiController           → AI 对话接口
  ├── AuthController         → 登录/密码修改
  └── GlobalExceptionHandler → 全局异常处理
demo/invoke/    ← AI 调用示例（4种方式）
  ├── SpringAiInvoke         → 主推方式
  ├── SdkAiInvoke            → DashScope SDK 方式
  ├── LangChainAiInvoke      → LangChain4J 方式
  ├── HttpAiInvoke           → HTTP 直连方式
  └── TestApiKey             → API Key 测试
filter/         ← 请求过滤器
  └── AuthFilter             → JWT Token 验证过滤器
model/          ← 数据模型
  ├── ChatHistoryDTO         → 聊天历史传输对象
  ├── ChatMessages           → 聊天消息实体
  ├── Conversation           → 会话实体
  └── User                   → 用户实体
rag/            ← 检索增强生成
  ├── LoveAppVectorStoreConfig       → 向量存储配置
  ├── LoveAppDocumentLoader          → 知识文档加载
  ├── LoveAppRagCloudAdvisorConfig   → RAG Advisor 配置（云端）
  ├── LoveAppRagCustomAdvisorFactory → RAG Advisor 工厂（自定义）
  ├── PgVectorVectorStoreConfig      → PGVector 配置
  ├── QueryRewriter                  → 查询改写
  ├── MyKeywordEnricher             → 关键词增强
  └── LoveAppContextualQueryAugmenterFactory → 上下文查询增强
repository/     ← 数据访问
  └── UserRepository          → MongoDB 用户仓库
service/        ← 业务服务
  ├── AuthService             → 认证服务
  └── UserService             → 用户服务
tool/           ← Agent 工具
  └── FileOperationTool       → 文件操作工具
```

### 3.2 请求流转路径

```
用户输入
  ↓
Vue 组件 (LoveChat.vue / ManusChat.vue)
  ↓ (Axios POST)
AiController
  ↓
Agent (Manus / ToolCallAgent)
  ├──→ Advisor 链（日志 → 提示词优化 → RAG 检索 → ...）
  ├──→ LLM 调用（DashScope qwen-plus）
  └──→ Tool 调用（如需）
  ↓
响应返回前端
```

### 3.3 Agent 工作模式

```
Manus 模式（通用助手）：
  System: 你是Manus... → User输入 → Advisor链 → LLM → Tool Calls → 最终回答

LoveApp 模式（恋爱助手）：
  System: 你是恋爱助手... → User输入 → Advisor链 → RAG检索知识 → LLM → 回答
```

---

## 四、代码生成规范

### 4.1 新增类时的决策树

```
新功能需要写代码？
├── 是 AI 调用相关？ → advisor/ 或 agent/ 或 demo/invoke/
├── 是业务 API？     → controller/ + service/ + model/
├── 是数据访问？     → repository/ + model/
├── 是配置类？       → config/
├── 是 Agent 工具？  → tool/
├── 是 RAG 相关？    → rag/
├── 是常量？         → constant/
└── 是前端页面？     → frontend/src/views/ + router/
```

### 4.2 代码风格强制规则

**Java：**
1. 使用 Lombok 减少样板代码（`@Slf4j`, `@RequiredArgsConstructor`, `@Data` 等）
2. 依赖注入使用构造方法注入（private final + Lombok）
3. Controller 只做转发，不包含业务逻辑
4. 所有 public 方法必须写 Javadoc（包括参数和返回值）
5. 日志使用 SLF4J（`log.info("msg={}", var)` 格式，禁止字符串拼接）
6. 异常抛出给 `GlobalExceptionHandler` 统一处理
7. 配置类使用 `@ConfigurationProperties` 绑定配置前缀

**Vue 3：**
1. 必须使用 `<script setup>` + Composition API
2. 所有 API 调用通过 `src/api/request.js` 的 Axios 实例
3. 组件名使用大驼峰
4. 样式使用 `scoped`
5. AI 的 Markdown 回复使用 marked + DOMPurify 渲染

### 4.3 代码审查要点（AI 自检清单）

AI 在输出代码前，应依次检查：
- [ ] 包路径是否正确？（`com.example.aiagent.xxx`）
- [ ] 是否加了 `@Slf4j`？
- [ ] 依赖注入是否使用构造方法？
- [ ] 新增配置项是否在 application.yml 中有默认值？
- [ ] 敏感信息（API Key、密码）是否硬编码了？（禁止）
- [ ] 异常是否会被全局处理器捕获？
- [ ] Agent 新增 Tool 是否注册了？
- [ ] Vue 组件是否使用了 `scoped` 样式？
- [ ] 前端 API 调用是否有错误处理？
- [ ] 新增文件是否放对了包/目录？

---

## 五、关键设计决策与约定

### 5.1 为什么这样设计？

| 决策 | 原因 |
|------|------|
| 四种 AI 调用方式并存 | 项目演进过程中的技术探索，当前主推 Spring AI |
| Advisor 链式处理 | 复用横切能力（日志、RAG、提示词优化），避免 Agent 代码臃肿 |
| Memory 分 MongoDB 和 File 两种 | 开发/生产分离，File 模式方便本地调试 |
| JWT 鉴权 | 轻量级，适合前后端分离 |
| PGVector 而非单独向量数据库 | 减少运维组件，PostgreSQL 一库多用 |

### 5.2 约定优于配置

1. **RAG 知识文档**放在 `src/main/resources/document/`，Markdown 格式
2. **聊天记忆**按会话 ID 组织，MongoDB 集合用下划线命名
3. **环境配置**：公共配置放 `application.yml`，环境特有配置放对应 profile
4. **API 路径**：所有接口以 `/api` 开头（已在 `server.servlet.context-path` 配置）

### 5.3 避免踩坑

⚠️ **重要陷阱：**
- `spring-ai-pgvector-store`（手动整合）与 `spring-ai-starter-vector-store-pgvector`（自动整合）二选一，不可同时使用
- LLM 调用可能因限流失败，调用方必须做好重试和降级
- 向量维度必须与模型输出一致（当前 1536 维对应 DashScope）
- iText 亚洲字体包（`font-asian`）当前仅在 test scope，生成中文 PDF 时需调整
- JWT Secret 长度需满足 HS256 要求（≥256 bits）

---

## 六、对 AI 自身的行为约束

### 6.1 通用原则

1. **保持克制**：不做过度设计，新功能遵循已有模式
2. **保持一致性**：代码风格、命名、结构与项目现有代码一致
3. **只动该动的**：一个 PR/任务只修改相关文件，不顺手重构无关代码
4. **文档同步**：新增功能或修改架构时，同步更新本文件

### 6.2 回答风格

- 解释代码改动时，说明**为什么**而非仅仅**是什么**
- 指出潜在风险和副作用
- 给出清晰的迁移路径（如果有破坏性变更）

### 6.3 当不确定时

遇到以下情况，应该向开发者提问澄清：
- 新增依赖的版本选择
- 涉及多方案的设计决策
- 不确定的业务逻辑含义
- 可能破坏现有功能的改动

---

## 七、项目文件地图（AI 快速索引）

```
后端关键文件：
├── pom.xml                               ← 依赖清单
├── src/main/java/com/example/aiagent/
│   ├── AiAgentApplication.java           ← 启动入口
│   ├── controller/AiController.java      ← AI 对话主入口
│   ├── agent/Manus.java                  ← 核心 Agent
│   ├── rag/LoveAppVectorStoreConfig.java ← RAG 向量配置
│   └── config/JwtProperties.java         ← JWT 配置绑定
├── src/main/resources/
│   ├── application.yml                   ← 公共配置
│   ├── document/                         ← RAG 知识文档目录
│   └── mcp-servers.json                  ← MCP 配置（可选）

前端关键文件：
├── frontend/src/
│   ├── App.vue                           ← 根组件
│   ├── main.js                           ← 入口
│   ├── router/index.js                   ← 路由表
│   ├── api/request.js                    ← Axios 封装
│   ├── views/ManusChat.vue              ← Manus 对话页
│   ├── views/LoveChat.vue                ← 恋爱助手对话页
│   └── views/Login.vue                   ← 登录页
```

---

## 八、快速参考

```bash
# 后端启动（本地）
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 后端构建
./mvnw clean package -DskipTests

# 前端启动
cd frontend && npm run dev

# 前端构建
cd frontend && npm run build
```

**API 入口：** `http://localhost:8123/api`
**Swagger 文档：** `http://localhost:8123/api/swagger-ui.html`
**默认登录：** `admin / admin`

---

> **版本**：1.0.0 | **适用项目**：ai-agent | **最后更新**：2026-06-30
>
> 本文件是 AI 编码助手的系统提示词，AI 在为此项目生成代码时应严格遵循上述指令。
> 项目结构或技术栈变更时，开发者应同步更新此文件。
