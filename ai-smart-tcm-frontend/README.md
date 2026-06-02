# AI Smart TCM Frontend

> AI 智能云中医问诊 & 智能体应用 —— 前端项目

基于 Vue 3 + Vite + TypeScript + Element Plus 开发的 AI 对话前端，包含两个核心应用：
**AI 云中医问诊** 与 **AI 超级智能体**。两个应用均通过 SSE（Server-Sent Events）实时接收 AI 回复，实现打字机效果。

---

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [环境要求](#环境要求)
- [可用脚本](#可用脚本)
- [核心功能](#核心功能)
- [后端接口约定](#后端接口约定)
- [SSE 消息处理规范](#sse-消息处理规范)
- [移动端适配](#移动端适配)
- [常见问题](#常见问题)

---

## 技术栈

| 类别       | 技术                              | 版本      |
| ---------- | --------------------------------- | --------- |
| 构建工具   | Vite                              | ^5.4.9    |
| 框架       | Vue                               | ^3.5.12   |
| 语言       | TypeScript                        | ^5.6.3    |
| 路由       | Vue Router                        | ^4.4.5    |
| UI 组件库  | Element Plus                      | ^2.8.6    |
| 图标       | @element-plus/icons-vue           | ^2.3.1    |
| HTTP       | Axios                             | ^1.7.7    |
| 自动导入   | unplugin-auto-import              | ^0.18.3   |
| 按需引入   | unplugin-vue-components           | ^0.27.4   |
| 运行时     | Node.js                           | >= 20.20.2|

---

## 项目结构

```
ai-smart-tcm-frontend/
├── index.html                     # 入口 HTML
├── package.json                   # 依赖与脚本
├── tsconfig.json                  # TS 配置
├── tsconfig.node.json             # Node 端 TS 配置
├── vite.config.ts                 # Vite 配置（含代理）
├── .env.development               # 开发环境变量
├── .env.production                # 生产环境变量
└── src/
    ├── main.ts                    # 应用入口
    ├── App.vue                    # 根组件
    ├── env.d.ts                   # 环境变量类型声明
    ├── vite-env.d.ts              # Vite 客户端类型
    ├── api/
    │   ├── request.ts             # Axios 实例封装
    │   ├── sse.ts                 # SSE 解析器（核心）
    │   └── ai.ts                  # AI 业务接口封装
    ├── components/
    │   └── ChatRoom.vue           # 通用聊天室组件
    ├── router/
    │   └── index.ts               # 路由配置
    ├── styles/
    │   └── index.css              # 全局样式
    ├── types/
    │   └── chat.ts                # 消息 / 回调类型定义
    ├── utils/
    │   └── uuid.ts                # UUID 工具
    └── views/
        ├── Home.vue               # 主页：应用切换
        ├── TCMAIChat.vue          # AI 云中医问诊页
        └── ManusChat.vue          # AI 超级智能体页
```

---

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

启动后访问 [http://localhost:5173](http://localhost:5173)。

开发环境下，Vite 会将 `/api/*` 代理到 `http://localhost:8123`（见 `vite.config.ts`）。

### 3. 构建生产包

```bash
npm run build
```

产物输出到 `dist/` 目录。

### 4. 预览生产包

```bash
npm run preview
```

---

## 环境要求

- **Node.js**: `>= 20.20.2`
- **包管理**: `npm` / `pnpm` / `yarn` 均可
- **后端服务**: 需在 `http://localhost:8123` 启动 Spring Boot 后端，并暴露 `/api/ai/*` 接口

### 环境变量

`.env.development`（开发环境）：

```
VITE_API_BASE_URL=http://localhost:8123
VITE_API_PREFIX=/api
```

`.env.production`（生产环境）：

```
VITE_API_BASE_URL=
VITE_API_PREFIX=/api
```

> 生产环境部署时，`VITE_API_BASE_URL` 留空表示走 Nginx 同源代理；如有跨域需求，可填写完整后端地址。

---

## 可用脚本

| 脚本                | 说明                              |
| ------------------- | --------------------------------- |
| `npm run dev`       | 启动开发服务器（默认端口 5173）   |
| `npm run build`     | 类型检查 + 生产构建               |
| `npm run preview`   | 预览构建产物                      |
| `npm run type-check`| 仅做 TypeScript 类型检查          |

---

## 核心功能

### 1. 应用中心（主页）

`/` 路径，展示两个 AI 应用的入口卡片，支持点击进入对应聊天室。

### 2. AI 云中医问诊

`/tcm-chat` 路径，对应后端 `GET /api/ai/tcm_app/chat/sse` 接口。

- 绿色主题，模拟中医师问诊
- 进入页面时自动生成唯一 `chatId`，用于区分会话
- 实时流式显示 AI 回复（打字机效果）
- 底部输入框支持 Enter 发送、Shift+Enter 换行

### 3. AI 超级智能体

`/manus-chat` 路径，对应后端 `GET /api/ai/manus/chat` 接口。

- 蓝色主题，通用智能体
- 同样的聊天室交互，支持中途停止生成

### 4. 通用聊天室能力

- **打字机效果**：每条 AI 回复对应一个气泡，文本碎片持续追加
- **中途停止**：AI 思考中可点击「停止」按钮主动中断
- **新会话**：清空当前消息并生成新 `chatId`
- **离开保护**：AI 正在回复时返回上一页会弹窗提示
- **自动滚动**：每收到一个碎片自动滚动到底部
- **错误处理**：网络异常时显示错误状态

---

## 后端接口约定

所有接口统一前缀：`http://localhost:8123/api`

### 1. AI 云中医问诊（SSE）

```
GET /api/ai/tcm_app/chat/sse?message={message}&chatId={chatId}
Content-Type: text/event-stream
```

后端实现：`Flux<ServerSentEvent<String>>`，每个 `data` 字段为一个文本碎片。

### 2. AI 超级智能体（SSE）

```
GET /api/ai/manus/chat?message={message}
Content-Type: text/event-stream
```

后端实现：`SseEmitter`，流式返回。

### 响应格式（SSE 规范）

```
data: 你好

data: ，我是

data: AI 中医助手

```

> 每个事件以空行（`\n\n`）分隔。前端 `SSEParser` 会按事件解析，并支持 `[DONE]` 终止标记。

---

## SSE 消息处理规范

**核心原则**：一次 SSE 调用 = 一个 AI 消息气泡。

```
[ 用户消息 ]                ← 用户在右侧
[ AI 消息气泡 ]              ← AI 在左侧
   ├── data: 你好             ┐
   ├── data: ，我是           │  持续追加到同一个气泡中
   ├── data: AI 助手          │  （打字机效果）
   └── data: [DONE]           ┘
[ 用户消息 ]
[ AI 消息气泡 ]              ← 下一个完整回复
```

**实现要点**（详见 `src/api/sse.ts` 与 `src/components/ChatRoom.vue`）：

1. 用户点击发送后，**先占位**一条空内容的 AI 消息（`status: pending`）
2. 启动 SSE 连接，每收到一个 `chunk`，**追加**到该占位消息的 `content` 字段
3. 同一个 AI 回复的所有碎片，都更新到 **同一个消息 id** 上
4. 流结束时（`onComplete`），将状态置为 `done`
5. 出错时（`onError`），将状态置为 `error` 并显示错误信息
6. 用户主动中断时，调用 `SSEParser.abort()` 关闭连接

---

## 移动端适配

- 顶部导航在窄屏下自动隐藏副标题
- 消息气泡最大宽度根据屏幕宽度自适应
- 底部输入区考虑了 `env(safe-area-inset-bottom)` 适配刘海屏
- 整体使用 `flex: 1 + overflow: auto` 实现聊天区域独立滚动
- 输入框支持 `autosize`（最多 4 行）后变高度

---

## 常见问题

### Q1: 后端跨域或接口 404？

检查 `vite.config.ts` 中的 `proxy` 配置是否正确指向后端地址，并确认后端 CORS 允许来自 `http://localhost:5173` 的请求。

### Q2: SSE 收不到消息？

1. 打开浏览器 DevTools 的 Network 面板，确认请求返回 `Content-Type: text/event-stream`
2. 检查后端是否设置 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` 或直接返回 `ServerSentEvent`
3. 反向代理（Nginx）需禁用缓冲：`proxy_buffering off;`

### Q3: 流式消息出现重复 / 顺序错乱？

确认前端每次发送都重新创建了一条 AI 消息占位（`activeMessageId`），所有 chunk 追加到同一条记录上。详见 `ChatRoom.vue` 的 `sendMessage` 方法。

### Q4: 如何自定义主题色？

在 `<ChatRoom>` 组件上设置 `theme-color="green" | "blue"`，再自定义 CSS 变量即可：

```vue
<ChatRoom theme-color="green" ... />
```

---

## License

MIT
