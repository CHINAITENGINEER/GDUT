<template>
  <div class="recommend-page">
    <header class="page-head">
      <div>
        <p class="kicker">ADAPTIVE AGENT</p>
        <h1>推荐中心</h1>
        <p class="desc">管理你的接单偏好与推荐权重，并查看系统为你推荐的任务。</p>
      </div>
      <div class="actions">
        <el-button @click="$router.back()">返回</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存画像</el-button>
      </div>
    </header>

    <el-tabs v-model="tab" class="tabs">
      <el-tab-pane label="为你推荐" name="tasks">
        <div class="panel">
          <div class="panel-head">
            <div class="panel-title">推荐任务</div>
            <el-button text type="primary" :loading="loadingTasks" @click="loadTasks">刷新</el-button>
          </div>

          <div v-loading="loadingTasks" class="grid">
            <div v-if="tasks.length === 0 && !loadingTasks" class="empty">
              <EmptyState description="暂无推荐任务，试试调整画像或稍后刷新" />
            </div>
            <article v-else v-for="item in tasks" :key="item.task.id" class="card">
              <div class="meta">
                <span class="score">匹配度 {{ toPercent(item.score) }}</span>
                <span class="reason">{{ item.recommendReason }}</span>
              </div>
              <TaskCard :task="item.task" />
            </article>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的画像" name="profile">
        <div class="panel">
          <div class="panel-head">
            <div class="panel-title">偏好设置</div>
            <div class="panel-sub">修改后点击右上角「保存画像」生效。</div>
          </div>

          <el-form label-position="top" class="form">
            <el-form-item label="能力标签（可多选）">
              <el-select v-model="form.abilityTags" multiple filterable allow-create default-first-option placeholder="输入并回车添加标签">
                <el-option v-for="t in abilityOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>

            <el-form-item label="偏好分类（可多选）">
              <el-select v-model="form.preferredCategoryIds" multiple placeholder="选择你更愿意接的任务类型">
                <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>

            <div class="row">
              <el-form-item label="偏好交付方式">
                <el-segmented v-model="form.preferredDeliveryType" :options="deliveryOptions" />
              </el-form-item>
              <el-form-item label="每日推荐数量上限">
                <el-input-number v-model="form.dailyRecommendLimit" :min="1" :max="50" />
              </el-form-item>
            </div>

            <div class="row">
              <el-form-item label="最低可接受金额">
                <el-input-number v-model="form.minAcceptAmount" :min="0" :max="99999" :precision="2" :step="1" />
              </el-form-item>
              <el-form-item label="最高可接受金额">
                <el-input-number v-model="form.maxAcceptAmount" :min="0" :max="99999" :precision="2" :step="1" />
              </el-form-item>
            </div>

            <el-divider />

            <div class="panel-head">
              <div class="panel-title">推荐权重</div>
              <div class="panel-sub">仅用于个性化排序，建议总和约等于 1。</div>
            </div>

            <div class="weights">
              <div v-for="k in weightKeys" :key="k" class="weight-item">
                <div class="weight-head">
                  <span class="weight-key">{{ weightLabel[k] }}</span>
                  <span class="weight-val">{{ (form.weights[k] ?? 0).toFixed(2) }}</span>
                </div>
                <el-slider v-model="form.weights[k]" :min="0" :max="1" :step="0.01" show-input />
              </div>
            </div>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { recommendationApi } from '@/api/recommendation'
import { useUserStore } from '@/stores/user'
import type { RecommendedTaskVO, RecommendationProfileVO } from '@/types'
import TaskCard from '@/components/TaskCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const userStore = useUserStore()

const tab = ref<'tasks' | 'profile'>('tasks')

// 切换到推荐 Tab 时自动刷新列表，确保画像变更后能立即看到新结果
watch(tab, (val) => {
  if (val === 'tasks') loadTasks()
})
const loadingTasks = ref(false)
const saving = ref(false)
const tasks = ref<RecommendedTaskVO[]>([])

// 与后端 Task.category 枚举保持一致（constants.ts CATEGORY_MAP）
const categories = [
  { label: '代取快递', value: 1 },
  { label: '资料整理', value: 2 },
  { label: '编程', value: 3 },
  { label: '代课占位', value: 4 },
  { label: '其他', value: 5 }
]

const deliveryOptions = [
  { label: '线上', value: 0 },
  { label: '线下', value: 1 }
]

const abilityOptions = [
  '写作/文案', 'PPT', '跑腿', '设计', '编程', '摄影', '翻译', '学习辅导'
]

