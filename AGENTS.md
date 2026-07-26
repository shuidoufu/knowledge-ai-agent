---
name: ai-agent-rules
description: AI Agent 项目的核心约束与架构指南。在为此项目生成代码、分析问题或回答问题前，必须先加载此技能。包含技术栈、包结构、代码风格、安全规范等完整指令。
---

# AI-Agent 项目规则

> 你正在处理 `ai-agent` 项目。以下指令**必须**遵守。
>
> **重要：每次完成代码修改后，必须同步更新此规则文件**，把本次修改中发现的重要陷阱、Bean 命名、配置要点等追加到对应章节，确保规则持续累积，避免重复踩坑。

---

## 🎯 项目身份

全栈 AI Agent 平台，基于 **Spring Boot 3.5.10 + Java 17 + Vue 3**，使用 **Spring AI 1.0.0-M6**，**对话模型使用 DeepSeek V4 Flash（通过 OpenAI 兼容接口）**，**向量模型使用阿里云百炼 DashScope（qwen-plus）**，支持 ReAct 模式 Agent、RAG 知识库（SimpleVectorStore 内存向量库）、JWT 鉴权。

---

## 🏗 包结构（代码该放哪）

```
advisor/     → Spring AI Advisor 横切逻辑（日志、提示词优化）
agent/       → Agent 核心（BaseAgent → ReActAgent → ToolCallAgent → Manus）
app/         → 业务应用（LoveApp）
chatmemory/  → 聊天记忆（MongoChatMemory / FileBasedChatMemory）
config/      → 全局配置（CORS, JWT, Auth, McpFallbackConfig）
controller/  → REST 控制器（仅做转发，不含业务逻辑）
demo/invoke/ → AI 调用示例（4种方式）
filter/      → JWT 鉴权过滤器
model/       → 数据模型 / DTO
rag/         → RAG 检索增强（向量存储、文档加载、查询改写）
repository/  → 数据访问层
service/     → 业务逻辑
tool/        → Agent 工具（FileOperationTool）
```

**请求流转**：Vue → Controller → Agent → Advisor链 → LLM → Tool调用 → 响应

---

## 📐 代码生成强制规则

### Java
- ✅ 使用 Lombok（`@Slf4j`, `@RequiredArgsConstructor`, `@Data`, `@Builder`）
- ✅ 依赖注入用构造方法（`private final` + `@RequiredArgsConstructor`）
- ✅ Controller 只做转发，不含业务逻辑
- ✅ 所有 public 方法写 Javadoc（纯文本格式，禁止使用 `<p>` `<ul>` `<li>` `<code>` 等 HTML 标签）
- ✅ 日志用 SLF4J 占位符格式：`log.info("action={}", action)`
- ✅ 异常抛给 `GlobalExceptionHandler` 统一处理
- ❌ 禁止硬编码敏感信息（API Key、密码、JWT Secret）
- ❌ 禁止字段注入 `@Autowired`

### Vue 3
- ✅ 必须使用 `<script setup>` + Composition API
- ✅ 所有 API 调用通过 `src/api/request.js` 的 Axios 实例
- ✅ 样式使用 `scoped`
- ✅ 组件名大驼峰（`LoveChat.vue`）
- ✅ AI 的 Markdown 回复使用 `marked` + `DOMPurify` 渲染
- ✅ 图标统一使用 SVG（禁止使用 emoji 作为 UI 图标 — ❌ `🗑️` `🚀` `⚙️`）

#### Vue 认证状态管理
- ✅ **认证状态必须使用响应式 ref 导出**：在 `src/utils/auth.js` 中导出 `ref`（如 `export const username = ref(...)`），组件直接引用响应式 ref，**禁止**在 `computed` 中直接调用 `getUsername()` 等读取 localStorage 的方法（localStorage 非响应式，缓存值不会随登出/登录更新）
- ✅ `setToken()` / `removeToken()` 必须同步更新 localStorage **和** 对应的 ref 值

#### Vue 表单交互模式
- ✅ **密码输入框**必须提供显示/隐藏切换按钮（眼睛 SVG 图标：睁开眼 `👁` = 显示明文，睁眼+斜线 = 隐藏）。按钮位于输入框右侧，`position: absolute`，`tabindex="-1"`
- ✅ **表单校验失败**时使用 shake 动画（`animation: shake 0.5s ease`）+ 红色错误提示文字
- ✅ **按钮与输入框冲突场景**（如编辑标题时点击编辑按钮会先触发 blur）：编辑/操作按钮使用 `@mousedown.prevent.stop` 而非 `@click`，确保保存逻辑在 `@blur` 之前执行；`saveTitle` 等 blur 回调中加守卫 `if (editingChatId.value !== chatIdToSave) return` 跳过孤儿回调

