<template>
  <div class="level-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">??</el-button>
      <h2>????</h2>
    </div>

    <el-card v-loading="loading">
      <div class="level-card">
        <div class="level-badge-large">
          <span class="level-num">Lv.{{ levelInfo?.level }}</span>
          <span class="level-name">{{ levelInfo?.levelName }}</span>
        </div>

        <div class="fee-discount">
          ????????<span>{{ ((levelInfo?.feeDiscount || 1) * 100).toFixed(0) }}%</span>
        </div>

        <div class="exp-progress">
          <div class="progress-label">
            <span>???</span>
            <span>{{ levelInfo?.exp }} / {{ levelInfo?.nextLevelExp || 'MAX' }}</span>
          </div>
          <el-progress :percentage="levelInfo?.progress || 0" :stroke-width="12" />
          <p class="exp-tip">?????? {{ levelInfo?.expToNext || 0 }} exp</p>
        </div>

        <div class="exp-rules">
          <h4>??????</h4>
          <ul>
            <li>???????? � 2</li>
            <li>5??? +10 exp</li>
            <li>4??? +5 exp</li>
          </ul>
        </div>
      </div>

      <el-divider />

      <div class="level-table">
        <h3>????</h3>
        <el-table :data="levelInfo?.levelTable || []" style="width: 100%">
          <el-table-column prop="level" label="??" width="100">
            <template #default="{ row }: any">
              Lv.{{ row.level }}
            </template>
          </el-table-column>
          <el-table-column prop="levelName" label="??" />
          <el-table-column prop="requiredExp" label="?????">
            <template #default="{ row }: any">
              {{ row.requiredExp === 0 ? '-' : row.requiredExp + ' exp' }}
            </template>
          </el-table-column>
          <el-table-column label="?????">
            <template #default="{ row }: any">
              {{ (row.feeDiscount * 100).toFixed(0) }}%
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import type { LevelInfoVO } from '@/types'

const loading = ref(false)
const levelInfo = ref<LevelInfoVO | null>(null)

async function loadLevelInfo() {
  loading.value = true
  try {
    const res = await userApi.getLevelInfo()
    levelInfo.value = res as LevelInfoVO
  } catch (e) {}
  loading.value = false
}

onMounted(() => {
  loadLevelInfo()
})
</script>

<style scoped lang="scss">
.level-page {
  max-width: 800px;
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

.level-card {
  text-align: center;
  padding: 20px;

  .level-badge-large {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 30px 60px;
    border-radius: 12px;
    margin-bottom: 20px;

    .level-num {
      font-size: 48px;
      font-weight: 700;
    }

    .level-name {
      font-size: 18px;
      margin-top: 8px;
    }
  }

  .fee-discount {
    font-size: 16px;
    color: #606266;
    margin-bottom: 24px;

    span {
      font-size: 24px;
      font-weight: 600;
      color: #67c23a;
    }
  }

  .exp-progress {
    max-width: 400px;
    margin: 0 auto 24px;

    .progress-label {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      color: #606266;
    }

    .exp-tip {
      margin-top: 8px;
      font-size: 14px;
      color: #909399;
    }
  }

  .exp-rules {
    text-align: left;
    max-width: 300px;
    margin: 0 auto;

    h4 {
      font-size: 14px;
      margin-bottom: 12px;
    }

    ul {
      list-style: none;
      padding: 0;

      li {
        padding: 4px 0;
        color: #606266;
        font-size: 14px;
      }
    }
  }
}

.level-table {
  h3 {
    margin-bottom: 16px;
  }
}
</style>
