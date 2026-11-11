<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

import AppIcon from '@/components/AppIcon.vue'

// 主页：极客风（终端 + 霓虹 + 故障艺术），展示并切换不同应用
const apps = [
  {
    path: '/love-app',
    icon: 'heart',
    dir: '~/apps/love-app',
    title: 'AI 恋爱大师',
    description: '你的贴心情感顾问：陪你聊恋爱话题、出主意、写情书，回复内容全程 SSE 流式输出。',
    tags: ['SSE 流式对话', '多会话隔离', '情感陪聊'],
    accent: 'pink',
    cmd: 'npm start love-app --tender',
  },
  {
    path: '/manus',
    icon: 'bot',
    dir: '~/apps/manus-agent',
    title: 'AI 超级智能体',
    description: '基于 Manus 的自主智能体：会自己规划任务并调用工具，一步步帮你完成复杂请求。',
    tags: ['自主规划', '工具调用', '实时进度'],
    accent: 'cyan',
    cmd: './manus --plan=auto --tools=all',
  },
]

// ---------- 终端打字机效果 ----------
const typedText = ref('')
const commands = [
  'whoami   # 一个热爱造轮子的开发者',
  'start love-app      # 和 AI 聊聊心事',
  'start manus-agent   # 让 AI 替你干活',
]

let typeTimer = null
let cmdIdx = 0
let charIdx = 0
let deleting = false

function typeLoop() {
  const cmd = commands[cmdIdx]
  if (!deleting) {
    charIdx += 1
    typedText.value = cmd.slice(0, charIdx)
    if (charIdx === cmd.length) {
      // 整句打完，停留一会再删除
      deleting = true
      typeTimer = setTimeout(typeLoop, 1800)
      return
    }
    typeTimer = setTimeout(typeLoop, 70)
  } else {
    charIdx -= 1
    typedText.value = cmd.slice(0, charIdx)
    if (charIdx === 0) {
      deleting = false
      cmdIdx = (cmdIdx + 1) % commands.length
      typeTimer = setTimeout(typeLoop, 400)
      return
    }
    typeTimer = setTimeout(typeLoop, 22)
  }
}

onMounted(() => {
  // 尊重系统“减少动态效果”设置：静态展示第一条命令
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduced) {
    typedText.value = commands[0]
    return
  }
  typeLoop()
})

onBeforeUnmount(() => {
  if (typeTimer) {
    clearTimeout(typeTimer)
  }
})
</script>

<template>
  <div class="home-page">
    <!-- 背景层：霓虹网格 + 漂移光晕 -->
    <div class="bg-grid" aria-hidden="true"></div>
    <div class="bg-glow glow-cyan" aria-hidden="true"></div>
    <div class="bg-glow glow-magenta" aria-hidden="true"></div>

    <div class="home-content">
      <!-- 顶部终端窗口 + 标题 -->
      <header class="hero">
        <div class="terminal">
          <div class="term-bar">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
            <span class="term-title">wcf@ai-agent: ~/apps</span>
          </div>
          <div class="term-body">
            <p class="term-line"><span class="prompt">$</span>whoami</p>
            <p class="term-out">ai-builder @ spring-ai × vue3</p>
            <p class="term-line"><span class="prompt">$</span>ls ./ready-apps</p>
            <p class="term-out accent">love-app   manus-agent</p>
            <p class="term-line">
              <span class="prompt">$</span>{{ typedText }}<span class="cursor"></span>
            </p>
          </div>
        </div>

        <h1 class="glitch" data-text="AI 应用广场">AI 应用广场</h1>
        <p class="hero-sub">// 选择你的智能体，把想象立刻执行 //</p>
        <div class="status-bar">
          <span class="status-dot"></span>
          SYSTEM ONLINE · 2 AGENTS READY · SSE STREAMING
        </div>
      </header>

      <!-- 应用卡片：终端窗口风格 -->
      <main class="app-grid">
        <router-link v-for="app in apps" :key="app.path" class="app-card" :class="app.accent" :to="app.path">
          <div class="card-bar">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
            <span class="card-dir">{{ app.dir }}</span>
            <span class="card-status">● online</span>
          </div>

          <div class="card-body">
            <div class="card-icon">
              <AppIcon :type="app.icon" :size="30" />
            </div>
            <h2>{{ app.title }}</h2>
            <p>{{ app.description }}</p>
            <div class="tag-list">
              <span v-for="tag in app.tags" :key="tag">{{ tag }}</span>
            </div>
            <div class="card-cmd"><span class="prompt">$</span>{{ app.cmd }}</div>
          </div>

          <div class="card-footer">
            <span class="enter-tip">启动应用</span>
            <span class="enter-arrow">→</span>
          </div>
        </router-link>
      </main>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  position: relative;
  min-height: calc(100vh - var(--footer-height));
  padding: clamp(28px, 5vw, 72px) 20px 60px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #e2e8f0;
  background:
    radial-gradient(1000px 500px at 50% -10%, rgba(56, 189, 248, 0.14), transparent 60%),
    #05070d;
}

