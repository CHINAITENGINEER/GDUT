<template>
  <div class="user-home-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
    </div>

    <el-card v-loading="loading">
      <div class="user-header">
        <UserAvatar :src="userInfo.avatar" :size="80" :name="userInfo.nickname" />
        <div class="user-info">
          <h1>{{ userInfo.nickname }}</h1>
          <div class="tags">
            <span>信誉分：{{ userInfo.creditScore }}</span>
            <LevelBadge :level="userInfo.level" show-name />
          </div>
          <div class="bio">{{ userInfo.bio || '这个人很神秘，暂未留下简介' }}</div>
        </div>
      </div>

      <el-divider />

      <div class="user-stats">
        <div class="stat-item">
          <span class="value">¥{{ (userInfo.totalEarned || 0).toFixed(2) }}</span>
          <span class="label">累计收入</span>
        </div>
        <div class="stat-item">
          <span class="value">{{ userInfo.completedCount || 0 }}</span>
          <span class="label">完成任务数</span>
        </div>
      </div>

      <el-divider />

      <div class="user-reviews">
        <h3>用户评价</h3>
        <div v-if="reviews.length === 0" class="no-reviews">暂无评价</div>
        <div v-else class="review-list">
          <div v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-header">
              <UserAvatar :src="review.reviewer.avatar" :size="32" :name="review.reviewer.nickname" />
              <span class="name">{{ review.reviewer.nickname }}</span>
              <StarRating :model-value="review.score" disabled />
            </div>
            <div class="review-content">{{ review.content }}</div>
            <div class="review-task">来自任务：{{ review.taskTitle }}</div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { reviewApi } from '@/api/review'
import LevelBadge from '@/components/LevelBadge.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import StarRating from '@/components/StarRating.vue'

const route = useRoute()

const loading = ref(false)
const userInfo = reactive<any>({
  avatar: '',
  nickname: '',
  bio: '',
  creditScore: 0,
  level: 1,
  totalEarned: 0,
  completedCount: 0
})
const reviews = ref<any[]>([])

async function loadUserInfo() {
  loading.value = true
  try {
    const res = await userApi.getUserById(route.params.id as string)
    Object.assign(userInfo, res)
  } catch (e) {}
  loading.value = false
}

async function loadReviews() {
  try {
    const res = await reviewApi.getUserReviews(route.params.id as string, { page: 1, pageSize: 10 })
    reviews.value = res.records
  } catch (e) {}
}

onMounted(() => {
  loadUserInfo()
  loadReviews()
})
</script>

<style scoped lang="scss">
.user-home-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.user-header {
  display: flex;
  gap: 20px;

  .user-info {
    flex: 1;

    h1 {
      font-size: 24px;
      margin-bottom: 8px;
    }

    .tags {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;
    }

    .bio {
      color: #606266;
    }
  }
}

.user-stats {
  display: flex;
  gap: 60px;

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    .value {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
    }

    .label {
      font-size: 14px;
      color: #909399;
    }
  }
}

.user-reviews {
  h3 {
    margin-bottom: 16px;
  }

  .no-reviews {
    color: #909399;
    text-align: center;
    padding: 20px;
  }

  .review-item {
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    .review-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .name {
        font-weight: 500;
      }
    }

    .review-content {
      color: #606266;
      margin-bottom: 8px;
    }

    .review-task {
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>
