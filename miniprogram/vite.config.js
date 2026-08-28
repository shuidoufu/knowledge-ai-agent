import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import esbuild from 'esbuild'

/**
 * 微信开发者工具预览/真机构建链的语法解析器较旧：
 * 1. 不认识 ?? ?. 等 ES2020 语法；
 * 2. 不支持 ES2018 的 Unicode 属性转义正则（/\p{L}/u 等）。
 * node_modules 依赖不会被 vite 默认转译，且 esbuild 无法转译正则字面量，
 * 这里对依赖模块先做正则等价替换，再降级 ES2015，避免预览/真机报语法错误。
 */

/**
 * Unicode 属性转义 → 等价 BMP 字符范围（覆盖中文/日文/韩文/西文/常用数字符号），
 * 语义与原 \p 分类近似，仅影响罕见字符分类，对聊天展示无感知
 */
const UNICODE_PROP_REPLACEMENTS = [
  // 先替换多字符属性名，避免被单字符前缀截胡
  ['\\p{Pi}', '\\u0022\\u0027\\u00AB\\u2018-\\u201F\\u2039\\u203A\\u275B\\u275C'],
  ['\\p{Ps}', '\\u0028\\u005B\\u007B\\u3008-\\u3011\\uFF08\\uFF3B\\uFF5B\\u201C\\u201E'],
  ['\\p{Pf}', '\\u0022\\u0027\\u00BB\\u201D\\u201F\\u300A\\u300C\\u300E\\u3010\\uFF09\\uFF3D\\uFF5D'],
  // 字母 / 数字 / 标点 / 符号
  ['\\p{L}', '\\u0041-\\u005A\\u0061-\\u007A\\u00C0-\\u02FF\\u0370-\\u1FFF\\u2C00-\\uD7FF\\uF900-\\uFFFD'],
  ['\\p{N}', '\\u0030-\\u0039\\u0660-\\u0669\\uFF10-\\uFF19'],
  ['\\p{P}', '\\u0021-\\u0023\\u0025-\\u0028\\u002A-\\u002F\\u003A-\\u003B\\u003F-\\u0040\\u005B\\u005D\\u005E\\u0060\\u007B-\\u007D\\u00A1\\u00AB\\u00B7\\u00BB\\u2010-\\u205E\\u3000-\\u303F\\uFF01-\\uFF5F\\uFF61-\\uFF65\\uFE50-\\uFE6F'],
  ['\\p{S}', '\\u0024\\u002B\\u003C-\\u003E\\u005C\\u005E\\u007C\\u007E\\u00A2-\\u00A9\\u00AC\\u00AE-\\u00B1\\u00B4\\u00B8\\u00D7\\u00F7\\u20A0-\\u20BF\\u2100-\\u214F\\u2190-\\u23FF\\u2600-\\u27BF'],
]

function replaceUnicodeProps(code) {
  let result = code
  for (const [pattern, replacement] of UNICODE_PROP_REPLACEMENTS) {
    result = result.split(pattern).join(replacement)
  }
  return result
}

/**
 * 对依赖模块（marked 等含新语法的包）做兼容化处理
 */
function downgradeDeps() {
  return {
    name: 'downgrade-deps',
    transform(code, id) {
      if (!id.includes('node_modules/marked')) return null
      let processed = replaceUnicodeProps(code)
      if (processed !== code) {
        const result = esbuild.transformSync(processed, { target: 'es2015', loader: 'js' })
        return { code: result.code, map: null }
      }
      return null
    }
  }
}

export default defineConfig({
  plugins: [
    uni(),
    downgradeDeps(),
  ],
})
