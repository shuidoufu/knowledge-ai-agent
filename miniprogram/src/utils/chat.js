import { getUsername } from './auth'

/**
 * 生成会话 ID：后端约束前缀 know_{username}_
 */
export function generateChatId() {
	const rand = Math.random().toString(36).slice(2, 10)
	return 'know_' + getUsername() + '_' + Date.now() + '_' + rand
}

/** 随机消息 ID（前端本地渲染用） */
export function generateMsgId() {
	return 'msg_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}

/** RAG 引用标记：流式回复末尾追加的行 */
const RAG_REFS_MARK = '<!--RAG_REFS-->'

/**
 * 解析流式回复全文：切分 RAG 引用标记前的正文与标记后的引用 JSON
 * 返回 { displayContent, references }
 */
export function parseRagReferences(fullText) {
	const markIndex = fullText.indexOf(RAG_REFS_MARK)
	if (markIndex < 0) {
		return { displayContent: fullText, references: [] }
	}
	const displayContent = fullText.slice(0, markIndex)
	let references = []
	try {
		const parsed = JSON.parse(fullText.slice(markIndex + RAG_REFS_MARK.length))
		if (Array.isArray(parsed)) {
			references = parsed
		}
	} catch (e) {
		// 引用解析失败时正文照常展示，引用不显示
	}
	return { displayContent, references }
}

/**
 * 历史消息映射：后端 MessageDocument { role, content, references }
 * role 为 'user' / 'assistant'，映射为前端消息结构
 */
export function mapHistoryMessages(list) {
	return (list || []).map((m) => ({
		id: generateMsgId(),
		role: m.role === 'user' ? 'user' : 'assistant',
		content: m.content || '',
		references: m.references || [],
		loading: false
	}))
}
