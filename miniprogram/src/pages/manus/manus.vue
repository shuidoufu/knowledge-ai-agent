<template>
	<view class="page">
		<view class="toolbar">
			<view class="tool-btn icon-btn" @tap="goHome">
				<image class="tool-icon" src="/static/icons/back.svg" mode="aspectFit" />
			</view>
			<view class="tool-title">AI 超级智能体</view>
			<view class="tool-status">{{ sending ? '执行中...' : '' }}</view>
		</view>

		<scroll-view v-if="messages.length" class="msg-list" scroll-y :scroll-into-view="scrollInto" scroll-with-animation>
			<view v-for="msg in messages" :key="msg.id">
				<chat-bubble :msg="msg" />
			</view>
			<!-- 底部锚点：始终置于列表末尾，scroll-into-view 滚到锚点即滚到最底 -->
			<view id="bottom-anchor" class="bottom-anchor"></view>
		</scroll-view>
		<!-- 空状态：垂直居中 -->
		<view v-else class="empty-area">
			<view class="empty-tip">
				<view class="empty-icon-wrap">
					<image class="empty-icon" src="/static/icons/robot.svg" mode="aspectFit" />
				</view>
				<text class="empty-title">AI 超级智能体</text>
				<text class="empty-sub">支持多步骤任务执行（搜索、文件处理、生成 PDF 等）</text>
			</view>
		</view>

		<view class="input-area">
			<view class="input-row">
				<view class="input-wrap chat-input">
					<textarea
						class="chat-textarea"
						v-model="input"
						placeholder="输入任务..."
						placeholder-style="color:#94A3B8"
						:disabled="sending"
						auto-height
						confirm-type="send"
						@confirm="send"
					/>
				</view>
				<view class="send-btn" :class="{ sending: sending }" @tap="sending ? stop() : send()">
					<image class="btn-icon" :src="sending ? '/static/icons/stop-white.svg' : '/static/icons/send-white.svg'" mode="aspectFit" />
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
/**
 * Manus 聊天页：纯流式聊天（无历史、无 RAG、无语音）
 */
import { ref, nextTick } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { isLoggedIn } from '../../utils/auth'
import { generateMsgId } from '../../utils/chat'
import { streamManusChat } from '../../utils/api'
import { stopSpeech } from '../../utils/tts'
import chatBubble from '../../components/chat-bubble.vue'

const messages = ref([])
const input = ref('')
const sending = ref(false)
const scrollInto = ref('')

let streamTask = null
let scrollTimer = null

onLoad(() => {
	if (!isLoggedIn()) {
		uni.reLaunch({ url: '/pages/login/login' })
	}
})

onShow(() => {
	if (!isLoggedIn()) {
		uni.reLaunch({ url: '/pages/login/login' })
	}
})

onUnload(() => {
	stop()
	stopSpeech()
})

/** 返回应用中心首页 */
function goHome() {
	stop()
	stopSpeech()
	uni.reLaunch({ url: '/pages/index/index' })
}

/**
 * 滚动到底部：固定底部锚点 + 节流 + 清空再设置（锚点恒定不触发，需重置）
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

function send() {
	const text = input.value.trim()
	if (!text || sending.value) return
	// 后端流式接口是 GET，超长文本会命中 Tomcat 8KB 请求头上限
	if (text.length > 4000) {
		uni.showToast({ title: '任务过长，请分段输入（≤4000字）', icon: 'none' })
		return
	}
	input.value = ''
	messages.value.push({ id: generateMsgId(), role: 'user', content: text, references: [], loading: false })
	messages.value.push({ id: generateMsgId(), role: 'assistant', content: '', references: [], loading: true })
	sending.value = true
	scrollToBottom()

	streamTask = streamManusChat(text, {
		onChunk: (chunk) => {
			const last = messages.value[messages.value.length - 1]
			if (last) {
				last.content += chunk
				scrollToBottom()
			}
		},
		onDone: () => {
			const last = messages.value[messages.value.length - 1]
			if (last) {
				last.loading = false
				// 发送后立即停止产生的空气泡：无内容则移除
				if (!last.content) {
					messages.value.pop()
				}
			}
			sending.value = false
			streamTask = null
		},
		onError: (err) => {
			const last = messages.value[messages.value.length - 1]
			if (last) {
				last.content = '执行失败：' + ((err && err.message) || '网络错误')
				last.loading = false
			}
			sending.value = false
			streamTask = null
		}
	})
}

function stop() {
	if (streamTask) {
		try {
			streamTask.abort()
		} catch (e) {
			// 已结束则忽略
		}
		streamTask = null
	}
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
	width: 88rpx;
	background: rgba(255, 255, 255, 0.75);
	border: 1rpx solid rgba(16, 185, 129, 0.3);
	border-radius: 24rpx;
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
}

.tool-status {
	font-size: 24rpx;
	color: #059669;
	min-width: 120rpx;
	text-align: right;
}

.msg-list {
	flex: 1;
	min-height: 0;
	padding: 16rpx 0 8rpx;
	box-sizing: border-box;
}

.bottom-anchor {
	height: 16rpx;
}

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
	padding: 16rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
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
