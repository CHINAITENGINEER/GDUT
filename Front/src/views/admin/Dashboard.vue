<template>
  <div class="admin-dashboard">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">待审核任务</div>
        <div class="stat-value">{{ pendingTotal }}</div>
        <router-link to="/admin/tasks/pending" class="stat-link">去审核</router-link>
      </div>
      <div class="stat-card">
        <div class="stat-label">平台手续费总收入</div>
        <div class="stat-value">¥{{ (tradeStats?.totalFeeIncome || 0).toFixed(2) }}</div>
        <router-link to="/admin/trades" class="stat-link">查看交易</router-link>
      </div>
      <div class="stat-card">
        <div class="stat-label">总结算任务数</div>
        <div class="stat-value">{{ tradeStats?.totalTaskCount || 0 }}</div>
        <router-link to="/admin/trades" class="stat-link">查看详情</router-link>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均手续费率</div>
        <div class="stat-value">{{ ((tradeStats?.avgFeeRate || 0) * 100).toFixed(2) }}%</div>
        <router-link to="/admin/fee-config" class="stat-link">配置费率</router-link>
      </div>
    </div>

    <div class="quick-grid">
      <router-link to="/admin/users" class="quick-card">
        <div class="quick-title">用户管理</div>
        <div class="quick-desc">查看用户状态、禁用或启用账号。</div>
      </router-link>
      <router-link to="/admin/disputes" class="quick-card">
        <div class="quick-title">争议处理</div>
        <div class="quick-desc">处理任务纠纷并记录处理意见。</div>
      </router-link>
      <router-link to="/admin/tasks/pending" class="quick-card">
        <div class="quick-title">任务审核</div>
        <div class="quick-desc">审核新发布任务并驳回不合规内容。</div>
      </router-link>
      <router-link to="/admin/trades" class="quick-card">
        <div class="quick-title">交易统计</div>
        <div class="quick-desc">查看结算记录与平台收益趋势。</div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import type { TradeStatsVO } from '@/types'

const tradeStats = ref<TradeStatsVO | null>(null)
const pendingTotal = ref(0)

async function loadDashboard() {
  try {
    const [statsRes, pendingRes] = await Promise.all([
      adminApi.getTradeStats(),
      adminApi.getPendingTasks({ page: 1, pageSize: 1 })
    ])
    tradeStats.value = statsRes as TradeStatsVO
    pendingTotal.value = (pendingRes as any).total || 0
  } catch (e) {}
}

onMounted(loadDashboard)
</script>

<style scoped lang="scss">
$primary: #4f46e5;
$border: #e5e7eb;
$text-main: #111827;
$text-sub: #6b7280;

.admin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  background: #fff;
  border: 1px solid $border;
  border-radius: 10px;
  padding: 18px;

  .stat-label {
    font-size: 13px;
    color: $text-sub;
  }

  .stat-value {
    margin-top: 10px;
    font-size: 28px;
    line-height: 1;
    font-weight: 800;
    color: $text-main;
  }

  .stat-link {
    display: inline-block;
    margin-top: 12px;
    font-size: 13px;
    color: $primary;
    text-decoration: none;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.quick-card {
  display: block;
  background: #fff;
  border: 1px solid $border;
  border-radius: 10px;
  padding: 18px;
  text-decoration: none;
  transition: all 0.15s ease;

  &:hover {
    border-color: $primary;
    transform: translateY(-1px);
  }

  .quick-title {
    font-size: 16px;
    font-weight: 700;
    color: $text-main;
  }

  .quick-desc {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.6;
    color: $text-sub;
  }
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