#### Vue 跨组件状态共享
- ✅ 使用 **`provide` / `inject`** 模式共享跨组件状态（如侧边栏折叠状态），禁止引入 Pinia/Vuex 等额外状态库

#### Vue 组件样式规范
- ✅ **CSS 自定义属性**在 `App.vue` 的 `:root` 中定义，命名统一：`--bg-xxx` / `--text-xxx` / `--border-xxx` / `--shadow-xxx`
- ✅ **返回按钮**统一使用胶囊形（`border-radius: 999px`）+ SVG 箭头图标 + 玻璃拟态背景
- ✅ **对话头像**：用户头像与 AI 头像大小一致（40×40px），使用 `margin-top` 对齐气泡第一行文字中心（约 10px）
- ✅ **侧边栏**：宽度固定 260px，历史项右侧 padding 留足空间（≥72px）给操作按钮组

## 🎨 前端 UI/UX 设计规则

**在进行任何前端 UI/UX 优化、样式修改、组件设计时，必须自动调用 `/ui-ux-pro-max` 技能**，以此获取设计系统的推荐和规范。

### 调用流程
1. 收到前端 UI/UX 相关的需求 → **必须先加载 Skill `ui-ux-pro-max`**
2. 根据需求关键词运行设计系统生成器获取推荐配色/字体/风格
3. 将推荐方案融入实际实现

### UI/UX 核心约束
- ✅ **图标使用 SVG**：使用 Heroicons / Lucide 等 SVG 图标集，禁止使用 emoji 作为 UI 图标
- ✅ **交互反馈**：所有可点击元素加 `cursor-pointer`，hover 状态有视觉反馈（颜色/阴影过渡 150-300ms）
- ✅ **触控目标**：最小 44×44px
- ✅ **玻璃拟态风格**：卡面和弹窗优先使用 `backdrop-filter: blur()` + 半透明背景 + 柔和阴影
- ✅ **无障碍**：文本对比度 ≥4.5:1，focus 状态可见
- ✅ **动效**：微交互 150-300ms，使用 `transform`/`opacity` 而非 `width`/`height`
- ✅ **响应式**：支持 375px / 768px / 1024px / 1440px 断点

### 配色体系（当前项目）
```
主色     → #6366F1（Indigo）
辅助色   → #818CF8
CTA色    → #10B981（Emerald）
背景色   → #F5F3FF
文字色   → #1E1B4B
边框色   → rgba(255,255,255,0.3)
阴影色   → rgba(99,102,241,0.08~0.15)
```

### 生产安全检查清单
- [ ] 修改默认演示账号 admin/admin
- [ ] 替换 JWT Secret（强随机 ≥256 bits）
- [ ] 替换 DashScope API Key
- [ ] 数据库密码使用环境变量
- [ ] 关闭或限制 Knife4j/Swagger 文档

---

## ⚠️ 已知陷阱（避免踩坑）

1. **PGVector 二选一**：`spring-ai-pgvector-store`（手动）与 `spring-ai-starter-vector-store-pgvector`（自动）不可同时使用
2. **向量维度 1536**：必须与 DashScope 模型输出一致
3. **JWT Secret 长度**：HS256 需要 ≥256 bits
4. **iText 中文 PDF 字体**：`font-asian` 模块的 `STSongStd-Light` 虽然能加载但**不会嵌入 PDF**，导致用户设备无此字体时显示乱码。**修复方案：** 优先使用 Windows 系统字体路径（`C:/Windows/Fonts/msyh.ttc,0`），iText 9 的 `PdfFontFactory.createFont(String, String)` 会自动嵌入系统字体到 PDF 中，文件约 170KB。注意：
    - TTC 文件需加 `,0` 索引后缀（如 `msyh.ttc,0`）
    - iText 9 不支持 `PdfFontFactory.createFont(String, String, boolean)` 3 参数重载，只能用 2 参数版本
    - 禁止在 pom.xml 中声明多个 `font-asian` 版本，否则导致字体注册冲突
