<template>
  <div class="home">
    <!-- 动态知识节点网络（签名元素） -->
    <div class="knowledge-network" aria-hidden="true">
      <svg viewBox="0 0 800 400" fill="none" class="network-svg">
        <!-- 连接线 -->
        <line x1="120" y1="80" x2="220" y2="140" class="conn conn-1" />
        <line x1="220" y1="140" x2="360" y2="80" class="conn conn-2" />
        <line x1="360" y1="80" x2="460" y2="160" class="conn conn-3" />
        <line x1="460" y1="160" x2="560" y2="80" class="conn conn-4" />
        <line x1="560" y1="80" x2="680" y2="140" class="conn conn-5" />
        <line x1="220" y1="140" x2="240" y2="260" class="conn conn-6" />
        <line x1="240" y1="260" x2="400" y2="300" class="conn conn-7" />
        <line x1="400" y1="300" x2="560" y2="260" class="conn conn-8" />
        <line x1="560" y1="260" x2="580" y2="140" class="conn conn-9" />
        <line x1="240" y1="260" x2="120" y2="300" class="conn conn-10" />
        <line x1="560" y1="260" x2="680" y2="300" class="conn conn-11" />
        <!-- 知识节点 -->
        <circle cx="120" cy="80" r="5" class="node node-1" />
        <circle cx="220" cy="140" r="4" class="node node-2" />
        <circle cx="360" cy="80" r="6" class="node node-3" />
        <circle cx="460" cy="160" r="4" class="node node-4" />
        <circle cx="560" cy="80" r="5" class="node node-5" />
        <circle cx="680" cy="140" r="3" class="node node-6" />
        <circle cx="240" cy="260" r="5" class="node node-7" />
        <circle cx="400" cy="300" r="8" class="node node-center" />
        <circle cx="560" cy="260" r="5" class="node node-8" />
        <circle cx="120" cy="300" r="4" class="node node-9" />
        <circle cx="680" cy="300" r="4" class="node node-10" />
        <circle cx="580" cy="140" r="3" class="node node-11" />
        <!-- 高亮节点（琥珀色） -->
        <circle cx="400" cy="300" r="14" class="node-glow" />
      </svg>
    </div>

    <!-- 英雄区 -->
    <header class="hero">
      <div class="hero-badge">✦ 知识 · 智能 ✦</div>
      <h1 class="hero-title">
        <span class="title-line">
          <span class="title-poem">你的第二大脑</span>
        </span>
        <span class="title-dot">·</span>
        <span class="title-line">
          <span class="title-poem">知识管家</span>
        </span>
      </h1>
      <p class="hero-desc">
        <span class="desc-line">你收藏的万千世界，写下的每一笔</span>
        <span class="desc-line">都化作星光</span>
        <span class="desc-line highlight">等你轻轻一问</span>
      </p>
    </header>

    <!-- 应用导航 -->
    <nav class="apps">
      <router-link to="/knowledge" class="app-card">
        <div class="app-icon-wrap">
          <svg viewBox="0 0 32 32" fill="none" class="app-icon">
            <path d="M6 5h14v18H6z" stroke="#10B981" stroke-width="1.5" fill="rgba(16,185,129,0.08)"/>
            <path d="M10 10h6M10 14h4M10 18h2" stroke="#10B981" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M20 9l5 2v16l-5-2V9z" stroke="#10B981" stroke-width="1.5" fill="rgba(16,185,129,0.08)"/>
          </svg>
        </div>
        <div class="app-info">
          <h2>个人知识库</h2>
          <p>基于笔记与收藏，智能检索问答</p>
        </div>
        <div class="app-arrow">
          <svg viewBox="0 0 24 24" fill="none"><path d="M5 12h14m-7-7l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
      </router-link>

      <router-link to="/manus" class="app-card">
        <div class="app-icon-wrap">
          <svg viewBox="0 0 32 32" fill="none" class="app-icon">
            <rect x="7" y="9" width="18" height="15" rx="3" stroke="#10B981" stroke-width="1.5" fill="rgba(16,185,129,0.08)"/>
            <circle cx="13" cy="17" r="2" fill="#10B981"/>
            <circle cx="19" cy="17" r="2" fill="#10B981"/>
            <path d="M16 6V3M8 9L5 6M24 9l3-3" stroke="#10B981" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="app-info">
          <h2>AI 超级智能体</h2>
          <p>多工具协同，智能任务执行</p>
        </div>
        <div class="app-arrow">
          <svg viewBox="0 0 24 24" fill="none"><path d="M5 12h14m-7-7l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
      </router-link>
    </nav>

    <!-- 知识来源诗意脚注 -->
    <div class="footnote" aria-hidden="true">
      <span class="fn-item">笔记</span>
      <span class="fn-dot">✦</span>
      <span class="fn-item">文档</span>
      <span class="fn-dot">✦</span>
      <span class="fn-item">收藏</span>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
