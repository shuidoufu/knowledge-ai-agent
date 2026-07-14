<template>
  <div class="chat-layout">
    <!-- Sidebar -->
    <div class="sidebar" :class="{ 'sidebar-open': isSidebarOpen }">
      <div class="sidebar-header">
        <h3>历史会话</h3>
        <button class="new-chat-btn" @click="createNewChat">
          <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M12 4v16M4 12h16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          新对话
        </button>
      </div>
      <div class="history-list" v-if="historyList.length > 0">
        <div 
          v-for="chat in historyList" 
          :key="chat.chatId"
          class="history-item"
          :class="{ active: chat.chatId === chatId }"
        >
          <div class="history-item-main" @click="loadHistoryChat(chat.chatId)">
            <!-- 编辑标题模式 -->
            <template v-if="editingChatId === chat.chatId">
              <input class="edit-title-input" v-model="editTitleText" @keydown.enter.prevent="saveTitle(chat.chatId)" @keydown.escape.prevent="cancelEdit" @blur="saveTitle(chat.chatId)" @click.stop autofocus />
            </template>
            <template v-else>
              <div class="history-title">{{ chat.title }}</div>
              <div class="history-time">{{ new Date(chat.createdAt).toLocaleString() }}</div>
            </template>
          </div>
          <div class="history-item-actions">
            <button class="history-action-btn edit" @mousedown.prevent.stop="startEditTitle(chat)" title="编辑标题">
              <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button class="history-action-btn delete" @click.stop="confirmDeleteChatId = chat.chatId" title="删除会话">
              <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M3 6h18M8 6V4a1 1 0 011-1h6a1 1 0 011 1v2M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6m5 4v7m4-7v7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
          </div>
        </div>
      </div>
      <div class="history-empty" v-else>
        暂无历史记录
      </div>
    </div>

    <!-- 侧边栏边缘切换按钮 -->
    <button class="toggle-sidebar-btn" :class="{ collapsed: !isSidebarOpen }" @click="toggleSidebar" :title="isSidebarOpen ? '收起历史会话' : '展开历史会话'">
      <svg v-if="isSidebarOpen" viewBox="0 0 24 24" fill="none" class="icon"><path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      <svg v-else viewBox="0 0 24 24" fill="none" class="icon"><path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
    </button>

    <!-- Main Chat Container -->
    <div class="chat-container">
        <div class="header">
          <div class="header-left">
            <button class="back-btn" @click="$router.push('/')">
              <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M19 12H5m7-7l-7 7 7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
              返回
            </button>
            <h2 class="header-title">{{ currentChatTitle }}</h2>
          </div>
        <div class="header-right">
          <span class="chat-id-display">{{ chatId }}</span>
        </div>
      </div>
    <div class="messages" ref="messagesRef">
      <!-- 空状态欢迎语 -->
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-robot">
          <svg viewBox="0 0 48 48" fill="none"><line x1="24" y1="4" x2="24" y2="12" stroke="#6366f1" stroke-width="2.5" stroke-linecap="round"/><circle cx="24" cy="3" r="2.5" fill="#6366f1"/><rect x="10" y="12" width="28" height="20" rx="6" stroke="#6366f1" stroke-width="2" fill="rgba(99,102,241,0.06)"/><circle cx="19" cy="22" r="3" fill="#6366f1"/><circle cx="29" cy="22" r="3" fill="#6366f1"/><line x1="18" y1="28" x2="30" y2="28" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/><rect x="14" y="32" width="20" height="10" rx="3" stroke="#6366f1" stroke-width="1.5" fill="rgba(99,102,241,0.04)"/></svg>
        </div>
        <h3>你好，我是 AI 恋爱大师</h3>
        <p>告诉我你的恋爱困惑，我会给你最贴心的建议 💡</p>
      </div>
      <div
        v-for="(msg, i) in messages"
        :key="i"
        :class="['message-row', msg.role]"
      >
        <div
          class="chat-avatar"
          :class="msg.role"
        >
          <template v-if="msg.role === 'assistant'">
            <div class="robot-avatar">
              <svg viewBox="0 0 48 48" fill="none" class="robot-svg">
                <line x1="24" y1="4" x2="24" y2="12" stroke="url(#robotGrad)" stroke-width="2.5" stroke-linecap="round" class="antenna"/>
                <circle cx="24" cy="3" r="2.5" fill="#6366f1" class="antenna-dot"/>
                <rect x="10" y="12" width="28" height="20" rx="6" stroke="url(#robotGrad)" stroke-width="2" fill="rgba(99,102,241,0.08)"/>
                <circle cx="19" cy="22" r="3.5" fill="#6366f1" class="eye eye-left"/>
                <circle cx="29" cy="22" r="3.5" fill="#6366f1" class="eye eye-right"/>
                <line x1="18" y1="28" x2="30" y2="28" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round" class="mouth"/>
                <rect x="14" y="32" width="20" height="10" rx="3" stroke="url(#robotGrad)" stroke-width="1.5" fill="rgba(99,102,241,0.04)"/>
                <circle cx="24" cy="37" r="1.5" fill="#818cf8" class="body-dot"/>
                <defs>
                  <linearGradient id="robotGrad" x1="0" y1="0" x2="48" y2="48">
                    <stop offset="0%" stop-color="#6366f1"/>
                    <stop offset="100%" stop-color="#818cf8"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
          </template>
            <template v-else><span class="chat-user-letter">{{ userAvatarLetter }}</span></template>
        </div>
        <div class="bubble-content" :class="{ 'markdown-body': msg.role === 'assistant' }">
          <template v-if="msg.role === 'user'">{{ msg.content.trim() }}</template>
          <div v-else>
            <div v-if="loading && i === messages.length - 1" class="streaming-text">{{ msg.content }}</div>
            <div v-else v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>
      </div>
      <!-- 打字指示器 -->
      <div v-if="loading" class="typing-indicator">
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
      </div>
    </div>
    <div class="input-area">
      <textarea
        v-model="inputText"
        placeholder="输入你的心事..."
        rows="2"
        :disabled="loading"
        @keydown.enter.exact.prevent="send"
      />
      <button class="send-btn" :class="{ 'stop-btn': loading }" :disabled="loading ? false : !inputText.trim()" @click="loading ? stopStream() : send()">
        <template v-if="loading">
          <svg viewBox="0 0 24 24" fill="none" class="btn-icon"><rect x="6" y="6" width="12" height="12" rx="2" fill="currentColor"/></svg>
          终止
        </template>
        <template v-else>
          <svg viewBox="0 0 24 24" fill="none" class="btn-icon"><path d="M5 12h14m-7-7l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          发送
        </template>
      </button>
    </div>
    </div>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="confirmDeleteChatId" class="modal-overlay" @click="confirmDeleteChatId = ''">
        <div class="modal-content" @click.stop>
          <div class="modal-icon">
            <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M3 6h18M8 6V4a1 1 0 011-1h6a1 1 0 011 1v2M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6m5 4v7m4-7v7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="modal-title">删除会话</div>
          <div class="modal-desc">确定要删除这个会话吗？删除后无法恢复。</div>
          <div class="modal-actions">
            <button class="modal-btn cancel" @click="confirmDeleteChatId = ''">取消</button>
            <button class="modal-btn confirm" @click="doDeleteChat(confirmDeleteChatId)">确认删除</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, inject } from 'vue'
