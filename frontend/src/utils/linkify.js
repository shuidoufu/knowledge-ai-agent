/**
 * 聊天消息中的裸地址自动链接化
 * AI 返回的下载地址（如 /api/files/pdf/xxx.pdf）是根相对路径，
 * 直接展示为文本时用户不知道要拼接当前站点前缀。这里把渲染后的 HTML
 * 中的纯文本网址/根相对路径自动转换为可点击链接，点击时浏览器自动
 * 基于当前站点解析完整地址。
 */

// 匹配绝对 URL（http/https/www.）与本应用根相对路径（/api/...），
// 排除协议相对地址（//）以免指向外部域名；/api/ 为后端 Context Path 前缀。
// 注意：带 /g 的正则 lastIndex 会跨调用残留，每次使用前必须重置
const URL_REGEX = /(?:https?:\/\/[^\s<>"'`]+|www\.[^\s<>"'`]+|\/api\/[^\s<>"'`]+)/g

// URL 尾部常见标点（中英文句号、逗号、括号等）不属于地址，需剔除
const TRAILING_PUNCT = /[.,;:!?'"、。，；：！？)）\]】}》」』]+$/

// 中日韩统一表意文字 / CJK 标点 / 全角符号
const CJK_CHARS = /[\u4e00-\u9fff\u3400-\u4dbf\u3000-\u303f\uff00-\uffef]/

/**
 * 剔除地址尾部粘连的中文内容（如 "/api/files/pdf/a.pdf。下载吧" 只保留 "/api/files/pdf/a.pdf"）。
 * 裁剪条件（满足其一）：
 *   1. 中文前是文件扩展名形态（.xxx 结尾）；
 *   2. 最后一个 / 之后存在 ASCII 内容（路径段/域名，如 "foo。谢谢"、"example.com。查看"）。
 * 纯中文文件名（如 "/api/files/pdf/洛克王国攻略"，最后一个 / 后全是中文）不裁剪。
 */
function stripTrailingCjk(url) {
  const tail = url.match(/([\u4e00-\u9fff\u3400-\u4dbf\u3000-\u303f\uff00-\uffef]+)$/)
  if (!tail) return url
  const before = url.slice(0, tail.index)
  const lastSlash = before.lastIndexOf('/')
  const afterLastSlash = lastSlash >= 0 ? before.slice(lastSlash + 1) : before
  if (/\.\w{1,10}$/.test(before) || /[A-Za-z0-9]/.test(afterLastSlash)) return before
  return url
}

/**
 * 生成安全 href：www. 前缀补 https://，其余原样（根相对路径由浏览器基于当前站点解析）
 */
function buildHref(raw) {
  if (/^www\./i.test(raw)) return 'https://' + raw
  return raw
}

/**
 * 将 HTML 文本节点中的裸网址转换为 <a> 链接
 * @param {string} html 已经 DOMPurify 清洗过的 HTML
 * @returns {string} 链接化后的 HTML
 */
export function linkifyHtml(html) {
  if (!html) return html
  // 全局正则 lastIndex 会残留，先重置再测试
  URL_REGEX.lastIndex = 0
  if (!URL_REGEX.test(html)) return html
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const walker = document.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT)
  const textNodes = []
  while (walker.nextNode()) {
    const node = walker.currentNode
    const parent = node.parentElement
    // 跳过代码块、行内代码、已存在的链接内的文本，避免误转换或嵌套链接
    if (parent && /^(A|PRE|CODE)$/.test(parent.tagName)) continue
    textNodes.push(node)
  }
  for (const node of textNodes) {
    const text = node.nodeValue
    if (!text) continue
    URL_REGEX.lastIndex = 0
    if (!URL_REGEX.test(text)) continue
    URL_REGEX.lastIndex = 0
    const frag = document.createDocumentFragment()
    let lastIndex = 0
    let match
    while ((match = URL_REGEX.exec(text)) !== null) {
      const rawUrl = match[0]
      const url = stripTrailingCjk(rawUrl)
      const cleaned = url.replace(TRAILING_PUNCT, '')
      if (!cleaned) continue
      // 前缀文本
      if (match.index > lastIndex) {
        frag.appendChild(document.createTextNode(text.slice(lastIndex, match.index)))
      }
      const a = document.createElement('a')
      a.href = buildHref(cleaned)
      a.target = '_blank'
      a.rel = 'noopener'
      a.textContent = cleaned
      frag.appendChild(a)
      // 被剔除的尾部标点/粘连中文保留为普通文本
      if (cleaned.length < rawUrl.length) {
        frag.appendChild(document.createTextNode(rawUrl.slice(cleaned.length)))
      }
      lastIndex = match.index + rawUrl.length
    }
    if (lastIndex < text.length) {
      frag.appendChild(document.createTextNode(text.slice(lastIndex)))
    }
    node.parentNode.replaceChild(frag, node)
  }
  return doc.body.innerHTML
}
