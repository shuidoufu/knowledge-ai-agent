<script>
export default {
	onLaunch: function () {
	},
	onShow: function () {
	},
	onHide: function () {
	},
}
</script>

<style>
/* 全局公共样式：配色对齐 Web 端（主色 #10B981 翠绿系） */
page {
	background-color: #ECFDF5;
	color: #064E3B;
	font-size: 28rpx;
	line-height: 1.6;
}

/* 主按钮（绿色渐变胶囊） */
.btn-primary {
	background: linear-gradient(135deg, #10B981, #34D399);
	color: #FFFFFF;
	border-radius: 999rpx;
	font-size: 30rpx;
	font-weight: 600;
	letter-spacing: 2rpx;
	box-shadow: 0 8rpx 24rpx rgba(16, 185, 129, 0.25);
}

.btn-primary[disabled] {
	opacity: 0.6;
}

/* 次要按钮 */
.btn-secondary {
	background: #FFFFFF;
	color: #064E3B;
	border-radius: 999rpx;
	font-size: 30rpx;
	border: 1rpx solid rgba(16, 185, 129, 0.3);
}

/* 危险按钮 */
.btn-danger {
	background: #ef4444;
	color: #FFFFFF;
	border-radius: 999rpx;
	font-size: 30rpx;
}

/* 玻璃拟态卡片 */
.glass-card {
	background: rgba(255, 255, 255, 0.75);
	backdrop-filter: blur(16rpx);
	border-radius: 24rpx;
	border: 1rpx solid rgba(255, 255, 255, 0.5);
	box-shadow: 0 8rpx 32rpx rgba(16, 185, 129, 0.1);
}

/* 输入框容器 */
.input-wrap {
	background: #FFFFFF;
	border-radius: 999rpx;
	border: 1rpx solid rgba(16, 185, 129, 0.25);
	padding: 0 32rpx;
	height: 88rpx;
	display: flex;
	align-items: center;
}

.input-wrap .input {
	flex: 1;
	font-size: 30rpx;
}

/* 弹窗遮罩 */
.mask {
	position: fixed;
	left: 0;
	top: 0;
	right: 0;
	bottom: 0;
	background: rgba(6, 78, 59, 0.4);
	z-index: 100;
}

/* 提示条 */
.toast-text {
	color: #FFFFFF;
	background: rgba(6, 78, 59, 0.85);
	padding: 16rpx 32rpx;
	border-radius: 999rpx;
	font-size: 26rpx;
}
</style>
