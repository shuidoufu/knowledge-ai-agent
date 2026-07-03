<template>
  <div class="change-password-page">
    <router-link to="/" class="back-link">← 返回应用中心</router-link>
    <div class="card">
      <h1>修改密码</h1>
      <p class="hint">为了保证安全，修改密码后会自动登出，请使用新密码重新登录。</p>
      
      <form @submit.prevent="submit" class="form">
        <div class="form-group">
          <label for="oldPassword">原密码</label>
          <input
            id="oldPassword"
            v-model="oldPassword"
            type="password"
            placeholder="请输入当前密码"
            class="input"
          />
        </div>
        <div class="form-group">
          <label for="newPassword">新密码</label>
          <input
            id="newPassword"
            v-model="newPassword"
            type="password"
            placeholder="请输入新密码"
            class="input"
          />
        </div>
        <div class="form-group">
          <label for="confirmPassword">确认新密码</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            class="input"
          />
        </div>
        
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="successMsg" class="success">{{ successMsg }}</p>

        <button type="submit" class="btn" :disabled="loading">
          {{ loading ? '提交中...' : '确认修改' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../api/request'
import { removeToken } from '../utils/auth'

const router = useRouter()
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const successMsg = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  successMsg.value = ''

  if (!oldPassword.value) {
    error.value = '请输入原密码'
    return
  }
  if (!newPassword.value || newPassword.value.length < 5) {
    error.value = '新密码不能少于 5 位'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的新密码不一致'
    return
  }
  if (oldPassword.value === newPassword.value) {
    error.value = '新密码不能与原密码相同'
    return
  }

  loading.value = true
  try {
    const { data } = await request.post('/auth/change-password', {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    })
    
    successMsg.value = data.message || '密码修改成功，即将跳转重新登录...'
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    
    // 登出当前用户
    setTimeout(() => {
      removeToken()
      // 清除服务端的token态(可选)
      request.post('/auth/logout').catch(() => {})
      router.push('/login')
    }, 1500)
    
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '修改失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.change-password-page {
  min-height: 100%;
  background: linear-gradient(160deg, #f0f4ff 0%, #fdf2f8 50%, #f0f4ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  position: relative;
  color: #1e293b;
}
.back-link {
  position: absolute;
  top: 1.5rem;
  left: 2rem;
  color: #64748b;
  text-decoration: none;
  font-size: 0.95rem;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  transition: color 0.2s;
}
.back-link:hover {
  color: #3b82f6;
}
.card {
  width: 100%;
  max-width: 400px;
  padding: 2rem;
  border-radius: 20px;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.3);
  box-shadow: 0 8px 32px rgba(99,102,241,0.08);
}
.card h1 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  text-align: center;
  color: #1e293b;
}
.hint {
  color: #64748b;
  font-size: 0.875rem;
  text-align: center;
  margin-bottom: 1.5rem;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.form-group label {
  font-size: 0.9rem;
  color: #475569;
  font-weight: 500;
}
.input {
  padding: 0.75rem 1rem;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #1e293b;
  font-size: 1rem;
  width: 100%;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input::placeholder {
  color: #94a3b8;
}
.input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}
.error {
  color: #ef4444;
  font-size: 0.875rem;
  margin-top: -0.5rem;
}
.success {
  color: #10b981;
  font-size: 0.875rem;
  margin-top: -0.5rem;
  text-align: center;
}
.btn {
  padding: 0.75rem 1rem;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  margin-top: 0.5rem;
  letter-spacing: 0.02em;
  transition: all 0.2s ease;
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
}
.btn:hover:not(:disabled) {
  opacity: 0.95;
  box-shadow: 0 6px 24px rgba(99,102,241,0.4);
  transform: translateY(-1px);
}
.btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
