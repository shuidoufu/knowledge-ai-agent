import { fetchTts } from './api'

let audioContext = null
let currentFile = ''

/**
 * TTS 语音播报：后端返回 MP3 字节流，写入临时文件后播放。
 * 注意：TTS 模型费用较高，测试时用短文本、成功即止。
 */
export function playTTS(text) {
	return new Promise((resolve, reject) => {
		if (!text || !text.trim()) {
			reject({ message: '没有可播报的内容' })
			return
		}
		stopSpeech()
		fetchTts(text.slice(0, 20000))
			.then((arrayBuffer) => {
				const fs = uni.getFileSystemManager()
				currentFile = uni.env.USER_DATA_PATH + '/tts_' + Date.now() + '.mp3'
				fs.writeFileSync(currentFile, arrayBuffer)
				audioContext = uni.createInnerAudioContext()
				audioContext.src = currentFile
				let settled = false
				audioContext.onEnded(() => {
					cleanup()
					if (!settled) {
						settled = true
						resolve()
					}
				})
				audioContext.onError((err) => {
					cleanup()
					if (!settled) {
						settled = true
						reject({ message: '播报失败：' + (err && err.errMsg ? err.errMsg : '未知错误') })
					}
				})
				audioContext.onStop(() => {
					cleanup()
					if (!settled) {
						settled = true
						resolve()
					}
				})
				audioContext.play()
			})
			.catch((err) => {
				reject(err)
			})
	})
}

/** 停止当前播报并释放资源 */
export function stopSpeech() {
	if (audioContext) {
		try {
			audioContext.stop()
			audioContext.destroy()
		} catch (e) {
			// 已释放则忽略
		}
		audioContext = null
	}
	cleanup()
}

function cleanup() {
	if (currentFile) {
		try {
			uni.getFileSystemManager().unlinkSync(currentFile)
		} catch (e) {
			// 文件不存在则忽略
		}
		currentFile = ''
	}
}
