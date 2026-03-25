<template>
  <div class="message-center-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h2>消息中心</h2>
      <el-button @click="handleReadAll">全部已读</el-button>
    </div>
    
    <el-tabs v-model="activeTab" @tab-change="loadMessages">
      <el-tab-pane label="系统消息" :name="0" />
      <el-tab-pane label="私信" :name="1" />
    </el-tabs>
    
    <div v-loading="loading" class="message-list">
      <div v-if="messageList.length === 0" class="empty-wrap">
        <EmptyState description="暂无消息" />
      </div>
      <div v-else>
        <div 
          v-for="msg in messageList" 
          :key="msg.id" 
          class="message-item"
          :class="{ unread: !msg.isRead }"
          @click="handleRead(msg)"
        >
          <div class="message-icon">
            <el-icon v-if="msg.type === 0" :size="24"><Bell /></el-icon>
            <el-icon v-else :size="24"><ChatDotRound /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="sender">{{ msg.type === 0 ? '系统消息' : msg.senderNickname }}</span>
              <span class="time">{{ formatRelative(msg.createdAt) }}</span>
            </div>
            <div class="message-body">{{ msg.content }}</div>
            <div v-if="msg.taskTitle" class="message-task">相关任务：{{ msg.taskTitle }}</div>
          </div>
          <span v-if="!msg.isRead" class="unread-dot"></span>
        </div>
      </div>
      
      <div v-if="messageList.length > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadMessages"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Bell, ChatDotRound } from '@element-plus/icons-vue'
import { messageApi } from '@/api/message'
import { formatRelative } from '@/utils/format'
import type { MessageVO } from '@/types'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const activeTab = ref<0 | 1>(0)
const messageList = ref<MessageVO[]>([])
const total = ref(0)
const router = useRouter()

const query = reactive({
  page: 1,
  pageSize: 20
})

async function loadMessages() {
  loading.value = true
  try {
    const res = await messageApi.list({
      page: query.page,
      pageSize: query.pageSize,
      type: activeTab.value
    })
    messageList.value = (res as any).records ?? []
    total.value = (res as any).total ?? 0
  } catch (e) {}
  loading.value = false
}

async function handleRead(msg: MessageVO) {
  if (!msg.isRead) {
    try {
      await messageApi.read(msg.id)
      msg.isRead = true
    } catch (e) {
      // 标记失败不影响跳�?    }
  }
  // 有关联任务则跳转到任务详�?  if (msg.taskId) {
    router.push(`/task/${msg.taskId}`)
  }
}

async function handleReadAll() {
  try {
    await messageApi.readAll()
    ElMessage.success('已全部标记为已读')
    // 重新加载消息列表以更新UI
    await loadMessages()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => {
  loadMessages()
})
</script>

<style scoped lang="scss">
.message-center-page {
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

.message-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: background 0.3s;
  
  &:hover {
    background: #f5f7fa;
  }
  
  &.unread {
    background: #ecf5ff;
  }
  
  .message-icon {
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f0f2f5;
    border-radius: 50%;
    color: #409eff;
  }
  
  .message-content {
    flex: 1;
    min-width: 0;
    
    .message-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      
      .sender {
        font-weight: 600;
        color: #303133;
      }
      
      .time {
        font-size: 12px;
        color: #909399;
      }
    }
    
    .message-body {
      color: #606266;
      font-size: 14px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .message-task {
      margin-top: 8px;
      font-size: 12px;
      color: #409eff;
    }
  }
  
  .unread-dot {
    width: 8px;
    height: 8px;
    background: #f56c6c;
    border-radius: 50%;
    flex-shrink: 0;
    margin-top: 6px;
  }
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
