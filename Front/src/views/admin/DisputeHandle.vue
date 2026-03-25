<template>
  <div class="dispute-handle">
    <div class="toolbar">
      <span class="section-title">争议处理</span>
      <span class="total-tip">共 {{ total }} 条待处理</span>
    </div>

    <div class="table-card">
      <el-table :data="disputes" v-loading="loading" stripe>
        <el-table-column prop="taskId" label="任务ID" width="120" />
        <el-table-column prop="taskTitle" label="任务标题" min-width="180" />
        <el-table-column prop="reason" label="争议原因" min-width="200" />
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 0 ? '待处理' : row.status === 1 ? '已解决' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              @click="openResolve(row)"
            >
              处理
            </el-button>
            <span v-else class="handled-tip">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadDisputes"
        />
      </div>
    </div>

    <!-- 处理弹窗 -->
    <el-dialog v-model="resolveDialogVisible" title="处理争议" width="480px">
      <div class="dispute-info" v-if="currentDispute">
        <div class="info-row"><span>任务：</span><span>{{ currentDispute.taskTitle }}</span></div>
        <div class="info-row"><span>争议原因：</span><span>{{ currentDispute.reason }}</span></div>
      </div>
      <el-divider />
      <el-form :model="resolveForm" label-width="80px">
        <el-form-item label="处理决定">
          <el-radio-group v-model="resolveForm.decision">
            <el-radio value="approve">支持申请方</el-radio>
            <el-radio value="reject">驳回申请</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="resolveForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请填写处理备注"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolving" @click="handleResolve">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const resolving = ref(false)
const disputes = ref<any[]>([])
const total = ref(0)
const resolveDialogVisible = ref(false)
const currentDispute = ref<any>(null)

const query = reactive({ page: 1, pageSize: 10 })
const resolveForm = reactive({ decision: 'approve' as 'approve' | 'reject', remark: '' })

async function loadDisputes() {
  loading.value = true
  try {
    const res = await adminApi.getDisputes(query)
    disputes.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (e) {}
  loading.value = false
}

function openResolve(dispute: any) {
  currentDispute.value = dispute
  resolveForm.decision = 'approve'
  resolveForm.remark = ''
  resolveDialogVisible.value = true
}

async function handleResolve() {
  if (!resolveForm.remark.trim()) {
    ElMessage.warning('请填写处理备注')
    return
  }
  resolving.value = true
  try {
    await adminApi.resolveDispute(currentDispute.value.taskId, {
      decision: resolveForm.decision,
      remark: resolveForm.remark
    })
    ElMessage.success('处理成功')
    resolveDialogVisible.value = false
    loadDisputes()
  } catch (e: any) {
    ElMessage.error(e.message || '处理失败')
  }
  resolving.value = false
}

onMounted(loadDisputes)
</script>

<style scoped lang="scss">
$primary: #4f46e5;
$border: #e5e7eb;
$text-main: #111827;
$text-sub: #6b7280;

.dispute-handle { display: flex; flex-direction: column; gap: 16px; }

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

.dispute-info {
  .info-row {
    display: flex;
    gap: 8px;
    padding: 6px 0;
    font-size: 14px;
    span:first-child { color: $text-sub; min-width: 80px; }
    span:last-child { color: $text-main; }
  }
}

.handled-tip { font-size: 13px; color: $text-sub; }

.pagination {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid $border;
}
</style>
