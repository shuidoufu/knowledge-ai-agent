<template>
  <div class="user-page">
    <div class="user-shell">
      <!-- 头部：返回 + 标题 -->
      <header class="page-header">
        <button type="button" class="back-link" @click="goBack">
          <ArrowLeft class="icon" size="16" />
          返回
        </button>
        <div class="header-texts">
          <h1 class="page-title">用户管理</h1>
          <p class="page-subtitle">查看用户信息，批量授予或取消管理员权限</p>
        </div>
      </header>

      <!-- 工具条：搜索 + 排序 + 批量 -->
      <section class="toolbar">
        <div class="search-wrap">
          <Search class="search-icon" size="18" />
          <input
            v-model="keyword"
            class="search-input"
            type="text"
            placeholder="搜索用户名..."
            aria-label="搜索用户名"
          />
        </div>
        <div ref="sortWrapRef" class="sort-wrap">
          <button
            type="button"
            class="sort-trigger"
            :class="{ open: sortOpen }"
            aria-haspopup="listbox"
            aria-controls="user-sort-menu"
            :aria-expanded="sortOpen ? 'true' : 'false'"
            aria-label="排序方式"
            @click.stop="toggleSortMenu"
            @keydown="handleSortKeydown"
          >
            <span class="sort-value">{{ sortLabel }}</span>
            <ChevronDown class="sort-caret" size="16" />
          </button>
        </div>
        <button type="button" class="ghost-btn" :class="{ active: batchMode }" @click="toggleBatchMode">
          <ListChecks class="icon" size="16" />
          {{ batchMode ? '退出批量' : '批量管理' }}
        </button>
      </section>

      <!-- 排序面板：Teleport 到 body + fixed 定位，避免被滚动容器裁剪 -->
      <Teleport to="body">
        <Transition name="sort-menu">
          <div
            v-if="sortOpen"
            id="user-sort-menu"
            ref="sortMenuRef"
            class="sort-menu-fixed"
            :style="sortMenuStyle"
            role="listbox"
            aria-label="排序方式"
            :aria-activedescendant="`user-sort-option-${activeSortIndex}`"
            @click.stop
          >
            <button
              v-for="(option, index) in SORT_OPTIONS"
              :id="`user-sort-option-${index}`"
              :key="option.value"
              type="button"
              class="sort-option"
              :class="{ active: sort === option.value, highlight: activeSortIndex === index }"
              role="option"
              tabindex="-1"
              :aria-selected="sort === option.value ? 'true' : 'false'"
              @click.stop="selectSort(option.value)"
            >
              <span class="sort-option-label">{{ option.label }}</span>
              <Check v-if="sort === option.value" class="sort-option-check" size="16" />
            </button>
          </div>
        </Transition>
      </Teleport>

      <!-- 用户列表 -->
      <section class="table-card">
        <div class="table-head">
          <span v-if="batchMode" class="col-check">
            <button
              type="button"
              class="check-circle"
              :class="{ selected: allSelected }"
              :disabled="selectableIds.length === 0"
              :aria-label="allSelected ? '取消全选' : '全选当前页'"
              @click="toggleSelectAll"
            >
              <Check v-if="allSelected" class="icon" size="13" />
            </button>
          </span>
          <span class="col-name">用户</span>
          <span class="col-role">权限</span>
          <span class="col-time">注册时间</span>
          <span class="col-time">最后登录</span>
          <span class="col-time">更新时间</span>
        </div>

        <div v-if="loading && items.length === 0" class="state-block">
          <span class="spinner" aria-hidden="true"></span>
          <span class="state-text">正在加载用户...</span>
        </div>

        <div v-else-if="items.length === 0 && loadError" class="state-block">
          <AlertCircle class="state-icon error" size="32" />
          <span class="state-text">{{ loadError }}</span>
          <button type="button" class="retry-btn" @click="loadUsers">重试</button>
        </div>

        <div v-else-if="items.length === 0" class="state-block">
          <Users class="state-icon" size="32" />
          <span class="state-text">{{ keyword ? '没有匹配的用户' : '暂无用户数据' }}</span>
        </div>

        <div v-else class="table-body">
          <div
            v-for="user in items"
            :key="user.userId"
            class="table-row"
            :class="{ selected: selected.includes(user.userId) }"
          >
            <span v-if="batchMode" class="col-check">
              <button
                type="button"
                class="check-circle"
                :class="{ selected: selected.includes(user.userId) }"
                :disabled="user.self"
                :title="user.self ? '不能修改自己的权限' : ''"
                :aria-label="user.self ? '不能修改自己的权限' : selected.includes(user.userId) ? '取消选择' : '选择该用户'"
                @click="toggleSelect(user.userId)"
              >
                <Check v-if="selected.includes(user.userId)" class="icon" size="13" />
              </button>
            </span>

            <div class="col-name">
              <UserRound class="user-icon" size="20" />
              <span class="name-texts">
                <span class="user-name">
                  {{ user.username }}
                  <span v-if="user.self" class="self-tag">当前账号</span>
                </span>
                <span class="user-id" :title="user.userId">{{ user.userId }}</span>
              </span>
            </div>

            <span class="col-role">
              <span class="role-badge" :class="user.role === ROLE_ADMIN ? 'admin' : 'normal'">
                <ShieldCheck v-if="user.role === ROLE_ADMIN" class="role-icon" size="14" />
                {{ roleText(user.role) }}
              </span>
            </span>

            <span class="col-time"><span class="time-label">注册</span>{{ formatTime(user.createdAt) }}</span>
            <span class="col-time"><span class="time-label">最后登录</span>{{ formatTime(user.lastLoginAt) }}</span>
            <span class="col-time"><span class="time-label">更新</span>{{ formatTime(user.updatedAt) }}</span>
          </div>
        </div>
      </section>

      <!-- 分页 -->
      <div class="pager">
        <span class="pager-info">共 {{ total }} 条 · 第 {{ page }}/{{ totalPages || 1 }} 页</span>
        <div class="pager-btns">
          <button type="button" class="pager-btn" :disabled="page <= 1" aria-label="上一页" @click="goPage(page - 1)">
            <ChevronLeft class="icon" size="16" />
          </button>
          <span class="pager-current">{{ page }} / {{ totalPages || 1 }}</span>
          <button
            type="button"
            class="pager-btn"
            :disabled="totalPages === 0 || page >= totalPages"
            aria-label="下一页"
            @click="goPage(page + 1)"
          >
            <ChevronRight class="icon" size="16" />
          </button>
        </div>
      </div>
    </div>

    <!-- 批量操作条 -->
    <div v-if="batchMode" class="batch-bar">
      <button type="button" class="batch-bar-btn cancel" @click="toggleBatchMode">取消</button>
      <button
        type="button"
        class="batch-bar-btn grant"
        :disabled="selected.length === 0"
        @click="askRoleChange(ROLE_ADMIN)"
      >
        设为管理员({{ selected.length }})
      </button>
      <button
        type="button"
        class="batch-bar-btn revoke"
        :disabled="selected.length === 0"
        @click="askRoleChange(ROLE_USER)"
      >
        取消管理员({{ selected.length }})
      </button>
    </div>

    <!-- 权限修改确认弹窗 -->
    <Teleport to="body">
      <div v-if="pendingRole !== null" class="modal-overlay" @click="closeRoleConfirm">
        <div
          class="modal-content"
          role="dialog"
          aria-modal="true"
          aria-labelledby="user-role-confirm-title"
          @click.stop
        >
          <div id="user-role-confirm-title" class="modal-title">{{ pendingRole === ROLE_ADMIN ? '设为管理员' : '取消管理员' }}</div>
          <div class="modal-desc">
            <template v-if="pendingRole === ROLE_ADMIN">
              将选中的 {{ selected.length }} 个用户设为管理员，对方将获得知识库管理与用户管理的访问权限，确认继续？
            </template>
            <template v-else>
              将选中的 {{ selected.length }} 个用户取消管理员权限，对方将立即失去管理页面的访问权限，确认继续？
            </template>
          </div>
          <div class="modal-actions">
            <button type="button" class="modal-btn cancel" @click="closeRoleConfirm">取消</button>
            <button
              type="button"
              class="modal-btn confirm"
              :class="{ green: pendingRole === ROLE_ADMIN }"
              :disabled="submitting"
              @click="doRoleChange"
            >
              {{ submitting ? '提交中...' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, inject, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { fetchUsers, batchUpdateUserRole } from '../api/request'
import {
  ArrowLeft,
  Search,
  ListChecks,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Check,
  Users,
  UserRound,
  ShieldCheck,
  AlertCircle,
} from '@lucide/vue'

const router = useRouter()
const showToast = inject('showToast', () => {})

const PAGE_SIZE = 10
const ROLE_USER = 0
const ROLE_ADMIN = 1

/** 排序选项（值与后端接口约定一致） */
const SORT_OPTIONS = [
  { value: 'time_desc', label: '按注册时间排序（最新优先）' },
  { value: 'time_asc', label: '按注册时间排序（最早优先）' },
  { value: 'name_asc', label: '按用户名排序' },
]

const items = ref([])
const total = ref(0)
const totalPages = ref(0)
const page = ref(1)
const keyword = ref('')
const sort = ref(SORT_OPTIONS[0].value)
const sortOpen = ref(false)
const sortWrapRef = ref(null)
const sortMenuStyle = ref({ top: '0px', left: '0px' })
const sortMenuRef = ref(null)
const activeSortIndex = ref(0)

const sortLabel = computed(
  () => (SORT_OPTIONS.find((item) => item.value === sort.value) || SORT_OPTIONS[0]).label,
)
const loading = ref(false)

const batchMode = ref(false)
const selected = ref([])
const pendingRole = ref(null)
const submitting = ref(false)
const loadError = ref('')

let searchTimer = null
let listRequestId = 0

/** 当前页可勾选的用户：排除当前登录账号（其权限不可修改） */
const selectableIds = computed(() => items.value.filter((user) => !user.self).map((user) => user.userId))

const allSelected = computed(
  () => selectableIds.value.length > 0 && selected.value.length === selectableIds.value.length,
)

/** 返回上一级页面（无历史时回首页） */
function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

function roleText(role) {
  return role === ROLE_ADMIN ? '管理员' : '普通用户'
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  const pad = (n) => String(n).padStart(2, '0')
  const day = `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())}`
  return `${day} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 统一错误提示：403 单独提示权限问题，其余优先后端返回的 message，网络类错误用中文兜底 */
function errorMessage(error, fallback) {
  if (error?.response?.status === 403) {
    return error.response.data?.message || '无管理权限，仅管理员可管理用户'
  }
  return error?.response?.data?.message || fallback
}

async function loadUsers() {
  // 请求序号：只采纳最新一次请求的响应，避免先发后到覆盖后发的
  const requestId = ++listRequestId
  loading.value = true
  loadError.value = ''
  try {
    const res = await fetchUsers({
      keyword: keyword.value,
      page: page.value,
      size: PAGE_SIZE,
      sort: sort.value,
    })
    if (requestId !== listRequestId) return
    const data = res.data || {}
    items.value = data.items || []
    total.value = data.total || 0
    totalPages.value = data.totalPages || 0
  } catch (error) {
    if (requestId !== listRequestId) return
    // 权限已被回收：清空列表并回首页，提示由首页统一展示
    if (error?.response?.status === 403) {
      items.value = []
      total.value = 0
      totalPages.value = 0
      batchMode.value = false
      selected.value = []
      router.replace({ path: '/', query: { msg: errorMessage(error, '无管理权限，仅管理员可管理用户') } })
      return
    }
    loadError.value = errorMessage(error, '用户列表加载失败')
    showToast(loadError.value, 'error')
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}

function goPage(target) {
  if (target < 1 || (totalPages.value > 0 && target > totalPages.value)) return
  selected.value = []
  page.value = target
}

function toggleBatchMode() {
  batchMode.value = !batchMode.value
  if (!batchMode.value) selected.value = []
}

function toggleSelect(userId) {
  const index = selected.value.indexOf(userId)
  if (index === -1) {
    selected.value.push(userId)
  } else {
    selected.value.splice(index, 1)
  }
}

function toggleSelectAll() {
  selected.value = allSelected.value ? [] : [...selectableIds.value]
}

/** 重新加载列表；非首页时借助页码监听回到首页 */
function reloadFromFirstPage() {
  if (page.value === 1) {
    loadUsers()
  } else {
    page.value = 1
  }
}

/** 点批量操作按钮：先二次确认再提交（权限变更影响面大） */
function askRoleChange(role) {
  if (selected.value.length === 0) return
  pendingRole.value = role
}

function closeRoleConfirm() {
  if (submitting.value) return
  pendingRole.value = null
}

async function doRoleChange() {
  if (pendingRole.value === null || selected.value.length === 0) return
  const role = pendingRole.value
  const userIds = [...selected.value]
  submitting.value = true
  try {
    const res = await batchUpdateUserRole(userIds, role)
    showToast(res.data?.message || '权限已更新', 'success', 4000)
    pendingRole.value = null
    selected.value = []
    await loadUsers()
  } catch (error) {
    showToast(errorMessage(error, '权限修改失败，请重试'), 'error')
    pendingRole.value = null
    selected.value = []
    // 校验失败（如目标包含自己）时重新拉取，反映服务端的真实状态
    await loadUsers()
  } finally {
    submitting.value = false
  }
}

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    // 筛选条件变化后旧的勾选不再可见，避免批量修改误作用到看不到的用户
    selected.value = []
    const alreadyFirstPage = page.value === 1
    page.value = 1
    if (alreadyFirstPage) loadUsers()
  }, 300)
})

/** 展开排序面板：高亮当前选项并按触发按钮位置定位 */
function openSortMenu() {
  activeSortIndex.value = Math.max(
    0,
    SORT_OPTIONS.findIndex((item) => item.value === sort.value),
  )
  sortOpen.value = true
  nextTick(positionSortMenu)
}

function toggleSortMenu() {
  if (sortOpen.value) {
    closeSortMenu()
    return
  }
  openSortMenu()
}

function closeSortMenu() {
  sortOpen.value = false
}

function selectSort(value) {
  sort.value = value
  sortOpen.value = false
  // 选项随面板一起卸载，把焦点交还触发按钮，避免焦点掉到 body
  nextTick(() => {
    const trigger = sortWrapRef.value ? sortWrapRef.value.querySelector('.sort-trigger') : null
    if (trigger) trigger.focus()
  })
}

/** 面板挂在 body 上（脱离滚动容器），用触发按钮的视口坐标定位；下方空间不足则向上翻转 */
function positionSortMenu() {
  const rect = sortWrapRef.value ? sortWrapRef.value.getBoundingClientRect() : null
  const menu = sortMenuRef.value
  if (!rect || !menu) return
  const gap = 8
  const menuHeight = menu.offsetHeight
  const menuWidth = menu.offsetWidth
  const flipUp = window.innerHeight - rect.bottom - gap < menuHeight && rect.top - gap >= menuHeight
  const top = flipUp ? rect.top - gap - menuHeight : rect.bottom + gap
  const left = Math.min(Math.max(8, rect.left), Math.max(8, window.innerWidth - menuWidth - 8))
  sortMenuStyle.value = { top: `${top}px`, left: `${left}px`, minWidth: `${rect.width}px` }
}

/** 触发按钮上的键盘操作：上下移动高亮、回车选中 */
function handleSortKeydown(event) {
  if (event.key === 'Tab') {
    closeSortMenu()
    return
  }
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    if (!sortOpen.value) {
      openSortMenu()
      return
    }
    const step = event.key === 'ArrowDown' ? 1 : -1
    activeSortIndex.value =
      (activeSortIndex.value + step + SORT_OPTIONS.length) % SORT_OPTIONS.length
    return
  }
  if (event.key === 'Enter' && sortOpen.value) {
    event.preventDefault()
    selectSort(SORT_OPTIONS[activeSortIndex.value].value)
  }
}

function handleGlobalKeydown(event) {
  if (event.key === 'Escape') {
    if (sortOpen.value) closeSortMenu()
    if (pendingRole.value !== null) closeRoleConfirm()
  }
}

function handleSortOutsideClick() {
  if (sortOpen.value) closeSortMenu()
}

watch(sort, () => {
  // 排序变化后旧的勾选不再可见，清空避免批量修改误作用到看不到的用户
  selected.value = []
  // 非首页时只改页码，由 page 监听统一触发加载，避免重复请求
  reloadFromFirstPage()
})

watch(page, () => {
  loadUsers()
})

onMounted(() => {
  loadUsers()
  document.addEventListener('click', handleSortOutsideClick)
  document.addEventListener('keydown', handleGlobalKeydown)
  window.addEventListener('scroll', handleSortOutsideClick, true)
})

onUnmounted(() => {
  clearTimeout(searchTimer)
  document.removeEventListener('click', handleSortOutsideClick)
  document.removeEventListener('keydown', handleGlobalKeydown)
  window.removeEventListener('scroll', handleSortOutsideClick, true)
})
</script>

<style scoped>
.user-page {
  min-height: 100%;
  padding: 2rem 2rem 6rem;
  background: linear-gradient(160deg, #f0fdf9 0%, #f5f7fb 45%, #ecfdf5 100%);
  color: #1e293b;
}

.user-shell {
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ===== 头部 ===== */
.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  min-height: 44px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: #10b981;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, box-shadow 0.2s;
}

.back-link:hover {
  background: rgba(255, 255, 255, 0.95);
  color: #059669;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.12);
}

.back-link .icon {
  width: 16px;
  height: 16px;
}

.header-texts {
  flex: 1 1 auto;
  min-width: 0;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #065f46;
  margin-bottom: 2px;
}

.page-subtitle {
  font-size: 0.85rem;
  color: #64748b;
}

/* ===== 工具条 ===== */
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.08);
}

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1 1 260px;
  min-width: 0;
}

.search-icon {
  position: absolute;
  left: 12px;
  width: 18px;
  height: 18px;
  color: #94a3b8;
  pointer-events: none;
}

.search-input {
  width: 100%;
  height: 44px;
  padding: 0 12px 0 38px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #1e293b;
  font-size: 0.95rem;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-input::placeholder {
  color: #94a3b8;
}

.search-input:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.sort-wrap {
  position: relative;
  flex-shrink: 0;
}

.sort-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 12px 0 14px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #1e293b;
  font-size: 0.9rem;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.sort-trigger:hover {
  background: #fff;
  border-color: rgba(16, 185, 129, 0.35);
}

.sort-trigger.open {
  background: #fff;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.sort-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sort-caret {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: #64748b;
  transition: transform 0.2s ease;
}

.sort-trigger.open .sort-caret {
  transform: rotate(180deg);
}

/* 排序面板：Teleport 到 body，fixed 定位 + 玻璃拟态 */
.sort-menu-fixed {
  position: fixed;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 12px 32px rgba(16, 185, 129, 0.16);
}

.sort-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-height: 44px;
  padding: 0 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #334155;
  font-size: 0.88rem;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.sort-option:hover,
.sort-option.highlight {
  background: rgba(16, 185, 129, 0.08);
  color: #047857;
}

.sort-option.active {
  color: #047857;
  font-weight: 600;
}

.sort-option-label {
  white-space: nowrap;
}

.sort-option-check {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: #10b981;
}

.sort-menu-enter-active,
.sort-menu-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.sort-menu-enter-from,
.sort-menu-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 44px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid rgba(16, 185, 129, 0.2);
  background: rgba(16, 185, 129, 0.06);
  color: #059669;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.ghost-btn:hover {
  background: rgba(16, 185, 129, 0.12);
}

.ghost-btn.active {
  background: #10b981;
  border-color: #10b981;
  color: #fff;
}

.ghost-btn .icon {
  width: 16px;
  height: 16px;
}

/* ===== 列表 ===== */
.table-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 8px 32px rgba(16, 185, 129, 0.09);
  overflow: hidden;
}

.table-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(16, 185, 129, 0.1);
  font-size: 0.82rem;
  font-weight: 600;
  color: #64748b;
  letter-spacing: 0.02em;
}

.table-body {
  display: flex;
  flex-direction: column;
}

.table-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.04);
  transition: background 0.2s ease;
}

.table-row:last-child {
  border-bottom: none;
}

.table-row:hover {
  background: #f8fafc;
}

.table-row.selected {
  background: rgba(52, 211, 153, 0.08);
}

/* 列宽容纳 44px 的可点区域，勾选圈本身仍是 22px */
.col-check {
  flex: 0 0 44px;
  width: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.col-name {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: #10b981;
}

.name-texts {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.self-tag {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
  font-size: 0.7rem;
  font-weight: 500;
}

.user-id {
  font-size: 0.78rem;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.col-role {
  flex: 0 0 110px;
  width: 110px;
}

/* 权限徽标 */
.role-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 500;
  white-space: nowrap;
}

.role-badge.admin {
  background: rgba(16, 185, 129, 0.1);
  color: #047857;
}

.role-badge.normal {
  background: #f1f5f9;
  color: #64748b;
}

.role-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.col-time {
  flex: 0 0 140px;
  width: 140px;
  font-size: 0.82rem;
  color: #94a3b8;
}

/* 时间字段前缀标签：窄屏（表格转卡片，表头隐藏）时才显示，用于区分三个时间列 */
.time-label {
  display: none;
}

/* 勾选圈（沿用历史对话批量管理样式）：视觉仍为 22px，靠伪元素把可点区域扩到 44×44 */
.check-circle {
  position: relative;
  width: 22px;
  height: 22px;
  padding: 0;
  border-radius: 50%;
  border: 1.5px solid #cbd5e1;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  transition: background 0.2s, border-color 0.2s;
}

.check-circle:hover:not(:disabled) {
  border-color: #10b981;
}

.check-circle.selected {
  background: #10b981;
  border-color: #10b981;
}

.check-circle:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.check-circle .icon {
  width: 13px;
  height: 13px;
}

.check-circle::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 44px;
  height: 44px;
  margin: -22px 0 0 -22px;
  border-radius: 50%;
}

/* ===== 空态 / 加载 ===== */
.state-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 56px 20px;
  color: #94a3b8;
  font-size: 0.9rem;
}

.state-icon {
  width: 32px;
  height: 32px;
  color: #a7f3d0;
}

.state-icon.error {
  color: #fca5a5;
}

.retry-btn {
  height: 44px;
  padding: 0 20px;
  border-radius: 12px;
  border: 1px solid rgba(16, 185, 129, 0.2);
  background: rgba(255, 255, 255, 0.8);
  color: #059669;
  font-size: 0.9rem;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.2s ease;
}

.retry-btn:hover {
  background: rgba(16, 185, 129, 0.12);
}

/* ===== 分页 ===== */
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 0 4px;
}

.pager-info {
  font-size: 0.85rem;
  color: #64748b;
}

.pager-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager-btn {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  border: 1px solid rgba(16, 185, 129, 0.2);
  background: rgba(255, 255, 255, 0.8);
  color: #10b981;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.pager-btn:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.12);
}

.pager-btn:disabled {
  color: #cbd5e1;
  border-color: #e2e8f0;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.5);
}

.pager-current {
  font-size: 0.85rem;
  color: #1e293b;
  min-width: 56px;
  text-align: center;
}

/* ===== 批量操作条 ===== */
.batch-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid rgba(16, 185, 129, 0.12);
  box-shadow: 0 -6px 24px rgba(15, 23, 42, 0.06);
}

.batch-bar-btn {
  flex: 1;
  min-width: 0;
  height: 44px;
  padding: 0 10px;
  border-radius: 12px;
  border: none;
  background: #f8fafc;
  color: #1e293b;
  font-size: 0.9rem;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.batch-bar-btn:hover:not(:disabled) {
  background: #f1f5f9;
}

.batch-bar-btn.cancel {
  flex: 0 0 auto;
  padding: 0 18px;
}

.batch-bar-btn.grant {
  color: #059669;
}

.batch-bar-btn.grant:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.1);
}

.batch-bar-btn.revoke {
  color: #ef4444;
}

.batch-bar-btn.revoke:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.08);
}

.batch-bar-btn:disabled {
  color: #d1d5db;
  cursor: not-allowed;
  background: #f8fafc;
}

/* ===== 弹窗（沿用玻璃拟态） ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(16, 185, 129, 0.08);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  width: 340px;
  max-width: 100%;
  padding: 2rem;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 16px 48px rgba(16, 185, 129, 0.12);
  text-align: center;
  animation: modalSlideUp 0.25s ease;
}

@keyframes modalSlideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e1b4b;
  margin-bottom: 0.5rem;
}

.modal-desc {
  font-size: 0.9rem;
  color: #64748b;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
}

.modal-btn {
  flex: 1;
  min-height: 44px;
  padding: 0.6rem;
  border-radius: 10px;
  border: none;
  font-size: 0.9rem;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-btn.cancel {
  background: #f1f5f9;
  color: #64748b;
}

.modal-btn.cancel:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.modal-btn.confirm {
  background: #ef4444;
  color: #fff;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.modal-btn.confirm:hover:not(:disabled) {
  background: #dc2626;
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.4);
  transform: translateY(-1px);
}

.modal-btn.confirm.green {
  background: linear-gradient(135deg, #34d399, #059669);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}

.modal-btn.confirm.green:hover:not(:disabled) {
  background: linear-gradient(135deg, #10b981, #047857);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.4);
  transform: translateY(-1px);
}

.modal-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* ===== 加载转圈 ===== */
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(16, 185, 129, 0.2);
  border-top-color: #10b981;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 可访问性 ===== */
.back-link:focus-visible,
.ghost-btn:focus-visible,
.sort-trigger:focus-visible,
.sort-option:focus-visible,
.pager-btn:focus-visible,
.check-circle:focus-visible,
.modal-btn:focus-visible,
.batch-bar-btn:focus-visible,
.retry-btn:focus-visible {
  outline: 2px solid #10b981;
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation-duration: 2s;
  }

  .modal-overlay,
  .modal-content {
    animation: none;
  }

  .modal-btn.confirm:hover:not(:disabled) {
    transform: none;
  }

  .sort-menu-enter-active,
  .sort-menu-leave-active,
  .sort-caret {
    transition: none;
  }
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .col-time {
    flex: 0 0 120px;
    width: 120px;
  }
}

@media (max-width: 768px) {
  .user-page {
    padding: 1rem 1rem 6rem;
  }

  .page-title {
    font-size: 1.25rem;
  }

  .toolbar {
    padding: 10px 12px;
  }

  /* 表格转卡片：表头隐藏，元信息换行展示 */
  .table-head {
    display: none;
  }

  .table-row {
    flex-wrap: wrap;
    row-gap: 8px;
    padding: 14px 16px;
    align-items: flex-start;
  }

  .col-check {
    order: 0;
    flex: 0 0 44px;
  }

  .col-name {
    order: 1;
    flex: 1 1 50%;
  }

  .col-role {
    order: 2;
    flex: 0 0 auto;
    width: auto;
    margin-left: auto;
  }

  .col-time {
    order: 3;
    flex: 0 0 auto;
    width: auto;
    font-size: 0.78rem;
  }

  /* 表头隐藏后靠标签区分三个时间字段 */
  .time-label {
    display: inline;
    margin-right: 4px;
    color: #94a3b8;
  }
}

@media (max-width: 480px) {
  .header-texts {
    flex: 1 1 100%;
    order: 2;
  }

  .back-link {
    order: 1;
  }

  .batch-bar-btn.grant,
  .batch-bar-btn.revoke {
    font-size: 0.82rem;
    padding: 0 6px;
  }
}
</style>
