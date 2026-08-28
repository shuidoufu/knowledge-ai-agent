/**
 * 图片本地化加载器
 *
 * 背景：安卓真机微信新内核限制 image 组件对 http + IP 地址图片的加载
 * （开发者工具正常、真机不显示），但 wx.request/downloadFile 网络栈不受此限制
 * （聊天流式能通即证明）。因此图片不交给 mp-html 内嵌 image 直接加载 http 代理地址，
 * 而是先用 uni.downloadFile 下载到本地临时文件，渲染时用本地路径（image 组件支持本地路径）。
 *
 * 缓存：模块级 Map（代理 URL -> 本地路径），同图复用；下载失败返回空串，渲染占位图。
 */

const pathMap = new Map()
const pendingMap = new Map()

/** 1px 透明 GIF 占位（未加载完成/失败时显示）——用本地静态文件而非 data URI：
 * 真机微信 image 组件对 base64 data URI 渲染不可靠（开发者工具正常），
 * 且 mp-html parser 对 src 含 data: 的图片设置 ignore，imgtap 事件不触发（点击重试失效） */
const EMPTY_GIF = '/static/img-placeholder.png'

export function getImagePlaceholder() {
	return EMPTY_GIF
}

/** 取已下载的本地路径，未就绪返回空串 */
export function getLocalImagePath(proxyUrl) {
	return pathMap.get(proxyUrl) || ''
}

/**
 * 预加载一条消息内的全部图片：提取 markdown 图片语法以及原生 <img src> 中的 URL，
 * 转为完整 URL 后逐个下载（并发受限），全部完成（成功或失败）后 resolve
 * 返回下载成功的数量（0 表示全部失败或没有图片）
 */
const MAX_CONCURRENT = 3
let activeDownloads = 0
const waitQueue = []

function acquire() {
	return new Promise((resolve) => {
		if (activeDownloads < MAX_CONCURRENT) {
			activeDownloads++
			resolve()
		} else {
			waitQueue.push(resolve)
		}
	})
}

function release() {
	activeDownloads--
	const next = waitQueue.shift()
	if (next) {
		activeDownloads++
		next()
	}
}

/** 受控下载：并发不超过 MAX_CONCURRENT */
export function loadImage(proxyUrl) {
	if (!proxyUrl) return Promise.resolve('')
	const cached = pathMap.get(proxyUrl)
	if (cached) return Promise.resolve(cached)
	if (pendingMap.has(proxyUrl)) return pendingMap.get(proxyUrl)

	const promise = acquire()
		.then(
			() =>
				new Promise((resolve) => {
					uni.downloadFile({
						url: proxyUrl,
						timeout: 60000,
						success: (res) => {
							if (res.statusCode === 200 && res.tempFilePath) {
								pathMap.set(proxyUrl, res.tempFilePath)
								resolve(res.tempFilePath)
							} else {
								resolve('')
							}
						},
						fail: () => {
							resolve('')
						}
					})
				})
		)
		.finally(() => {
			release()
			pendingMap.delete(proxyUrl)
		})
	pendingMap.set(proxyUrl, promise)
	return promise
}

export async function preloadMessageImages(content, proxyImageUrl) {
	if (!content) return 0
	const urls = []
	// markdown 图片语法 ![alt](url)
	const mdRe = /!\[[^\]]*\]\(([^)\s]+)(?:\s+["'][^"']*["'])?\)/g
	let m
	while ((m = mdRe.exec(content)) !== null) {
		if (m[1]) urls.push(m[1])
	}
	// 原生 <img src="url">（marked 放行的原始 HTML）
	const imgRe = /<img\s+src=["']([^"']+)["']/g
	while ((m = imgRe.exec(content)) !== null) {
		if (m[1]) urls.push(m[1])
	}
	if (!urls.length) return 0
	const map = await Promise.all(
		urls.map((u) => loadImage(proxyImageUrl(u)).then((p) => (p ? 1 : 0)))
	)
	return map.reduce((a, b) => a + b, 0)
}
