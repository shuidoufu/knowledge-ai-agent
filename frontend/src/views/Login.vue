<template>
  <div class="login-page">
    <!-- 抽象装饰 -->
    <div class="deco" aria-hidden="true">
      <svg viewBox="0 0 800 800" fill="none">
        <circle cx="150" cy="200" r="180" fill="url(#g1)" opacity="0.3"/>
        <circle cx="650" cy="600" r="160" fill="url(#g2)" opacity="0.25"/>
        <circle cx="700" cy="150" r="100" fill="url(#g3)" opacity="0.15"/>
        <defs>
          <radialGradient id="g1" cx="50%" cy="50%"><stop offset="0%" stop-color="#6366f1" stop-opacity="0.6"/><stop offset="100%" stop-color="#6366f1" stop-opacity="0"/></radialGradient>
          <radialGradient id="g2" cx="50%" cy="50%"><stop offset="0%" stop-color="#ec4899" stop-opacity="0.6"/><stop offset="100%" stop-color="#ec4899" stop-opacity="0"/></radialGradient>
          <radialGradient id="g3" cx="50%" cy="50%"><stop offset="0%" stop-color="#818cf8" stop-opacity="0.5"/><stop offset="100%" stop-color="#818cf8" stop-opacity="0"/></radialGradient>
        </defs>
      </svg>
    </div>

    <router-link to="/" class="back-link">
	      <ArrowLeft class="icon" size="16" />
	      返回
	    </router-link>
    <div class="login-card">
      <h1>{{ isRegister ? '注册' : '登录' }}</h1>
      <p class="hint">{{ isRegister ? '创建账号后可体验所有 AI 功能' : '登录后可使用个人知识助手、超级智能体等会话功能' }}</p>
      <form @submit.prevent="submit" class="form" :class="{ shake: shaking }">
        <input
          v-model="username"
          type="text"
          placeholder="用户名"
          autocomplete="username"
          class="input"
        />

        <div class="pwd-input-wrap">
          <input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="密码"
            autocomplete="current-password"
            class="input pwd-input"
          />
          <button type="button" class="pwd-toggle" @click="showPassword = !showPassword" tabindex="-1">
	            <EyeOff v-if="showPassword" class="pwd-eye-icon" size="20" />
	            <Eye v-else class="pwd-eye-icon" size="20" />
	          </button>
	        </div>

	        <!-- 注册模式：确认密码 -->
	        <div v-if="isRegister" class="pwd-input-wrap">
	          <input
	            v-model="confirmPassword"
	            :type="showConfirmPwd ? 'text' : 'password'"
	            placeholder="确认密码"
	            autocomplete="new-password"
	            class="input pwd-input"
	          />
	          <button type="button" class="pwd-toggle" @click="showConfirmPwd = !showConfirmPwd" tabindex="-1">
	            <EyeOff v-if="showConfirmPwd" class="pwd-eye-icon" size="20" />
	            <Eye v-else class="pwd-eye-icon" size="20" />
	          </button>
        </div>

        <!-- 注册模式：图片验证码 -->
        <div v-if="isRegister" class="captcha-row">
          <input
            v-model="captchaCode"
            type="text"
            placeholder="验证码"
            class="input captcha-input"
            maxlength="4"
          />
          <img
            v-if="captchaImage"
            :src="captchaImage"
            class="captcha-img"
            alt="验证码"
            title="点击刷新验证码"
            @click="fetchCaptcha"
          />
          <div v-else class="captcha-placeholder" @click="fetchCaptcha">获取验证码</div>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" class="btn" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? (isRegister ? '注册中...' : '登录中...') : (isRegister ? '注册' : '登录') }}
        </button>
      </form>
      <div class="toggle-mode">
        <a href="#" @click.prevent="toggleMode">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </a>
      </div>
      <p v-if="!isRegister" class="demo-hint">演示账号：admin / admin</p>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { request } from '../api/request'
import { setToken } from '../utils/auth'
import { ArrowLeft, Eye, EyeOff } from '@lucide/vue'

