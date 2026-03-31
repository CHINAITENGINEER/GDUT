<template>
  <div class="home-page">
    <!-- 推荐任务弹窗 -->
    <el-dialog
      v-model="showRecommendDialog"
      title="✨ 为你推荐任务"
      width="760px"
      :close-on-click-modal="true"
      class="recommend-dialog"
    >
      <div v-if="recommendList.length === 0" class="dialog-empty">
        <EmptyState description="暂无推荐任务，试试完善你的推荐画像" />
        <el-button type="primary" style="margin-top:16px" @click="goToRecommendation">去完善画像</el-button>
      </div>
      <div v-else class="dialog-grid">
        <article v-for="item in recommendList" :key="item.task.id" class="recommend-card">
          <div class="recommend-meta">
            <span class="recommend-score">匹配度 {{ toPercent(item.score) }}</span>
            <span class="recommend-reason">{{ item.recommendReason }}</span>
          </div>
          <TaskCard :task="item.task" @click="showRecommendDialog = false" />
        </article>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-checkbox v-model="neverShowAgain" @change="handleNeverShow">不再自动推荐</el-checkbox>
          <div class="dialog-actions">
            <el-button text @click="goToRecommendation">查看更多推荐</el-button>
            <el-button type="primary" @click="showRecommendDialog = false">知道了</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <header class="topbar">
      <div class="topbar-left">
        <span class="brand">🎓 校园任务</span>
      </div>
      <div class="topbar-center">
        <el-input
          v-model="keyword"
          placeholder="搜索任务..."
          clearable
          style="width: 320px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="topbar-right">
        <el-button type="success" @click="$router.push('/rally')">实时组队</el-button>
        <el-button v-if="userStore.isAcceptor" type="warning" @click="$router.push('/recommendation')">✨ 推荐任务</el-button>
        <el-button-group class="role-switch">
          <el-button
            :type="userStore.isPublisher ? 'success' : 'default'"
            @click="switchRole('publisher')"
          >
            发布者
          </el-button>
          <el-button
            :type="userStore.isAcceptor ? 'primary' : 'default'"
            @click="switchRole('acceptor')"
          >
            接单者
          </el-button>
        </el-button-group>
        <el-button v-if="userStore.isPublisher" type="primary" @click="$router.push('/task/publish')">发布任务</el-button>
        <el-badge :value="messageStore.unreadCount || 0" :hidden="!messageStore.unreadCount">
          <el-button :icon="Bell" circle @click="$router.push('/messages')" />
        </el-badge>
        <el-dropdown @command="handleCommand">
          <UserAvatar :src="userStore.userInfo?.avatar" :name="userStore.userInfo?.nickname" :size="36" style="cursor:pointer" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人主页</el-dropdown-item>
              <el-dropdown-item command="myTasks">我的任务</el-dropdown-item>
              <el-dropdown-item command="level">等级中心</el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAcceptor" command="recommendation">推荐中心</el-dropdown-item>
              <el-dropdown-item command="paymentRecords">支付记录</el-dropdown-item>
              <el-dropdown-item command="settlementRecords">结算记录</el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>管理后台</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-switch
          v-model="isDark"
          inline-prompt
          active-text="🌙"
          inactive-text="☀️"
          @change="toggleTheme"
        />
      </div>
    </header>

    <section v-if="userStore.isAcceptor && recommendList.length" class="recommend-panel">
      <div class="section-head">
        <div>
          <p class="section-kicker">ADAPTIVE AGENT</p>
          <h2>为你推荐</h2>
          <p class="section-desc">基于你的技能标签、偏好分类、佣金接受区间和距离限制进行智能匹配。</p>
        </div>
        <el-button text type="primary" @click="loadRecommendations">刷新推荐</el-button>
      </div>

      <div class="recommend-grid">
        <article v-for="item in recommendList" :key="item.task.id" class="recommend-card">
          <div class="recommend-meta">
            <span class="recommend-score">匹配度 {{ toPercent(item.score) }}</span>
            <span class="recommend-reason">{{ item.recommendReason }}</span>
          </div>
          <TaskCard :task="item.task" />
        </article>
      </div>
    </section>

    <div class="filter-bar">
      <div class="category-list">
        <span
          v-for="cat in categories"
          :key="cat.value"
          class="cat-tag"
          :class="{ active: selectedCategory === cat.value }"
          @click="selectCategory(cat.value)"
        >{{ cat.label }}</span>
      </div>
      <div class="sort-bar">
        <el-select v-model="sortBy" size="small" @change="handleSearch" style="width:120px">
          <el-option label="最新发布" value="newest" />
          <el-option label="金额最低" value="amount_asc" />
          <el-option label="金额最高" value="amount_desc" />
        </el-select>
        <el-select v-model="roleFilter" size="small" @change="handleSearch" style="width:120px">
          <el-option label="全部任务" value="" />
          <el-option label="我发布的" value="published" />
          <el-option label="我接的" value="grabbed" />
        </el-select>
      </div>
    </div>

    <div v-loading="loading" class="task-grid-area">
      <div v-if="taskList.length === 0 && !loading" class="empty-wrap">
        <EmptyState description="暂无任务" show-action @action="$router.push('/task/publish')" />
      </div>
      <div v-else class="task-grid">
        <TaskCard v-for="task in taskList" :key="task.id" :task="task" />
      </div>

      <div v-if="total > query.pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadTasks"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { useTheme } from '@/composables/useTheme'
