<template>
	<view v-if="visible" class="panel-mask" @tap="close">
		<view class="panel glass-card" @tap.stop>
			<view class="panel-header">
				<text class="panel-title">历史对话</text>
				<view class="panel-close" @tap="close">
					<image class="panel-close-icon" src="/static/icons/close.svg" mode="aspectFit" />
				</view>
			</view>

			<scroll-view scroll-y class="panel-list">
				<view
					v-for="chat in historyList"
					:key="chat.chatId"
					class="history-item"
					:class="{ active: chat.chatId === currentChatId }"
					@tap="onItemTap(chat)"
					@longpress="openMenu(chat, $event)"
				>
					<!-- 批量模式圆圈勾选 -->
					<view v-if="batchMode" class="check-circle" :class="{ checked: isSelected(chat.chatId) }">
						<text v-if="isSelected(chat.chatId)" class="check-mark">✓</text>
					</view>
					<view class="item-info">
						<view class="item-title">{{ chat.title || '新对话' }}</view>
						<view class="item-time">{{ formatTime(chat.updatedAt || chat.createdAt) }}</view>
					</view>
					<view v-if="!batchMode" class="item-more" @tap.stop="openMenu(chat, $event)">
						<image class="item-more-icon" src="/static/icons/more.svg" mode="aspectFit" />
					</view>
				</view>
				<view v-if="!historyList.length" class="empty">暂无历史对话</view>
			</scroll-view>

			<!-- 批量模式底部按钮条 -->
			<view v-if="batchMode" class="batch-bar">
				<view class="batch-btn" @tap="exitBatchMode">取消</view>
				<view class="batch-btn delete" :class="{ disabled: !selectedChatIds.length }" @tap="onBatchDelete">
					删除 ({{ selectedChatIds.length }})
				</view>
			</view>
		</view>

		<!-- 三点菜单遮罩：点击任意处先关闭菜单（不动面板） -->
		<view v-if="menuChat" class="menu-mask" @tap.stop="closeMenu"></view>
		<!-- 三点菜单（悬浮） -->
		<view v-if="menuChat" class="menu" :style="{ top: menuTop + 'px' }" @tap.stop>
			<view class="menu-item" @tap="onBatchManage">
				<image class="menu-icon" src="/static/icons/list-checks.svg" mode="aspectFit" />
				<text>批量管理</text>
			</view>
			<view class="menu-item" @tap="onRename">
				<image class="menu-icon" src="/static/icons/pencil.svg" mode="aspectFit" />
				<text>修改标题</text>
			</view>
			<view class="menu-item danger" @tap="onDeleteOne">
				<image class="menu-icon" src="/static/icons/trash-red.svg" mode="aspectFit" />
				<text>删除对话</text>
			</view>
		</view>

		<!-- 修改标题弹窗 -->
		<view v-if="editVisible" class="mask" @tap="editVisible = false">
			<view class="dialog glass-card" @tap.stop>
				<view class="dialog-title">修改标题</view>
				<view class="input-wrap edit-input">
					<input class="input" v-model="editTitle" placeholder="输入新标题" placeholder-style="color:#94A3B8" />
				</view>
				<view class="dialog-btns">
					<view class="btn cancel" @tap="editVisible = false">取消</view>
					<view class="btn green" @tap="onSaveTitle">确定</view>
				</view>
			</view>
		</view>

		<!-- 删除确认 -->
		<confirm-dialog
			:visible="delConfirmVisible"
			title="删除对话记录"
			content="删除后内容将无法恢复，确认删除选中记录？"
			confirm-text="删除"
			danger
			@confirm="doDelete"
			@cancel="delConfirmVisible = false"
		/>
	</view>
</template>

<script setup>
/**
 * 历史会话弹层：列表切换 / 三点菜单（批量管理、修改标题、删除）/
 * 批量管理模式（圆圈勾选 + 底部按钮条）对齐 Web 端交互
 * emits: close, select(chatId), deleted-current
 */
import { ref, watch } from 'vue'
import { fetchHistory, updateChatTitle, batchDeleteChats } from '../utils/api'
import confirmDialog from './confirm-dialog.vue'

const props = defineProps({
	visible: { type: Boolean, default: false },
	currentChatId: { type: String, default: '' }
})

const emit = defineEmits(['close', 'select', 'deleted-current'])

