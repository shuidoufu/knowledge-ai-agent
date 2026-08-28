import { getBaseURL, getServerRoot } from './request'

/**
 * Markdown 预处理（在 marked 渲染前执行）：
 * - 代码块/行内代码先提取占位，URL 改写不污染代码内容
 * - 裸 /api/ 根相对路径（后端下载链接，如 /api/files/pdf/xxx.pdf）包成 markdown 链接
 * - http(s) 裸 URL 由 marked 的 gfm autolink 处理；图片代理在 marked renderer 中处理
 */

/**
 * 图片地址代处理：
 * - http(s) 外链 → 走后端 /image-proxy 代理（防盗链）
 * - /api/ 开头的后端相对路径（如 /api/files/download/文件名，prompt.yml 引导 AI
 *   用此格式展示已保存图片）→ 拼接服务器根地址成完整 URL
 */
export function proxyImageUrl(url) {
	const u = (url || '').trim()
	if (/^https?:\/\//i.test(u)) {
		return getBaseURL() + '/image-proxy?url=' + encodeURIComponent(u)
	}
	if (/^\/api\//.test(u)) {
		// 中文文件名需编码（downloadFile 对未编码中文路径会失败）
		const parts = u.split('/')
		const last = parts.pop()
		return getServerRoot() + '/' + parts.join('/') + '/' + encodeURIComponent(last)
	}
	return u
}

const PH_PRE = '\u0000MP_'
const PH_SUF = '\u0000'

function extractCode(text, placeholders) {
	return text
		.replace(/```[\s\S]*?```/g, (m) => {
			placeholders.push(m)
			return PH_PRE + (placeholders.length - 1) + PH_SUF
		})
		.replace(/`[^`\n]+`/g, (m) => {
			placeholders.push(m)
			return PH_PRE + (placeholders.length - 1) + PH_SUF
		})
}

function restoreCode(text, placeholders) {
	return text.replace(new RegExp(PH_PRE + '(\\d+)' + PH_SUF, 'g'), (m, idx) => {
		return placeholders[parseInt(idx, 10)] || m
	})
}

export function preprocessMarkdown(markdown) {
	const placeholders = []
	let text = extractCode(markdown || '', placeholders)
	text = text.replace(/\/api\/[^\s)\]"'，。；、！？]+/g, (m, offset, full) => {
		const prev = offset > 0 ? full.charAt(offset - 1) : ''
		// 前驱是字母/数字/斜杠/括号/引号时跳过（避免命中已包裹链接或代理 URL 中缀）
		if (prev && /[A-Za-z0-9/([\\"'`]/.test(prev)) return m
		// 裁剪尾部英文标点并保留在链接外
		const trimmed = m.replace(/[.,;:>]+$/, '')
		if (!trimmed) return m
		return '[' + trimmed + '](' + trimmed + ')' + m.slice(trimmed.length)
	})
	return restoreCode(text, placeholders)
}

/** 链接分类：文件下载 / 外链 / 其他，返回 { type, url } */
export function classifyLink(href) {
	const h = (href || '').trim()
	if (/^\/api\/files\//.test(h)) {
		return { type: 'file', url: getServerRoot() + h }
	}
	if (/^https?:\/\//i.test(h)) {
		return { type: 'web', url: h }
	}
	return { type: 'other', url: h }
}
