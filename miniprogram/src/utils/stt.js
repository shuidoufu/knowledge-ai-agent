import { uploadStt } from './api'

/**
 * 语音输入 STT：录音 → 封装 16kHz 单声道 16bit WAV → 上传后端识别。
 * 平台差异：Android 录音输出 PCM（基础库 2.20.1+），需自封装 WAV 头；
 * iOS 尝试 format:'wav' 直出（采样率/位深需真机验证，不满足时降级同 PCM 方案）。
 * 注意：微信开发者工具模拟器不支持录音，必须在真机上测试。
 */

let recorder = null
let state = 'idle' // idle | recording
let platform = ''

function getPlatform() {
	if (!platform) {
		platform = (uni.getSystemInfoSync().platform || '').toLowerCase()
	}
	return platform
}

function isIOS() {
	return getPlatform() === 'ios'
}

/** 44 字节 WAV 头 + PCM 数据 */
function wrapWav(pcmArrayBuffer, sampleRate, channels, bitsPerSample) {
	const pcm = new Uint8Array(pcmArrayBuffer)
	const dataLength = pcm.length
	const buffer = new ArrayBuffer(44 + dataLength)
	const view = new DataView(buffer)
	const writeStr = (offset, str) => {
		for (let i = 0; i < str.length; i++) {
			view.setUint8(offset + i, str.charCodeAt(i))
		}
	}
	const byteRate = (sampleRate * channels * bitsPerSample) / 8
	const blockAlign = (channels * bitsPerSample) / 8
	writeStr(0, 'RIFF')
	view.setUint32(4, 36 + dataLength, true)
	writeStr(8, 'WAVE')
	writeStr(12, 'fmt ')
	view.setUint32(16, 16, true)
	view.setUint16(20, 1, true)
	view.setUint16(22, channels, true)
	view.setUint32(24, sampleRate, true)
	view.setUint32(28, byteRate, true)
	view.setUint16(32, blockAlign, true)
	view.setUint16(34, bitsPerSample, true)
	writeStr(36, 'data')
	view.setUint32(40, dataLength, true)
	new Uint8Array(buffer, 44).set(pcm)
	return buffer
}

function cleanup(filePath) {
	if (filePath) {
		try {
			uni.getFileSystemManager().unlinkSync(filePath)
		} catch (e) {
			// 文件不存在则忽略
		}
	}
}

/**
 * 开始录音。回调：onStart()、onStop(识别文本)、onError(提示信息)
 */
export function startRecording(onStart, onStop, onError) {
	if (state === 'recording') {
		return
	}
	const rm = uni.getRecorderManager()
	recorder = rm
	rm.onStop((res) => {
		state = 'idle'
		const filePath = res.tempFilePath
		if (!filePath) {
			onError && onError('没有录到声音，请重试')
			return
		}
		handleRecordedFile(filePath, onStop, onError)
	})
	rm.onError((err) => {
		state = 'idle'
		onError && onError('录音失败：开发者工具模拟器不支持录音，请在真机上使用语音输入')
	})
	const options = {
		duration: 60000,
		sampleRate: 16000,
		numberOfChannels: 1,
		format: isIOS() ? 'wav' : 'PCM'
	}
	// 注意：不设 encodeBitRate——微信限制其范围 24000-96000，
	// 且 PCM/WAV 为无压缩格式，码率由采样率与位深决定，该参数不适用
	rm.start(options)
	state = 'recording'
	onStart && onStart()
}

/** 停止录音并触发上传识别 */
export function stopRecording() {
	if (recorder && state === 'recording') {
		recorder.stop()
	}
}

export function isRecording() {
	return state === 'recording'
}

function handleRecordedFile(filePath, onStop, onError) {
	const finish = (wavPath) => {
		uploadStt(wavPath)
			.then((res) => {
				cleanup(wavPath)
				onStop && onStop((res && res.text) || '')
			})
			.catch((err) => {
				cleanup(wavPath)
				onError && onError((err && err.message) || '语音识别失败')
			})
	}
	if (isIOS()) {
		// iOS wav 直出：采样率/位深需真机验证是否符合 16kHz 16bit 单声道
		finish(filePath)
		return
	}
	// Android PCM：封装 WAV 头后上传
	try {
		const pcm = uni.getFileSystemManager().readFileSync(filePath)
		const wavPath = uni.env.USER_DATA_PATH + '/stt_' + Date.now() + '.wav'
		uni.getFileSystemManager().writeFileSync(wavPath, wrapWav(pcm, 16000, 1, 16))
		cleanup(filePath)
		finish(wavPath)
	} catch (e) {
		cleanup(filePath)
		onError && onError('录音文件处理失败')
	}
}