/* ===== 知识网络背景（签名元素） ===== */
.knowledge-network {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
  mask-image: linear-gradient(to bottom, black 55%, transparent 90%);
  -webkit-mask-image: linear-gradient(to bottom, black 55%, transparent 90%);
}
.network-svg {
  width: 100%;
  height: 100%;
  opacity: 0.6;
}
.conn {
  stroke: rgba(16,185,129,0.12);
  stroke-width: 1.5;
  stroke-dasharray: 4 4;
  animation: dashFlow 12s linear infinite;
}
.conn-1 { animation-delay: 0s; }
.conn-2 { animation-delay: 0.6s; }
.conn-3 { animation-delay: 1.2s; }
.conn-4 { animation-delay: 1.8s; }
.conn-5 { animation-delay: 2.4s; }
.conn-6 { animation-delay: 3s; }
.conn-7 { animation-delay: 3.6s; }
.conn-8 { animation-delay: 4.2s; }
.conn-9 { animation-delay: 4.8s; }
.conn-10 { animation-delay: 5.4s; }
.conn-11 { animation-delay: 6s; }
.conn-12 { animation-delay: 6.6s; }

@keyframes dashFlow {
  from { stroke-dashoffset: 0; }
  to { stroke-dashoffset: -40; }
}

.node {
  fill: #34D399;
  animation: nodePulse 3s ease-in-out infinite;
}
.node-1 { animation-delay: 0s; }
.node-2 { animation-delay: 0.4s; }
.node-3 { animation-delay: 0.8s; }
.node-4 { animation-delay: 1.2s; }
.node-5 { animation-delay: 1.6s; }
.node-6 { animation-delay: 2s; }
.node-7 { animation-delay: 2.4s; }
.node-8 { animation-delay: 2.8s; }
.node-9 { animation-delay: 3.2s; }
.node-10 { animation-delay: 3.6s; }
.node-11 { animation-delay: 4s; }
.node-12 { animation-delay: 4.4s; }

.node-center {
  fill: #10B981;
  animation: nodeCenterPulse 2s ease-in-out infinite;
}
.node-glow {
  fill: none;
  stroke: #F59E0B;
  stroke-width: 1.5;
  opacity: 0.5;
  animation: glowPulse 3s ease-in-out infinite;
}

@keyframes nodePulse {
  0%, 100% { r: 4; opacity: 0.6; }
  50% { r: 6; opacity: 1; }
}
@keyframes nodeCenterPulse {
  0%, 100% { r: 8; opacity: 0.8; }
  50% { r: 11; opacity: 1; }
}
@keyframes glowPulse {
  0%, 100% { r: 14; opacity: 0.3; stroke-width: 1; }
  50% { r: 22; opacity: 0.6; stroke-width: 2; }
}

.dot-float {
  fill: rgba(16,185,129,0.3);
  animation: dotFloat 8s ease-in-out infinite;
}
.d1 { animation-delay: 0s; }
.d2 { animation-delay: 1.6s; }
.d3 { animation-delay: 3.2s; }
.d4 { animation-delay: 4.8s; }
.d5 { animation-delay: 6s; }
.d6 { animation-delay: 2.4s; }

@keyframes dotFloat {
  0%, 100% { transform: translate(0, 0); opacity: 0.3; }
  25% { transform: translate(10px, -15px); opacity: 0.6; }
  50% { transform: translate(-5px, -8px); opacity: 0.4; }
  75% { transform: translate(8px, 5px); opacity: 0.7; }
}