import { streamLoveChat, request, updateChatTitle, deleteChat } from '../api/request'
import { username as reactiveUsername } from '../utils/auth'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const chatId = ref('')
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref(null)
const abortController = ref(null)

// Sidebar state from App.vue
const isSidebarOpen = inject('isSidebarOpen', ref(true))
const setSidebarOpen = inject('setSidebarOpen', (v) => {})

// History state
const historyList = ref([])

// 编辑标题状态
const editingChatId = ref('')
const editTitleText = ref('')
// 删除确认状态
const confirmDeleteChatId = ref('')

// 当前会话标题
const currentChatTitle = computed(() => {
  if (!chatId.value) return 'AI 恋爱大师'
  const found = historyList.value.find(c => c.chatId === chatId.value)
  return found ? found.title : 'AI 恋爱大师'
})

const userAvatarLetter = computed(() => {
  const name = reactiveUsername.value
  return name ? name.trim().charAt(0).toUpperCase() : '?'
})
const userAvatarColor = computed(() => {
  const name = reactiveUsername.value
  if (!name) return '#64748b'
  let n = 0
  for (let i = 0; i < name.length; i++) n += name.charCodeAt(i)
  const hues = ['#6366f1', '#8b5cf6', '#ec4899', '#ef4444', '#f97316', '#22c56e', '#14b8a6', '#3b82f6']
  return hues[n % hues.length]
})