const router = useRouter()
const route = useRoute()
const showToast = inject('showToast', () => {})

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const captchaCode = ref('')
const captchaImage = ref('')
const captchaKey = ref('')
const error = ref('')
const loading = ref(false)
const isRegister = ref(false)
const shaking = ref(false)
const showPassword = ref(false)
const showConfirmPwd = ref(false)

onMounted(() => {
  // 如果 URL 中有 msg 参数（如「请先登录后再访问该页面」），显示提示
  if (route.query.msg) {
    nextTick(() => showToast(route.query.msg, 'info'))
  }
  // 如果是被重定向到登录页（有 returnUrl），不清空演示账号
  if (route.query.returnUrl) return
  username.value = 'admin'
  password.value = 'admin'
})

async function fetchCaptcha() {
  try {
    const { data } = await request.get('/auth/captcha')
    captchaKey.value = data.captchaKey
    captchaImage.value = data.captchaImage
  } catch (e) {
    console.error('Failed to fetch captcha:', e)
  }
}

function toggleMode() {
  isRegister.value = !isRegister.value
  error.value = ''
  confirmPassword.value = ''
  captchaCode.value = ''
  captchaImage.value = ''
  captchaKey.value = ''
  if (isRegister.value) {
    if (username.value === 'admin' && password.value === 'admin') {
      username.value = ''
      password.value = ''
    }
    fetchCaptcha()
  }
}