5. **LLM 调用限流**：调用方必须做好重试和降级
6. **MCP 客户端**：默认禁用（`spring.ai.mcp.client.enabled: false`）。如需启用，需配置有效的 `mcp-servers.json`。禁用时，由 `config/McpFallbackConfig.java` 提供空的 `ToolCallbackProvider` 替代 Bean，保证 `@Resource` 注入不失败。**不要删除或修改 `McpFallbackConfig.java`，否则启动会失败。**
7. **localStorage 非响应式**：Vue `computed` 或 `watch` 中直接读取 `localStorage.getItem()` 不会追踪变化，必须通过 ref 代理。所有认证相关组件必须从 `auth.js` 导入响应式 ref 而非调用 `getUsername()`
8. **blur 与 click 竞态**：输入框聚焦时点击外部按钮，`@blur` 先于 `@click` 触发。如果 blur 处理了保存并清空状态，click 可能会错误地重新进入编辑模式。修复方案：操作按钮使用 `@mousedown.prevent.stop`，blur 回调加 `editingChatId` 守卫
9. **CSS position: absolute 容器塌陷**：绝对定位元素不占用父容器空间。若父容器无显式宽度，子元素宽度可能为 0（如胶囊 bar 使用 `position: absolute; left: 24px; right: 0` 但父容器仅 48px 宽时，bar 实际只有 24px）。优先使用 flex + 负边距方案替代绝对定位
10. **Spring AI Alibaba Bean 命名全是小写 dashscope**：自动注册的 Bean 名称为 `dashscopeChatModel`、`dashscopeEmbeddingModel`（**全小写 dashscope**，不是驼峰 `dashScope`）。使用 `@Qualifier` 时必须拼写准确
11. **@Resource 按字段名匹配，大小写敏感**：`@Resource private ChatModel dashScopeChatModel;`（大写 S）不会匹配 bean 名 `dashscopeChatModel`（小写 s），会回退到按类型匹配。如果类型有多个候选则报错。**字段名必须与 bean 名完全一致**
12. **多模型共存时必须用 @Qualifier 区分**：当 `spring-ai-alibaba-starter`（千问）和 `spring-ai-openai-spring-boot-starter`（DeepSeek）同时存在时，会有 2 个 `ChatModel` Bean 和 2 个 `EmbeddingModel` Bean。每个注入点都必须明确指定用哪个：
    - 对话 → `@Qualifier("openAiChatModel")`（DeepSeek）
    - 向量化 → `@Qualifier("dashscopeEmbeddingModel")`（千问）
    - 查询重写 → `@Qualifier("openAiChatModel")`（DeepSeek）
13. **YAML 配置缩进严格**：YAML 对缩进极其敏感，修改 `application.yml` 等配置文件时必须使用正确层级，禁止混用 Tab 和空格。多 profile（local/prod）可能覆盖主配置
14. **切换模型时 embedding 需独立配置**：DeepSeek 的 embedding API 可能不可用（返回 404），需要保留千问的 DashScope 配置专门用于向量化。`spring.ai.openai.embedding.options.model` 可能需要单独指定
15. **@Tool 方法名不能重载（overload）**：Spring AI 的 `@Tool` 注解使用 Java 方法名作为工具注册名。同名的重载方法会导致启动时报错 `Multiple tools with the same name found in sources`。同一个类中每个 `@Tool` 方法必须有不同的方法名。需要用多参数方法替代重载，AI 模型会通过 `@ToolParam` 的描述决定传参

---

## 🔑 关键配置

- 后端端口：`8123` | Context Path：`/api`
- **对话模型**：DeepSeek V4 Flash（通过 `spring.ai.openai.*` 配置）
- **向量模型**：千问 Qwen-Plus（通过 `spring.ai.dashscope.*` 配置）
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
- [ ] SVG 图标是否替代了 emoji？
- [ ] 密码输入框是否加了显示/隐藏切换？
- [ ] 认证相关组件是否从 `auth.js` 导入响应式 ref 而非调用 `getUsername()`？
- [ ] 编辑/操作按钮是否在必要时使用了 `@mousedown.prevent.stop` 避免 blur 竞态？
- [ ] Vue 组件用了 `scoped`？
- [ ] 文件放在正确的包/目录？
- [ ] **已完成自测试？**（后端编译+启动、前端编译+功能验证、核心接口测试）
- [ ] **本次修改是否有需要写入规则的新发现？**（Bean 命名、配置要点、踩坑记录等 → 追加到对应章节）