/* ---------- 背景层 ---------- */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(56, 189, 248, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 189, 248, 0.07) 1px, transparent 1px);
  background-size: 42px 42px;
  -webkit-mask-image: radial-gradient(ellipse 90% 70% at 50% 0%, #000 25%, transparent 78%);
  mask-image: radial-gradient(ellipse 90% 70% at 50% 0%, #000 25%, transparent 78%);
  pointer-events: none;
}

.bg-glow {
  position: absolute;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  pointer-events: none;
  animation: drift 16s ease-in-out infinite alternate;
}

.glow-cyan {
  top: -180px;
  left: -140px;
  background: rgba(34, 211, 238, 0.3);
}

.glow-magenta {
  top: 8%;
  right: -180px;
  background: rgba(244, 63, 122, 0.26);
  animation-delay: -8s;
}

@keyframes drift {
  from {
    transform: translate(0, 0);
  }
  to {
    transform: translate(70px, 46px);
  }
}

.home-content {
  position: relative;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* ---------- 终端窗口 ---------- */
.terminal {
  width: min(620px, 100%);
  border-radius: 14px;
  background: rgba(10, 15, 28, 0.85);
  border: 1px solid rgba(56, 189, 248, 0.22);
  box-shadow:
    0 0 44px rgba(56, 189, 248, 0.12),
    inset 0 0 60px rgba(56, 189, 248, 0.04);
  backdrop-filter: blur(8px);
  text-align: left;
  animation: rise 0.7s ease both;
}

.term-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(56, 189, 248, 0.15);
}

.dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot.red {
  background: #ff5f57;
}

.dot.yellow {
  background: #febc2e;
}

.dot.green {
  background: #28c840;
}

.term-title {
  margin-left: 8px;
  font-family: var(--font-mono);
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.term-body {
  padding: 18px 20px 22px;
  font-family: var(--font-mono);
  font-size: 13.5px;
  line-height: 2;
}

.term-line {
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-all;
}

.term-line .prompt {
  color: #38f0e0;
  margin-right: 10px;
  user-select: none;
}

.term-out {
  color: #94a3b8;
  padding-left: 26px;
}

.term-out.accent {
  color: #7dd3fc;
}

.cursor {
  display: inline-block;
  width: 8px;
  height: 16px;
  margin-left: 4px;
  vertical-align: -3px;
  background: #38f0e0;
  animation: blink 1s steps(1) infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---------- 标题（故障艺术） ---------- */
.hero {
  text-align: center;
  margin-bottom: 52px;
}

.glitch {
  position: relative;
  margin-top: 40px;
  font-size: clamp(40px, 7vw, 72px);
  font-weight: 900;
  letter-spacing: 8px;
  color: #f4f7ff;
  text-shadow: 0 0 30px rgba(56, 189, 248, 0.35);
}

.glitch::before,
.glitch::after {
  content: attr(data-text);
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
}

.glitch::before {
  color: #38f0e0;
  clip-path: inset(0 0 58% 0);
  animation: glitch-top 3.4s infinite steps(2, end);
}

.glitch::after {
  color: #ff5c8a;
  clip-path: inset(62% 0 0 0);
  animation: glitch-bottom 2.9s infinite steps(2, end);
}

@keyframes glitch-top {
  0%,
  86%,
  100% {
    opacity: 0;
    transform: none;
  }
  88% {
    opacity: 0.9;
    transform: translate(-5px, -2px);
  }
  93% {
    opacity: 0.9;
    transform: translate(4px, 1px);
  }
  97% {
    opacity: 0;
    transform: none;
  }
}

@keyframes glitch-bottom {
  0%,
  80%,
  100% {
    opacity: 0;
    transform: none;
  }
  83% {
    opacity: 0.9;
    transform: translate(5px, 2px);
  }
  88% {
    opacity: 0.9;
    transform: translate(-4px, -1px);
  }
  92% {
    opacity: 0;
    transform: none;
  }
}

.hero-sub {
  margin-top: 14px;
  font-family: var(--font-mono);
  font-size: 14px;
  letter-spacing: 2px;
  color: #64748b;
}

.status-bar {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 26px;
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid rgba(52, 211, 153, 0.35);
  background: rgba(52, 211, 153, 0.08);
  color: #34d399;
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 1px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 10px #34d399;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  50% {
    opacity: 0.35;
  }
}

/* ---------- 应用卡片（终端窗口风格） ---------- */
.app-grid {
  position: relative;
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 28px;
}

.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  background: rgba(11, 16, 32, 0.78);
  border: 1px solid var(--card-border, rgba(56, 189, 248, 0.2));
  overflow: hidden;
  backdrop-filter: blur(10px);
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease,
    border-color 0.3s ease;
}

/* hover 扫光效果 */
.app-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: -80%;
  width: 60%;
  height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.07), transparent);
  transform: skewX(-20deg);
  transition: left 0.55s ease;
  pointer-events: none;
}

