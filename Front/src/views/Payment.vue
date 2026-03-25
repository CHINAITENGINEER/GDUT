<template>
  <div class="payment-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h2>支付确认</h2>
    </div>

    <el-card v-loading="loading">
      <div class="payment-info" v-if="task">
        <h3>任务信息</h3>
        <div class="info-row">
          <span>任务标题</span>
          <span>{{ task.title }}</span>
        </div>
        <div class="info-row">
          <span>任务金额</span>
          <span class="amount">¥{{ task.amount.toFixed(2) }}</span>
        </div>
      </div>

      <el-divider />

      <div class="payment-method">
        <h3>支付方式</h3>
        <el-radio-group v-model="payType">
          <el-radio :value="0">
            <div class="pay-option">
              <el-icon :size="28"><Coin /></el-icon>
              <span>余额支付</span>
            </div>
          </el-radio>
          <el-radio :value="1">
            <div class="pay-option">
              <el-icon :size="28"><ChatDotRound /></el-icon>
              <span>微信支付</span>
            </div>
          </el-radio>
        </el-radio-group>
      </div>

      <el-divider />

      <div class="payment-total" v-if="task">
        <span>应付总额</span>
        <span class="amount">¥{{ task.amount.toFixed(2) }}</span>
      </div>

      <el-button type="primary" size="large" :loading="paying" @click="handlePay">
        立即支付
      </el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Coin, ChatDotRound } from '@element-plus/icons-vue'
import { taskApi } from '@/api/task'
import { paymentApi } from '@/api/payment'
import type { TaskDetailVO } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paying = ref(false)
const task = ref<TaskDetailVO | null>(null)
const payType = ref<0 | 1>(0)

async function loadTask() {
  loading.value = true
  try {
    const taskId = route.query.taskId as string
    task.value = await taskApi.detail(taskId)
  } catch (e) {}
  loading.value = false
}

async function handlePay() {
  if (!task.value) return
  paying.value = true
  try {
    await paymentApi.pay({
      taskId: task.value.id,
      payType: payType.value
    })
    ElMessage.success('支付成功')
    router.push(`/task/${task.value.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '支付失败')
  }
  paying.value = false
}

onMounted(() => {
  if (route.query.taskId) {
    loadTask()
  }
})
</script>

<style scoped lang="scss">
.payment-page {
  max-width: 600px;
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

.payment-info,
.payment-method,
.payment-total {
  h3 {
    margin-bottom: 16px;
    font-size: 16px;
  }
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;

  .amount {
    font-size: 20px;
    font-weight: 600;
    color: #f56c6c;
  }
}

.payment-method {
  .el-radio-group {
    display: flex;
    gap: 20px;
  }

  .pay-option {
    display: flex;
    align-items: center;
    gap: 8px;

    .el-icon {
      color: #409eff;
    }
  }
}

.payment-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .amount {
    font-size: 28px;
    font-weight: 700;
    color: #f56c6c;
  }
}

.el-button[type='primary'] {
  width: 100%;
}
</style>