function generateChatId() {
  const name = reactiveUsername.value || 'anonymous'
  return `love_${name}_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

async function fetchHistoryList() {
  try {
    const res = await request.get('/ai/love_app/chat/history')
    historyList.value = res.data
  } catch (error) {
    console.error('Failed to load history list:', error)
  }
}

async function loadHistoryChat(loadChatId) {
  if (chatId.value === loadChatId) return
  if (window.innerWidth <= 768) {
    isSidebarOpen.value = false
  }
  try {
    const res = await request.get(`/ai/love_app/chat/history/${loadChatId}`)
    chatId.value = loadChatId
    messages.value = res.data.map(m => ({
      role: m.role.toLowerCase(),
      content: m.content
    }))
    scrollToBottom()
  } catch (error) {
    console.error('Failed to load history chat:', error)
  }
}

function createNewChat() {
  chatId.value = generateChatId()
  messages.value = []
  if (window.innerWidth <= 768) {
    isSidebarOpen.value = false
  }
}

// 侧边栏切换（同步到 App.vue）
function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value
}

// 编辑标题
function startEditTitle(chat) {
  // 如果已在编辑此会话：保存并退出编辑模式
  if (editingChatId.value === chat.chatId) {
    saveTitleImmediate(chat.chatId)
    return
  }
  // 如果正在编辑另一个会话，先保存
  if (editingChatId.value) {
    saveTitleImmediate(editingChatId.value)
  }
  // 进入编辑模式
  editingChatId.value = chat.chatId
  editTitleText.value = chat.title
}
function cancelEdit() {
  editingChatId.value = ''
  editTitleText.value = ''
}
async function saveTitle(chatIdToSave) {
  // 如果已经退出编辑模式（例如由 mousedown 处理），跳过 blur 回调
  if (editingChatId.value !== chatIdToSave) return
  const title = editTitleText.value.trim()
  if (!title) {
    editingChatId.value = ''
    return
  }
  // If title hasn't changed, just close edit
  const original = historyList.value.find(c => c.chatId === chatIdToSave)?.title
  if (title === original) {
    editingChatId.value = ''
    return
  }
  await saveTitleImmediate(chatIdToSave)
}
async function saveTitleImmediate(chatIdToSave) {
  const title = editTitleText.value.trim()
  if (!title) {
    editingChatId.value = ''
    return
  }
  try {
    await updateChatTitle(chatIdToSave, title)
    editingChatId.value = ''
    fetchHistoryList()
  } catch (error) {
    console.error('Failed to update title:', error)
    editingChatId.value = ''
  }
}

// 删除会话
async function doDeleteChat(deleteId) {
  try {
    await deleteChat(deleteId)
    confirmDeleteChatId.value = ''
    if (deleteId === chatId.value) {
      createNewChat()
    }
    fetchHistoryList()
  } catch (error) {
    console.error('Failed to delete chat:', error)
    confirmDeleteChatId.value = ''
  }
}

onMounted(() => {
  chatId.value = generateChatId()
  fetchHistoryList()
  
  if (window.innerWidth <= 768) {
    isSidebarOpen.value = false
  }
})

/** AI 回复：渲染为安全的 Markdown HTML */
function renderMarkdown(content) {
  if (!content) return ''
  const renderer = new marked.Renderer()
  renderer.link = ({ href, title, text }) => `<a target="_blank" href="${href}" title="${title || ''}">${text}</a>`
  renderer.heading = ({ depth, text }) => `<h${depth}>${text}</h${depth}>`
  const rawHtml = marked.parse(content, { renderer, gfm: true })
  return DOMPurify.sanitize(rawHtml)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

function send() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()

  const aiIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  loading.value = true

  // 创建 AbortController 用于终止请求
  const controller = new AbortController()
  abortController.value = controller

  streamLoveChat(text, chatId.value, {
    onChunk(chunk) {
      if (chunk) {
        messages.value[aiIndex].content += chunk
      }
      scrollToBottom()
    },
    onDone() {
      loading.value = false
      abortController.value = null
      scrollToBottom()
      fetchHistoryList()
    },
    onError(err) {
      loading.value = false
      abortController.value = null
      if (err?.name === 'AbortError') return
      messages.value[aiIndex].content = '回复失败：' + (err?.message || '网络错误')
      scrollToBottom()
      fetchHistoryList()
    },
  }, controller.signal)
}

/** 终止 AI 回复 */
function stopStream() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
    loading.value = false
  }
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100%;
  width: 100%;
  background: #f5f7fb;
  overflow: hidden;
  position: relative;
}

/* Sidebar Styles */
.sidebar {
  width: 260px;
  min-width: 260px;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-right: 1px solid rgba(255,255,255,0.3);
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s ease;
  z-index: 10;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
}
.sidebar-open {
  margin-left: 0;
}
.sidebar:not(.sidebar-open) {
  margin-left: -260px;
  border-right: none;
}

.sidebar-header {
  padding: 20px 20px 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  gap: 8px;
}
.sidebar-header h3 {
  margin: 0;
  color: #1e293b;
  font-size: 1.1rem;
  white-space: nowrap;
}
.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #ec4899, #db2777);
  color: #fff;
  border: none;
  padding: 7px 14px;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(236,72,153,0.3);
}
.new-chat-btn:hover {
  opacity: 0.95;
  box-shadow: 0 6px 20px rgba(236,72,153,0.4);
  transform: translateY(-1px);
}
.new-chat-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(236,72,153,0.3);
}
.new-chat-btn .icon {
  width: 16px;
  height: 16px;
}

/* 侧边栏边缘切换按钮 */
.toggle-sidebar-btn {
  position: absolute;
  top: 50%;
  left: 260px;
  transform: translateY(-50%) translateX(-50%);
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 52px;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 0 10px 10px 0;
  color: #6366f1;
  cursor: pointer;
  box-shadow: 2px 0 8px rgba(99,102,241,0.08);
  transition: color 0.2s, box-shadow 0.2s, left 0.3s ease;
}
.toggle-sidebar-btn:hover {
  color: #4f46e5;
  box-shadow: 2px 0 16px rgba(99,102,241,0.15);
}
.toggle-sidebar-btn .icon {
  width: 18px;
  height: 18px;
}
.toggle-sidebar-btn.collapsed {
  left: 0;
  transform: translateY(-50%) translateX(0);
  border-radius: 0 8px 8px 0;
  border-left: none;
}

.history-list {
  flex-grow: 1;
  overflow-y: scroll;
  overflow-x: hidden;
  padding: 10px 10px 10px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.history-list::-webkit-scrollbar {
  width: 5px;
}
.history-list::-webkit-scrollbar-track {
  background: transparent;
}
.history-list::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 3px;
}
.history-item {
  padding: 0;
  border-radius: 10px;
  background: transparent;
  transition: background 0.2s;
  box-sizing: border-box;
  position: relative;
}
.history-item:hover {
  background: #f8fafc;
}
.history-item.active {
  background: rgba(236, 72, 153, 0.06);
}
.history-item-main {
  cursor: pointer;
  padding: 12px 72px 12px 16px;
}
.history-title {
  color: #1e293b;
  font-size: 0.95rem;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.history-time {
  color: #94a3b8;
  font-size: 0.8rem;
}
.history-empty {
  padding: 30px;
  text-align: center;
  color: #94a3b8;
  font-size: 0.9rem;
}

/* 历史项操作按钮 */
.history-item-actions {
  display: none;
  position: absolute;
  right: 8px;
  top: 10px;
  gap: 6px;
}
.history-item:hover .history-item-actions {
  display: flex;
}
.history-action-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: rgba(99,102,241,0.1);
  color: #6366f1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s;
}
.history-action-btn .icon {
  width: 16px;
  height: 16px;
}
.history-action-btn:hover {
  background: rgba(99,102,241,0.2);
  color: #4f46e5;
}
.history-action-btn.delete:hover {
  background: rgba(239,68,68,0.15);
  color: #ef4444;
}

/* 编辑标题行 */
.edit-title-input {
  width: 100%;
  padding: 4px 8px;
  border: 1px solid #6366f1;
  border-radius: 6px;
  font-size: 0.85rem;
  background: #ffffff;
  color: #1e1b4b;
  outline: none;
}

/* 头像多彩动效 */
@keyframes hueCycle {
  from { filter: hue-rotate(0deg); }
  to { filter: hue-rotate(360deg); }
}
.chat-avatar.user {
  background: transparent;
}
.chat-user-letter {
  font-family: 'Space Grotesk', 'Plus Jakarta Sans', sans-serif;
  font-size: 1rem;
  font-weight: 700;
  color: #6366f1;
  text-shadow: 0 0 8px rgba(99,102,241,0.2);
}

/* AI 机器人头像 */
.robot-avatar {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.robot-svg {
  width: 40px;
  height: 40px;
  overflow: visible;
}
.antenna-dot {
  animation: dotPulse 2s ease-in-out infinite;
}
@keyframes dotPulse {
  0%, 100% { opacity: 1; r: 2.5; }
  50% { opacity: 0.4; r: 2; }
}
.eye {
  animation: eyeBlink 4s ease-in-out infinite;
}
.eye-left { animation-delay: 0s; }
.eye-right { animation-delay: 0.08s; }
@keyframes eyeBlink {
  0%, 96%, 100% { opacity: 1; ry: 3.5; }
  98% { opacity: 0.3; ry: 0.5; }
}
.mouth {
  animation: mouthMove 3s ease-in-out infinite;
}
@keyframes mouthMove {
  0%, 100% { x1: 18; x2: 30; }
  50% { x1: 20; x2: 28; }
}
.body-dot {
  animation: bodyBreathe 3s ease-in-out infinite;
}
@keyframes bodyBreathe {
  0%, 100% { opacity: 0.6; r: 1.5; }
  50% { opacity: 1; r: 2; }
}
.chat-avatar.assistant {
  animation: float 4s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

/* 删除确认弹窗 - 玻璃模态 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(99,102,241,0.08);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.modal-content {
  width: 340px;
  padding: 2rem;
  border-radius: 20px;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.3);
  box-shadow: 0 16px 48px rgba(99,102,241,0.12);
  text-align: center;
  animation: modalSlideUp 0.25s ease;
}
@keyframes modalSlideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
.modal-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 1rem;
  background: rgba(239,68,68,0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ef4444;
}
.modal-icon .icon {
  width: 24px;
  height: 24px;
}
.modal-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e1b4b;
  margin-bottom: 0.5rem;
}
.modal-desc {
  font-size: 0.9rem;
  color: #64748b;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}
.modal-actions {
  display: flex;
  gap: 0.75rem;
}
.modal-btn {
  flex: 1;
  padding: 0.6rem;
  border-radius: 10px;
  border: none;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}
.modal-btn.cancel {
  background: #f1f5f9;
  color: #64748b;
}
.modal-btn.cancel:hover {
  background: #e2e8f0;
  color: #1e293b;
}
.modal-btn.confirm {
  background: #ef4444;
  color: #fff;
  box-shadow: 0 4px 12px rgba(239,68,68,0.3);
}
.modal-btn.confirm:hover {
  background: #dc2626;
  box-shadow: 0 6px 20px rgba(239,68,68,0.4);
  transform: translateY(-1px);
}

/* Main Chat Container Styles */
.chat-container {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  background: #f5f7fb;
  min-height: 0;
}

.header {
  flex-shrink: 0;
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255,255,255,0.3);
  box-shadow: 0 1px 4px rgba(99,102,241,0.06);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex-shrink: 1;
}
.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(99,102,241,0.08);
  border: 1px solid rgba(255,255,255,0.3);
  color: #6366f1;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
}
.back-btn .icon {
  width: 16px;
  height: 16px;
}
.back-btn:hover {
  background: rgba(99,102,241,0.15);
  color: #4f46e5;
  box-shadow: 0 2px 8px rgba(99,102,241,0.1);
}
.header-title {
  font-size: 1.1rem;
  margin: 0;
  color: #1e293b;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.chat-id-display {
  font-size: 0.75rem;
  color: #94a3b8;
  display: none;
}
.messages {
  flex: 1;
  overflow-y: scroll;
  overflow-x: hidden;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.messages::-webkit-scrollbar {
  width: 5px;
}
.messages::-webkit-scrollbar-track {
  background: transparent;
}
.messages::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 3px;
}

/* 消息入场动效 */
@keyframes messageIn {
  from { opacity: 0; transform: translateY(12px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}
.message-row {
  animation: messageIn 0.3s ease both;
}

/* 空状态欢迎语 */
.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 2rem;
  color: #64748b;
}
.welcome-robot {
  width: 64px;
  height: 64px;
  margin-bottom: 1.25rem;
  opacity: 0.6;
}
.welcome-robot svg { width: 100%; height: 100%; }
.welcome h3 {
  font-size: 1.15rem;
  color: #1e1b4b;
  margin-bottom: 0.5rem;
  font-weight: 600;
}
.welcome p {
  font-size: 0.9rem;
  line-height: 1.6;
  max-width: 280px;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0.5rem 0;
  margin-left: 3rem;
}
.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c7d2fe;
  animation: typingBounce 1.4s ease-in-out infinite both;
}
.typing-dot:nth-child(1) { animation-delay: 0s; }
.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes typingBounce {
  0%, 80%, 100% { transform: translateY(0); background: #c7d2fe; }
  40% { transform: translateY(-8px); background: #6366f1; }
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  max-width: 92%;
}
.message-row.assistant {
  align-self: flex-start;
}
.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.chat-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  font-weight: 600;
  color: #fff;
  background: transparent;
  margin-top: 10px;
}
.bubble-content {
  padding: 1rem 1.5rem;
  border-radius: 14px;
  white-space: normal;
  word-break: break-word;
  text-align: left;
  line-height: 1.8;
}
.streaming-text {
  white-space: pre-wrap;
  line-height: 1.8;
}
.message-row.user .bubble-content {
  width: fit-content;
  max-width: min(85%, 65ch);
  background: linear-gradient(135deg, #ec4899, #db2777);
  color: #fff;
}
.message-row.assistant .bubble-content {
  min-width: 12em;
  max-width: min(85%, 65ch);
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #1e293b;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
/* Enhanced Markdown styles for light theme */
.markdown-body :deep(p) {
  margin-bottom: 0.9em;
  line-height: 1.8;
}
.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}
.markdown-body :deep(ol) {
  margin-bottom: 0.8em;
  padding-left: 1.8em;
  list-style: none;
  counter-reset: li-counter;
}
.markdown-body :deep(ol > li) {
  counter-increment: li-counter;
  position: relative;
  padding-left: 0.5em;
  margin-bottom: 0.6em;
  line-height: 1.7;
}
.markdown-body :deep(ol > li::before) {
  content: counter(li-counter) ".";
  position: absolute;
  left: -1.8em;
  width: 1.5em;
  text-align: right;
  color: #6366f1;
  font-weight: 600;
}
.markdown-body :deep(ul) {
  margin-bottom: 0.8em;
  padding-left: 1.5em;
}
.markdown-body :deep(li) {
  margin-bottom: 0.5em;
  line-height: 1.7;
}
.markdown-body :deep(ul > li) {
  list-style: none;
  position: relative;
  padding-left: 0.5em;
}
.markdown-body :deep(ul > li::before) {
  content: '';
  position: absolute;
  left: -1.2em;
  top: 0.65em;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a5b4fc;
}
.markdown-body :deep(ul ul), .markdown-body :deep(ol ul),
.markdown-body :deep(ul ol), .markdown-body :deep(ol ol) {
  margin-top: 0.4em;
  margin-bottom: 0.4em;
}
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3),
.markdown-body :deep(h4), .markdown-body :deep(h5), .markdown-body :deep(h6) {
  margin-top: 1.4em;
  margin-bottom: 0.6em;
  font-weight: 600;
  line-height: 1.4;
  color: #0f172a;
}
.markdown-body :deep(h3) {
  padding-left: 12px;
  border-left: 3px solid #6366f1;
}
.markdown-body :deep(h4) {
  color: #4f46e5;
  font-size: 1em;
}
.markdown-body :deep(h1:first-child), .markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child) {
  margin-top: 0;
}
.markdown-body :deep(strong) {
  color: #0f172a;
  font-weight: 600;
}
.markdown-body :deep(blockquote) {
  margin: 1em 0;
  padding: 0.8em 1.2em;
  border-left: 3px solid #a5b4fc;
  background: rgba(99,102,241,0.04);
  border-radius: 0 8px 8px 0;
  color: #475569;
  line-height: 1.7;
}
.markdown-body :deep(blockquote p) {
  margin-bottom: 0.4em;
}
.markdown-body :deep(blockquote p:last-child) {
  margin-bottom: 0;
}
.markdown-body :deep(hr) {
  margin: 1.2em 0;
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, #e0e7ff, transparent);
}
.markdown-body :deep(code) {
  background: #f1f5f9;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Cascadia Code', 'Fira Code', monospace;
  font-size: 0.9em;
  color: #ec4899;
}
.markdown-body :deep(pre) {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 1em;
  border-radius: 10px;
  overflow-x: auto;
  margin-bottom: 0.8em;
}
.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  color: #1e293b;
}
.markdown-body :deep(a) {
  color: #ec4899;
  text-decoration: underline;
}
.input-area {
  flex-shrink: 0;
  padding: 1rem;
  border-top: 1px solid rgba(255,255,255,0.3);
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: sticky;
  bottom: 0;
  z-index: 10;
}
.input-area textarea {
  flex: 1;
  min-height: 44px;
  max-height: 120px;
  padding: 0.6rem 0.75rem;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #1e293b;
  resize: none;
  font-size: 0.95rem;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input-area textarea:focus {
  outline: none;
  border-color: #ec4899;
  box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.1);
}
.send-btn {
  background: linear-gradient(135deg, #ec4899, #db2777);
  border: none;
  color: #fff;
  padding: 0 28px;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 600;
  height: 44px;
  letter-spacing: 0.02em;
  transition: all 0.2s ease;
  box-shadow: 0 4px 14px rgba(236,72,153,0.3);
}
.send-btn:hover:not(:disabled) {
  opacity: 0.95;
  box-shadow: 0 6px 24px rgba(236,72,153,0.4);
  transform: translateY(-1px);
}
.send-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(236,72,153,0.3);
}
.send-btn:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
  opacity: 0.7;
  box-shadow: none;
}
.send-btn.stop-btn {
  background: #ef4444;
  box-shadow: 0 4px 14px rgba(239,68,68,0.3);
  display: flex;
  align-items: center;
  gap: 6px;
}
.send-btn.stop-btn:hover {
  background: #dc2626;
  box-shadow: 0 6px 24px rgba(239,68,68,0.4);
  transform: translateY(-1px);
}
.btn-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 260px;
    margin-left: 0;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    border-right: 1px solid #e2e8f0;
    box-shadow: none;
    z-index: 20;
  }
  .sidebar.sidebar-open {
    transform: translateX(0);
    width: 260px;
    box-shadow: 5px 0 30px rgba(0,0,0,0.1);
  }
  .sidebar:not(.sidebar-open) {
    margin-left: 0;
    border-right: none;
  }
  .toggle-sidebar-btn {
    display: none;
  }
  .chat-id-display {
    display: none;
  }
}
</style>
