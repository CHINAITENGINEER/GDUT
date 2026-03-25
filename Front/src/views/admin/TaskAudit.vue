<template>
  <div class="task-audit">
    <div class="toolbar">
      <span class="section-title">待审核任务</span>
      <span class="total-tip">共 {{ total }} 条待审核</span>
    </div>

    <div class="table-card">
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column prop="title" label="任务标题" min-width="200" />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <CategoryTag :category="row.category" :category-name="row.categoryName" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">
            <span class="money">¥{{ row.amount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布者" width="140">
          <template #default="{ row }">
            {{ row.publisher?.nickname }}
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleAudit(row, true)">通过</el-button>
            <el-button type="danger" size="small" @click="openReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadTasks"
        />
      </div>
    </div>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回原因" width="400px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请填写驳回原因"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="auditing" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import { formatTime } from '@/utils/format'
import CategoryTag from '@/components/CategoryTag.vue'

const loading = ref(false)
const auditing = ref(false)
const tasks = ref<any[]>([])
const total = ref(0)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentTask = ref<any>(null)

const query = reactive({ page: 1, pageSize: 10 })

async function loadTasks() {
  loading.value = true
  try {
    const res = await adminApi.getPendingTasks(query)
    tasks.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (e) {}
  loading.value = false
}

async function handleAudit(task: any, pass: boolean) {
  try {
    await adminApi.auditTask(task.id, { pass })
    ElMessage.success(pass ? '已通过审核' : '已驳回')
    loadTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function openReject(task: any) {
  currentTask.value = task
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  auditing.value = true
  try {
    await adminApi.auditTask(currentTask.value.id, { pass: false, rejectReason: rejectReason.value })
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadTasks()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
  auditing.value = false
}

onMounted(loadTasks)
</script>

<style scoped lang="scss">
$primary: #4f46e5;
$border: #e5e7eb;
$text-main: #111827;
$text-sub: #6b7280;

.task-audit { display: flex; flex-direction: column; gap: 16px; }

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 16px 20px;
  border-radius: 10px;
  border: 1px solid $border;

  .section-title { font-size: 15px; font-weight: 700; color: $text-main; }
  .total-tip { font-size: 13px; color: $text-sub; }
}

.table-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid $border;
  overflow: hidden;

  :deep(.el-table) {
    --el-table-border-color: #{$border};
    --el-table-header-bg-color: #f9fafb;
  }
}

.money { font-weight: 600; color: $text-main; }

.pagination {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid $border;
}
</style>
