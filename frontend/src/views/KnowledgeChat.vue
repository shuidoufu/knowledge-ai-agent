<template>
  <div class="chat-layout">
    <!-- Sidebar -->
    <div class="sidebar" :class="{ 'sidebar-open': isSidebarOpen }">
      <div class="sidebar-header">
        <h3>历史会话</h3>
        <button class="new-chat-btn" @click="createNewChat">
	          <Plus class="icon" size="16" />
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
              <div class="history-time">{{ new Date(chat.updatedAt || chat.createdAt).toLocaleString() }}</div>
            </template>
          </div>
          <div class="history-item-actions">
            <button class="history-action-btn edit" @mousedown.prevent.stop="startEditTitle(chat)" title="编辑标题">
	              <Pencil class="icon" size="16" />
	            </button>
	            <button class="history-action-btn delete" @click.stop="confirmDeleteChatId = chat.chatId" title="删除会话">
	              <Trash2 class="icon" size="16" />
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
	      <ChevronLeft v-if="isSidebarOpen" class="icon" size="18" />
	      <ChevronRight v-else class="icon" size="18" />
	    </button>

    <!-- 移动端抽屉遮罩 -->
    <div v-if="isMobile && isSidebarOpen" class="sidebar-backdrop" @click="closeSidebar"></div>

    <!-- Main Chat Container -->
    <div class="chat-container">
        <div class="header">
          <div class="header-left">
            <button class="sidebar-menu-btn" @click="toggleSidebar" title="历史会话" aria-label="历史会话">
	              <Menu class="icon" size="18" />
	            </button>
            <button class="back-btn" @click="$router.push('/')">
	              <ArrowLeft class="icon" size="16" />
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
	        <div class="welcome-icon-wrap">
	          <svg viewBox="0 0 48 48" fill="none" class="welcome-icon">
	            <path d="M8 8h20v24H8z" stroke="#10B981" stroke-width="1.8" fill="rgba(16,185,129,0.06)"/>
	            <path d="M12 14h12M12 18h8M12 22h4" stroke="#10B981" stroke-width="1.8" stroke-linecap="round"/>
	            <path d="M28 12l6 2v24l-6-2V12z" stroke="#10B981" stroke-width="1.8" fill="rgba(16,185,129,0.06)"/>
	            <circle cx="28" cy="30" r="2" fill="#F59E0B" opacity="0.7"/>
	          </svg>
	        </div>
	        <h3>你的知识库已就绪</h3>
	        <p class="welcome-desc">基于你的笔记文档与网页收藏，AI 帮你回忆、理解和思考</p>
	        <div class="welcome-tips">
	          <span class="tip-item">
	            <svg viewBox="0 0 24 24" fill="none" class="tip-icon"><path d="M12 4v16M4 12h16" stroke="#F59E0B" stroke-width="2" stroke-linecap="round"/></svg>
	            新建会话
	          </span>
	          <span class="tip-item">
	            <svg viewBox="0 0 24 24" fill="none" class="tip-icon"><circle cx="11" cy="11" r="6" stroke="#10B981" stroke-width="2"/><path d="M20 20l-4.3-4.3" stroke="#10B981" stroke-width="2" stroke-linecap="round"/></svg>
	            检索知识库
	          </span>
	          <span class="tip-item">
	            <svg viewBox="0 0 24 24" fill="none" class="tip-icon"><path d="M21 12a9 9 0 11-9-9" stroke="#10B981" stroke-width="2" stroke-linecap="round"/><path d="M12 6v6l3 3" stroke="#10B981" stroke-width="2" stroke-linecap="round"/></svg>
	            快速问答
	          </span>
	        </div>
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
		                <circle cx="24" cy="3" r="2.5" fill="#93C5FD" class="antenna-dot"/>
		                <rect x="10" y="12" width="28" height="20" rx="6" stroke="url(#robotGrad)" stroke-width="2" fill="rgba(147,197,253,0.1)"/>
		                <circle cx="19" cy="22" r="3.5" fill="#93C5FD" class="eye eye-left"/>
		                <circle cx="29" cy="22" r="3.5" fill="#93C5FD" class="eye eye-right"/>
		                <line x1="18" y1="28" x2="30" y2="28" stroke="#BFDBFE" stroke-width="1.8" stroke-linecap="round" class="mouth"/>
		                <rect x="14" y="32" width="20" height="10" rx="3" stroke="url(#robotGrad)" stroke-width="1.5" fill="rgba(147,197,253,0.06)"/>
		                <circle cx="24" cy="37" r="1.5" fill="#BFDBFE" class="body-dot"/>
		                <defs>
		                  <linearGradient id="robotGrad" x1="0" y1="0" x2="48" y2="48">
		                    <stop offset="0%" stop-color="#93C5FD"/>
		                    <stop offset="100%" stop-color="#DBEAFE"/>
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
            <!-- 播报按钮 + 知识库引用（分割线下方区域） -->
            <div class="speech-refs-area">
              <!-- 语音播报按钮（当前隐藏） -->
              <button
                v-if="false"
                class="speech-btn"
                :class="{ speaking: msg._speaking }"
                @click="toggleSpeech(msg)"
                :title="msg._speaking ? '停止播报' : '语音播报'"
              >
                <Volume2 v-if="!msg._speaking" size="14" />
                <VolumeX v-else size="14" />
                <span>{{ msg._speaking ? '停止' : '播报' }}</span>
              </button>
              <!-- RAG 引用切片展示 -->
              <div v-if="msg.references && msg.references.length > 0" class="rag-references">
		              <div class="rag-refs-header">
			                <FileText class="refs-icon" size="16" />
			                <span>知识库引用 <span class="refs-count">{{ msg.references.length }}</span></span>
			                <button class="refs-toggle" @click="msg._refsCollapsed = !msg._refsCollapsed">
			                  <ChevronDown class="toggle-icon" :class="{ rotated: !msg._refsCollapsed }" size="18" />
			                </button>
			              </div>
			              <div v-if="!msg._refsCollapsed" class="rag-refs-list">
			                <div v-for="(ref, ri) in msg.references" :key="ri" class="rag-ref-item">
			                  <div class="rag-ref-index">{{ ref.index }}</div>
			                  <div class="rag-ref-body">
			                    <div class="rag-ref-content">{{ ref.content }}</div>
			                    <div v-if="ref.metadata && ref.metadata.filename" class="rag-ref-source">
			                      <svg viewBox="0 0 24 24" fill="none" class="source-icon"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6z" stroke="currentColor" stroke-width="1.5"/><path d="M14 2v6h6" stroke="currentColor" stroke-width="1.5"/></svg>
			                      {{ ref.metadata.filename }}
			                    </div>
			                  </div>
			                </div>
			              </div>
		            </div>
            </div>
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
      <div class="input-toolbar">
        <label class="rag-toggle" title="开启后 AI 会从知识库检索相关内容回答问题" @click="ragEnabled = !ragEnabled">
	          <Search class="rag-toggle-icon" size="16" />
	          <span>知识库检索</span>
          <div class="toggle-switch" :class="{ active: ragEnabled }">
            <div class="toggle-knob"></div>
          </div>
        </label>
      </div>
      <textarea
        v-model="inputText"
        placeholder="输入你想了解的内容..."
        rows="2"
        :disabled="loading"
        @keydown.enter.prevent="onInputEnter"
      />
      <!-- 语音输入（麦克风录音转文字） -->
      <button
        class="mic-btn"
        :class="{ recording }"
        @click="toggleRecording"
        :disabled="transcribing || loading"
        :title="recording ? '停止录音并转写 (' + recordSeconds + 's)' : (transcribing ? '正在转写...' : '语音输入')"
      >
        <span v-if="transcribing" class="mic-spinner"></span>
        <MicOff v-else-if="recording" size="18" />
        <Mic v-else size="18" />
      </button>
      <button class="send-btn" :class="{ 'stop-btn': loading }" :disabled="loading ? false : !inputText.trim()" @click="loading ? stopStream() : send()">
	        <template v-if="loading">
	          <Square class="btn-icon" size="18" />
	          <span class="btn-text">终止</span>
	        </template>
	        <template v-else>
	          <ArrowRight class="btn-icon" size="18" />
	          <span class="btn-text">发送</span>
	        </template>
	      </button>
    </div>
    </div>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="confirmDeleteChatId" class="modal-overlay" @click="confirmDeleteChatId = ''">
        <div class="modal-content" @click.stop>
          <div class="modal-icon">
	            <Trash2 class="icon" size="24" />
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

    <!-- 图片预览弹窗 -->
    <Teleport to="body">
      <div v-if="previewImage.show" class="image-preview-overlay" @click.self="closePreview">
        <button class="image-preview-close" @click="closePreview" title="关闭">
	          <X size="22" />
	        </button>
        <img :src="previewImage.src" :alt="previewImage.alt" class="image-preview-img" @click.self="closePreview" />
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, inject } from 'vue'
import { streamKnowledgeChat, streamKnowledgeChatRag, request, updateChatTitle, deleteChat } from '../api/request'
import { username as reactiveUsername } from '../utils/auth'
import { linkifyHtml } from '../utils/linkify'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { Plus, Pencil, Trash2, ChevronLeft, ChevronRight, ArrowLeft, Search, ChevronDown, ArrowRight, Square, X, FileText, Volume2, VolumeX, Mic, MicOff, Menu } from '@lucide/vue'

