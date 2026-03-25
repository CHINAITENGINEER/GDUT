<template>
  <div class="records-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h2>支付记录</h2>
    </div>

    <el-card v-loading="loading">
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="taskTitle" label="任务" />
        <el-table-column label="金额">
          <template #default="{ row }">
            ¥{{ row.amount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="支付方式">
          <template #default="{ row }">
            {{ row.payType === 0 ? '余额支付' : '微信支付' }}
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 1 ? 'success' : row.payStatus === 2 ? 'info' : 'warning'">
              {{ ['待支付', '已支付', '已退款'][row.payStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadRecords"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { paymentApi } from '@/api/payment'
import { formatTime } from '@/utils/format'
import type { PaymentRecordVO } from '@/types'

const loading = ref(false)
const records = ref<PaymentRecordVO[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  pageSize: 10
})

async function loadRecords() {
  loading.value = true
  try {
    const res = await paymentApi.getRecords(query)
    records.value = res.records
    total.value = res.total
  } catch (e) {}
  loading.value = false
}

onMounted(loadRecords)
</script>

<style scoped lang="scss">
.records-page {
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

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
