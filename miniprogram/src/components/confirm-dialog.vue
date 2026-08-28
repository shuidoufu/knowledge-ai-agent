<template>
	<view v-if="visible" class="mask" @tap="onCancel">
		<view class="dialog glass-card" @tap.stop>
			<view class="dialog-title">{{ title }}</view>
			<view v-if="content" class="dialog-content">{{ content }}</view>
			<view class="dialog-btns">
				<view class="btn cancel" @tap="onCancel">{{ cancelText }}</view>
				<view class="btn confirm" :class="danger ? 'danger' : 'green'" @tap="onConfirm">{{ confirmText }}</view>
			</view>
		</view>
	</view>
</template>

<script setup>
/**
 * 通用确认弹窗：删除/批量删除等危险操作统一走这里
 * props: visible, title, content, confirmText, cancelText, danger
 * emits: confirm, cancel
 */
const props = defineProps({
	visible: { type: Boolean, default: false },
	title: { type: String, default: '提示' },
	content: { type: String, default: '' },
	confirmText: { type: String, default: '确认' },
	cancelText: { type: String, default: '取消' },
	danger: { type: Boolean, default: false }
})

const emit = defineEmits(['confirm', 'cancel'])

function onConfirm() {
	emit('confirm')
}

function onCancel() {
	emit('cancel')
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
	width: 580rpx;
	background: #FFFFFF;
	padding: 40rpx 36rpx 32rpx;
}

.dialog-title {
	font-size: 32rpx;
	font-weight: 700;
	color: #064E3B;
	text-align: left;
}

.dialog-content {
	font-size: 28rpx;
	color: #64748B;
	margin-top: 20rpx;
	text-align: left;
	line-height: 1.6;
}

.dialog-btns {
	display: flex;
	margin-top: 40rpx;
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

.btn.confirm.green {
	background: #10B981;
	color: #FFFFFF;
}

.btn.confirm.danger {
	background: #ef4444;
	color: #FFFFFF;
}
</style>