const historyList = ref([])
const batchMode = ref(false)
const selectedChatIds = ref([])
const menuChat = ref(null)
const menuTop = ref(0)
const editVisible = ref(false)
const editTitle = ref('')
const editChatId = ref('')
const deleteChatId = ref('')
const delConfirmVisible = ref(false)
const deleting = ref(false)

watch(
	() => props.visible,
	(v) => {
		if (v) {
			loadHistory()
		}
	}
)

async function loadHistory() {
	try {
		historyList.value = await fetchHistory()
	} catch (e) {
		uni.showToast({ title: (e && e.message) || '加载历史失败', icon: 'none' })
	}
}

function close() {
	closeMenu()
	exitBatchMode()
	emit('close')
}

function isSelected(id) {
	return selectedChatIds.value.indexOf(id) >= 0
}

function onItemTap(chat) {
	if (batchMode.value) {
		toggleSelect(chat.chatId)
		return
	}
	emit('select', chat.chatId)
}

function toggleSelect(id) {
	const idx = selectedChatIds.value.indexOf(id)
	if (idx >= 0) {
		selectedChatIds.value.splice(idx, 1)
	} else {
		selectedChatIds.value.push(id)
	}
}

/** 三点菜单：弹出位置贴近点击项，下方空间不足时向上展开 */
function openMenu(chat, event) {
	menuChat.value = chat
	selectedChatIds.value = [chat.chatId]
	const touchY = event && event.touches && event.touches.length ? event.touches[0].clientY : 0
	const sys = uni.getSystemInfoSync()
	const windowH = sys.windowHeight || 600
	const menuH = 200
	if (touchY && touchY + menuH > windowH) {
		menuTop.value = Math.max(windowH - menuH - 20, 80)
	} else {
		menuTop.value = touchY || 160
	}
}

function closeMenu() {
	menuChat.value = null
}

function onBatchManage() {
	selectedChatIds.value = []
	batchMode.value = true
	menuChat.value = null
}

function exitBatchMode() {
	batchMode.value = false
	selectedChatIds.value = []
}

function onRename() {
	editChatId.value = menuChat.value.chatId
	editTitle.value = menuChat.value.title || ''
	editVisible.value = true
	menuChat.value = null
}

async function onSaveTitle() {
	const title = editTitle.value.trim()
	if (!title) {
		uni.showToast({ title: '标题不能为空', icon: 'none' })
		return
	}
	try {
		await updateChatTitle(editChatId.value, title)
		editVisible.value = false
		editChatId.value = ''
		uni.showToast({ title: '标题已更新', icon: 'none' })
		loadHistory()
	} catch (e) {
		uni.showToast({ title: (e && e.message) || '修改失败', icon: 'none' })
	}
}

function onDeleteOne() {
	deleteChatId.value = menuChat.value.chatId
	menuChat.value = null
	delConfirmVisible.value = true
}

async function doDelete() {
	if (deleting.value) return
	deleting.value = true
	try {
		// 单项删除或批量删除统一走批量接口，避免并发
		const ids = batchMode.value ? selectedChatIds.value : [deleteChatId.value]
		await batchDeleteChats(ids)
		delConfirmVisible.value = false
		const deletedCurrent = ids.indexOf(props.currentChatId) >= 0
		exitBatchMode()
		deleteChatId.value = ''
		uni.showToast({ title: '已删除', icon: 'none' })
		if (deletedCurrent) {
			emit('deleted-current')
		}
		loadHistory()
	} catch (e) {
		delConfirmVisible.value = false
		exitBatchMode()
		deleteChatId.value = ''
		uni.showToast({ title: (e && e.message) || '删除失败', icon: 'none' })
	} finally {
		deleting.value = false
	}
}

function onBatchDelete() {
	if (!selectedChatIds.value.length) return
	delConfirmVisible.value = true
}

function formatTime(t) {
	if (!t) return ''
	const d = new Date(t)
	const pad = (n) => (n < 10 ? '0' + n : '' + n)
	return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}
</script>

<style scoped>
.panel-mask {
	position: fixed;
	left: 0;
	top: 0;
	right: 0;
	bottom: 0;
	background: rgba(6, 78, 59, 0.4);
	z-index: 150;
}

.panel {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	height: 70vh;
	background: rgba(255, 255, 255, 0.96);
	border-radius: 32rpx 32rpx 0 0;
	display: flex;
	flex-direction: column;
	padding-bottom: env(safe-area-inset-bottom);
}

