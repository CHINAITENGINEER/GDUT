<template>
  <div class="my-tasks-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.push('/')">返回</el-button>
      <h2>我的任务</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="我发布的" name="published" />
      <el-tab-pane label="我抢单的" name="grabbed" />
    </el-tabs>

    <div class="filter-bar">
      <span>状态筛选：</span>
      <el-select v-model="statusFilter" placeholder="全部" clearable @change="handleFilterChange">
        <el-option label="全部" :value="undefined" />
        <el-option label="待审核" :value="0" />
        <el-option label="待接单" :value="1" />
        <el-option label="已抢单待协商" :value="2" />
        <el-option label="待支付" :value="3" />
        <el-option label="进行中" :value="4" />
        <el-option label="已完成" :value="5" />
        <el-option label="已结算" :value="6" />
        <el-option label="已互评" :value="7" />
        <el-option label="已取消" :value="8" />
      </el-select>
    </div>

    <div v-loading="loading" class="task-list">
      <div v-if="taskList.length === 0" class="empty-wrap">
        <EmptyState
          :description="activeTab === 'published' ? '暂无发布的任务' : '暂无抢单的任务'"
          :show-action="activeTab === 'published'"
        />
      </div>
      <div v-else class="card-grid">
        <TaskCard v-for="task in taskList" :key="task.id" :task="task" />
      </div>

      <div v-if="taskList.length > 0" class="pagination-wrap">
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
import { ArrowLeft } from '@element-plus/icons-vue'
import { taskApi } from '@/api/task'
import type { TaskCardVO } from '@/types'
import TaskCard from '@/components/TaskCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const activeTab = ref<'published' | 'grabbed'>('published')
const taskList = ref<TaskCardVO[]>([])
const total = ref(0)
const statusFilter = ref<number | undefined>(undefined)

const query = reactive({
  page: 1,
  pageSize: 12
})

async function loadTasks() {
  loading.value = true
  try {
    const params: { page: number; pageSize: number; status?: number } = {
      page: query.page,
      pageSize: query.pageSize
    }
    if (statusFilter.value !== undefined) {
      params.status = statusFilter.value
    }

    const res = activeTab.value === 'published'
      ? await taskApi.myPublished(params)
      : await taskApi.myGrabbed(params)

    taskList.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    taskList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  query.page = 1
  statusFilter.value = undefined
  loadTasks()
}

function handleFilterChange() {
  query.page = 1
  loadTasks()
}

onMounted(loadTasks)
</script>

<style scoped lang="scss">
.my-tasks-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;

  h2 {
    flex: 1;
  }
}

.filter-bar {
  margin-bottom: 20px;

  .el-select {
    width: 150px;
  }
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
