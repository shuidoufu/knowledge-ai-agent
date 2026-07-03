const TOKEN_KEY = 'ai_agent_token'
const USERNAME_KEY = 'ai_agent_username'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token, username) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  if (username != null) localStorage.setItem(USERNAME_KEY, String(username))
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
}

export function getUsername() {
  return localStorage.getItem(USERNAME_KEY) || ''
}

export function isLoggedIn() {
  return !!getToken()
}
