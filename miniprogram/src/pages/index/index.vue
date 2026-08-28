<template>
	<view class="page">
		<!-- 品牌区 -->
		<view class="brand">
			<view class="brand-logo">知</view>
			<view class="brand-name">知问答</view>
			<view class="brand-desc">你的个人 AI 助手</view>
		</view>

		<!-- 应用入口卡片 -->
		<view class="apps">
			<view class="app-card glass-card" hover-class="app-card-hover" @tap="goKnowledge">
				<view class="app-icon icon-knowledge">
					<image class="icon" src="/static/icons/book.svg" mode="aspectFit" />
				</view>
				<view class="app-info">
					<view class="app-title">个人知识助手</view>
					<view class="app-desc">基于知识库的问答 · RAG 引用 · 语音</view>
				</view>
				<view class="app-arrow">
					<image class="icon" src="/static/icons/arrow-right.svg" mode="aspectFit" />
				</view>
			</view>

			<view class="app-card glass-card" hover-class="app-card-hover" @tap="goManus">
				<view class="app-icon icon-manus">
					<image class="icon" src="/static/icons/robot.svg" mode="aspectFit" />
				</view>
				<view class="app-info">
					<view class="app-title">AI 超级智能体</view>
					<view class="app-desc">多步骤任务执行 · 搜索 · 生成 PDF</view>
				</view>
				<view class="app-arrow">
					<image class="icon" src="/static/icons/arrow-right.svg" mode="aspectFit" />
				</view>
			</view>
		</view>

		<!-- 用户信息卡片 -->
		<view class="user-card glass-card">
			<view class="user-avatar">{{ avatarChar }}</view>
			<view class="user-info">
				<view class="user-name">{{ username || '未登录' }}</view>
				<view class="user-tip">个人测试环境</view>
			</view>
			<view class="user-actions">
				<view class="user-action" @tap="showServer = true">
					<image class="icon" src="/static/icons/gear.svg" mode="aspectFit" />
					<text class="action-text">服务器</text>
				</view>
				<view class="user-action" @tap="onLogout">
					<image class="icon" src="/static/icons/logout.svg" mode="aspectFit" />
					<text class="action-text">退出</text>
				</view>
			</view>
		</view>

		<server-config :visible="showServer" :value="serverUrl" @close="showServer = false" @save="onSaveServer" />
	</view>
</template>

<script setup>
/**
 * 应用中心首页：应用入口卡片 + 用户信息 + 服务器地址设置
 */
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getUsername, isLoggedIn, removeToken } from '../../utils/auth'
import { logout } from '../../utils/api'
import { getBaseURL, setBaseURL } from '../../utils/request'
import serverConfig from '../../components/server-config.vue'

const username = ref('')
const serverUrl = ref('')
const showServer = ref(false)

const avatarChar = computed(() => (username.value ? username.value.charAt(0).toUpperCase() : '知'))

onLoad(() => {
	username.value = getUsername()
	serverUrl.value = getBaseURL()
})

onShow(() => {
	username.value = getUsername()
	serverUrl.value = getBaseURL()
	if (!isLoggedIn()) {
		uni.reLaunch({ url: '/pages/login/login' })
	}
})

function goKnowledge() {
	uni.navigateTo({ url: '/pages/knowledge/knowledge' })
}

function goManus() {
	uni.navigateTo({ url: '/pages/manus/manus' })
}

function onSaveServer(url) {
	setBaseURL(url)
	serverUrl.value = url
	showServer.value = false
	uni.showToast({ title: '服务器地址已保存', icon: 'none' })
}

function onLogout() {
	uni.showModal({
		title: '退出登录',
		content: '确认退出当前账号？',
		success: (res) => {
			if (!res.confirm) return
			logout().catch(() => {
				// 登出接口失败不阻塞本地登出
			})
			removeToken()
			uni.reLaunch({ url: '/pages/login/login' })
		}
	})
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	padding: 0 40rpx 60rpx;
	box-sizing: border-box;
	background: linear-gradient(180deg, #ECFDF5 0%, #D1FAE5 100%);
}

.brand {
	padding-top: 100rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.brand-logo {
	width: 128rpx;
	height: 128rpx;
	border-radius: 36rpx;
	background: linear-gradient(135deg, #10B981, #34D399);
	color: #FFFFFF;
	font-size: 64rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 16rpx 40rpx rgba(16, 185, 129, 0.35);
}

.brand-name {
	font-size: 44rpx;
	font-weight: 700;
	color: #064E3B;
	margin-top: 24rpx;
	letter-spacing: 4rpx;
}

.brand-desc {
	font-size: 26rpx;
	color: #059669;
	margin-top: 10rpx;
}

.apps {
	margin-top: 60rpx;
}

.app-card {
	display: flex;
	align-items: center;
	padding: 32rpx 28rpx;
	margin-bottom: 28rpx;
	transition: transform 0.2s ease;
}

.app-card-hover {
	transform: scale(0.98);
}

.app-icon {
	width: 96rpx;
	height: 96rpx;
	border-radius: 24rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.icon-knowledge {
	background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
}

.icon-manus {
	background: linear-gradient(135deg, #F0FDF4, #D1FAE5);
}

.icon {
	width: 48rpx;
	height: 48rpx;
}

.app-info {
	flex: 1;
	min-width: 0;
	margin-left: 24rpx;
}

.app-title {
	font-size: 32rpx;
	font-weight: 700;
	color: #064E3B;
}

.app-desc {
	font-size: 24rpx;
	color: #64748B;
	margin-top: 8rpx;
}

.app-arrow {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	background: #ECFDF5;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.app-arrow .icon {
	width: 32rpx;
	height: 32rpx;
}

.user-card {
	margin-top: 40rpx;
	display: flex;
	align-items: center;
	padding: 28rpx;
}

.user-avatar {
	width: 88rpx;
	height: 88rpx;
	border-radius: 50%;
	background: #ECFDF5;
	color: #10B981;
	font-size: 40rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.user-info {
	flex: 1;
	min-width: 0;
	margin-left: 20rpx;
}

.user-name {
	font-size: 30rpx;
	font-weight: 600;
	color: #064E3B;
}

.user-tip {
	font-size: 22rpx;
	color: #94A3B8;
	margin-top: 4rpx;
}

.user-actions {
	display: flex;
	flex-shrink: 0;
}

.user-action {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-left: 28rpx;
}

.user-action .icon {
	width: 40rpx;
	height: 40rpx;
}

.action-text {
	font-size: 20rpx;
	color: #64748B;
	margin-top: 6rpx;
}
</style>
