<template>
  <div class="user-manage">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="搜索昵称 / 手机号 / 学号"
          clearable
          style="width: 280px"
          @keyup.enter="loadUsers"
          @clear="loadUsers"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="loadUsers">
          <el-option label="正常" :value="0" />
          <el-option label="已禁用" :value="1" />
        </el-select>
        <el-button type="primary" @click="loadUsers">查询</el-button>
      </div>
      <div class="toolbar-right">
        <span class="total-tip">共 {{ total }} 名用户</span>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar">{{ row.nickname?.charAt(0) }}</el-avatar>
              <div>
                <div class="user-nickname">{{ row.nickname }}</div>
                <div class="user-phone">{{ row.phone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="studentId" label="学号" width="130" />
        <el-table-column label="等级" width="90" align="center">
          <template #default="{ row }">
            <span class="level-badge">Lv.{{ row.level }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信誉分" width="90" align="center" />
        <el-table-column label="余额" width="110" align="right">
          <template #default="{ row }">
            <span class="money">¥{{ (row.balance || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总收入" width="110" align="right">
          <template #default="{ row }">
            <span class="money">¥{{ (row.totalEarned || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              :type="row.status === 0 ? 'danger' : 'success'"
              size="small"
              plain
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 0 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadUsers"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const users = ref<any[]>([])
const total = ref(0)
const keyword = ref('')
const statusFilter = ref<number | null>(null)

const query = reactive({ page: 1, pageSize: 12 })

async function loadUsers() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== null) params.status = statusFilter.value
    const res = await adminApi.getUsers(params)
    users.value = res.records
    total.value = res.total
  } catch (e) {}
  loading.value = false
}

async function handleToggleStatus(user: any) {
  const newStatus = user.status === 0 ? 1 : 0
  const action = newStatus === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${user.nickname}」吗？`, '操作确认', {
      confirmButtonText: action,
      cancelButtonText: '取消',
      type: newStatus === 1 ? 'warning' : 'info'
    })
    await adminApi.updateUserStatus(user.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

onMounted(loadUsers)
</script>

<style scoped lang="scss">
$primary: #4f46e5;
$border: #e5e7eb;
$text-main: #111827;
$text-sub: #6b7280;

.user-manage { display: flex; flex-direction: column; gap: 16px; }

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 16px 20px;
  border-radius: 10px;
  border: 1px solid $border;

  .toolbar-left { display: flex; align-items: center; gap: 10px; }
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
    --el-table-header-text-color: #{$text-sub};
    --el-table-row-hover-bg-color: #fafafa;
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .user-nickname { font-size: 14px; font-weight: 600; color: $text-main; }
  .user-phone { font-size: 12px; color: $text-sub; margin-top: 2px; }
}

.level-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #ede9fe;
  color: $primary;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}

.money { font-weight: 600; color: $text-main; }

.pagination {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid $border;
}
</style>