.app-card:hover::after {
  left: 120%;
}

.app-card.pink {
  --card-accent: #ff5c8a;
  --card-glow: rgba(255, 92, 138, 0.28);
  --card-soft: rgba(255, 92, 138, 0.14);
  --card-border-soft: rgba(255, 92, 138, 0.4);
}

.app-card.cyan {
  --card-accent: #38bdf8;
  --card-glow: rgba(56, 189, 248, 0.28);
  --card-soft: rgba(56, 189, 248, 0.14);
  --card-border-soft: rgba(56, 189, 248, 0.4);
}

.app-card:hover {
  transform: translateY(-8px);
  border-color: var(--card-accent);
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.5),
    0 0 32px var(--card-glow);
}

.card-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  font-family: var(--font-mono);
  font-size: 12px;
  color: #64748b;
}

.card-dir {
  margin-left: 6px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-status {
  color: #34d399;
}

.card-body {
  padding: 24px 24px 20px;
  flex: 1;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--card-accent);
  background: var(--card-soft);
  border: 1px solid var(--card-border-soft);
  box-shadow: 0 0 18px var(--card-soft);
}

.app-card h2 {
  margin-top: 16px;
  font-size: 22px;
  color: #f4f7ff;
}

.app-card p {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.85;
  color: #94a3b8;
  flex: 1;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  font-family: var(--font-mono);
  font-size: 12px;
}

.tag-list span {
  padding: 3px 9px;
  border-radius: 6px;
  color: var(--card-accent);
  background: var(--card-soft);
  border: 1px solid var(--card-border-soft);
}

.card-cmd {
  margin-top: 18px;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(2, 6, 17, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.14);
  font-family: var(--font-mono);
  font-size: 12.5px;
  color: #7dd3fc;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-cmd .prompt {
  color: #38f0e0;
  margin-right: 8px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  font-family: var(--font-mono);
}

.enter-tip {
  color: var(--card-accent);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 3px;
}

.enter-arrow {
  color: var(--card-accent);
  font-size: 18px;
  transition: transform 0.25s ease;
}

.app-card:hover .enter-arrow {
  transform: translateX(6px);
}

/* ---------- 响应式 ---------- */
@media (max-width: 640px) {
  .home-page {
    padding: 24px 14px 40px;
  }

  .hero {
    margin-bottom: 32px;
  }

  .glitch {
    margin-top: 28px;
    letter-spacing: 4px;
  }

  .hero-sub {
    font-size: 12px;
  }

  .status-bar {
    font-size: 10.5px;
    letter-spacing: 0.5px;
    padding: 7px 14px;
  }

  .term-body {
    font-size: 12px;
    line-height: 1.9;
  }

  .app-grid {
    gap: 18px;
  }

  .card-body {
    padding: 20px 18px 16px;
  }
}
</style>