const weightLabel: Record<string, string> = {
  ability: '能力匹配',
  category: '分类偏好',
  amount: '金额区间',
  delivery: '交付方式'
}
const weightKeys = Object.keys(weightLabel)

const form = reactive<RecommendationProfileVO>({
  abilityTags: [],
  preferredCategoryIds: [],
  preferredDeliveryType: 0,
  minAcceptAmount: 1,
  maxAcceptAmount: 500,
  dailyRecommendLimit: 10,
  weights: { ability: 0.35, category: 0.30, amount: 0.25, delivery: 0.10 }
})

const isAcceptor = computed(() => userStore.isAcceptor)

function toPercent(score: number) {
  return `${Math.round(score * 100)}%`
}

async function loadProfile() {
  try {
    const p = await recommendationApi.getProfile()
    form.abilityTags = p.abilityTags ?? []
    form.preferredCategoryIds = p.preferredCategoryIds ?? []
    form.preferredDeliveryType = p.preferredDeliveryType ?? 0
    form.minAcceptAmount = p.minAcceptAmount ?? 1
    form.maxAcceptAmount = p.maxAcceptAmount ?? 500
    form.dailyRecommendLimit = p.dailyRecommendLimit ?? 10
    form.weights = { ...form.weights, ...(p.weights ?? {}) }
  } catch (e: any) {
    // 不阻塞页面，可能是首次没有画像
    ElMessage.warning(e?.message || '画像读取失败，可先保存一次初始化')
  }
}

async function loadTasks() {
  if (!isAcceptor.value) {
    tasks.value = []
    return
  }
  loadingTasks.value = true
  try {
    tasks.value = await recommendationApi.recommendTasks(12)
  } catch (e: any) {
    tasks.value = []
    ElMessage.error(e?.message || '加载推荐失败')
  } finally {
    loadingTasks.value = false
  }
}

async function save() {
  saving.value = true
  try {
    if (form.minAcceptAmount > form.maxAcceptAmount) {
      ElMessage.warning('最低金额不能大于最高金额')
      return
    }
    await recommendationApi.saveProfile({
      abilityTags: form.abilityTags,
      preferredCategoryIds: form.preferredCategoryIds,
      preferredDeliveryType: form.preferredDeliveryType,
      minAcceptAmount: form.minAcceptAmount,
      maxAcceptAmount: form.maxAcceptAmount,      dailyRecommendLimit: form.dailyRecommendLimit,
      weights: form.weights
    })
    ElMessage.success('画像已保存')
    await loadTasks()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadProfile()
  await loadTasks()
})
</script>

<style scoped lang="scss">
.recommend-page {
  padding: 22px 22px 40px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
  .kicker {
    margin: 0;
    font-size: 12px;
    letter-spacing: 0.2em;
    color: #63e6be;
  }
  h1 {
    margin: 6px 0 4px;
    font-size: 26px;
  }
  .desc {
    margin: 0;
    color: rgba(168, 173, 181, 1);
    font-size: 14px;
  }
  .actions {
    display: flex;
    gap: 10px;
    align-items: center;
  }
}

.tabs :deep(.el-tabs__content) {
  padding-top: 10px;
}

.panel {
  border: 1px solid rgba(58, 69, 86, 1);
  border-radius: 16px;
  background: rgba(37, 45, 61, 0.75);
  padding: 16px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}
.panel-title {
  font-weight: 800;
}
.panel-sub {
  color: rgba(168, 173, 181, 1);
  font-size: 12px;
}

.grid {
  min-height: 120px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.empty {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  padding: 34px 0;
}
.card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.score,
.reason {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: 999px;
  padding: 0 12px;
  font-size: 12px;
}
.score {
  background: rgba(99, 230, 190, 0.16);
  color: #8ef0d0;
  border: 1px solid rgba(99, 230, 190, 0.25);
  font-weight: 700;
}
.reason {
  background: rgba(91, 158, 245, 0.14);
  color: #9fc4ff;
  border: 1px solid rgba(91, 158, 245, 0.2);
}

.form {
  max-width: 980px;
}
.row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.weights {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.weight-item {
  border: 1px solid rgba(58, 69, 86, 0.8);
  border-radius: 14px;
  padding: 12px;
  background: rgba(26, 31, 46, 0.4);
}
.weight-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  .weight-key { font-weight: 700; }
  .weight-val { color: rgba(168, 173, 181, 1); font-size: 12px; }
}

@media (max-width: 920px) {
  .row { grid-template-columns: 1fr; }
  .weights { grid-template-columns: 1fr; }
}
</style>

