<template>
	<view class="page">
		<!-- 顶部工具栏：返回 / 历史 / 新对话 / 标题 / RAG 开关 -->
		<view class="toolbar">
			<view class="tool-btn icon-btn" @tap="goHome">
				<image class="tool-icon" src="/static/icons/back.svg" mode="aspectFit" />
			</view>
			<view class="tool-btn icon-btn" @tap="openHistory">
				<image class="tool-icon" src="/static/icons/history.svg" mode="aspectFit" />
			</view>
			<view class="tool-btn icon-btn" @tap="newChat">
				<image class="tool-icon" src="/static/icons/plus.svg" mode="aspectFit" />
			</view>
			<view class="tool-title">{{ currentTitle }}</view>
			<view class="rag-toggle" :class="{ on: ragEnabled }" @tap="toggleRag">
				<text class="rag-text">RAG</text>
				<view class="rag-dot"></view>
			</view>
		</view>

		<!-- 消息列表 -->
		<scroll-view v-if="messages.length" class="msg-list" scroll-y :scroll-into-view="scrollInto" scroll-with-animation>
			<view v-for="msg in messages" :key="msg.id">
				<chat-bubble :msg="msg" />
			</view>
			<!-- 底部锚点：始终置于列表末尾，scroll-into-view 滚到锚点即滚到最底（长消息也不漏） -->
			<view id="bottom-anchor" class="bottom-anchor"></view>
		</scroll-view>
		<!-- 空状态：垂直居中 -->
		<view v-else class="empty-area">
			<view class="empty-tip">
				<view class="empty-icon-wrap">
					<image class="empty-icon" src="/static/icons/book.svg" mode="aspectFit" />
				</view>
				<text class="empty-title">向你的个人知识库提问</text>
				<text class="empty-sub">支持 RAG 检索引用 · 语音输入 · PDF 下载</text>
			</view>
		</view>

		<!-- 输入区 -->
		<view class="input-area">
			<view class="input-row">
				<view class="input-wrap chat-input">
					<textarea
						class="chat-textarea"
						v-model="input"
						placeholder="输入问题..."
						placeholder-style="color:#94A3B8"
						:disabled="sending"
						auto-height
						confirm-type="send"
						@confirm="send"
					/>
				</view>
				<view class="mic-btn" :class="{ recording: recording }" @tap="toggleRecord">
					<image class="btn-icon" :src="recording ? '/static/icons/mic-white.svg' : '/static/icons/mic.svg'" mode="aspectFit" />
				</view>
				<view class="send-btn" :class="{ sending: sending }" @tap="sending ? stop() : send()">
					<image class="btn-icon" :src="sending ? '/static/icons/stop-white.svg' : '/static/icons/send-white.svg'" mode="aspectFit" />
				</view>
			</view>
		</view>

		<!-- 历史会话弹层 -->
		<history-panel
			:visible="historyVisible"
			:current-chat-id="chatId"
			@close="historyVisible = false"
			@select="onSelectChat"
			@deleted-current="onDeletedCurrent"
		/>
	</view>
</template>

<script setup>
/**
 * 知识聊天页：流式聊天（RAG 开关）、语音输入、历史会话管理、新建对话
 */
import { ref, nextTick } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { isLoggedIn } from '../../utils/auth'
import { generateChatId, generateMsgId, parseRagReferences, mapHistoryMessages } from '../../utils/chat'
import { streamKnowledgeChat, streamKnowledgeChatRag, fetchChatDetail, fetchHistory } from '../../utils/api'
import { startRecording, stopRecording, isRecording } from '../../utils/stt'
import { stopSpeech } from '../../utils/tts'
import chatBubble from '../../components/chat-bubble.vue'
import historyPanel from '../../components/history-panel.vue'

const RAG_REFS_MARK = '<!--RAG_REFS-->'

const messages = ref([])
const input = ref('')
const chatId = ref('')
const ragEnabled = ref(true)
const sending = ref(false)
const historyVisible = ref(false)
const recording = ref(false)
const currentTitle = ref('新对话')
const scrollInto = ref('')
const historyList = ref([])

let streamTask = null
let sentChatId = ''
let scrollTimer = null

