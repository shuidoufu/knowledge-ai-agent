<template>
  <div class="app">
    <main class="main-content">
      <router-view />
    </main>

    <!-- 左下角用户模块 -->
    <div class="user-dock">
      <!-- 未登录：显示登录入口 -->
      <div v-if="!loggedIn" class="dock-trigger" @click="goLogin" title="登录">
        <div class="dock-avatar-ring">
          <div class="dock-avatar">
            <svg class="dock-guest-icon" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.5"/><path d="M4 21v-1a6 6 0 016-6h4a6 6 0 016 6v1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
        </div>
      </div>
      <!-- 已登录：头像 + 上拉菜单 -->
      <div v-else ref="avatarWrapRef" class="dock-avatar-wrap">
        <button
          type="button"
          class="dock-trigger"
          @click.stop="showDropdown = !showDropdown"
          title="账号"
        >
          <div class="dock-avatar-ring">
            <div class="dock-avatar">
              <span class="dock-letter">{{ avatarLetter }}</span>
            </div>
          </div>
        </button>
        <Transition name="dock-dropdown">
          <div v-show="showDropdown" class="dock-dropdown-panel">
            <div class="dock-dropdown-name">{{ username }}</div>
            <button type="button" class="dock-dropdown-item" @click="goChangePassword">
              修改密码
            </button>
            <button type="button" class="dock-dropdown-item" @click="logout">
              登出
            </button>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { request } from './api/request'
import { isLoggedIn, getUsername, removeToken } from './utils/auth'

const router = useRouter()
const route = useRoute()
const showDropdown = ref(false)
const avatarWrapRef = ref(null)

const loggedIn = ref(isLoggedIn())
watch(() => route.path, () => {
  loggedIn.value = isLoggedIn()
}, { immediate: true })

const username = computed(() => getUsername())
const avatarLetter = computed(() => {
  const name = getUsername()
  return name ? name.trim().charAt(0).toUpperCase() : ''
})
const avatarBgColor = computed(() => {
  const name = getUsername()
  if (!name) return '#64748b'
  let n = 0
  for (let i = 0; i < name.length; i++) n += name.charCodeAt(i)
  const hues = ['#6366f1', '#8b5cf6', '#ec4899', '#ef4444', '#f97316', '#22c55e', '#14b8a6', '#3b82f6']
  return hues[n % hues.length]
})

function goLogin() { router.push('/login') }
function closeDropdown() { showDropdown.value = false }
function handleClickOutside(e) {
  if (avatarWrapRef.value && !avatarWrapRef.value.contains(e.target)) closeDropdown()
}
function goChangePassword() { closeDropdown(); router.push('/change-password') }
function logout() {
  request.post('/auth/logout').catch(() => {})
  removeToken()
  loggedIn.value = false
  closeDropdown()
  router.push('/')
}

onMounted(() => { document.addEventListener('click', handleClickOutside) })
onUnmounted(() => { document.removeEventListener('click', handleClickOutside) })
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');
:root {
  --bg-page: #f5f3ff;
  --bg-card: #ffffff;
  --text-primary: #1e1b4b;
  --text-secondary: #6366f1;
  --text-muted: #94a3b8;
  --border-light: #e0e7ff;
  --border-lighter: #eef2ff;
  --shadow-sm: 0 1px 3px rgba(99,102,241,0.08);
  --shadow-md: 0 4px 16px rgba(99,102,241,0.10);
  --shadow-lg: 0 10px 40px rgba(99,102,241,0.12);
  --pink: #ec4899;
  --blue: #3b82f6;
  --primary: #6366f1;
  --primary-light: #818cf8;
  --cta: #10b981;
  --glass-bg: rgba(255,255,255,0.75);
  --glass-border: rgba(255,255,255,0.3);
  --glass-shadow: 0 8px 32px rgba(99,102,241,0.08);
}
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #94a3b8; }
html { scrollbar-width: thin; scrollbar-color: #cbd5e1 transparent; }
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app, .app {
  height: 100%;
  font-family: 'Plus Jakarta Sans', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg-page);
  color: var(--text-primary);
}
html, body { overflow-x: hidden; }
.app { display: flex; flex-direction: column; }

/* 全局头部 */
.main-content { flex: 1; min-height: 0; }

/* 邮箱和社交动态发光 */
@keyframes ringSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@keyframes letterGlow {
  0%, 100% { text-shadow: 0 0 12px rgba(99,102,241,0.3), 0 0 30px rgba(99,102,241,0.1); }
  50% { text-shadow: 0 0 20px rgba(99,102,241,0.5), 0 0 50px rgba(99,102,241,0.2); }
}
@keyframes letterShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

/* ===== 左下角用户模块 ===== */
.user-dock {
  position: fixed;
  left: 20px;
  bottom: 20px;
  z-index: 999;
}
.dock-trigger {
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 透明外圈 - 渐变色环 */
.dock-avatar-ring {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dock-avatar-ring::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  background: conic-gradient(#6366f1, #818cf8, #a78bfa, #c4b5fd, #6366f1);
  animation: ringSpin 4s linear infinite;
  mask: radial-gradient(farthest-side, transparent calc(100% - 2.5px), #fff calc(100% - 2.5px));
  -webkit-mask: radial-gradient(farthest-side, transparent calc(100% - 2.5px), #fff calc(100% - 2.5px));
}
.dock-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: transparent;
  cursor: pointer;
  transition: transform 0.2s;
}
.dock-avatar:hover {
  transform: scale(1.08);
}
.dock-letter {
  font-family: 'Space Grotesk', 'Plus Jakarta Sans', sans-serif;
  font-size: 1.4rem;
  font-weight: 700;
  background: linear-gradient(135deg, #6366f1, #818cf8, #a78bfa);
  background-size: 200% 200%;
  animation: letterShift 3s ease-in-out infinite, letterGlow 2.5s ease-in-out infinite;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  user-select: none;
}
.dock-guest-icon {
  width: 24px;
  height: 24px;
  color: #a5b4fc;
  opacity: 0.7;
}
.dock-avatar-wrap {
  position: relative;
}

/* 上拉菜单（从底部弹出） */
.dock-dropdown-panel {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 0;
  min-width: 160px;
  padding: 0.4rem 0;
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 14px;
  box-shadow: 0 -8px 32px rgba(99,102,241,0.1);
  z-index: 100;
}
.dock-dropdown-name {
  padding: 0.6rem 1rem;
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 600;
  border-bottom: 1px solid var(--border-lighter);
}
.dock-dropdown-item {
  display: block;
  width: 100%;
  padding: 0.6rem 1rem;
  border: none;
  background: none;
  color: var(--text-secondary);
  font-size: 0.875rem;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.dock-dropdown-item:hover {
  background: rgba(99,102,241,0.08);
  color: var(--primary);
}

/* 下拉动画（向上弹出） */
.dock-dropdown-enter-active,
.dock-dropdown-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.dock-dropdown-enter-from,
.dock-dropdown-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
