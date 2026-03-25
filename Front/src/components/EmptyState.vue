<template>
  <div class="empty-state">
    <div class="empty-illustration">
      <div class="empty-icon">
        <svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#5b9ef5;stop-opacity:0.3" />
              <stop offset="100%" style="stop-color:#4dd0e1;stop-opacity:0.1" />
            </linearGradient>
          </defs>
          <circle cx="100" cy="100" r="80" fill="url(#grad1)" opacity="0.5" />
          <path d="M 60 80 L 140 80 L 140 120 L 60 120 Z" fill="none" stroke="#5b9ef5" stroke-width="3" opacity="0.4" />
          <path d="M 70 90 L 130 90 L 130 110 L 70 110 Z" fill="none" stroke="#4dd0e1" stroke-width="2" opacity="0.3" />
          <circle cx="100" cy="100" r="30" fill="none" stroke="#5b9ef5" stroke-width="2" opacity="0.2" />
        </svg>
      </div>
    </div>
    <h3 class="empty-title">{{ description || '暂无任务' }}</h3>
    <p v-if="showAction" class="empty-desc">试试调整筛选条件或发布新任务</p>
    <p v-else class="empty-desc">暂时还没有相关任务</p>
    <slot>
      <el-button v-if="showAction" type="primary" size="large" @click="handleAction" class="empty-action">
        {{ actionText || '立即发布' }}
      </el-button>
    </slot>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = withDefaults(defineProps<{
  description?: string
  actionText?: string
  showAction?: boolean
}>(), {
  showAction: true
})

const emit = defineEmits(['action'])

const router = useRouter()

function handleAction() {
  emit('action')
  router.push('/task/publish')
}
</script>

<style scoped lang="scss">
$accent-blue: #5b9ef5;
$accent-cyan: #4dd0e1;
$text-primary: #e8eaed;
$text-secondary: #a8adb5;
$card-bg: #252d3d;

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  animation: fadeInUp 0.6s ease-out;
}

.empty-illustration {
  margin-bottom: 32px;
  animation: float 3s ease-in-out infinite;
}

.empty-icon {
  width: 160px;
  height: 160px;
  margin: 0 auto;
  
  svg {
    width: 100%;
    height: 100%;
    filter: drop-shadow(0 8px 24px rgba($accent-blue, 0.2));
  }
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}

.empty-desc {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 32px;
  line-height: 1.6;
}

.empty-action {
  padding: 12px 32px;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba($accent-blue, 0.3);
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: 0 12px 32px rgba($accent-blue, 0.4);
    transform: translateY(-2px);
  }
}

body.theme-light {
  .empty-title {
    color: #111827;
  }

  .empty-desc {
    color: #6b7280;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}
</style>