.panel-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 28rpx 32rpx;
	border-bottom: 1rpx solid rgba(16, 185, 129, 0.15);
}

.panel-title {
	font-size: 32rpx;
	font-weight: 700;
	color: #064E3B;
}

.panel-close {
	width: 88rpx;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin: -16rpx -16rpx 0 0;
}

.panel-close-icon {
	width: 36rpx;
	height: 36rpx;
}

.panel-list {
	flex: 1;
	min-height: 0;
	padding: 8rpx 0;
}

.history-item {
	display: flex;
	align-items: center;
	padding: 24rpx 32rpx;
	border-bottom: 1rpx solid rgba(16, 185, 129, 0.08);
}

.history-item.active {
	background: #ECFDF5;
}

.check-circle {
	width: 40rpx;
	height: 40rpx;
	border-radius: 50%;
	border: 2rpx solid #CBD5E1;
	margin-right: 20rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.check-circle.checked {
	background: #111827;
	border-color: #111827;
}

.check-mark {
	color: #FFFFFF;
	font-size: 24rpx;
}

.item-info {
	flex: 1;
	min-width: 0;
}

.item-title {
	font-size: 28rpx;
	color: #064E3B;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.item-time {
	font-size: 22rpx;
	color: #94A3B8;
	margin-top: 6rpx;
}

.item-more {
	width: 88rpx;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: -16rpx;
	flex-shrink: 0;
}

.item-more-icon {
	width: 40rpx;
	height: 40rpx;
}

.empty {
	text-align: center;
	color: #94A3B8;
	font-size: 26rpx;
	padding: 80rpx 0;
}

.batch-bar {
	display: flex;
	padding: 20rpx 32rpx;
	border-top: 1rpx solid rgba(16, 185, 129, 0.15);
	background: rgba(245, 247, 250, 0.9);
}

.batch-btn {
	flex: 1;
	height: 80rpx;
	line-height: 80rpx;
	text-align: center;
	border-radius: 16rpx;
	font-size: 30rpx;
	background: #F8FAFC;
	color: #111827;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
}

.batch-btn.delete {
	color: #ef4444;
	margin-left: 20rpx;
}

.batch-btn.disabled {
	opacity: 0.4;
}

/* 三点菜单遮罩：透明全屏，点击先关菜单 */
.menu-mask {
	position: fixed;
	left: 0;
	top: 0;
	right: 0;
	bottom: 0;
	z-index: 280;
	background: transparent;
}

/* 三点菜单 */
.menu {
	position: fixed;
	right: 24rpx;
	z-index: 300;
	background: #FFFFFF;
	border-radius: 20rpx;
	box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.18);
	overflow: hidden;
	min-width: 320rpx;
}

.menu-item {
	display: flex;
	align-items: center;
	height: 96rpx;
	padding: 0 32rpx;
	font-size: 28rpx;
	color: #064E3B;
	border-bottom: 1rpx solid rgba(16, 185, 129, 0.1);
}

.menu-item:last-child {
	border-bottom: none;
}

.menu-item.danger {
	color: #ef4444;
}

.menu-icon {
	width: 36rpx;
	height: 36rpx;
	margin-right: 20rpx;
}

/* 改标题弹窗 */
.mask {
	position: fixed;
	left: 0;
	top: 0;
	right: 0;
	bottom: 0;
	background: rgba(6, 78, 59, 0.45);
	z-index: 250;
	display: flex;
	align-items: center;
	justify-content: center;
}

.dialog {
	width: 600rpx;
	background: #FFFFFF;
	padding: 40rpx 36rpx 32rpx;
}

.dialog-title {
	font-size: 32rpx;
	font-weight: 700;
	color: #064E3B;
}

.edit-input {
	margin-top: 28rpx;
	height: 84rpx;
}

.input {
	font-size: 28rpx;
}

.dialog-btns {
	display: flex;
	margin-top: 36rpx;
}

.btn {
	flex: 1;
	height: 80rpx;
	line-height: 80rpx;
	text-align: center;
	border-radius: 999rpx;
	font-size: 30rpx;
}

.btn.cancel {
	background: #F1F5F9;
	color: #334155;
	margin-right: 20rpx;
}

.btn.green {
	background: #10B981;
	color: #FFFFFF;
}
</style>
