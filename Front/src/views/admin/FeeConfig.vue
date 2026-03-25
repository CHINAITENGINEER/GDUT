<template>
  <div class="fee-config">
    <div class="config-header">
      <div>
        <h3>手续费配置</h3>
        <p class="sub">设置不同任务金额区间对应的手续费率（百分比）</p>
      </div>
      <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
    </div>

    <div v-loading="loading" class="config-body">
      <div v-for="(cfg, idx) in configList" :key="idx" class="config-row">
        <div class="row-index">区间 {{ idx + 1 }}</div>
        <div class="row-range">
          <span>¥{{ cfg.minAmount }}</span>
          <span class="sep">~</span>
          <span>{{ cfg.maxAmount != null ? '¥' + cfg.maxAmount : '不限上限' }}</span>
        </div>
        <div class="row-rate">
          <span class="rate-label">手续费率</span>
          <el-input-number v-model="cfg.feeRate" :min="0" :max="10" :step="0.1" :precision="2" size="small" style="width:120px" />
          <span class="rate-unit">%</span>
        </div>
        <div class="row-active">
          <el-switch v-model="cfg.isActive" active-text="启用" inactive-text="停用" />
        </div>
        <div class="row-action">
          <el-button type="danger" plain size="small" @click="removeConfig(idx)">删除</el-button>
        </div>
      </div>

      <el-button class="add-btn" @click="addConfig">+ 新增区间</el-button>

      <el-alert type="info" :closable="false" style="margin-top:16px">
        <template #title>手续费率范围建议为 0% ~ 10%，保存后立即生效。</template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const configList = reactive<any[]>([])

async function loadConfig() {
  loading.value = true
  try {
    const res = await adminApi.getFeeConfig()
    configList.splice(0, configList.length, ...res.configs.map((c: any) => ({ ...c, feeRate: +(c.feeRate * 100).toFixed(2) })))
  } catch (e) {}
  loading.value = false
}

function addConfig() {
  const last = configList[configList.length - 1]
  configList.push({ minAmount: last?.maxAmount ?? 0, maxAmount: null, feeRate: 5, isActive: true })
}

function removeConfig(idx: number) {
  configList.splice(idx, 1)
}

async function handleSave() {
  saving.value = true
  try {
    await adminApi.updateFeeConfig({
      configs: configList.map(c => ({
        minAmount: c.minAmount,
        maxAmount: c.maxAmount,
        feeRate: +(c.feeRate / 100).toFixed(4),
        isActive: c.isActive
      }))
    })
    ElMessage.success('手续费配置已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
  saving.value = false
}

onMounted(loadConfig)
</script>

<style scoped lang="scss">
$primary: #4f46e5;
$border: #e5e7eb;
$text-main: #111827;
$text-sub: #6b7280;

.fee-config { display: flex; flex-direction: column; gap: 0; }

.config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 20px 24px;
  border: 1px solid $border;
  border-radius: 10px 10px 0 0;
  border-bottom: none;

  h3 { font-size: 16px; font-weight: 700; color: $text-main; margin-bottom: 4px; }
  .sub { font-size: 13px; color: $text-sub; }
}

.config-body {
  background: #fff;
  border: 1px solid $border;
  border-radius: 0 0 10px 10px;
  padding: 20px 24px;
}

.config-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 14px 16px;
  background: #f9fafb;
  border: 1px solid $border;
  border-radius: 8px;
  margin-bottom: 10px;

  .row-index { width: 70px; font-size: 13px; font-weight: 700; color: $primary; }
  .row-range {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
    color: $text-main;

    .sep { color: $text-sub; }
  }

  .row-rate {
    display: flex;
    align-items: center;
    gap: 8px;

    .rate-label, .rate-unit { font-size: 13px; color: $text-sub; }
  }

  .row-active { min-width: 100px; }
}

.add-btn {
  margin-top: 4px;
  border-style: dashed;
  width: 100%;
}
</style>