onLoad(() => {
	if (!isLoggedIn()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	chatId.value = generateChatId()
})

onShow(() => {
	if (!isLoggedIn()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
})

onUnload(() => {
	stopStream()
	if (isRecording()) {
		stopRecording()
	}
	stopSpeech()
})

/** 返回应用中心首页 */
function goHome() {
	stopStream()
	stopSpeech()
	uni.reLaunch({ url: '/pages/index/index' })
}

/** 新建对话：停流 + 新会话 ID + 清空消息 */
function newChat() {
	stopStream()
	if (isRecording()) {
		stopRecording()
	}
	stopSpeech()
	chatId.value = generateChatId()
	messages.value = []
	input.value = ''
	currentTitle.value = '新对话'
	scrollInto.value = ''
	historyVisible.value = false
	uni.showToast({ title: '已开启新对话', icon: 'none' })
}

function toggleRag() {
	ragEnabled.value = !ragEnabled.value
}

function openHistory() {
	historyVisible.value = true
}

/**
 * 滚动到底部：用固定的底部锚点（bottom-anchor），scroll-into-view 始终滚到锚点，
 * 避免目标为长消息时只滚到消息顶部（最新内容在屏幕下方）。
 * 锚点 id 恒定不触发滚动 → 先清空再设置；流式高频调用节流 150ms
 */
function scrollToBottom() {
	if (!messages.value.length) return
	if (scrollTimer) return
	scrollTimer = setTimeout(() => {
		scrollTimer = null
		nextTick(() => {
			scrollInto.value = ''
			setTimeout(() => {
				scrollInto.value = 'bottom-anchor'
			}, 80)
		})
	}, 150)
}

async function send() {
	const text = input.value.trim()
	if (!text || sending.value) return
	// 后端流式接口是 GET，超长文本会命中 Tomcat 8KB 请求头上限
	if (text.length > 4000) {
		uni.showToast({ title: '问题过长，请分段提问（≤4000字）', icon: 'none' })
		return
	}
	input.value = ''
	messages.value.push({ id: generateMsgId(), role: 'user', content: text, references: [], loading: false })
	messages.value.push({ id: generateMsgId(), role: 'assistant', content: '', references: [], loading: true })
	sending.value = true
	sentChatId = chatId.value
	scrollToBottom()

	const handlers = {
		onChunk: (chunk) => {
			if (chatId.value !== sentChatId) return
			const last = messages.value[messages.value.length - 1]
			if (!last) return
			last.content = appendChunk(last.content, chunk)
			scrollToBottom()
		},
		onDone: (fullText) => {
			finishSend()
			// 流归属校验：期间切换了会话则丢弃旧流结果
			if (chatId.value !== sentChatId) return
			const last = messages.value[messages.value.length - 1]
			if (last) {
				const parsed = parseRagReferences(fullText || last.content)
				last.content = parsed.displayContent
				last.references = parsed.references
				last.loading = false
				// 发送后立即停止产生的空气泡：无内容则移除
				if (!last.content) {
					messages.value.pop()
				}
			}
		},
		onError: (err) => {
			finishSend()
			if (chatId.value !== sentChatId) return
			const last = messages.value[messages.value.length - 1]
			if (last) {
				last.content = '回复失败：' + ((err && err.message) || '网络错误')
				last.loading = false
			}
		}
	}

	try {
		streamTask = ragEnabled.value
			? streamKnowledgeChatRag(text, chatId.value, handlers)
			: streamKnowledgeChat(text, chatId.value, handlers)
	} catch (e) {
		handlers.onError(e)
	}
}

/** 流式 chunk 追加：出现 RAG 引用标记后不再追加（标记后是引用 JSON） */
function appendChunk(content, chunk) {
	const merged = content + chunk
	const idx = merged.indexOf(RAG_REFS_MARK)
	return idx >= 0 ? merged.slice(0, idx) : merged
}

function finishSend() {
	sending.value = false
	streamTask = null
	refreshHistoryTitle()
}

function stop() {
	stopStream()
}

function stopStream() {
	if (streamTask) {
		try {
			streamTask.abort()
		} catch (e) {
			// 已结束则忽略
		}
		streamTask = null
	}
}

/** 流式结束后刷新历史标题 */
async function refreshHistoryTitle() {
	try {
		const list = await fetchHistory()
		historyList.value = list
		const mine = list.find((c) => c.chatId === chatId.value)
		if (mine && mine.title) {
			currentTitle.value = mine.title
		}
	} catch (e) {
		// 标题刷新失败不阻塞
	}
}

/** 切换历史会话：先中止进行中的流式，避免旧流写入新会话 */
async function onSelectChat(targetId) {
	historyVisible.value = false
	stopStream()
	if (targetId === chatId.value && messages.value.length) return
	try {
		const detail = await fetchChatDetail(targetId)
		messages.value = mapHistoryMessages(detail)
		chatId.value = targetId
		currentTitle.value = '加载中...'
		refreshHistoryTitle()
		scrollToBottom()
	} catch (e) {
		uni.showToast({ title: (e && e.message) || '加载会话失败', icon: 'none' })
	}
}

/** 当前会话被删除：新建会话 */
function onDeletedCurrent() {
	chatId.value = generateChatId()
	messages.value = []
	currentTitle.value = '新对话'
	scrollInto.value = ''
}

/** 语音输入：点击切换录音/停止，识别结果回填输入框 */
function toggleRecord() {
	if (recording.value) {
		stopRecording()
		// 识别结果回调里置 false
		return
	}
	startRecording(
		() => {
			recording.value = true
		},
		(text) => {
			recording.value = false
			if (text) {
				input.value = (input.value ? input.value + ' ' : '') + text
			} else {
				uni.showToast({ title: '未识别到内容', icon: 'none' })
			}
		},
		(errMsg) => {
			recording.value = false
			uni.showToast({ title: errMsg || '语音识别失败', icon: 'none' })
		}
	)
}
</script>

<style scoped>
.page {
	height: 100vh;
	height: 100dvh; /* 真机浏览器动态视口，避免底部被系统条遮挡 */
	overflow: hidden;
	display: flex;
	flex-direction: column;
	background: #ECFDF5;
}

.toolbar {
	display: flex;
	align-items: center;
	padding: 16rpx 20rpx;
	background: rgba(255, 255, 255, 0.85);
	border-bottom: 1rpx solid rgba(16, 185, 129, 0.15);
}

.tool-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	min-width: 88rpx;
	background: rgba(255, 255, 255, 0.75);
	border: 1rpx solid rgba(16, 185, 129, 0.3);
	border-radius: 24rpx;
	margin-right: 16rpx;
	flex-shrink: 0;
}

.tool-icon {
	width: 40rpx;
	height: 40rpx;
}

.tool-title {
	flex: 1;
	min-width: 0;
	text-align: center;
	font-size: 28rpx;
	font-weight: 600;
	color: #064E3B;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	padding: 0 12rpx;
}

/* RAG 开关：胶囊开关样式 */
.rag-toggle {
	display: flex;
	align-items: center;
	height: 88rpx;
	padding: 0 24rpx;
	border-radius: 24rpx;
	border: 1rpx solid rgba(16, 185, 129, 0.3);
	background: rgba(255, 255, 255, 0.75);
	flex-shrink: 0;
	transition: all 0.2s ease;
}

.rag-toggle.on {
	background: linear-gradient(135deg, #10B981, #34D399);
	border-color: #10B981;
}

.rag-text {
	font-size: 26rpx;
	font-weight: 600;
	color: #059669;
}

.rag-toggle.on .rag-text {
	color: #FFFFFF;
}

.rag-dot {
	width: 16rpx;
	height: 16rpx;
	border-radius: 50%;
	background: #CBD5E1;
	margin-left: 12rpx;
}

.rag-toggle.on .rag-dot {
	background: #FFFFFF;
}

.msg-list {
	flex: 1;
	min-height: 0;
	padding: 16rpx 0 8rpx;
	box-sizing: border-box;
}

/* 滚动到底部的锚点占位 */
.bottom-anchor {
	height: 16rpx;
}

/* 空状态：垂直水平居中 */
.empty-area {
	flex: 1;
	min-height: 0;
	display: flex;
	align-items: center;
	justify-content: center;
}

.empty-tip {
	display: flex;
	flex-direction: column;
	align-items: center;
	color: #94A3B8;
}

.empty-icon-wrap {
	width: 128rpx;
	height: 128rpx;
	border-radius: 50%;
	background: rgba(16, 185, 129, 0.08);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 32rpx;
}

.empty-icon {
	width: 64rpx;
	height: 64rpx;
	opacity: 0.6;
}

.empty-title {
	font-size: 30rpx;
	color: #94A3B8;
}

.empty-sub {
	font-size: 24rpx;
	margin-top: 16rpx;
	color: #CBD5E1;
}

.input-area {
	padding: 16rpx 20rpx calc(20rpx + env(safe-area-inset-bottom));
	background: rgba(255, 255, 255, 0.9);
	border-top: 1rpx solid rgba(16, 185, 129, 0.15);
}

.input-row {
	display: flex;
	align-items: flex-end;
}

.chat-input {
	flex: 1;
	min-height: 88rpx;
	padding: 0 28rpx;
	border-radius: 28rpx;
	box-sizing: border-box;
	display: flex;
	align-items: center;
}

.chat-textarea {
	width: 100%;
	min-height: 44rpx;
	font-size: 30rpx;
	line-height: 44rpx;
	max-height: 200rpx;
}

.mic-btn {
	width: 88rpx;
	height: 88rpx;
	border-radius: 24rpx;
	background: rgba(255, 255, 255, 0.9);
	border: 1rpx solid rgba(16, 185, 129, 0.3);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-left: 16rpx;
	flex-shrink: 0;
	transition: all 0.2s ease;
}

.mic-btn.recording {
	background: #ef4444;
	border-color: #ef4444;
	animation: pulse 1s infinite;
}

@keyframes pulse {
	0%, 100% {
		box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4);
	}
	50% {
		box-shadow: 0 0 0 16rpx rgba(239, 68, 68, 0);
	}
}

.send-btn {
	width: 88rpx;
	height: 88rpx;
	border-radius: 24rpx;
	background: linear-gradient(135deg, #10B981, #34D399);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-left: 16rpx;
	flex-shrink: 0;
	box-shadow: 0 6rpx 16rpx rgba(16, 185, 129, 0.3);
	transition: all 0.2s ease;
}

.send-btn.sending {
	background: #64748B;
	box-shadow: none;
}

.btn-icon {
	width: 44rpx;
	height: 44rpx;
}
</style>
