<template>
  <div class="task-card" @click="goDetail">
    <div class="card-header">
      <CategoryTag :category="task.category" />
      <TaskStatusTag :status="task.status" />
    </div>
    <div class="card-title">{{ task.title }}</div>
    <div class="card-amount">¥{{ task.amount.toFixed(2) }}</div>
    <div class="card-deadline">截止: {{ formatTime(task.deadline) }}</div>
    <div v-if="task.publisher" class="card-publisher">
      <UserAvatar :src="task.publisher?.avatar" :name="task.publisher?.nickname" :size="24" />
      <span class="nickname">{{ task.publisher?.nickname || '未知用户' }}</span>
      <span class="credit">信誉分: {{ task.publisher?.creditScore || 0 }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { TaskCardVO } from '@/types'
import { formatTime } from '@/utils/format'
import CategoryTag from './CategoryTag.vue'
import TaskStatusTag from './TaskStatusTag.vue'
import UserAvatar from './UserAvatar.vue'

const props = defineProps<{
  task: TaskCardVO
}>()

const router = useRouter()

function goDetail() {
  router.push(`/task/${props.task.id}`)
}
</script>

<style scoped lang="scss">
$card-bg:     #252d3d;
$border-dark: #3a4556;
$text-primary: #e8eaed;
$text-secondary: #a8adb5;
$accent-blue: #5b9ef5;
$accent-orange: #ffb74d;
$accent-green: #81c784;

.task-card {
  background: linear-gradient(135deg, rgba($card-bg, 0.9) 0%, rgba($card-bg, 0.7) 100%);
  border: 1px solid $border-dark;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(12px);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, $accent-blue, $accent-orange, $accent-green);
    opacity: 0;
    transition: opacity 0.4s ease;
    background-size: 200% 100%;
    animation: shimmer 3s linear infinite;
  }
  
  &::after {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba($accent-blue, 0.1) 0%, transparent 70%);
    opacity: 0;
    transition: opacity 0.4s ease;
    pointer-events: none;
  }
  
  &:hover {
    border-color: $accent-blue;
    background: linear-gradient(135deg, rgba($card-bg, 1) 0%, rgba($card-bg, 0.85) 100%);
    box-shadow: 0 16px 48px rgba($accent-blue, 0.25), 0 0 0 1px rgba($accent-blue, 0.1);
    transform: translateY(-6px) scale(1.02);
    
    &::before {
      opacity: 1;
    }
    
    &::after {
      opacity: 1;
    }
    
    .card-title {
      color: $accent-blue;
    }
    
    .card-amount {
      transform: scale(1.05);
    }
  }
}

body.theme-light {
  .task-card {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.88) 100%);
    border-color: #e5e7eb;
  }

  .card-title {
    color: #111827;
  }

  .card-deadline {
    color: #6b7280;
  }

  .card-publisher {
    border-top: 1px solid rgba(229, 231, 235, 0.9);

    .nickname {
      color: #6b7280;
    }
  }
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s ease;
  letter-spacing: -0.3px;
}

.card-amount {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, $accent-orange, #ff9800, $accent-orange);
  background-size: 200% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
  transition: transform 0.3s ease;
  animation: gradient-shift 3s ease infinite;
}

@keyframes gradient-shift {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.card-deadline {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
  
  &::before {
    content: '⏱';
    opacity: 0.6;
  }
}

.card-publisher {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba($border-dark, 0.5);
  
  .nickname {
    color: $text-secondary;
    font-weight: 500;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .credit {
    color: $accent-green;
    font-weight: 600;
    font-family: 'Fira Code', monospace;
    white-space: nowrap;
  }
}
</style>
