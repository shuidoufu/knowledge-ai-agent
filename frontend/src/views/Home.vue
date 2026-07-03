<template>
  <div class="home">
    <header class="header">
      <h1>AI 应用中心</h1>
      <p>选择下方应用开始使用</p>
    </header>
    <nav class="nav">
      <router-link to="/love" class="card card-love">
        <span class="card-icon">
          <svg viewBox="0 0 32 32" fill="none"><path d="M16 28S6 20 6 13a6 6 0 0111-4 6 6 0 0111 4c0 7-10 15-10 15z" fill="#ec4899" stroke="#db2777" stroke-width="1.2"/></svg>
        </span>
        <h2>AI 恋爱大师</h2>
        <p>倾诉恋爱难题，获取专属建议</p>
      </router-link>
      <router-link to="/manus" class="card card-manus">
        <span class="card-icon">
          <svg viewBox="0 0 32 32" fill="none"><rect x="6" y="10" width="20" height="14" rx="3" stroke="#6366f1" stroke-width="1.5" fill="rgba(99,102,241,0.1)"/><circle cx="12" cy="17" r="1.5" fill="#6366f1"/><circle cx="20" cy="17" r="1.5" fill="#6366f1"/><path d="M16 8V4m-8 6L5 7m18 3l3-3M11 24v2a1 1 0 001 1h8a1 1 0 001-1v-2" stroke="#6366f1" stroke-width="1.5" stroke-linecap="round"/></svg>
        </span>
        <h2>AI 超级智能体</h2>
        <p>多工具协同，智能任务执行</p>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
</script>

<style scoped>
@keyframes flowLight {
  0%   { background-position: 0% 50%; }
  50%  { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}
@media (prefers-reduced-motion: reduce) {
  .home { animation: none !important; }
  .card { animation: none !important; }
}
.home {
  min-height: 100%;
  background: linear-gradient(135deg, #f5f3ff, #e0e7ff, #fce7f3, #d1fae5, #f5f3ff);
  background-size: 400% 400%;
  animation: flowLight 20s ease-in-out infinite;
  color: #1e1b4b;
  padding: 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.header {
  text-align: center;
  margin-bottom: 3rem;
}
.header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  color: #1e1b4b;
}
.header p {
  color: #4f46e5;
  font-size: 1rem;
  font-weight: 500;
}
.nav {
  display: flex;
  gap: 2rem;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
  max-width: 600px;
}
.card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  flex: 1 1 240px;
  min-width: 220px;
  max-width: 280px;
  padding: 2rem;
  border-radius: 20px;
  text-decoration: none;
  color: inherit;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
  border: 1px solid rgba(255,255,255,0.3);
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 4px 16px rgba(99,102,241,0.08);
  animation: fadeInUp 0.6s ease both;
  outline: none;
}
.card:first-child { animation-delay: 0.1s; }
.card:last-child  { animation-delay: 0.25s; }
.card:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 16px 40px rgba(99,102,241,0.15);
}
.card:focus-visible {
  box-shadow: 0 0 0 3px #6366f1, 0 8px 24px rgba(99,102,241,0.12);
}
.card-love {
  border-color: rgba(236,72,153,0.25);
}
.card-love:hover {
  box-shadow: 0 16px 40px rgba(236,72,153,0.18);
}
.card-manus {
  border-color: rgba(99,102,241,0.25);
}
.card-manus:hover {
  box-shadow: 0 16px 40px rgba(99,102,241,0.18);
}
.card-icon {
  width: 56px;
  height: 56px;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card-icon svg {
  width: 40px;
  height: 40px;
}
.card h2 {
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
  color: #1e1b4b;
}
.card p {
  font-size: 0.875rem;
  color: #6366f1;
}
</style>
