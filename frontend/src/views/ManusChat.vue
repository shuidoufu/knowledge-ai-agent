<template>
  <div class="chat-page">
    <header class="chat-header">
      <router-link to="/" class="back">
        <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M19 12H5m7-7l-7 7 7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        返回
      </router-link>
      <h1>AI 超级智能体</h1>
    </header>
    <div class="messages" ref="messagesRef">
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
    </div>
    <div class="input-area">
      <textarea
        v-model="inputText"
        placeholder="输入你的任务或问题..."
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
    <!-- 图片预览弹窗 -->
    <Teleport to="body">
      <div v-if="previewImage.show" class="image-preview-overlay" @click.self="closePreview">
        <button class="image-preview-close" @click="closePreview" title="关闭">
          <svg viewBox="0 0 24 24" fill="none"><path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
        <img :src="previewImage.src" :alt="previewImage.alt" class="image-preview-img" @click.self="closePreview" />
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { streamManusChat } from '../api/request'
import { username as reactiveUsername } from '../utils/auth'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref(null)
const abortController = ref(null)

// 图片预览状态
const previewImage = ref({ show: false, src: '', alt: '' })
function openPreview(src, alt) {
  previewImage.value = { show: true, src, alt }
}
function closePreview() {
  previewImage.value = { show: false, src: '', alt: '' }
}

const userAvatarLetter = computed(() => {
  const name = reactiveUsername.value
  return name ? name.trim().charAt(0).toUpperCase() : '?'
})
const userAvatarColor = computed(() => {
  const name = reactiveUsername.value
  if (!name) return '#64748b'
  let n = 0
  for (let i = 0; i < name.length; i++) n += name.charCodeAt(i)
  const hues = ['#6366f1', '#8b5cf6', '#ec4899', '#ef4444', '#f97316', '#22c55e', '#14b8a6', '#3b82f6']
  return hues[n % hues.length]
})

/** AI 回复：渲染为安全的 Markdown HTML */
	function renderMarkdown(content) {
	  if (!content) return ''
		  const renderer = new marked.Renderer()
		  renderer.link = ({ href, title, text }) => `<a target="_blank" href="${href}" title="${title || ''}">${text}</a>`
		  renderer.heading = ({ depth, text }) => `<h${depth}>${text}</h${depth}>`
		  renderer.del = ({ text }) => text
		  renderer.image = ({ href, title, text }) => {
	    const escapedSrc = encodeURIComponent(href)
	    const escapedAlt = text ? text.replace(/"/g, '&quot;') : ''
	    return `<img src="/api/image-proxy?url=${escapedSrc}" alt="${escapedAlt}" class="chat-image" loading="lazy"`
	      + ` style="width:100%;height:auto;display:block;border-radius:8px;margin:4px 0;"`
	      + ` onclick="window.__previewImage && window.__previewImage(this.src, this.alt)"`
	      + ` onerror="this.style.display='none'" />`
	  }
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

  const controller = new AbortController()
  abortController.value = controller

  streamManusChat(text, {
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
    },
    onError(err) {
      loading.value = false
      abortController.value = null
      if (err?.name === 'AbortError') return
      messages.value[aiIndex].content = '回复失败：' + (err?.message || '网络错误')
      scrollToBottom()
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

onMounted(() => {
  // 注册全局图片预览函数（供 Markdown 渲染的 img onclick 调用）
  window.__previewImage = (src, alt) => {
    openPreview(src, alt)
  }
  // ESC 键关闭预览
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && previewImage.value.show) {
      closePreview()
    }
  })
})

onUnmounted(() => {
  delete window.__previewImage
})
</script>

<style scoped>
.chat-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fb;
  color: #1e293b;
}
.chat-header {
  flex-shrink: 0;
  padding: 0 1.25rem;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 1rem;
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255,255,255,0.3);
  box-shadow: 0 1px 4px rgba(99,102,241,0.06);
}
.back {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6366f1;
  text-decoration: none;
  font-size: 0.85rem;
  font-weight: 500;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(99,102,241,0.08);
  border: 1px solid rgba(255,255,255,0.3);
  white-space: nowrap;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
}
.back .icon {
  width: 16px;
  height: 16px;
}
.back:hover {
  background: rgba(99,102,241,0.15);
  color: #4f46e5;
  box-shadow: 0 2px 8px rgba(99,102,241,0.1);
}
.chat-header h1 {
  font-size: 1.1rem;
  flex: 1;
  font-weight: 600;
  color: #1e1b4b;
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
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
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
.chat-avatar.assistant {
  background: transparent;
  animation: float 4s ease-in-out infinite;
}
/* 机器人头像 */
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
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
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
  font-size: 0.9rem;
  font-weight: 700;
  color: #6366f1;
  text-shadow: 0 0 8px rgba(99,102,241,0.2);
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
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
}
.message-row.assistant .bubble-content {
  min-width: 12em;
  max-width: min(85%, 65ch);
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #1e293b;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  line-height: 1.8;
}
/* Enhanced Markdown styles for light theme */
/* Enhanced Markdown styles */
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
  color: #3b82f6;
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
  background: #93c5fd;
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
  border-left: 3px solid #3b82f6;
}
.markdown-body :deep(h4) {
  color: #2563eb;
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
  border-left: 3px solid #93c5fd;
  background: rgba(59,130,246,0.04);
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
  background: linear-gradient(90deg, transparent, #bfdbfe, transparent);
}
.markdown-body :deep(code) {
  background: #f1f5f9;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Cascadia Code', 'Fira Code', monospace;
  font-size: 0.9em;
  color: #3b82f6;
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
  color: #3b82f6;
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
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.send-btn {
  padding: 0 28px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  font-weight: 600;
  height: 44px;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
}
.send-btn:hover:not(:disabled) {
  opacity: 0.95;
  box-shadow: 0 6px 24px rgba(99,102,241,0.4);
  transform: translateY(-1px);
}
.send-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
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

/* 聊天图片样式 — 使用非 scoped 样式确保匹配 v-html 内容 */

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

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes modalSlideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>