import { taskApi } from '@/api/task'
import { recommendationApi } from '@/api/recommendation'
import type { TaskCardVO, RecommendedTaskVO } from '@/types'
import TaskCard from '@/components/TaskCard.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const { isDark, toggleTheme } = useTheme()

const loading = ref(false)
const keyword = ref('')
const selectedCategory = ref<number | ''>('')
const sortBy = ref<'newest' | 'amount_asc' | 'amount_desc'>('newest')
const roleFilter = ref<'' | 'published' | 'grabbed'>('')
const taskList = ref<TaskCardVO[]>([])
const recommendList = ref<RecommendedTaskVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 12 })

// 推荐弹窗状态
const showRecommendDialog = ref(false)
const neverShowAgain = ref(false)

const NEVER_SHOW_KEY = computed(() => `recommend_dialog_hidden_${userStore.userInfo?.id}`)

function handleNeverShow(val: boolean | string | number) {
  if (val) {
    localStorage.setItem(NEVER_SHOW_KEY.value, '1')
  } else {
    localStorage.removeItem(NEVER_SHOW_KEY.value)
  }
}

function goToRecommendation() {
  showRecommendDialog.value = false
  router.push('/recommendation')
}

const categories: Array<{ label: string; value: number | '' }> = [
  { label: '全部', value: '' },
  { label: '学习辅导', value: 1 },
  { label: '跑腿代购', value: 2 },
  { label: '技能服务', value: 3 },
  { label: '设计创作', value: 4 },
  { label: '其他', value: 5 },
]

async function loadTasks() {
  loading.value = true
  try {
    let res

    if (roleFilter.value === 'published') {
      res = await taskApi.myPublished({ page: query.page, pageSize: query.pageSize })
    } else if (roleFilter.value === 'grabbed') {
      res = await taskApi.myGrabbed({ page: query.page, pageSize: query.pageSize })
    } else {
      const params: { page: number; pageSize: number; keyword?: string; category?: number; sortBy: 'newest' | 'amount_asc' | 'amount_desc' } = {
        page: query.page,
        pageSize: query.pageSize,
        sortBy: sortBy.value
      }
      if (keyword.value.trim()) params.keyword = keyword.value.trim()
      if (selectedCategory.value !== '') params.category = selectedCategory.value
      res = await taskApi.list(params)
    }

    taskList.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (e: any) {
    taskList.value = []
    total.value = 0
    ElMessage.error(e.message || '加载任务失败')
  } finally {
    loading.value = false
  }
}

async function loadRecommendations() {
  if (!userStore.isAcceptor) {
    recommendList.value = []
    return
  }
  try {
    recommendList.value = await recommendationApi.recommendTasks(6)
  } catch {
    recommendList.value = []
  }
}

function maybeShowRecommendDialog() {
  if (!userStore.isAcceptor) return
  const hidden = localStorage.getItem(NEVER_SHOW_KEY.value)
  if (hidden) {
    neverShowAgain.value = true
    return
  }
  if (recommendList.value.length > 0) {
    showRecommendDialog.value = true
  }
}

function handleSearch() {
  query.page = 1
  loadTasks()
}

function selectCategory(val: number | '') {
  selectedCategory.value = val
  handleSearch()
}

function toPercent(score: number) {
  return `${Math.round(score * 100)}%`
}

async function switchRole(role: 'publisher' | 'acceptor') {
  if (userStore.currentRole === role) return
  try {
    await userStore.switchRole(role)
    await loadRecommendations()
    ElMessage.success(role === 'publisher' ? '已切换为发布者' : '已切换为接单者')
  } catch (e: any) {
    ElMessage.error(e.message || '切换身份失败')
  }
}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push(`/user/${userStore.userInfo?.id}`)
  } else if (cmd === 'myTasks') {
    router.push('/my/tasks')
  } else if (cmd === 'level') {
    router.push('/level')
  } else if (cmd === 'paymentRecords') {
    router.push('/payment/records')
  } else if (cmd === 'settlementRecords') {
    router.push('/settlement/records')
  } else if (cmd === 'recommendation') {
    router.push('/recommendation')
  } else if (cmd === 'admin') {
    router.push('/admin')
  }
}