const chatId = ref('')
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref(null)
const abortController = ref(null)

// RAG 知识库搜索开关
const ragEnabled = ref(true)

// Sidebar state from App.vue
const isSidebarOpen = inject('isSidebarOpen', ref(true))
const setSidebarOpen = inject('setSidebarOpen', (v) => {})
const showToast = inject('showToast', () => {})

// 移动端检测（<=768px）
const isMobile = ref(false)
function updateIsMobile() {
  isMobile.value = window.innerWidth <= 768
}

// History state
const historyList = ref([])

// 编辑标题状态
const editingChatId = ref('')
const editTitleText = ref('')
// 删除确认状态
const confirmDeleteChatId = ref('')

// 图片预览状态
const previewImage = ref({ show: false, src: '', alt: '' })
function openPreview(src, alt) {
  previewImage.value = { show: true, src, alt }
}
function closePreview() {
  previewImage.value = { show: false, src: '', alt: '' }
}

// 当前会话标题
const currentChatTitle = computed(() => {
  if (!chatId.value) return '个人知识助手'
  const found = historyList.value.find(c => c.chatId === chatId.value)
  return found ? found.title : '个人知识助手'
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
  const hues = ['#6366f1', '#8b5cf6', '#0D9488', '#ef4444', '#f97316', '#22c56e', '#14b8a6', '#3b82f6']
  return hues[n % hues.length]
})

