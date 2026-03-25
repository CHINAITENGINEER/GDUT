<template>
  <div class="profile-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h2>个人主页</h2>
    </div>

    <el-card v-loading="loading">
      <div class="profile-header">
        <UserAvatar :src="userStore.userInfo?.avatar" :name="userStore.userInfo?.nickname" :size="80" />
        <div class="profile-info">
          <h1>{{ userStore.userInfo?.nickname }}</h1>
          <div class="badges">
            <LevelBadge :level="userStore.userInfo?.level || 1" show-name />
            <span class="credit">信誉分：{{ userStore.userInfo?.creditScore }}</span>
          </div>
          <div class="bio">{{ profile?.bio || '这个人很懒，什么都没留下' }}</div>
        </div>
        <el-button type="primary" @click="editDialogVisible = true">编辑资料</el-button>
      </div>

      <el-divider />

      <div class="profile-stats">
        <div class="stat-item">
          <span class="value">¥{{ (profile?.balance || 0).toFixed(2) }}</span>
          <span class="label">余额</span>
        </div>
        <div class="stat-item">
          <span class="value">¥{{ (profile?.totalEarned || 0).toFixed(2) }}</span>
          <span class="label">总收入</span>
        </div>
        <div class="stat-item">
          <span class="value">{{ profile?.exp || 0 }}</span>
          <span class="label">经验值</span>
        </div>
      </div>

      <el-divider />

      <div class="profile-links">
        <router-link to="/my/tasks" class="link-item">
          <el-icon><List /></el-icon>
          <span>我的任务</span>
        </router-link>
        <router-link to="/payment/records" class="link-item">
          <el-icon><Wallet /></el-icon>
          <span>支付记录</span>
        </router-link>
        <router-link to="/settlement/records" class="link-item">
          <el-icon><Money /></el-icon>
          <span>结算记录</span>
        </router-link>
        <router-link to="/level" class="link-item">
          <el-icon><Star /></el-icon>
          <span>等级中心</span>
        </router-link>
      </div>
    </el-card>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑资料" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="技能标签">
          <el-select v-model="editForm.skills" multiple allow-create filterable placeholder="输入技能后回车添加" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, List, Wallet, Money, Star } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { UserProfileVO } from '@/types'
import UserAvatar from '@/components/UserAvatar.vue'
import LevelBadge from '@/components/LevelBadge.vue'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const editDialogVisible = ref(false)
const profile = ref<UserProfileVO | null>(null)

const editForm = reactive({
  nickname: '',
  bio: '',
  skills: [] as string[]
})

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await userApi.getProfile()
    editForm.nickname = profile.value.nickname
    editForm.bio = profile.value.bio
    editForm.skills = profile.value.skills || []
  } catch (e) {}
  loading.value = false
}

async function handleSave() {
  saving.value = true
  try {
    await userApi.updateProfile({
      nickname: editForm.nickname,
      bio: editForm.bio,
      skills: editForm.skills
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await loadProfile()
    await userStore.getProfile()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
  saving.value = false
}

onMounted(loadProfile)
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;

  h2 { flex: 1; }
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 20px;

  .profile-info {
    flex: 1;

    h1 { font-size: 22px; margin-bottom: 8px; }

    .badges {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 10px;

      .credit { font-size: 13px; color: #909399; }
    }

    .bio { color: #606266; font-size: 14px; }
  }
}

.profile-stats {
  display: flex;
  gap: 60px;

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .value { font-size: 22px; font-weight: 700; color: #303133; }
    .label { font-size: 13px; color: #909399; }
  }
}

.profile-links {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;

  .link-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    background: #f5f7fa;
    border-radius: 8px;
    text-decoration: none;
    color: #303133;
    font-size: 14px;
    transition: background 0.2s;

    &:hover { background: #ecf5ff; color: #409eff; }
  }
}
</style>
