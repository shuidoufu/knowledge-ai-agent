<template>
	<view class="page">
		<view class="brand">
			<view class="brand-logo">知</view>
			<view class="brand-name">知问答</view>
			<view class="brand-desc">个人知识助手</view>
		</view>

		<!-- 服务器地址设置入口 -->
		<view class="server-row" @tap="showServerConfig = true">
			<text class="server-label">服务器：</text>
			<text class="server-value">{{ serverUrl || '未配置（点击设置）' }}</text>
			<text class="server-edit">修改</text>
		</view>

		<view class="form glass-card">
			<view class="tab-row">
				<view class="tab" :class="{ active: mode === 'login' }" @tap="mode = 'login'">登录</view>
				<view class="tab" :class="{ active: mode === 'register' }" @tap="switchRegister">注册</view>
			</view>

			<view class="input-wrap field">
				<input class="input" v-model="username" placeholder="用户名" placeholder-style="color:#94A3B8" />
			</view>
			<view class="input-wrap field">
				<input class="input" v-model="password" password placeholder="密码" placeholder-style="color:#94A3B8" />
			</view>
			<view v-if="mode === 'register'" class="input-wrap field">
				<input class="input" v-model="confirmPassword" password placeholder="确认密码" placeholder-style="color:#94A3B8" />
			</view>
			<!-- 注册需要图形验证码 -->
			<view v-if="mode === 'register'" class="captcha-row">
				<view class="input-wrap captcha-input">
					<input class="input" v-model="captchaCode" placeholder="验证码" placeholder-style="color:#94A3B8" />
				</view>
				<image v-if="captchaImage" class="captcha-img" :src="captchaImage" mode="aspectFit" @tap="refreshCaptcha" />
			</view>

			<button class="btn-primary submit" :disabled="submitting" @tap="onSubmit">
				{{ submitting ? '处理中...' : mode === 'login' ? '登 录' : '注 册' }}
			</button>
		</view>

		<view class="tip">个人测试用小程序 · 后端地址可在上方配置</view>

		<server-config :visible="showServerConfig" :value="serverUrl" @close="showServerConfig = false" @save="onSaveServer" />
	</view>
</template>

<script setup>
/**
 * 登录 / 注册二合一页（注册需图形验证码），服务器地址设置入口
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { login, register, getCaptcha } from '../../utils/api'
import { setToken, isLoggedIn } from '../../utils/auth'
import { getBaseURL, setBaseURL } from '../../utils/request'
import serverConfig from '../../components/server-config.vue'

const mode = ref('login')
const username = ref('admin')
const password = ref('admin')
const confirmPassword = ref('')
const captchaKey = ref('')
const captchaCode = ref('')
const captchaImage = ref('')
const submitting = ref(false)
const serverUrl = ref('')
const showServerConfig = ref(false)

onLoad(() => {
	serverUrl.value = getBaseURL()
	// 已登录直接进入首页
	if (isLoggedIn()) {
		uni.reLaunch({ url: '/pages/index/index' })
	}
})

async function refreshCaptcha() {
	try {
		const data = await getCaptcha()
		captchaKey.value = data.captchaKey
		captchaImage.value = data.captchaImage
	} catch (e) {
		uni.showToast({ title: '验证码加载失败', icon: 'none' })
	}
}

async function switchRegister() {
	mode.value = 'register'
	if (!captchaImage.value) {
		await refreshCaptcha()
	}
}

function onSaveServer(url) {
	setBaseURL(url)
	serverUrl.value = url
	showServerConfig.value = false
	uni.showToast({ title: '服务器地址已保存', icon: 'none' })
}

function validate() {
	if (!username.value.trim()) {
		uni.showToast({ title: '请输入用户名', icon: 'none' })
		return false
	}
	if (!password.value) {
		uni.showToast({ title: '请输入密码', icon: 'none' })
		return false
	}
	if (mode.value === 'register') {
		if (!confirmPassword.value || confirmPassword.value !== password.value) {
			uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
			return false
		}
		if (!captchaCode.value.trim()) {
			uni.showToast({ title: '请输入验证码', icon: 'none' })
			return false
		}
	}
	return true
}

async function onSubmit() {
	if (!serverUrl.value) {
		uni.showToast({ title: '请先配置服务器地址', icon: 'none' })
		return
	}
	if (!validate() || submitting.value) return
	submitting.value = true
	try {
		let data
		if (mode.value === 'login') {
			data = await login(username.value.trim(), password.value)
		} else {
			data = await register({
				username: username.value.trim(),
				password: password.value,
				captchaKey: captchaKey.value,
				captchaCode: captchaCode.value.trim()
			})
		}
		setToken(data.token, data.username)
		uni.reLaunch({ url: '/pages/index/index' })
	} catch (e) {
		const msg = (e && e.message) || '操作失败'
		uni.showToast({ title: msg, icon: 'none' })
		if (mode.value === 'register' && /验证码|用户名/.test(msg)) {
			refreshCaptcha()
		}
	} finally {
		submitting.value = false
	}
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 0 48rpx;
	box-sizing: border-box;
	background: linear-gradient(180deg, #ECFDF5 0%, #D1FAE5 100%);
}

.brand {
	margin-top: 120rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.brand-logo {
	width: 120rpx;
	height: 120rpx;
	border-radius: 32rpx;
	background: linear-gradient(135deg, #10B981, #34D399);
	color: #FFFFFF;
	font-size: 60rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 12rpx 32rpx rgba(16, 185, 129, 0.3);
}

.brand-name {
	font-size: 44rpx;
	font-weight: 700;
	color: #064E3B;
	margin-top: 24rpx;
}

.brand-desc {
	font-size: 26rpx;
	color: #059669;
	margin-top: 8rpx;
}

.server-row {
	margin-top: 40rpx;
	background: rgba(255, 255, 255, 0.7);
	border: 1rpx solid rgba(16, 185, 129, 0.2);
	border-radius: 999rpx;
	padding: 16rpx 28rpx;
	display: flex;
	align-items: center;
	font-size: 24rpx;
	max-width: 90%;
}

.server-label {
	color: #64748B;
	flex-shrink: 0;
}

.server-value {
	color: #064E3B;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	flex: 1;
}

.server-edit {
	color: #059669;
	font-weight: 600;
	margin-left: 16rpx;
	flex-shrink: 0;
}

.form {
	width: 100%;
	margin-top: 48rpx;
	padding: 48rpx 40rpx;
	box-sizing: border-box;
}

.tab-row {
	display: flex;
	margin-bottom: 40rpx;
}

.tab {
	flex: 1;
	text-align: center;
	font-size: 32rpx;
	color: #94A3B8;
	padding-bottom: 16rpx;
	border-bottom: 4rpx solid transparent;
}

.tab.active {
	color: #059669;
	font-weight: 700;
	border-bottom-color: #10B981;
}

.field {
	margin-bottom: 28rpx;
}

.captcha-row {
	display: flex;
	margin-bottom: 28rpx;
}

.captcha-input {
	flex: 1;
	margin-right: 20rpx;
}

.captcha-img {
	width: 200rpx;
	height: 88rpx;
	border-radius: 16rpx;
	background: #F8FAFC;
	border: 1rpx solid rgba(16, 185, 129, 0.2);
}

.submit {
	margin-top: 16rpx;
	height: 92rpx;
	line-height: 92rpx;
}

.tip {
	margin-top: 48rpx;
	font-size: 22rpx;
	color: #94A3B8;
}
</style>
