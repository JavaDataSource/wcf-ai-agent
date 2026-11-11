# wcf-ai-agent

基于 Spring Boot 3 + Spring AI / Spring AI Alibaba 的 AI 智能体实战项目，包含 **AI 恋爱大师**（RAG 增强对话应用）与 **AI 超级智能体 YuManus**（自主规划、多工具调用的 ReAct Agent），并配套 Vue 3 前端与 MCP 图片搜索服务。

## 项目亮点

- 🔁 **三种 AI 调用方式**：OpenAI SDK / Spring AI / LangChain4j / DashScope SDK / HTTP 等多种主流接入演示
- 💬 **AI 恋爱大师（LoveApp）**：多会话记忆 + RAG 知识库 + 查询重写 / 关键词增强 / ReReading Advisor 等进阶技巧
- 🤖 **AI 超级智能体（YuManus）**：基于 BaseAgent → ReActAgent → ToolCallAgent 的分层设计，支持自主规划、最多 20 步循环思考和流式输出
- 🛠️ **丰富的自定义工具**：联网搜索、网页抓取、文件读写、PDF 生成、终端执行、资源下载、图片搜索（MCP）
- 📦 **RAG 全流程实践**：PgVector 向量存储、文档切分、云端/本地知识库 Advisor 工厂
- 🌐 **配套前端**：Vue 3 + Vite 的「AI 应用广场」，SSE 流式打字机渲染（见 [wcf-ai-agent-frontend](wcf-ai-agent-frontend/README.md)）

## 技术选型

| 分类 | 技术 |
| --- | --- |
| 核心 | Java 21、Spring Boot 3.5.7、Spring AI 1.0.0、Spring AI Alibaba 1.0.0.2 |
| 大模型 | 阿里云 DashScope（通义千问 qwen-plus）、Ollama（本地模型） |
| RAG / 向量库 | PostgreSQL + PgVector、Markdown Document Reader、自定义 TextSplitter / KeywordEnricher |
| 智能体 | ReAct 模式 Agent、ChatMemory（基于 Kryo 的文件持久化）、Tool Calling |
| MCP | spring-ai-starter-mcp-client + 自研图片搜索 MCP Server |
| 工具库 | Hutool、Jsoup、iText 9（PDF 生成）、jsonschema-generator |
| 接口文档 | Knife4j（OpenAPI 3）+ Swagger UI |
| 前端 | Vue 3、Vite 6、Vue Router、EventSource（SSE） |

## 项目结构

```
wcf-ai-agent
├── src/main/java/com/wcf/wcfaiagent
│   ├── agent        # 智能体：BaseAgent / ReActAgent / ToolCallAgent / YuManus
│   ├── advisor      # 提示词与日志增强器（MyLoggerAdvisor、ReReadingAdvisor）
│   ├── app          # AI 恋爱大师应用 LoveApp
│   ├── chatmemory   # 基于文件的会话记忆持久化
│   ├── config       # CORS 等配置
│   ├── controller   # AI 接口层（同步 / SSE 流式）
│   ├── demo         # 各类大模型调用方式演示（SDK、Spring AI、Ollama、HTTP…）
│   ├── rag          # 向量库配置、文档加载、查询增强、检索 Advisor 工厂
│   ├── tools        # 自定义工具（搜索、抓取、文件、PDF、终端、下载、停止）
│   └── controller   # HealthController / AiController
├── src/main/resources
│   ├── application.yml          # 主配置（默认激活 local 环境）
│   ├── application-local.yml    # 本地开发配置
│   └── application-prod.yml     # 生产配置
├── wcf-ai-agent-frontend              # Vue 3 前端「AI 应用广场」
├── wcf-image-search-mcp-server        # 自研 MCP 图片搜索服务（SSE）
└── docs                               # 项目文档
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+（项目自带 `mvnw`）
- Node.js 18+（前端）

### 配置密钥

在 `src/main/resources/application.yml`（或 `application-local.yml`）中替换为你自己的 Key：

```yaml
spring:
  ai:
    dashscope:
      api-key: <你的阿里云百炼 API Key>
search-api:
  api-key: <你的 SearchAPI Key>
```

> 可选能力（按需开启）：Ollama 本地模型（`localhost:11434`）、PgVector 向量库（`spring.datasource` 与 `spring.ai.vectorstore.pgvector`）、MCP 服务连接（`spring.ai.mcp.client`），配置示例见 `application.yml` 中的注释。

### 启动后端

```bash
# Windows
mvnw.cmd spring-boot:run
# 或 Linux / macOS
./mvnw spring-boot:run
```

后端默认地址：`http://localhost:8123/api`

### 启动前端

```bash
cd wcf-ai-agent-frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`（开发环境已通过 Vite 代理 `/api` 到后端 8123 端口）。

## 核心 API

统一前缀：`http://localhost:8123/api`

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/ai/love_app/chat/sync` | GET | AI 恋爱大师同步对话（`message`、`chatId`） |
| `/ai/love_app/chat/sse` | GET | AI 恋爱大师 SSE 流式对话 |
| `/ai/love_app/chat/server_sent_event` | GET | SSE 结构化（ServerSentEvent）流式对话 |
| `/ai/love_app/chat/sse_emitter` | GET | SseEmitter 实现的流式对话（3 分钟超时） |
| `/ai/manus/chat` | GET | YuManus 超级智能体流式对话（`message`） |

完整接口文档：启动后访问 Knife4j 在线文档

```
http://localhost:8123/api/doc.html
```

## 子项目说明

### wcf-ai-agent-frontend

Vue 3 + Vite 前端，包含主页、AI 恋爱大师、AI 超级智能体三个页面，聊天室风格界面，支持 SSE 打字机流式渲染、会话隔离与新建会话。详见 [wcf-ai-agent-frontend/README.md](wcf-ai-agent-frontend/README.md)。

### wcf-image-search-mcp-server

基于 Spring AI MCP Server（SSE 传输）的图片搜索服务，可直接对接本项目或其他支持 MCP 的客户端，作为智能体的工具扩展。启动后默认监听 `8127` 端口，在后端 `application.yml` 中 uncomment MCP Client 相关配置即可接入。

## 参考文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
- [阿里云百炼控制台](https://bailian.console.aliyun.com/)
- 项目其他说明见 [HELP.md](HELP.md) 与 [docs](docs/) 目录