/* ===== 流体彩色背景 ===== */
.home {
  min-height: 100%;
  color: #065F46;
  padding: 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  gap: 2.5rem;
}
.home::before {
  content: '';
  position: absolute;
  inset: -50%;
  background:
    radial-gradient(ellipse at 20% 30%, rgba(16,185,129,0.2) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(99,102,241,0.15) 0%, transparent 45%),
    radial-gradient(ellipse at 50% 80%, rgba(236,72,153,0.12) 0%, transparent 40%),
    radial-gradient(ellipse at 75% 60%, rgba(251,146,60,0.12) 0%, transparent 40%),
    radial-gradient(ellipse at 30% 70%, rgba(59,130,246,0.12) 0%, transparent 40%);
  filter: blur(50px);
  animation:
    bgFlow1 18s ease-in-out infinite,
    bgFlow2 22s ease-in-out infinite,
    bgFlow3 14s ease-in-out infinite,
    bgFlow4 20s ease-in-out infinite,
    bgFlow5 16s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}
.home::after {
  content: '';
  position: absolute;
  inset: -100%;
  background: conic-gradient(
    from 0deg,
    transparent,
    rgba(99,102,241,0.03),
    transparent,
    rgba(236,72,153,0.03),
    transparent,
    rgba(16,185,129,0.03),
    transparent,
    rgba(251,146,60,0.03),
    transparent
  );
  animation: conicFlow 30s linear infinite;
  filter: blur(60px);
  pointer-events: none;
  z-index: 0;
}
@keyframes bgFlow1 {
  0%, 100% { background-position: 20% 30%, 80% 20%, 50% 80%, 75% 60%, 30% 70%; }
  25% { background-position: 60% 20%, 40% 60%, 80% 40%, 50% 80%, 70% 30%; }
  50% { background-position: 80% 50%, 20% 70%, 30% 30%, 30% 40%, 60% 80%; }
  75% { background-position: 40% 70%, 60% 40%, 60% 70%, 80% 30%, 20% 50%; }
}
@keyframes bgFlow2 { 0%,100% { background-position: 20% 30%; } 50% { background-position: 80% 70%; } }
@keyframes bgFlow3 { 0%,100% { background-position: 50% 80%; } 50% { background-position: 10% 20%; } }
@keyframes bgFlow4 { 0%,100% { background-position: 75% 60%; } 50% { background-position: 25% 40%; } }
@keyframes bgFlow5 { 0%,100% { background-position: 30% 70%; } 50% { background-position: 70% 30%; } }
@keyframes conicFlow {
  from { transform: rotate(0deg) scale(1); }
  to { transform: rotate(360deg) scale(1.2); }
}