onMounted(async () => {
  await Promise.all([loadTasks(), loadRecommendations()])
  maybeShowRecommendDialog()
})
</script>

<style scoped lang="scss">
$border: #3a4556;
$bg: #1a1f2e;
$card-bg: #252d3d;
$text: #e8eaed;
$text-sub: #a8adb5;
$accent: #5b9ef5;
$accent-2: #63e6be;

.home-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(91, 158, 245, 0.14), transparent 24%),
    radial-gradient(circle at top left, rgba(99, 230, 190, 0.12), transparent 20%),
    $bg;
  color: $text;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  background: $card-bg;
  border-bottom: 1px solid $border;
  position: sticky;
  top: 0;
  z-index: 100;

  .brand {
    font-size: 18px;
    font-weight: 800;
    color: $text;
    letter-spacing: 0.5px;
  }

  .topbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.role-switch :deep(.el-button) {
  min-width: 72px;
}

.recommend-panel {
  margin: 24px 24px 0;
  padding: 24px;
  border: 1px solid rgba(91, 158, 245, 0.25);
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(37, 45, 61, 0.95), rgba(37, 45, 61, 0.7));
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;

  h2 {
    margin: 6px 0;
    font-size: 26px;
  }
}

.section-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: $accent-2;
}

.section-desc {
  margin: 0;
  color: $text-sub;
  font-size: 14px;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.recommend-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recommend-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.recommend-score,
.recommend-reason {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: 999px;
  padding: 0 12px;
  font-size: 12px;
}

.recommend-score {
  background: rgba(99, 230, 190, 0.16);
  color: #8ef0d0;
  border: 1px solid rgba(99, 230, 190, 0.25);
  font-weight: 700;
}

.recommend-reason {
  background: rgba(91, 158, 245, 0.14);
  color: #9fc4ff;
  border: 1px solid rgba(91, 158, 245, 0.2);
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  margin-top: 20px;
  background: $card-bg;
  border-bottom: 1px solid $border;

  .category-list {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .cat-tag {
    padding: 5px 14px;
    border-radius: 20px;
    font-size: 13px;
    cursor: pointer;
    border: 1px solid $border;
    color: $text-sub;
    transition: all 0.15s;

    &:hover { border-color: $accent; color: $accent; }
    &.active { background: $accent; border-color: $accent; color: #fff; }
  }

  .sort-bar {
    display: flex;
    gap: 8px;
  }
}

.task-grid-area {
  padding: 24px;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.empty-wrap {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

// 推荐弹窗
.recommend-dialog {
  :deep(.el-dialog__header) {
    padding: 20px 24px 12px;
    font-size: 18px;
    font-weight: 800;
  }
  :deep(.el-dialog__body) {
    padding: 0 24px 8px;
    max-height: 60vh;
    overflow-y: auto;
  }
  :deep(.el-dialog__footer) {
    padding: 12px 24px 20px;
    border-top: 1px solid rgba(58, 69, 86, 0.6);
  }
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  padding: 8px 0 4px;
}

.dialog-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.dialog-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

body.theme-light {
  .home-page {
    background:
      radial-gradient(circle at top right, rgba(79, 70, 229, 0.08), transparent 24%),
      radial-gradient(circle at top left, rgba(16, 185, 129, 0.08), transparent 20%),
      #f3f4f6;
    color: #111827;
  }

  .topbar, .filter-bar { background: #ffffff; border-color: #e5e7eb; }
  .brand { color: #111827; }
  .recommend-panel {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94));
    border-color: rgba(79, 70, 229, 0.12);
  }
  .section-desc { color: #6b7280; }
  .recommend-score {
    background: rgba(16, 185, 129, 0.10);
    color: #047857;
    border-color: rgba(16, 185, 129, 0.18);
  }
  .recommend-reason {
    background: rgba(79, 70, 229, 0.08);
    color: #4338ca;
    border-color: rgba(79, 70, 229, 0.15);
  }
  .cat-tag {
    border-color: #e5e7eb;
    color: #6b7280;
    &.active { background: #4f46e5; border-color: #4f46e5; color: #fff; }
  }
}
</style>
