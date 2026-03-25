<template>
  <div class="home-page">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { useTheme } from '@/composables/useTheme'
import { taskApi } from '@/api/task'
import type { TaskCardVO } from '@/types'
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
const total = ref(0)
const query = reactive({ page: 1, pageSize: 12 })

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

function handleSearch() {
  query.page = 1
  loadTasks()
}

function selectCategory(val: number | '') {
  selectedCategory.value = val
  handleSearch()
}

async function switchRole(role: 'publisher' | 'acceptor') {
  if (userStore.currentRole === role) return
  try {
    await userStore.switchRole(role)
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
  } else if (cmd === 'admin') {
    router.push('/admin')
  }
}

onMounted(loadTasks)
</script>

<style scoped lang="scss">
$border: #3a4556;
$bg: #1a1f2e;
$card-bg: #252d3d;
$text: #e8eaed;
$text-sub: #a8adb5;
$accent: #5b9ef5;

.home-page {
  min-height: 100vh;
  background: $bg;
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

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
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

body.theme-light {
  .home-page { background: #f3f4f6; color: #111827; }
  .topbar, .filter-bar { background: #ffffff; border-color: #e5e7eb; }
  .brand { color: #111827; }
  .cat-tag { border-color: #e5e7eb; color: #6b7280;
    &.active { background: #4f46e5; border-color: #4f46e5; color: #fff; }
  }
}
</style>
