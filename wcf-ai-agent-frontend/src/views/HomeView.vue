<script setup>
// 主页：用于切换不同的应用
const apps = [
  {
    path: '/love-app',
    icon: '💘',
    title: 'AI 恋爱大师',
    description: '你的贴心情感顾问：陪你聊恋爱话题、出主意、写情书，回复内容全程 SSE 流式输出。',
    tags: ['SSE 流式对话', '多会话隔离', '情感陪聊'],
    accent: 'pink',
  },
  {
    path: '/manus',
    icon: '🚀',
    title: 'AI 超级智能体',
    description: '基于 Manus 的自主智能体：会自己规划任务并调用工具，一步步帮你完成复杂请求。',
    tags: ['自主规划', '工具调用', '实时进度'],
    accent: 'blue',
  },
]
</script>

<template>
  <div class="home-page">
    <header class="hero">
      <span class="hero-badge">🤖 WCF AI Agent</span>
      <h1>AI 应用广场</h1>
      <p>选择一个应用，开始你的 AI 之旅</p>
    </header>

    <main class="app-grid">
      <router-link v-for="app in apps" :key="app.path" class="app-card" :class="app.accent" :to="app.path">
        <span class="app-icon">{{ app.icon }}</span>
        <h2>{{ app.title }}</h2>
        <p>{{ app.description }}</p>
        <div class="tag-list">
          <span v-for="tag in app.tags" :key="tag">{{ tag }}</span>
        </div>
        <span class="enter-tip">开始使用 →</span>
      </router-link>
    </main>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64px 24px;
  background:
    radial-gradient(600px 300px at 20% 0%, rgba(255, 138, 182, 0.16), transparent 70%),
    radial-gradient(600px 300px at 80% 0%, rgba(79, 110, 247, 0.16), transparent 70%),
    var(--color-bg);
}

/* ---------- 标题区 ---------- */
.hero {
  text-align: center;
  margin-bottom: 56px;
}

.hero-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--color-primary);
  background: #fff;
  border: 1px solid #d9e0ff;
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.05);
}

.hero h1 {
  margin-top: 20px;
  font-size: 40px;
  letter-spacing: 2px;
}

.hero p {
  margin-top: 12px;
  font-size: 15px;
  color: var(--color-text-secondary);
}

/* ---------- 应用卡片 ---------- */
.app-grid {
  width: 100%;
  max-width: 920px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 28px;
}

.app-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 32px 28px;
  border-radius: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-card);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  overflow: hidden;
}

.app-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 5px;
  background: var(--card-accent, var(--color-primary));
}

.app-card.pink {
  --card-accent: linear-gradient(90deg, #ff8ab6, #ff6b9d);
}

.app-card.blue {
  --card-accent: linear-gradient(90deg, #5b8cff, #4f6ef7);
}

.app-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 40px rgba(31, 35, 41, 0.14);
}

.app-icon {
  font-size: 44px;
  line-height: 1;
}

.app-card h2 {
  margin-top: 18px;
  font-size: 22px;
}

.app-card p {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  flex: 1;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.tag-list span {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--color-primary);
  background: #eef1ff;
}

.app-card.pink .tag-list span {
  color: #e0447c;
  background: #ffeef4;
}

.enter-tip {
  margin-top: 22px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.app-card.pink .enter-tip {
  color: #e0447c;
}

@media (max-width: 640px) {
  .hero h1 {
    font-size: 30px;
  }
}
</style>
