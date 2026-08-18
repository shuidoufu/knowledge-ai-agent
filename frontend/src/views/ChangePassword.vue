<template>
  <div class="change-password-page">
    <router-link to="/" class="back-link">
	      <ArrowLeft class="icon" size="16" />
	      返回应用中心
	    </router-link>
    <div class="card">
      <h1>修改密码</h1>
      <p class="hint">为了保证安全，修改密码后会自动登出，请使用新密码重新登录。</p>
      
      <form @submit.prevent="submit" class="form">
        <div class="form-group">
          <label for="oldPassword">原密码</label>
          <div class="pwd-input-wrap">
            <input
              id="oldPassword"
              v-model="oldPassword"
              :type="showOldPwd ? 'text' : 'password'"
              placeholder="请输入当前密码"
              class="input pwd-input"
            />
            <button type="button" class="pwd-toggle" @click="showOldPwd = !showOldPwd" tabindex="-1">
	              <EyeOff v-if="showOldPwd" class="pwd-eye-icon" size="20" />
	              <Eye v-else class="pwd-eye-icon" size="20" />
	            </button>
	          </div>
	        </div>
	        <div class="form-group">
	          <label for="newPassword">新密码</label>
	          <div class="pwd-input-wrap">
	            <input
	              id="newPassword"
	              v-model="newPassword"
	              :type="showNewPwd ? 'text' : 'password'"
	              placeholder="请输入新密码"
	              class="input pwd-input"
	            />
	            <button type="button" class="pwd-toggle" @click="showNewPwd = !showNewPwd" tabindex="-1">
	              <EyeOff v-if="showNewPwd" class="pwd-eye-icon" size="20" />
	              <Eye v-else class="pwd-eye-icon" size="20" />
	            </button>
	          </div>
	        </div>
	        <div class="form-group">
	          <label for="confirmPassword">确认新密码</label>
	          <div class="pwd-input-wrap">
	            <input
	              id="confirmPassword"
	              v-model="confirmPassword"
	              :type="showConfirmPwd ? 'text' : 'password'"
	              placeholder="请再次输入新密码"
	              class="input pwd-input"
	            />
	            <button type="button" class="pwd-toggle" @click="showConfirmPwd = !showConfirmPwd" tabindex="-1">
	              <EyeOff v-if="showConfirmPwd" class="pwd-eye-icon" size="20" />
	              <Eye v-else class="pwd-eye-icon" size="20" />
	            </button>
          </div>
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
import { ArrowLeft, Eye, EyeOff } from '@lucide/vue'

const router = useRouter()
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const successMsg = ref('')
const loading = ref(false)
const showOldPwd = ref(false)
const showNewPwd = ref(false)
const showConfirmPwd = ref(false)

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
.card {
	  width: 100%;
	  max-width: 400px;
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
	}
	.card h1 {
	  font-size: 1.5rem;
	  margin-bottom: 0.5rem;
	  text-align: center;
	  color: #065F46;
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
	  border-color: #10B981;
	  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
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
	  background: linear-gradient(135deg, #10B981, #059669);
	  color: #fff;
	  font-size: 1rem;
	  font-weight: 600;
	  cursor: pointer;
	  margin-top: 0.5rem;
	  letter-spacing: 0.02em;
	  transition: all 0.2s ease;
	  box-shadow: 0 4px 14px rgba(5,150,105,0.3);
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
  .change-password-page { padding: 1rem; }
  .card { padding: 1.5rem 1.25rem; }
  .back-link {
    top: calc(0.75rem + env(safe-area-inset-top));
    left: 0.75rem;
  }
  .input { font-size: 1rem; }
}
</style>
