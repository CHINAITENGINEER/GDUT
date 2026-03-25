<template>
  <div class="trade-stats">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">??</div>
        <div class="stat-info">
          <div class="stat-value">?{{ (stats?.totalFeeIncome || 0).toFixed(2) }}</div>
          <div class="stat-label">??????</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">??</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats?.totalTaskCount || 0 }}</div>
          <div class="stat-label">??????</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">??</div>
        <div class="stat-info">
          <div class="stat-value">?{{ (stats?.totalTaskAmount || 0).toFixed(2) }}</div>
          <div class="stat-label">?????</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">??</div>
        <div class="stat-info">
          <div class="stat-value">{{ ((stats?.avgFeeRate || 0) * 100).toFixed(2) }}%</div>
          <div class="stat-label">??????</div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="toolbar">
        <span class="section-title">????</span>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="?"
          start-placeholder="????"
          end-placeholder="????"
          value-format="x"
          style="width: 260px"
          @change="onDateChange"
        />
      </div>
      <el-table :data="trades" v-loading="loading" stripe>
        <el-table-column prop="taskId" label="??ID" width="120" />
        <el-table-column label="????" width="120" align="right">
          <template #default="{ row }: any"><span class="money">?{{ row.taskAmount?.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="???" width="110" align="right">
          <template #default="{ row }: any"><span class="fee">?{{ row.feeAmount?.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="????" width="120" align="right">
          <template #default="{ row }: any"><span class="real">?{{ row.realAmount?.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="????" width="100" align="center">
          <template #default="{ row }: any">{{ (row.feeDiscount * 100).toFixed(0) }}%</template>
        </el-table-column>
        <el-table-column label="?????" width="110" align="center">
          <template #default="{ row }: any"><span class="level">Lv.{{ row.levelAtSettle }}</span></template>
        </el-table-column>
        <el-table-column label="????" min-width="160">
          <template #default="{ row }: any">{{ formatTime(row.settledAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="query.page" :page-size="query.pageSize" :total="total"
          layout="total, prev, pager, next" @current-change="loadTrades" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { formatTime } from '@/utils/format'
import type { TradeStatsVO, TradeRecordVO } from '@/types'

const loading = ref(false)
const stats = ref<TradeStatsVO | null>(null)
const trades = ref<TradeRecordVO[]>([])
const total = ref(0)
const dateRange = ref<[number, number] | null>(null)
const query = reactive({ page: 1, pageSize: 10 })

async function loadStats() {
  try {
    const params: any = {}
    if (dateRange.value) { params.startTime = dateRange.value[0]; params.endTime = dateRange.value[1] }
    const res = await adminApi.getTradeStats(params)
    stats.value = res as TradeStatsVO
  } catch (e) {}
}

async function loadTrades() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value) { params.startTime = dateRange.value[0]; params.endTime = dateRange.value[1] }
    const res = await adminApi.getTrades(params)
    trades.value = (res as any).records ?? []
    total.value = (res as any).total ?? 0
  } catch (e) {}
  loading.value = false
}

function onDateChange() { loadStats(); loadTrades() }

onMounted(() => { loadStats(); loadTrades() })
</script>

<style scoped lang="scss">
$primary: #4f46e5; $border: #e5e7eb; $text-main: #111827; $text-sub: #6b7280;
.trade-stats { display: flex; flex-direction: column; gap: 16px; }
.stats-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px;
}
.stat-card {
  background: #fff; border: 1px solid $border; border-radius: 10px; padding: 20px;
  display: flex; align-items: center; gap: 14px;
  .stat-icon { font-size: 28px; }
  .stat-value { font-size: 22px; font-weight: 800; color: $text-main; }
  .stat-label { font-size: 13px; color: $text-sub; margin-top: 2px; }
}
.table-card {
  background: #fff; border: 1px solid $border; border-radius: 10px; overflow: hidden;
  :deep(.el-table) { --el-table-header-bg-color: #f9fafb; --el-table-border-color: #{$border}; }
}
.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid $border;
  .section-title { font-size: 15px; font-weight: 700; color: $text-main; }
}
.money { font-weight: 700; color: $text-main; }
.fee { font-weight: 600; color: #dc2626; }
.real { font-weight: 700; color: #059669; }
.level { display: inline-block; padding: 2px 8px; background: #ede9fe; color: $primary; border-radius: 20px; font-size: 12px; font-weight: 700; }
.pagination { padding: 16px 20px; display: flex; justify-content: flex-end; border-top: 1px solid $border; }
</style>
