<script setup>
// 根组件：页面出口 + 全站统一页脚（版权信息，仿面试鸭风格）
</script>

<template>
  <div class="app-shell">
    <router-view class="page-area" />

    <footer class="site-footer">
      <div class="footer-inner">
        <span class="footer-brand">⚡ AI 应用广场</span>
        <nav class="footer-links" aria-label="站内导航">
          <router-link to="/">主页</router-link>
          <router-link to="/love-app">AI 恋爱大师</router-link>
          <router-link to="/manus">AI 超级智能体</router-link>
        </nav>
        <p class="footer-copy">Copyright © 2024-2026 wcf-ai-agent · 基于 Vue 3 + Spring AI 构建 · 仅供学习交流</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 路由页面撑满页脚以上空间 */
.page-area {
  flex: 1 0 auto;
}

/* ---------- 全站页脚 ---------- */
.site-footer {
  flex-shrink: 0;
  background: #0b0f19;
  border-top: 1px solid rgba(56, 189, 248, 0.14);
  color: #8a93a6;
  font-size: 13px;
}

.footer-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 10px 24px;
  min-height: var(--footer-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 24px;
}

.footer-brand {
  font-family: var(--font-mono);
  font-weight: 600;
  letter-spacing: 1px;
  color: #cbd5f5;
}

.footer-links {
  display: flex;
  gap: 20px;
}

.footer-links a {
  color: #8a93a6;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: #38f0e0;
}

.footer-copy {
  font-family: var(--font-mono);
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 640px) {
  .footer-inner {
    flex-direction: column;
    justify-content: center;
    text-align: center;
    padding: 12px 16px;
    gap: 8px;
  }

  .footer-copy {
    line-height: 1.7;
  }
}
</style>
