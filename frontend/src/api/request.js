import axios from 'axios'
import { getToken, removeToken } from '../utils/auth'

// const BASE_URL = import.meta.env.DEV ? '' : 'http://localhost:8123'
const BASE_URL = import.meta.env.DEV ? '' : 'http://192.168.198.100' // 用于服务器/虚拟机

export const request = axios.create({
  baseURL: BASE_URL + '/api',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求头带上 token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

// 401 未登录/过期：清除 token 并跳转登录页，并带上 returnUrl
request.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      removeToken()
      const returnUrl = encodeURIComponent(window.location.pathname + window.location.search || '/')
      window.location.href = '/login?returnUrl=' + returnUrl
    }
    return Promise.reject(err)
  }
)

/**
 * 纯文本流式聊天 - 恋爱大师
 * 使用 /chat/stream 端点，无 SSE 包装，兼容换行符
 */
export function streamLoveChat(message, chatId, { onChunk, onDone, onError }) {
  const url = new URL(BASE_URL + '/api/ai/love_app/chat/stream', window.location.origin)
  url.searchParams.set('message', message)
  url.searchParams.set('chatId', chatId)

  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token

  fetch(url.toString(), { method: 'GET', headers })
    .then((res) => {
      if (res.status === 401) {
        removeToken()
        const returnUrl = encodeURIComponent(window.location.pathname + window.location.search || '/')
        window.location.href = '/login?returnUrl=' + returnUrl
        return
      }
      if (!res.ok) throw new Error(res.statusText)
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            onDone?.()
            return
          }
          const raw = decoder.decode(value, { stream: true })
          if (raw) onChunk?.(raw)
          read()
        }).catch(onError)
      }
      read()
    })
    .catch(onError)
}

/**
 * SSE 流式请求 - 超级智能体
 */
export function streamManusChat(message, { onChunk, onDone, onError }) {
  const url = new URL(BASE_URL + '/api/ai/manus/chat', window.location.origin)
  url.searchParams.set('message', message)

  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = 'Bearer ' + token

  fetch(url.toString(), { method: 'GET', headers })
    .then((res) => {
      if (res.status === 401) {
        removeToken()
        const returnUrl = encodeURIComponent(window.location.pathname + window.location.search || '/')
        window.location.href = '/login?returnUrl=' + returnUrl
        return
      }
      if (!res.ok) throw new Error(res.statusText)
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            onDone?.()
            return
          }
          const raw = decoder.decode(value, { stream: true })
          if (raw) onChunk?.(raw)
          read()
        }).catch(onError)
      }
      read()
    })
    .catch(onError)
}

/**
 * 更新会话标题
 */
export function updateChatTitle(chatId, title) {
  return request.put(`/ai/love_app/chat/history/${chatId}/title`, { title })
}

/**
 * 删除会话
 */
export function deleteChat(chatId) {
  return request.delete(`/ai/love_app/chat/history/${chatId}`)
}
