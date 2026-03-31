<template>
  <div class="task-detail-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h2>任务详情</h2>
    </div>

    <div v-loading="loading">
      <el-card v-if="task" class="detail-card">
        <div class="task-title-row">
          <h1>{{ task.title }}</h1>
          <TaskStatusTag :status="task.status" :status-name="task.statusName" />
        </div>

        <div class="task-meta">
          <CategoryTag :category="task.category" :category-name="task.categoryName" />
          <span class="amount">¥{{ task.amount.toFixed(2) }}</span>
          <span class="delivery">{{ task.deliveryType === 0 ? '线上交付' : '线下交付' }}</span>
          <span class="deadline">截止：{{ formatTime(task.deadline) }}</span>
        </div>

        <el-divider />

        <div class="task-desc">
          <h3>任务描述</h3>
          <p>{{ task.description }}</p>
        </div>

        <div v-if="task.taskImages?.length" class="task-images">
          <h3>任务图片</h3>
          <div class="image-list">
            <el-image
              v-for="(img, idx) in task.taskImages"
              :key="idx"
              :src="img"
              :preview-src-list="task.taskImages"
              fit="cover"
              style="width: 120px; height: 120px; border-radius: 8px"
            />
          </div>
        </div>

        <el-divider />

        <div class="publisher-info">
          <h3>发布者</h3>
          <div class="user-row">
            <UserAvatar :src="task.publisher.avatar" :name="task.publisher.nickname" :size="40" />
            <router-link :to="`/user/${task.publisher.id}`" class="user-name">{{ task.publisher.nickname }}</router-link>
            <span class="credit">信誉分：{{ task.publisher.creditScore }}</span>
          </div>
        </div>

        <div v-if="task.acceptor" class="acceptor-info">
          <el-divider />
          <h3>接单者</h3>
          <div class="user-row">
            <UserAvatar :src="task.acceptor.avatar" :name="task.acceptor.nickname" :size="40" />
            <router-link :to="`/user/${task.acceptor.id}`" class="user-name">{{ task.acceptor.nickname }}</router-link>
            <LevelBadge :level="task.acceptor.level" show-name />
            <span class="discount">手续费折扣：{{ (task.acceptor.feeDiscount * 100).toFixed(0) }}%</span>
          </div>
        </div>

        <el-divider />

        <div class="action-bar">
          <el-button
            v-if="canGrab"
            type="primary"
            size="large"
            :loading="actionLoading"
            @click="handleGrab"
          >
            抢单
          </el-button>
          <el-button
            v-if="canConfirm"
            type="success"
            size="large"
            :loading="actionLoading"
            @click="handleConfirm"
          >
            确认接单
          </el-button>
          <el-button
            v-if="canConfirm"
            type="danger"
            plain
            size="large"
            :loading="actionLoading"
            @click="handleReject"
          >
            拒绝接单
          </el-button>
          <el-button
            v-if="canCancelGrab"
            type="warning"
            plain
            size="large"
            :loading="actionLoading"
            @click="handleCancelGrab"
          >
            取消抢单
          </el-button>
          <el-button
            v-if="canPay"
            type="success"
            size="large"
            @click="goToPay"
          >
            去支付
          </el-button>
          <el-button
            v-if="canSubmit"
            type="primary"
            size="large"
            :loading="actionLoading"
            @click="handleSubmit"
          >
            提交成果
          </el-button>
          <el-button
            v-if="canVerify"
            type="success"
            size="large"
            :loading="actionLoading"
            @click="handleVerify(true)"
          >
            验收通过
          </el-button>
          <el-button
            v-if="canVerify"
            type="danger"
            plain
            size="large"
            :loading="actionLoading"
            @click="handleVerify(false)"
          >
            验收拒绝
          </el-button>
          <el-button
            v-if="canCancel"
            type="danger"
            plain
            size="large"
            :loading="actionLoading"
            @click="handleCancel"
          >
            取消任务
          </el-button>
        </div>

        <div v-if="task.grabRecordId" class="chat-section">
          <ChatPanel :grab-record-id="task.grabRecordId" />
        </div>
      </el-card>

      <EmptyState v-else-if="!loading" description="任务不存在" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { taskApi } from '@/api/task'
import { recommendationApi } from '@/api/recommendation'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/format'
import type { TaskDetailVO } from '@/types'
import TaskStatusTag from '@/components/TaskStatusTag.vue'
import CategoryTag from '@/components/CategoryTag.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import LevelBadge from '@/components/LevelBadge.vue'
import ChatPanel from '@/components/ChatPanel.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const actionLoading = ref(false)
const task = ref<TaskDetailVO | null>(null)

const canGrab = computed(() =>
  task.value?.status === 1 &&
  userStore.isAcceptor &&
  task.value.publisher.id !== userStore.userInfo?.id
)