/* ===== 英雄区 ===== */
.hero {
  text-align: center;
  position: relative;
  z-index: 1;
  animation: heroIn 0.8s ease both;
}
@keyframes heroIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 16px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 400;
  letter-spacing: 0.15em;
  color: #10B981;
  background: rgba(16,185,129,0.08);
  border: 1px solid rgba(16,185,129,0.12);
  margin-bottom: 1.25rem;
}
.hero-title {
  font-size: 2.8rem;
  font-weight: 700;
  line-height: 1.3;
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.title-line {
  display: flex;
  align-items: baseline;
  gap: 0.2em;
}
.title-base {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-weight: 300;
  color: #94a3b8;
  font-size: 0.7em;
  letter-spacing: 0.08em;
  margin-right: 0.3em;
}
.title-poem {
  font-family: 'Ma Shan Zheng', 'KaiTi', cursive;
  font-weight: 400;
  font-size: 1.1em;
  color: #065F46;
  letter-spacing: 0.06em;
  line-height: 1.2;
}
.title-dot {
  font-family: 'Playfair Display', serif;
  font-size: 0.8rem;
  color: #10B981;
  opacity: 0.5;
  margin: 2px 0;
}

/* ===== 诗意描述 ===== */
.hero-desc {
  font-size: 1rem;
  line-height: 2;
  color: #64748b;
  max-width: 440px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.desc-line {
  display: block;
}
.desc-line.highlight {
  color: #F59E0B;
  font-weight: 500;
  font-size: 1.05em;
}
.hero-desc {
  font-size: 1.05rem;
  color: #64748b;
  line-height: 1.7;
  max-width: 440px;
}
.desc-highlight {
  color: #F59E0B;
  font-weight: 500;
}
.break {
  display: none;
}

@media (min-width: 480px) {
  .break { display: block; }
  .hero-title { font-size: 3.4rem; }
}

/* ===== 应用卡片 ===== */
.apps {
  display: flex;
  gap: 1.25rem;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
  max-width: 520px;
  position: relative;
  z-index: 1;
}
.app-card {
  flex: 1 1 220px;
  min-width: 200px;
  max-width: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 1.75rem 1.5rem;
  border-radius: 20px;
  text-decoration: none;
  color: inherit;
  background: rgba(255,255,255,0.5);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.4);
  border-bottom: 1px solid rgba(16,185,129,0.06);
  box-shadow:
    0 4px 20px rgba(16,185,129,0.06),
    0 0 0 1px rgba(255,255,255,0.3) inset;
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.35s ease,
              background 0.35s ease;
  animation: cardIn 0.6s ease both;
  gap: 0.75rem;
  outline: none;
}
.app-card:first-child { animation-delay: 0.15s; }
.app-card:last-child { animation-delay: 0.3s; }
@keyframes cardIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
.app-card:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow:
    0 16px 40px rgba(16,185,129,0.12),
    0 0 0 1px rgba(255,255,255,0.5) inset;
  background: rgba(255,255,255,0.65);
}
.app-card:focus-visible {
  box-shadow: 0 0 0 3px #10B981, 0 8px 24px rgba(16,185,129,0.12);
}
.app-icon-wrap {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(16,185,129,0.06);
}
.app-icon {
  width: 32px;
  height: 32px;
}
.app-info h2 {
  font-size: 1.15rem;
  font-weight: 600;
  color: #065F46;
  margin-bottom: 0.3rem;
}
.app-info p {
  font-size: 0.85rem;
  color: #64748b;
  line-height: 1.5;
}
.app-arrow {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(16,185,129,0.08);
  color: #10B981;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.25rem;
  transition: transform 0.3s ease, background 0.3s ease;
}
.app-arrow svg {
  width: 16px;
  height: 16px;
}
.app-card:hover .app-arrow {
  transform: translateX(4px);
  background: rgba(16,185,129,0.15);
}

/* ===== 诗意脚注 ===== */
.footnote {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  position: relative;
  z-index: 1;
  animation: heroIn 0.8s ease 0.5s both;
  opacity: 0.4;
}
.fn-item {
  font-family: 'Ma Shan Zheng', 'KaiTi', cursive;
  font-size: 1.3rem;
  color: #94a3b8;
  letter-spacing: 0.15em;
}
.fn-dot {
  font-size: 0.7rem;
  color: #10B981;
  opacity: 0.5;
}

@media (max-width: 480px) {
  .home { padding: 1.25rem; gap: 1.75rem; }
  .hero-title { font-size: 2rem; }
  .hero-desc { font-size: 0.9rem; }
  .apps { max-width: 100%; }
  /* 应用卡片改为横向布局（图标左 + 文字中 + 箭头右），充分利用窄屏宽度 */
  .app-card {
    flex-direction: row;
    align-items: center;
    text-align: left;
    flex: 1 1 100%;
    min-width: 0;
    max-width: 100%;
    width: 100%;
    padding: 0.9rem 1rem;
    gap: 0.85rem;
  }
  .app-icon-wrap {
    width: 46px;
    height: 46px;
    border-radius: 12px;
    flex-shrink: 0;
  }
  .app-icon {
    width: 28px;
    height: 28px;
  }
  .app-info {
    flex: 1;
    min-width: 0;
  }
  .app-info h2 {
    font-size: 1.05rem;
    margin-bottom: 0.15rem;
  }
  .app-info p {
    font-size: 0.8rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .app-arrow {
    margin-top: 0;
    width: 30px;
    height: 30px;
    flex-shrink: 0;
  }
  /* 底部脚注预留空间，避免与左下角用户模块重叠 */
  .footnote {
    margin-bottom: 2.5rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .home, .hero, .app-card, .stats { animation: none !important; }
  .conn, .node, .dot-float, .node-glow { animation: none !important; }
  .home::before, .home::after { animation: none !important; }
}
</style>
