import { ref } from 'vue'

const TOKEN_KEY = 'ai_agent_token'
const USERNAME_KEY = 'ai_agent_username'
const ADMIN_KEY = 'ai_agent_is_admin'

/** Reactive shared state — components watch these instead of reading localStorage directly */
export const token = ref(localStorage.getItem(TOKEN_KEY) || '')
export const username = ref(localStorage.getItem(USERNAME_KEY) || '')
export const isAdmin = ref(localStorage.getItem(ADMIN_KEY) === '1')

export function getToken() {
  return token.value
}

export function setToken(newToken, newUsername, newIsAdmin) {
  if (newToken) localStorage.setItem(TOKEN_KEY, newToken)
  if (newUsername != null) localStorage.setItem(USERNAME_KEY, String(newUsername))
  token.value = newToken || ''
  username.value = newUsername ? String(newUsername) : ''
  setAdmin(newIsAdmin)
}

/** 更新管理员标识（来自登录响应或 /auth/me），未传值时保持原值 */
export function setAdmin(value) {
  if (value === undefined || value === null) return
  const normalized = value === true
  isAdmin.value = normalized
  localStorage.setItem(ADMIN_KEY, normalized ? '1' : '0')
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
  localStorage.removeItem(ADMIN_KEY)
  token.value = ''
  username.value = ''
  isAdmin.value = false
}

export function getUsername() {
  return username.value
}

export function isLoggedIn() {
  return !!token.value
}