// status=2: 发布者确认接单
const canConfirm = computed(() =>
  task.value?.status === 2 &&
  task.value.publisher.id === userStore.userInfo?.id
)

// status=2: 接单者取消抢单（协商阶段反悔）
const canCancelGrab = computed(() =>
  task.value?.status === 2 &&
  task.value.acceptor?.id === userStore.userInfo?.id
)

// status=3: 发布者去支付
const canPay = computed(() =>
  task.value?.status === 3 &&
  task.value.publisher.id === userStore.userInfo?.id
)

// status=4: 接单者提交成果
const canSubmit = computed(() =>
  task.value?.status === 4 &&
  task.value.acceptor?.id === userStore.userInfo?.id
)

// status=4: 发布者验收
const canVerify = computed(() =>
  task.value?.status === 4 &&
  task.value.publisher.id === userStore.userInfo?.id
)

// status=0/1: 发布者取消任务
const canCancel = computed(() =>
  (task.value?.status === 1 || task.value?.status === 0) &&
  task.value.publisher.id === userStore.userInfo?.id
)

async function loadTask() {
  loading.value = true
  try {
    task.value = await taskApi.detail(route.params.id as string)
  } catch (e) {}
  loading.value = false
}

async function handleGrab() {
  if (!task.value) return
  actionLoading.value = true
  try {
    await taskApi.grab(task.value.id)
    ElMessage.success('抢单成功，请等待发布者确认')
    await loadTask()
  } catch (e: any) {
    ElMessage.error(e.message || '抢单失败')
  }
  actionLoading.value = false
}

async function handleCancel() {
  if (!task.value) return
  actionLoading.value = true
  try {
    await taskApi.cancel(task.value.id)
    ElMessage.success('已取消任务')
    await loadTask()
  } catch (e: any) {
    ElMessage.error(e.message || '取消失败')
  }
  actionLoading.value = false
}

async function handleConfirm() {
  if (!task.value) return
  actionLoading.value = true
  try {
    await taskApi.confirm(task.value.id)
    ElMessage.success('已确认接单，请发布者完成支付')
    await loadTask()
  } catch (e: any) {
    ElMessage.error(e.message || '确认失败')
  }
  actionLoading.value = false
}

async function handleReject() {
  if (!task.value) return
  actionLoading.value = true
  try {
    await taskApi.reject(task.value.id)
    ElMessage.success('已拒绝接单')
    await loadTask()
  } catch (e: any) {
    ElMessage.error(e.message || '拒绝失败')
  }
  actionLoading.value = false
}

async function handleCancelGrab() {
  if (!task.value) return
  actionLoading.value = true
  try {
    await taskApi.cancelGrab(task.value.id)
    ElMessage.success('已取消抢单')
    await loadTask()
  } catch (e: any) {
    ElMessage.error(e.message || '取消抢单失败')
  }
  actionLoading.value = false
}

async function handleSubmit() {
  if (!task.value) return
  actionLoading.value = true
  try {
    await taskApi.submit(task.value.id, { proofUrls: [] })
    ElMessage.success('成果已提交，等待发布者验收')
    await loadTask()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  }
  actionLoading.value = false
}

async function handleVerify(pass: boolean) {
  if (!task.value) return
  actionLoading.value = true
  try {
    await taskApi.verify(task.value.id, { pass })
    ElMessage.success(pass ? '验收通过' : '已拒绝验收')
    await loadTask()
  } catch (e: any) {
    ElMessage.error(e.message || '验收操作失败')
  }
  actionLoading.value = false
}
function goToPay() {
  router.push({ path: '/payment/pay', query: { taskId: task.value?.id } })
}

onMounted(async () => {
  await loadTask()
  // 接单者查看详情时触发轻量权重反馈，失败静默不影响主流程
  if (userStore.isAcceptor && task.value?.id) {
    recommendationApi.onTaskClick(task.value.id).catch(() => {})
  }
})
</script>

<style scoped lang="scss">
.task-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;

  h2 { flex: 1; }
}

.task-title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;

  h1 { flex: 1; font-size: 22px; }
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  font-size: 14px;
  color: #606266;

  .amount {
    font-size: 20px;
    font-weight: 700;
    color: #f56c6c;
  }
}

.task-desc {
  h3 { margin-bottom: 12px; }
  p { color: #606266; line-height: 1.8; }
}

.task-images {
  margin-top: 16px;
  h3 { margin-bottom: 12px; }
  .image-list { display: flex; gap: 10px; flex-wrap: wrap; }
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;

  .user-name { font-weight: 600; color: #409eff; text-decoration: none; }
  .credit, .discount { font-size: 13px; color: #909399; }
}

.action-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.chat-section {
  margin-top: 24px;
}
</style>
