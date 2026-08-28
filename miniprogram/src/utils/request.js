import { DEFAULT_BASE_URL, STORAGE_BASE_URL, STREAM_TIMEOUT } from './config'
import { getToken, removeToken } from './auth'

/**
 * 服务器地址两层解析：本地设置优先，未设置回退默认常量
 * 上线转正式服务器只改 config.js 的 DEFAULT_BASE_URL，此处无需变动
 */
export function getBaseURL() {
	const saved = uni.getStorageSync(STORAGE_BASE_URL)
	return saved || DEFAULT_BASE_URL
}

/** 服务器根地址（去掉末尾 /api，用于拼接后端返回的 /api/files/... 等根相对路径） */
export function getServerRoot() {
	return getBaseURL().replace(/\/api\/?$/, '')
}

export function setBaseURL(url) {
	const normalized = (url || '').trim().replace(/\/+$/, '')
	uni.setStorageSync(STORAGE_BASE_URL, normalized)
	return normalized
}

/** token 失效（后端 JWT 密钥重启随机生成，重启后旧 token 全部失效）统一跳登录 */
function handleUnauthorized() {
	removeToken()
	uni.reLaunch({
		url: '/pages/login/login'
	})
}

/**
 * 通用 JSON 请求
 * options: { url, method, data, header, noAuth, timeout, responseType }
 * 401 时自动清登录态并跳登录页
 */
export function request(options) {
	return new Promise((resolve, reject) => {
		const token = getToken()
		const header = { ...(options.header || {}) }
		if (token && !options.noAuth) {
			header['Authorization'] = 'Bearer ' + token
		}
		uni.request({
			url: /^https?:\/\//.test(options.url) ? options.url : getBaseURL() + options.url,
			method: options.method || 'GET',
			data: options.data,
			header,
			timeout: options.timeout || 60000,
			responseType: options.responseType || 'text',
			success: (res) => {
				if (res.statusCode === 401) {
					handleUnauthorized()
					reject({ code: 401, message: '登录已过期，请重新登录' })
					return
				}
				if (res.statusCode >= 200 && res.statusCode < 300) {
					resolve(res.data)
					return
				}
				const msg = (res.data && res.data.message) || ('请求失败(' + res.statusCode + ')')
				reject({ code: res.statusCode, message: msg })
			},
			fail: (err) => {
				reject({ code: -1, message: err.errMsg || '网络请求失败' })
			}
		})
	})
}

/**
 * UTF-8 解码：优先使用环境自带 TextDecoder，否则手动解码
 */
function decodeUtf8(buf) {
	if (typeof TextDecoder !== 'undefined') {
		return new TextDecoder('utf-8').decode(buf)
	}
	let out = ''
	let i = 0
	const len = buf.length
	while (i < len) {
		const b = buf[i]
		if (b < 0x80) {
			out += String.fromCharCode(b)
			i += 1
		} else if (b < 0xE0) {
			out += String.fromCharCode(((b & 0x1F) << 6) | (buf[i + 1] & 0x3F))
			i += 2
		} else if (b < 0xF0) {
			out += String.fromCharCode(((b & 0x0F) << 12) | ((buf[i + 1] & 0x3F) << 6) | (buf[i + 2] & 0x3F))
			i += 3
		} else {
			out += String.fromCharCode(((b & 0x07) << 18) | ((buf[i + 1] & 0x3F) << 12) | ((buf[i + 2] & 0x3F) << 6) | (buf[i + 3] & 0x3F))
			i += 4
		}
	}
	return out
}

/**
 * 流式 chunk 解码：chunk 边界可能切断 UTF-8 多字节字符，
 * 末尾不完整序列缓存到 pending，下次拼接解码，避免中文乱码
 */
function decodeStreamChunk(bytes, pending) {
	let buf = bytes
	if (pending.length > 0) {
		buf = new Uint8Array(pending.length + bytes.length)
		buf.set(pending)
		buf.set(bytes, pending.length)
	}
	let cut = buf.length
	let i = buf.length - 1
	let contCount = 0
	while (i >= 0 && (buf[i] & 0xC0) === 0x80) {
		i--
		contCount++
	}
	if (i >= 0 && buf[i] >= 0xC0 && buf[i] <= 0xF7) {
		const need = buf[i] < 0xE0 ? 1 : buf[i] < 0xF0 ? 2 : 3
		if (contCount < need) {
			cut = i
		}
	}
	pending.length = 0
	for (let j = cut; j < buf.length; j++) {
		pending.push(buf[j])
	}
	return decodeUtf8(buf.subarray(0, cut))
}

/**
 * 流式请求（GET + enableChunked，需基础库 2.20.1+）
 * handlers: { onChunk(增量文本), onDone(完整文本), onError(err) }
 * options.sse: true 时按 SSE 帧（data:xxx\n\n）解析再回调（manus 接口是 SseEmitter，
 *   knowledge 两个流式接口是纯文本流，无需解析）
 * 返回 requestTask，可调用 task.abort() 停止生成
 */
export function streamRequest(url, query, handlers, options = {}) {
	const token = getToken()
	const qs = Object.keys(query || {})
		.map((k) => k + '=' + encodeURIComponent(query[k]))
		.join('&')
	const fullUrl = getBaseURL() + url + (qs ? '?' + qs : '')
	const pending = []
	let fullText = ''
	let sseBuffer = ''
	let parsedFull = ''

	const emitChunk = (text) => {
		if (!options.sse) {
			if (text) {
				fullText += text
				if (handlers.onChunk) handlers.onChunk(text)
			}
			return
		}
		// SSE 帧解析：帧以 \n\n 结束，剥离 data: 前缀
		sseBuffer += text
		let idx
		while ((idx = sseBuffer.indexOf('\n\n')) >= 0) {
			const frame = sseBuffer.slice(0, idx)
			sseBuffer = sseBuffer.slice(idx + 2)
			const content = frame.replace(/^data:\s?/, '').replace(/\r/g, '')
			if (content) {
				parsedFull += content
				if (handlers.onChunk) handlers.onChunk(content)
			}
		}
	}

	const done = () => {
		const result = options.sse ? parsedFull : fullText
		if (handlers.onDone) handlers.onDone(result)
	}

	const task = uni.request({
		url: fullUrl,
		method: 'GET',
		header: token ? { Authorization: 'Bearer ' + token } : {},
		enableChunked: true,
		timeout: STREAM_TIMEOUT,
		success: (res) => {
			if (res.statusCode === 401) {
				handleUnauthorized()
				if (handlers.onError) handlers.onError({ code: 401, message: '登录已过期，请重新登录' })
				return
			}
			if (res.statusCode >= 300) {
				if (handlers.onError) handlers.onError({ code: res.statusCode, message: '请求失败(' + res.statusCode + ')' })
				return
			}
			// 降级路径：基础库不支持 enableChunked 时一次性返回完整数据
			if (res.data && typeof res.data === 'string' && !fullText && !parsedFull) {
				emitChunk(res.data)
			}
			done()
		},
		fail: (err) => {
			// 用户主动 abort 时静默，由调用方 onDone 收尾
			const isAbort = err && /abort/i.test(err.errMsg || '')
			if (!isAbort && handlers.onError) handlers.onError({ code: -1, message: err.errMsg || '网络请求失败' })
			if (isAbort) done()
		}
	})

	if (task.onChunkReceived) {
		task.onChunkReceived((res) => {
			const text = decodeStreamChunk(new Uint8Array(res.data), pending)
			if (text) {
				emitChunk(text)
			}
		})
	}
	return task
}