async function submit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名和密码'
    shaking.value = true
    await nextTick()
    setTimeout(() => { shaking.value = false }, 500)
    return
  }

  // 注册模式校验
  if (isRegister.value) {
    if (!confirmPassword.value) {
      error.value = '请确认密码'
      shaking.value = true
      await nextTick()
      setTimeout(() => { shaking.value = false }, 500)
      return
    }
    if (password.value !== confirmPassword.value) {
      error.value = '两次输入的密码不一致'
      shaking.value = true
      await nextTick()
      setTimeout(() => { shaking.value = false }, 500)
      return
    }
    if (!captchaCode.value) {
      error.value = '请输入验证码'
      shaking.value = true
      await nextTick()
      setTimeout(() => { shaking.value = false }, 500)
      return
    }
  }

  loading.value = true
  try {
    const url = isRegister.value ? '/auth/register' : '/auth/login'
    const body = {
      username: username.value.trim(),
      password: password.value,
    }
    if (isRegister.value) {
      body.captchaKey = captchaKey.value
      body.captchaCode = captchaCode.value
    }
    const { data } = await request.post(url, body)
    setToken(data.token, data.username)
    showToast(isRegister.value ? '注册成功' : '登录成功', 'success')
    const returnUrl = route.query.returnUrl || '/'
    setTimeout(() => router.replace(returnUrl), 600)
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '操作失败，请重试'
    error.value = msg
    showToast(msg, 'error')
    shaking.value = true
    await nextTick()
    setTimeout(() => { shaking.value = false }, 500)
    // 注册失败时刷新验证码
    if (isRegister.value) fetchCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-8px); }
  40% { transform: translateX(8px); }
  60% { transform: translateX(-5px); }
  80% { transform: translateX(5px); }
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.login-page {
  min-height: 100%;
  background: linear-gradient(160deg, #f0f4ff 0%, #fdf2f8 50%, #f0f4ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  position: relative;
  overflow: hidden;
}
.deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.deco svg { width: 100%; height: 100%; }
	.back-link {
	  position: absolute;
	  top: 1.25rem;
	  left: 1.5rem;
	  color: #10B981;
	  text-decoration: none;
	  font-size: 0.85rem;
	  font-weight: 500;
	  display: inline-flex;
	  align-items: center;
	  gap: 4px;
	  padding: 6px 14px;
	  border-radius: 999px;
	  background: rgba(255,255,255,0.6);
	  backdrop-filter: blur(8px);
	  -webkit-backdrop-filter: blur(8px);
	  border: 1px solid rgba(255,255,255,0.4);
	  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
	  z-index: 1;
	}
	.back-link .icon {
	  width: 16px;
	  height: 16px;
	}
	.back-link:hover {
	  background: rgba(255,255,255,0.9);
	  color: #059669;
	  box-shadow: 0 2px 8px rgba(16,185,129,0.1);
	}
.login-card {
	  width: 100%;
	  max-width: 380px;
	  padding: 2rem;
	  border-radius: 20px;
	  background: rgba(255,255,255,0.55);
	  backdrop-filter: blur(20px);
	  -webkit-backdrop-filter: blur(20px);
	  border: 1px solid rgba(255,255,255,0.4);
	  border-bottom: 1px solid rgba(99,102,241,0.06);
	  box-shadow:
	    0 8px 32px rgba(99,102,241,0.08),
	    0 0 0 1px rgba(255,255,255,0.5) inset;
	  position: relative;
	  z-index: 1;
	}
	.login-card h1 {
	  color: #065F46;
	  font-size: 1.5rem;
	  margin-bottom: 0.5rem;
	  text-align: center;
	  font-weight: 700;
	}
.hint {
  color: #64748b;
  font-size: 0.875rem;
  text-align: center;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form.shake {
  animation: shake 0.5s ease;
}
.input {
  padding: 0.75rem 1rem;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #1e1b4b;
  font-size: 1rem;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}
.input::placeholder { color: #94a3b8; }
	.input:focus {
	  outline: none;
	  border-color: #10B981;
	  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
	  transform: scale(1.01);
	}
.error {
  color: #ef4444;
  font-size: 0.875rem;
  margin: -0.25rem 0 0;
  font-weight: 500;
}
	.btn {
	  padding: 0.75rem 1rem;
	  border-radius: 12px;
	  border: none;
	  background: linear-gradient(135deg, #10B981, #059669);
	  color: #fff;
	  font-size: 1rem;
	  font-weight: 600;
	  cursor: pointer;
	  margin-top: 0.5rem;
	  letter-spacing: 0.02em;
	  transition: all 0.2s ease;
	  box-shadow: 0 4px 14px rgba(5,150,105,0.3);
	  display: flex;
	  align-items: center;
	  justify-content: center;
	  gap: 8px;
	}
	.btn:hover:not(:disabled) {
	  opacity: 0.95;
	  box-shadow: 0 6px 24px rgba(5,150,105,0.4);
	  transform: translateY(-1px);
	}
	.btn:active:not(:disabled) {
	  transform: translateY(0);
	  box-shadow: 0 2px 8px rgba(5,150,105,0.3);
	}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  flex-shrink: 0;
}
.demo-hint {
  margin-top: 1.25rem;
  color: #64748b;
  font-size: 0.85rem;
  text-align: center;
}
.toggle-mode {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 0.9rem;
}
	.toggle-mode a {
	  color: #10B981;
	  text-decoration: none;
	  font-weight: 500;
	  transition: color 0.2s;
	}
	.toggle-mode a:hover {
	  color: #059669;
	  text-decoration: underline;
	}

/* 验证码行 */
.captcha-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  width: 100%;
}
.captcha-input {
  flex: 1;
  min-width: 0;
  letter-spacing: 0.15em;
}
.captcha-img {
  width: 90px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
  transition: opacity 0.2s;
}
.captcha-img:hover {
  opacity: 0.8;
}
.captcha-placeholder {
  width: 90px;
  height: 36px;
  border-radius: 8px;
  border: 1px dashed #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  color: #94a3b8;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color 0.2s, color 0.2s;
}
.captcha-placeholder:hover {
	  border-color: #10B981;
	  color: #10B981;
	}

/* 密码输入框显示/隐藏 */
.pwd-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.pwd-input {
  width: 100%;
  padding-right: 2.5rem !important;
}
.pwd-toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
  padding: 0;
}
.pwd-toggle:hover {
	  color: #10B981;
	  background: rgba(16,185,129,0.06);
	}
.pwd-eye-icon {
  width: 20px;
  height: 20px;
}

/* ===== 移动端适配 ===== */
@media (max-width: 480px) {
  .login-page { padding: 1rem; }
  .login-card { padding: 1.5rem 1.25rem; }
  .back-link {
    top: calc(0.75rem + env(safe-area-inset-top));
    left: 0.75rem;
  }
  .input { font-size: 1rem; }
  .captcha-img, .captcha-placeholder { width: 80px; }
}
</style>