function generateChatId() {
  const name = reactiveUsername.value || 'anonymous'
  return `know_${name}_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

async function fetchHistoryList() {
  try {
    const res = await request.get('/ai/knowledge/chat/history')
    historyList.value = res.data
  } catch (error) {
    console.error('Failed to load history list:', error)
  }
}

async function loadHistoryChat(loadChatId) {
  if (chatId.value === loadChatId) return
  if (isMobile.value) {
    isSidebarOpen.value = false
  }
  // 切换会话时清空输入框，避免残留内容误发到其他会话
  inputText.value = ''
  try {
    const res = await request.get(`/ai/knowledge/chat/history/${loadChatId}`)
    chatId.value = loadChatId
    messages.value = res.data.map(m => ({
      role: m.role.toLowerCase(),
      content: m.content,
      references: m.references || [],
      _refsCollapsed: true
    }))
    scrollToBottom()
  } catch (error) {
    console.error('Failed to load history chat:', error)
  }
}

function createNewChat() {
  chatId.value = generateChatId()
  messages.value = []
  if (isMobile.value) {
    isSidebarOpen.value = false
  }
}

// 侧边栏切换（同步到 App.vue）
function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value
}

// 关闭侧边栏（移动端抽屉遮罩点击）
function closeSidebar() {
  isSidebarOpen.value = false
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
    showToast('会话已删除', 'success')
  } catch (error) {
    console.error('Failed to delete chat:', error)
    confirmDeleteChatId.value = ''
    showToast('删除失败，请重试', 'error')
  }
}

onMounted(() => {
  chatId.value = generateChatId()
  fetchHistoryList()

  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)

  if (isMobile.value) {
    isSidebarOpen.value = false
  }

  // 注册全局图片预览函数（供 Markdown 渲染的 img onclick 调用）
  window.__previewImage = (src, alt) => {
    openPreview(src, alt)
  }
  // 图片加载失败自动隐藏（DOMPurify 会剥离 img 的 onerror 属性，改用事件捕获监听）
  document.addEventListener('error', handleImageError, true)
  // ESC 键关闭预览
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && previewImage.value.show) {
      closePreview()
    }
  })
})

/** 聊天图片加载失败时隐藏（避免显示裂图） */
function handleImageError(event) {
  const target = event.target
  if (target && target.tagName === 'IMG' && target.classList.contains('chat-image')) {
    target.style.display = 'none'
  }
}

onUnmounted(() => {
  window.removeEventListener('resize', updateIsMobile)
  delete window.__previewImage
  document.removeEventListener('error', handleImageError, true)
  stopSpeech()
  if (recording.value) {
    recording.value = false
    clearInterval(recordTimer)
    if (scriptProcessor) {
      scriptProcessor.disconnect()
      scriptProcessor = null
    }
    if (audioContext) {
      audioContext.close().catch(() => {})
      audioContext = null
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach((track) => track.stop())
      mediaStream = null
    }
  }
})

	/** AI 回复：渲染为安全的 Markdown HTML */
	function renderMarkdown(content) {
	  if (!content) return ''
			  const renderer = new marked.Renderer()
			  renderer.link = ({ href, title, text }) => `<a target="_blank" href="${href}" title="${title || ''}">${text}</a>`
			  renderer.heading = ({ depth, text }) => `<h${depth}>${text}</h${depth}>`
			  renderer.del = ({ text }) => text // 去掉删除线，只保留文字
			  renderer.image = ({ href, title, text }) => {
		    const escapedSrc = encodeURIComponent(href)
		    const escapedAlt = text ? text.replace(/"/g, '&quot;') : ''
		    // 通过后端代理加载图片，绕过防盗链
		    return `<img src="/api/image-proxy?url=${escapedSrc}" alt="${escapedAlt}" class="chat-image" loading="lazy"`
		      + ` style="width:100%;height:auto;display:block;border-radius:8px;margin:4px 0;"`
		      + ` onclick="window.__previewImage && window.__previewImage(this.src, this.alt)"`
		      + ` onerror="this.style.display='none'" />`
		  }
	  const rawHtml = marked.parse(content, { renderer, gfm: true })
	  // 清洗后把裸地址（含 /api/... 根相对路径）转为可点击链接，点击自动拼接当前站点
	  return linkifyHtml(DOMPurify.sanitize(rawHtml))
	}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

/**
 * 输入框回车处理
 * 纯 Enter 发送消息；Ctrl/Cmd + Enter 在光标处插入换行
 */
function onInputEnter(event) {
  if (event.ctrlKey || event.metaKey) {
    insertNewline(event)
  } else {
    send()
  }
}

/** 在光标位置插入换行 */
function insertNewline(event) {
  const el = event.target
  const start = el.selectionStart
  const end = el.selectionEnd
  inputText.value = inputText.value.slice(0, start) + '\n' + inputText.value.slice(end)
  nextTick(() => {
    el.selectionStart = el.selectionEnd = start + 1
  })
}

function send() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()
  const aiIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '', references: [], _refsCollapsed: true })
  loading.value = true

  // 创建 AbortController 用于终止请求
  const controller = new AbortController()
  abortController.value = controller

  // RAG 模式：使用带引用标注的流式接口
  if (ragEnabled.value) {
    streamKnowledgeChatRag(text, chatId.value, {
      onChunk(chunk) {
        if (chunk) {
          messages.value[aiIndex].content += chunk
        }
        scrollToBottom()
      },
      onDone(refs) {
        if (refs) {
          // 提取纯文本内容（去掉 <!--RAG_REFS--> 标记后的部分）
          messages.value[aiIndex].content = refs.displayContent
          messages.value[aiIndex].references = refs.references
        }
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
  } else {
    // 普通模式：不使用 RAG
    streamKnowledgeChat(text, chatId.value, {
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
}

/** 终止 AI 回复 */
function stopStream() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
    loading.value = false
  }
}

// 语音播报
let speechAudio = null // 当前播放的 Audio 对象（非响应式）
let speechUrl = null   // 当前播放音频的 Blob URL（非响应式）

/** 停止当前语音播报并复位所有播报状态 */
function stopSpeech() {
  if (speechAudio) {
    speechAudio.pause()
    speechAudio.onended = null
    speechAudio.onerror = null
    speechAudio = null
  }
  if (speechUrl) {
    URL.revokeObjectURL(speechUrl)
    speechUrl = null
  }
  for (const m of messages.value) {
    if (m._speaking) m._speaking = false
  }
}

/** 播报/停止 AI 消息语音 */
async function toggleSpeech(msg) {
  // 点击正在播报的消息 → 停止
  if (msg._speaking) {
    stopSpeech()
    return
  }
  if (!msg.content || loading.value) return
  stopSpeech()
  msg._speaking = true
  try {
    const res = await request.post('/speech/tts', { text: msg.content }, {
      responseType: 'blob',
      timeout: 120000,
    })
    if (!msg._speaking) return // 等待期间已被停止
    const url = URL.createObjectURL(res.data)
    speechUrl = url
    const audio = new Audio(url)
    speechAudio = audio
    audio.onended = () => {
      URL.revokeObjectURL(url)
      if (speechUrl === url) speechUrl = null
      msg._speaking = false
      speechAudio = null
    }
    audio.onerror = () => {
      URL.revokeObjectURL(url)
      if (speechUrl === url) speechUrl = null
      msg._speaking = false
      speechAudio = null
      showToast('语音播报失败', 'error')
    }
    await audio.play()
  } catch (err) {
    if (speechUrl) {
      URL.revokeObjectURL(speechUrl)
      speechUrl = null
    }
    msg._speaking = false
    showToast('语音播报失败：' + (err?.message || '网络错误'), 'error')
  }
}

// 语音输入（STT：麦克风录音转文字）
const recording = ref(false)      // 是否正在录音
const transcribing = ref(false)   // 是否正在转写
const recordSeconds = ref(0)      // 录音秒数
const RECORD_MAX_SECONDS = 60     // 录音时长上限（秒）
let mediaStream = null            // 麦克风音频流
let audioContext = null           // 录音音频上下文
let scriptProcessor = null        // 录音处理器
let pcmChunks = []                // 收集的 PCM 分片
let recordTimer = null            // 录音计时器

/** 开始/停止录音，停止后转写并填入输入框 */
async function toggleRecording() {
  if (recording.value) {
    stopRecording()
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    showToast('当前浏览器不支持录音', 'error')
    return
  }
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioContext = new AudioContext()
    // 确保音频上下文运行（合成点击可能不被识别为用户手势导致 suspended）
    await audioContext.resume()
    const source = audioContext.createMediaStreamSource(mediaStream)
    scriptProcessor = audioContext.createScriptProcessor(4096, 1, 1)
    pcmChunks = []
    const sourceRate = audioContext.sampleRate
    // 采集音频并降采样为 16kHz 单声道 16bit PCM
    scriptProcessor.onaudioprocess = (event) => {
      const input = event.inputBuffer.getChannelData(0)
      const targetRate = 16000
      if (sourceRate !== targetRate) {
        const ratio = sourceRate / targetRate
        const outLen = Math.floor(input.length / ratio)
        const out = new Int16Array(outLen)
        for (let i = 0; i < outLen; i++) {
          out[i] = Math.max(-1, Math.min(1, input[Math.floor(i * ratio)])) * 0x7FFF
        }
        pcmChunks.push(out)
      } else {
        const out = new Int16Array(input.length)
        for (let i = 0; i < input.length; i++) {
          out[i] = Math.max(-1, Math.min(1, input[i])) * 0x7FFF
        }
        pcmChunks.push(out)
      }
    }
    source.connect(scriptProcessor)
    // 静音节点连接扬声器保持音频图活跃（Web Audio 无输出节点的图不执行处理，采集不触发）
    // 增益为 0 不产生可听输出，因此无回声
    const silentGain = audioContext.createGain()
    silentGain.gain.value = 0
    scriptProcessor.connect(silentGain)
    silentGain.connect(audioContext.destination)
    recording.value = true
    recordSeconds.value = 0
    recordTimer = setInterval(() => {
      recordSeconds.value++
      if (recordSeconds.value >= RECORD_MAX_SECONDS) {
        stopRecording() // 超时自动停止
      }
    }, 1000)
  } catch (err) {
    showToast('无法访问麦克风：' + (err?.message || '权限被拒绝'), 'error')
  }
}

/** 停止录音，编码 WAV 并上传转写 */
function stopRecording() {
  if (!recording.value) return
  recording.value = false
  clearInterval(recordTimer)
  if (scriptProcessor) {
    scriptProcessor.disconnect()
    scriptProcessor.onaudioprocess = null
    scriptProcessor = null
  }
  if (audioContext) {
    audioContext.close().catch(() => {})
    audioContext = null
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach((track) => track.stop())
    mediaStream = null
  }
  const totalLen = pcmChunks.reduce((sum, chunk) => sum + chunk.length, 0)
  if (totalLen === 0) {
    showToast('未录到有效音频', 'error')
    return
  }
  const pcm = new Int16Array(totalLen)
  let offset = 0
  for (const chunk of pcmChunks) {
    pcm.set(chunk, offset)
    offset += chunk.length
  }
  uploadForRecognition(encodeWav(pcm, 16000))
}

/** PCM 数据封装为标准 WAV Blob（44 字节头 + 16bit 单声道） */
function encodeWav(pcm, sampleRate) {
  const buffer = new ArrayBuffer(44 + pcm.length * 2)
  const view = new DataView(buffer)
  const writeStr = (off, str) => {
    for (let i = 0; i < str.length; i++) view.setUint8(off + i, str.charCodeAt(i))
  }
  writeStr(0, 'RIFF')
  view.setUint32(4, 36 + pcm.length * 2, true)
  writeStr(8, 'WAVE')
  writeStr(12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)             // PCM 编码
  view.setUint16(22, 1, true)             // 单声道
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true) // 字节率
  view.setUint16(32, 2, true)             // 块对齐
  view.setUint16(34, 16, true)            // 位深
  writeStr(36, 'data')
  view.setUint32(40, pcm.length * 2, true)
  for (let i = 0; i < pcm.length; i++) {
    view.setInt16(44 + i * 2, pcm[i], true)
  }
  return new Blob([buffer], { type: 'audio/wav' })
}

/** 上传 WAV 转写，结果填入输入框 */
async function uploadForRecognition(wavBlob) {
  transcribing.value = true
  try {
    const formData = new FormData()
    formData.append('file', wavBlob, 'speech.wav')
    // 覆盖实例默认的 application/json（request.js 全局设置），
    // 置空后浏览器自动生成 multipart/form-data 及 boundary
    const res = await request.post('/speech/stt', formData, {
      timeout: 90000,
      headers: { 'Content-Type': undefined },
    })
    const text = res.data?.text
    if (text) {
      inputText.value = text
      showToast('语音已转文字', 'success')
    } else {
      showToast('未识别到语音内容', 'error')
    }
  } catch (err) {
    showToast('语音识别失败：' + (err?.message || '网络错误'), 'error')
  } finally {
    transcribing.value = false
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
	  background: rgba(255,255,255,0.55);
	  backdrop-filter: blur(20px);
	  -webkit-backdrop-filter: blur(20px);
	  border-right: 1px solid rgba(255,255,255,0.4);
	  border-bottom: 1px solid rgba(16,185,129,0.06);
	  box-shadow:
	    2px 0 20px rgba(16,185,129,0.06),
	    0 0 0 1px rgba(255,255,255,0.5) inset;
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

/* 移动端抽屉遮罩（点击关闭侧边栏） */
.sidebar-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.3);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  z-index: 15;
  animation: fadeIn 0.2s ease;
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
	  background: linear-gradient(135deg, #34D399, #059669);
	  color: #fff;
	  border: none;
	  padding: 7px 14px;
	  border-radius: 10px;
	  font-size: 0.9rem;
	  font-weight: 500;
	  cursor: pointer;
	  transition: all 0.2s ease;
	  box-shadow: 0 4px 12px rgba(5,150,105,0.3);
	}
	.new-chat-btn:hover {
	  opacity: 0.95;
	  box-shadow: 0 6px 20px rgba(5,150,105,0.4);
	  transform: translateY(-1px);
	}
	.new-chat-btn:active {
	  transform: translateY(0);
	  box-shadow: 0 2px 8px rgba(5,150,105,0.3);
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
	  color: #10B981;
	  cursor: pointer;
	  box-shadow: 2px 0 8px rgba(16,185,129,0.08);
	  transition: color 0.2s, box-shadow 0.2s, left 0.3s ease;
	}
	.toggle-sidebar-btn:hover {
	  color: #059669;
	  box-shadow: 2px 0 16px rgba(16,185,129,0.15);
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
	  background: rgba(52, 211, 153, 0.06);
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
	  background: rgba(16,185,129,0.1);
	  color: #10B981;
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
	  background: rgba(16,185,129,0.2);
	  color: #059669;
	}
	.history-action-btn.delete:hover {
	  background: rgba(239,68,68,0.15);
	  color: #ef4444;
	}

/* 编辑标题行 */
.edit-title-input {
	  width: 100%;
	  padding: 4px 8px;
	  border: 1px solid #10B981;
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
	  color: #10B981;
	  text-shadow: 0 0 8px rgba(16,185,129,0.2);
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
	  background: rgba(16,185,129,0.08);
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
	  box-shadow: 0 16px 48px rgba(16,185,129,0.12);
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
  /* 允许收缩到容器宽度以内（防止输入区/内容把容器撑宽导致右侧被裁切） */
  min-width: 0;
}

.header {
	  flex-shrink: 0;
	  height: 60px;
	  padding: 0 20px;
	  display: flex;
	  align-items: center;
	  justify-content: space-between;
	  background: rgba(255,255,255,0.6);
	  backdrop-filter: blur(16px);
	  -webkit-backdrop-filter: blur(16px);
	  border-bottom: 1px solid rgba(255,255,255,0.3);
	  box-shadow:
	    0 1px 4px rgba(16,185,129,0.06),
	    0 0 0 1px rgba(255,255,255,0.4) inset;
	}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex-shrink: 1;
}

/* 移动端侧边栏开关按钮（桌面隐藏，移动端显示） */
.sidebar-menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border: none;
  border-radius: 12px;
  background: rgba(16,185,129,0.08);
  color: #10B981;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.sidebar-menu-btn:hover {
  background: rgba(16,185,129,0.15);
  color: #059669;
}
.sidebar-menu-btn .icon {
  width: 20px;
  height: 20px;
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
	  background: rgba(16,185,129,0.08);
	  border: 1px solid rgba(255,255,255,0.3);
	  color: #10B981;
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
	  background: rgba(16,185,129,0.15);
	  color: #059669;
	  box-shadow: 0 2px 8px rgba(16,185,129,0.1);
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
  animation: welcomeFadeIn 0.6s ease both;
}
@keyframes welcomeFadeIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
.welcome-icon-wrap {
  width: 72px;
  height: 72px;
  margin-bottom: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  background: rgba(16,185,129,0.06);
  border: 1px solid rgba(16,185,129,0.1);
}
.welcome-icon {
  width: 48px;
  height: 48px;
}
.welcome h3 {
  font-size: 1.25rem;
  color: #065F46;
  margin-bottom: 0.5rem;
  font-weight: 600;
}
.welcome-desc {
  font-size: 0.9rem;
  line-height: 1.6;
  max-width: 300px;
  color: #94a3b8;
  margin-bottom: 1.5rem;
}
.welcome-tips {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: center;
}
.tip-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 0.8rem;
  color: #64748b;
  background: rgba(255,255,255,0.5);
  border: 1px solid rgba(255,255,255,0.3);
  backdrop-filter: blur(8px);
}
.tip-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
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
	  background: #6EE7B7;
	  animation: typingBounce 1.4s ease-in-out infinite both;
	}
	.typing-dot:nth-child(1) { animation-delay: 0s; }
	.typing-dot:nth-child(2) { animation-delay: 0.2s; }
	.typing-dot:nth-child(3) { animation-delay: 0.4s; }
	@keyframes typingBounce {
	  0%, 80%, 100% { transform: translateY(0); background: #6EE7B7; }
	  40% { transform: translateY(-8px); background: #10B981; }
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
  overflow-wrap: anywhere;
  text-align: left;
  line-height: 1.8;
  min-width: 0;
}
.streaming-text {
  white-space: pre-wrap;
  line-height: 1.8;
}
.message-row.user .bubble-content {
	  width: fit-content;
	  max-width: min(85%, 65ch);
	  background: linear-gradient(135deg, #34D399, #10B981);
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
/* AI 回复语音播报按钮 + 知识库引用区域（顶部分割线） */
.speech-refs-area {
  margin-top: 16px;
  border-top: 1px solid rgba(245, 158, 11, 0.12);
  padding-top: 12px;
}
.speech-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 11px 14px;
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.08);
  color: #10B981;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.speech-btn:hover {
  background: rgba(16, 185, 129, 0.15);
  border-color: rgba(16, 185, 129, 0.5);
}
.speech-btn.speaking {
  background: #10B981;
  border-color: #10B981;
  color: #fff;
}
/* 麦克风录音按钮 */
.mic-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(16, 185, 129, 0.3);
  background: rgba(16, 185, 129, 0.08);
  color: #10B981;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}
.mic-btn:hover {
  background: rgba(16, 185, 129, 0.15);
  border-color: rgba(16, 185, 129, 0.5);
}
.mic-btn.recording {
  background: #EF4444;
  border-color: #EF4444;
  color: #fff;
  animation: mic-pulse 1.2s ease-in-out infinite;
}
.mic-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.mic-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(16, 185, 129, 0.3);
  border-top-color: #10B981;
  border-radius: 50%;
  animation: mic-rotate 0.8s linear infinite;
}
@keyframes mic-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.5); }
  50% { box-shadow: 0 0 0 8px rgba(239, 68, 68, 0); }
}
@keyframes mic-rotate {
  to { transform: rotate(360deg); }
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
	  color: #10B981;
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
	  background: #6EE7B7;
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
	  border-left: 3px solid #10B981;
	}
	.markdown-body :deep(h4) {
	  color: #059669;
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
	  border-left: 3px solid #6EE7B7;
	  background: rgba(16,185,129,0.04);
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
	  background: linear-gradient(90deg, transparent, #A7F3D0, transparent);
	}
.markdown-body :deep(code) {
	  background: #f1f5f9;
	  padding: 0.2em 0.4em;
	  border-radius: 4px;
	  font-family: 'Cascadia Code', 'Fira Code', monospace;
	  font-size: 0.9em;
	  color: #10B981;
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
	  color: #10B981;
	  text-decoration: underline;
	}
/* Markdown 表格：超出宽度时气泡内横向滚动，不撑破布局 */
.markdown-body :deep(table) {
  display: block;
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
  margin-bottom: 0.8em;
  font-size: 0.9em;
  line-height: 1.5;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e2e8f0;
  padding: 0.4em 0.6em;
  text-align: left;
  white-space: normal;
  word-break: break-word;
}
.markdown-body :deep(th) {
  background: rgba(16, 185, 129, 0.06);
  font-weight: 600;
  color: #065F46;
}
.input-area {
  flex-shrink: 0;
  padding: 1rem;
  border-top: 1px solid rgba(255,255,255,0.3);
  display: flex;
  gap: 0.75rem;
  align-items: center;
  background: rgba(255,255,255,0.6);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow:
	    0 -4px 16px rgba(16,185,129,0.04),
	    0 0 0 1px rgba(255,255,255,0.4) inset;
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
	  border-color: #34D399;
	  box-shadow: 0 0 0 3px rgba(52, 211, 153, 0.1);
	}
.send-btn {
	  display: flex;
	  align-items: center;
	  gap: 6px;
	  background: linear-gradient(135deg, #34D399, #10B981);
	  border: none;
	  color: #fff;
	  padding: 0 28px;
	  border-radius: 12px;
	  cursor: pointer;
	  font-weight: 600;
	  height: 44px;
	  letter-spacing: 0.02em;
	  transition: all 0.2s ease;
	  box-shadow: 0 4px 14px rgba(52,211,153,0.3);
	}
.send-btn:hover:not(:disabled) {
	  opacity: 0.95;
	  box-shadow: 0 6px 24px rgba(52,211,153,0.4);
	  transform: translateY(-1px);
	}
	.send-btn:active:not(:disabled) {
	  transform: translateY(0);
	  box-shadow: 0 2px 8px rgba(52,211,153,0.3);
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
  /* 移动端头部：显示侧边栏开关，返回按钮紧凑 */
  .sidebar-menu-btn {
    display: flex;
  }
  .back-btn {
    padding: 6px 10px;
  }
  /* 移动端发送按钮：仅图标，节省横向空间 */
  .send-btn {
    padding: 0;
    width: 44px;
    justify-content: center;
  }
  .btn-text {
    display: none;
  }
  /* 移动端消息区与输入区适配 */
  .messages {
    padding: 1rem 0.75rem;
    gap: 1rem;
  }
  .message-row {
    max-width: 96%;
  }
  /* 移动端输入区：RAG 工具栏独占一行，输入框/按钮换行排布，避免内容被挤出 */
  .input-area {
    flex-wrap: wrap;
    padding: 0.75rem;
    padding-bottom: calc(0.75rem + env(safe-area-inset-bottom));
  }
  .input-toolbar {
    flex: 1 1 100%;
    width: 100%;
  }
  /* iOS 聚焦输入框不自动放大（<16px 会触发） */
  .input-area textarea {
    font-size: 1rem;
  }
}


/* 图片预览弹窗 */
.image-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
  cursor: zoom-out;
}
.image-preview-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.4);
  animation: modalSlideUp 0.25s ease;
  cursor: default;
}
.image-preview-close {
  position: fixed;
  top: 16px;
  right: 16px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.2);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 2001;
}
.image-preview-close:hover {
  background: rgba(255,255,255,0.25);
}
.image-preview-close svg {
  width: 22px;
  height: 22px;
}

/* ==================== RAG 切换开关 ==================== */
.input-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.rag-toggle {
	  display: flex;
	  align-items: center;
	  gap: 8px;
	  cursor: pointer;
	  user-select: none;
	  font-size: 0.82rem;
	  color: #065F46;
	  padding: 4px 10px 4px 8px;
	  border-radius: 8px;
	  background: rgba(16,185,129,0.05);
	  border: 1px solid rgba(16,185,129,0.1);
	  transition: all 0.2s ease;
	}
	.rag-toggle:hover {
	  background: rgba(16,185,129,0.1);
	  border-color: rgba(16,185,129,0.2);
	}
	.rag-toggle-icon {
	  width: 16px;
	  height: 16px;
	  flex-shrink: 0;
	  color: #10B981;
	}
	.toggle-switch {
	  width: 32px;
	  height: 18px;
	  border-radius: 999px;
	  background: #cbd5e1;
	  position: relative;
	  transition: background 0.25s ease;
	  flex-shrink: 0;
	}
	.toggle-switch.active {
	  background: #10B981;
	}
.toggle-knob {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform 0.25s ease;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
}
.toggle-switch.active .toggle-knob {
  transform: translateX(14px);
}

/* ==================== RAG 引用切片展示 ==================== */
.rag-references {
  margin-top: 12px;
}
.rag-refs-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  color: #10B981;
  font-weight: 500;
  margin-bottom: 8px;
}
.refs-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: #F59E0B;
}
.refs-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(245,158,11,0.12);
  color: #D97706;
  font-size: 0.7rem;
  font-weight: 700;
  margin-left: 2px;
}
.refs-toggle {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  padding: 2px;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}
.refs-toggle:hover {
  color: #10B981;
}
.toggle-icon {
  width: 18px;
  height: 18px;
  transition: transform 0.25s ease;
}
.toggle-icon.rotated {
  transform: rotate(180deg);
}
.rag-refs-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rag-ref-item {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  background: rgba(255,255,255,0.5);
  border: 1px solid rgba(16,185,129,0.08);
  border-left: 3px solid #34D399;
  border-radius: 12px;
  transition: background 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.02);
}
.rag-ref-item:hover {
  background: rgba(255,255,255,0.75);
  box-shadow: 0 4px 12px rgba(16,185,129,0.06);
}
.rag-ref-index {
  font-size: 0.75rem;
  font-weight: 700;
  color: #D97706;
  background: rgba(245,158,11,0.1);
  border-radius: 8px;
  padding: 3px 8px;
  height: fit-content;
  white-space: nowrap;
  flex-shrink: 0;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  letter-spacing: 0.02em;
}
.rag-ref-body {
  flex: 1;
  min-width: 0;
}
.rag-ref-content {
  font-size: 0.82rem;
  color: #475569;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}
.rag-ref-source {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: rgba(16,185,129,0.04);
  border-radius: 6px;
  width: fit-content;
}
.source-icon {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  color: #10B981;
}
.rag-ref-source::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #cbd5e1;
}

/* 流式文本中引用标记样式 */
.streaming-text :deep([rag-ref]) {
	  color: #10B981;
	  font-weight: 600;
	  cursor: pointer;
	}
</style>
