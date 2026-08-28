<template>
	<view v-if="visible" class="mask" @tap="onCancel">
		<view class="dialog glass-card" @tap.stop>
			<view class="dialog-title">服务器地址</view>
			<view class="dialog-desc">小程序无地址栏，需要配置后端地址。支持局域网 IP（http://192.168.x.x:8123/api）或 HTTPS 隧道域名。</view>
			<view class="input-wrap config-input">
				<input class="input" v-model="url" placeholder="例：http://192.168.1.100:8123/api" placeholder-style="color:#94A3B8" />
			</view>
			<view class="dialog-btns">
				<view class="btn cancel" @tap="onCancel">取消</view>
				<view class="btn green" @tap="onSave">保存</view>
			</view>
		</view>
	</view>
</template>

<script setup>
/**
 * 服务器地址设置弹窗：保存到本地，request.js 每次请求读取。
 * 上线转正式服务器后移除本弹窗入口即可（组件代码保留，不影响地址解析）
 */
import { ref, watch } from 'vue'
import { BASE_URL_EXAMPLE } from '../utils/config'

const props = defineProps({
	visible: { type: Boolean, default: false },
	value: { type: String, default: '' }
})

const emit = defineEmits(['close', 'save'])

const url = ref('')

watch(
	() => props.visible,
	(v) => {
		if (v) {
			url.value = props.value || ''
		}
	}
)

function onSave() {
	let u = url.value.trim()
	if (!u) {
		uni.showToast({ title: '请输入服务器地址', icon: 'none' })
		return
	}
	if (!/^https?:\/\//i.test(u)) {
		uni.showToast({ title: '地址需以 http:// 或 https:// 开头', icon: 'none' })
		return
	}
	// 自动补全 /api 后缀（后端 context-path），避免用户只填根地址导致请求 404
	if (!/\/api\/?$/.test(u)) {
		u = u.replace(/\/+$/, '') + '/api'
	}
	emit('save', u)
}

function onCancel() {
	emit('close')
}
</script>

<style scoped>
.mask {
	position: fixed;
	left: 0;
	top: 0;
	right: 0;
	bottom: 0;
	background: rgba(6, 78, 59, 0.45);
	z-index: 200;
	display: flex;
	align-items: center;
	justify-content: center;
}

.dialog {
	width: 620rpx;
	background: #FFFFFF;
	padding: 40rpx 36rpx 32rpx;
}

.dialog-title {
	font-size: 32rpx;
	font-weight: 700;
	color: #064E3B;
}

.dialog-desc {
	font-size: 24rpx;
	color: #64748B;
	margin-top: 16rpx;
	line-height: 1.6;
}

.config-input {
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
