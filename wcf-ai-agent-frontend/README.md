# AI 应用广场（前端）

基于 Vue 3 + Vite 的前端项目，包含三个页面：

| 页面 | 路由 | 说明 |
| --- | --- | --- |
| 主页 | `/` | 应用入口，切换不同应用 |
| AI 恋爱大师 | `/love-app` | 粉色温柔风卡片布局，SSE 调用 `doChatWithLoveAppSse`，会话 id 即后端 `chatId` |
| AI 超级智能体 | `/manus` | 蓝色科技风卡片布局，SSE 调用 `doChatWithManus`，每个执行步骤单独一个气泡 |

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
    ├── composables
    │   └── useSessions.js  # 会话管理：多会话列表 + localStorage 持久化
    ├── components
    │   └── ChatRoom.vue    # 通用聊天室组件（双主题卡片布局、SSE 流式渲染、打字机、停止、历史会话）
    └── views
        ├── HomeView.vue    # 主页：应用切换入口
        ├── LoveAppChat.vue # 页面 1：AI 恋爱大师（love 主题，合并流式模式）
        └── ManusChat.vue   # 页面 2：AI 超级智能体（manus 主题，per-event-bubble 分气泡模式）
```

## 功能说明

- 卡片式布局（仿设计稿）：页面背景 + 居中圆角卡片，左侧边栏（品牌区 / 新建会话按钮 /
  历史会话列表）+ 右侧聊天区 + 底部快捷提问与输入区；
  两个主题：`manus`（顶部通栏品牌 + 通栏输入区）、`love`（品牌在侧边栏、
  输入区只在聊天列下方），由 `theme` 属性切换；
- 消息气泡：AI 消息在左（渐变色默认头像：机器人 💠 / 爱心，见 `AppIcon.vue`），
  用户消息在右（“你”头像），气泡内文字统一左对齐，每条消息下方带 `HH:mm` 时间戳；
- 多会话管理：会话列表持久化在 `localStorage`，支持新建 / 切换 / 刷新后恢复上次会话；
  会话标题自动取首条用户消息；AI 恋爱大师的会话 id 直接作为后端 `chatId`，
  同一会话共享上下文，不同会话相互隔离；对话内容也按会话持久化（流式过程中每 2 秒节流保存）；
- SSE 流式渲染：AI 回复以“打字机”效果（30ms 自适应速率）逐段显示，支持随时「停止」；
- 分气泡模式（`per-event-bubble`）：Manus 的每条 SSE 消息对应一个执行步骤，独占一个气泡；
- 健壮性收尾（三层保障）：
  1. 后端在流结束前发送 `[DONE]` 结束标记，前端收到后主动关闭连接
     （避免 EventSource 自动重连导致智能体被重复执行，见后端 `BaseAgent.runStream`）；
  2. `onerror` 统一收尾（连接异常 / 服务端正常关闭）；
  3. 前端兜底看门狗：定时检测 `readyState === CLOSED`，以及连接空闲超过 3 分钟
     无数据时主动断开，界面不会卡在“输出中”；
- 响应式：PC / 平板（≥1024px 侧边栏收窄）/ 手机（≤768px 隐藏侧边栏、显示返回主页
  圆形按钮，≤640px 快捷提问横向滑动、气泡与输入区紧凑布局）；
- SEO：`index.html` 内置 description / keywords / Open Graph / Twitter 卡片 /
  JSON-LD 结构化数据，路由切换时由 `router.afterEach` 按页面动态更新标题与元信息，
  另有 `public/robots.txt`；Lighthouse SEO / Accessibility / Best Practices 均为 100 分；
- 全站页脚：深色页脚统一展示品牌、站内链接与版权信息（仿面试鸭风格）；
- 主页（极客风）：深色霓虹网格背景 + 漂移光晕、终端窗口打字机动画、故障艺术标题、
  系统状态栏、终端窗口式应用卡片（hover 扫光 + 霓虹辉光），并尊重系统“减少动态效果”设置；
- 体验细节：Enter 发送 / Shift+Enter 换行、输入框高度自适应、消息区自动滚动到底部、
  连接异常与后端未启动的错误提示、空会话快捷提问。
