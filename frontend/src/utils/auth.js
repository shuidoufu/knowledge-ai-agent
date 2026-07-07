import { ref } from 'vue'

const TOKEN_KEY = 'ai_agent_token'
const USERNAME_KEY = 'ai_agent_username'

/** Reactive shared state — components watch these instead of reading localStorage directly */
export const token = ref(localStorage.getItem(TOKEN_KEY) || '')
export const username = ref(localStorage.getItem(USERNAME_KEY) || '')

export function getToken() {
  return token.value
}

export function setToken(newToken, newUsername) {
  if (newToken) localStorage.setItem(TOKEN_KEY, newToken)
  if (newUsername != null) localStorage.setItem(USERNAME_KEY, String(newUsername))
  token.value = newToken || ''
  username.value = newUsername ? String(newUsername) : ''
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
  token.value = ''
  username.value = ''
}

export function getUsername() {
  return username.value
}

export function isLoggedIn() {
  return !!token.value
}
