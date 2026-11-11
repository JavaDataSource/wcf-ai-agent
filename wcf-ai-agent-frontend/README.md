# AI 应用广场（前端）

基于 Vue 3 + Vite 的前端项目，包含三个页面：

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 主页 | `/` | 应用入口，切换不同应用 |
| AI 恋爱大师 | `/love-app` | 聊天室风格，SSE 调用 `doChatWithLoveAppSse`，进入页面自动生成聊天室 id |
| AI 超级智能体 | `/manus` | 聊天室风格，SSE 调用 `doChatWithManus`，实时展示智能体输出 |

## 技术选型

- Vue 3（组合式 API `<script setup>`）
- Vite 6 构建工具
- Vue Router 4 路由
- Axios 请求库（`src/utils/request.js` 统一封装，供普通 HTTP 接口使用）

> 说明：两个对话接口均以 SSE（`text/event-stream`）方式返回流式数据，
> 浏览器端的 Axios（基于 XHR）无法读取流式响应，因此 SSE 对话使用原生
> `EventSource` 实现（见 `ChatRoom.vue`），这也是 EventSource 的标准使用场景。

## 快速开始

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 3. 打包构建
npm run build
```

前提：先启动本地 SpringBoot 后端（接口地址前缀 `http://localhost:8123/api`）。

## 后端接口对接

| 后端接口 | 前端调用位置 |
| --- | --- |
| `GET /api/ai/love_app/chat/sse?message=&chatId=` | `src/views/LoveAppChat.vue` |
| `GET /api/ai/manus/chat?message=` | `src/views/ManusChat.vue` |

- **跨域处理**：开发环境通过 `vite.config.js` 中的 `server.proxy` 把 `/api`
  代理到 `http://localhost:8123`，前端代码统一使用相对路径 `/api/...`，无需后端额外配置 CORS。
- **生产部署**：建议用 Nginx 把 `/api` 反向代理到后端；或设置环境变量
  `VITE_API_BASE_URL=http://localhost:8123/api` 并在后端开启 CORS。

## 目录结构

```
wcf-ai-agent-frontend
├── index.html
├── vite.config.js          # 开发代理：/api -> http://localhost:8123
└── src
    ├── main.js
    ├── App.vue
    ├── assets/main.css     # 全局样式
    ├── router/index.js     # 路由：主页 / 恋爱大师 / 超级智能体
    ├── utils/request.js    # Axios 实例（普通接口用）
    ├── components
    │   └── ChatRoom.vue    # 通用聊天室组件（SSE 流式渲染、打字动画、停止/新建会话）
    └── views
        ├── HomeView.vue    # 主页：应用切换入口
        ├── LoveAppChat.vue # 页面 1：AI 恋爱大师（自动生成会话 id）
        └── ManusChat.vue   # 页面 2：AI 超级智能体
```

## 功能说明

- 聊天室布局：聊天记录在上方（用户消息靠右、AI 消息靠左），输入框在下方；
- SSE 流式渲染：AI 回复以“打字机”效果逐段显示，支持随时「停止」；
- 会话隔离：AI 恋爱大师进入页面自动生成 `chatId`（存在 `sessionStorage`，
  同一标签页刷新不丢失），点「新建会话」可重新生成并清空记录；
- 体验细节：Enter 发送 / Shift+Enter 换行、输入框高度自适应、消息区自动滚动到底部、
  连接异常与后端未启动的错误提示、快捷提问。
