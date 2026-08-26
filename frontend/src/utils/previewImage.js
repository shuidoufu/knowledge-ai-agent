import { ref } from 'vue'

/**
 * 图片预览共享状态（全站单例）。
 *
 * Markdown 渲染的图片通过 window.__previewImage 打开预览；
 * 两个聊天页共用同一份状态，避免各页自行注册/删除全局函数时互相覆盖。
 */
export const previewImage = ref({ show: false, src: '', alt: '' })

export function openPreview(src, alt) {
  previewImage.value = { show: true, src, alt }
}

export function closePreview() {
  previewImage.value = { show: false, src: '', alt: '' }
}

// 供 v-html 渲染的 img onclick 调用（模块加载时注册一次即可）
window.__previewImage = (src, alt) => openPreview(src, alt)
