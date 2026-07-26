# AI 应用中心 - 前端

Vue3 + Vite + Axios，对接 Spring Boot 后端 SSE 接口。

## 功能

- **主页**：切换进入「个人知识助手」或「AI 超级智能体」
- **个人知识助手**：聊天室风格，用户消息在右、AI 在左，进入页面自动生成会话 ID，通过 SSE 调用 `GET /api/ai/knowledge/chat/sse?message=xxx&chatId=xxx` 实时展示回复
- **AI 超级智能体**：同样聊天室风格，通过 SSE 调用 `GET /api/ai/manus/chat?message=xxx` 实时展示回复

## 技术栈

- Vue 3（Composition API）
- Vue Router 4
- Axios（普通请求）；SSE 使用原生 `fetch` + ReadableStream

## 开发

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 http://localhost:5173，接口通过 Vite 代理到 `http://localhost:8123`（需先启动后端）。

## 构建

```bash
npm run build
```

产物在 `dist/`。生产环境请求会发往 `http://localhost:8123/api`，若部署域名不同请在 `src/api/request.js` 中修改 `BASE_URL` 或使用环境变量。
