<template>
  <div class="rally-page page-container">
    <div class="page-header"><h2>实时召集组队</h2><el-button @click="$router.push('/')">返回首页</el-button></div>

    <div class="rally-layout">
      <el-card shadow="never">
        <template #header><div class="card-title">发起召集</div></template>
        <el-form label-position="top" :model="form" @submit.prevent>
          <el-form-item label="活动类型"><el-select v-model="form.type"><el-option :value="1" label="运动" /><el-option :value="2" label="游戏" /></el-select></el-form-item>
          <el-form-item label="活动标题"><el-input v-model="form.title" maxlength="100" show-word-limit /></el-form-item>
          <el-form-item label="召集人数"><el-input-number v-model="form.recruitCount" :min="1" :max="100" style="width:100%" /></el-form-item>
          <el-form-item label="发起时间"><el-date-picker v-model="form.startTime" type="datetime" style="width:100%" format="YYYY-MM-DD HH:mm" value-format="x" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="300" show-word-limit /></el-form-item>
          <el-button type="primary" :loading="creating" style="width:100%" @click="createRally">立即发起</el-button>
        </el-form>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-row"><span class="card-title">召集大厅</span><div><el-tag :type="lobbyConnected ? 'success' : 'info'" effect="plain">{{ lobbyConnected ? '实时已连接' : '实时未连接' }}</el-tag><el-button text @click="loadActivities(true)">刷新</el-button></div></div>
        </template>
        <div v-loading="loading" class="activity-list">
          <div v-for="item in activities" :key="item.id" class="activity-item" :class="{ active: selectedId === item.id }" @click="selectActivity(item.id)">
            <div class="top"><div class="title">{{ item.title }}</div><el-tag size="small" effect="dark">{{ item.typeName }}</el-tag></div>
            <div class="meta">发起人：{{ item.organizerNickname }}</div>
            <div class="meta">人数：{{ item.currentCount }}/{{ item.recruitCount }}</div>
            <div class="meta">时间：{{ formatTime(item.startTime) }}</div>
          </div>
          <el-empty v-if="!loading && !activities.length" description="暂无进行中的召集活动" />
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-row"><span class="card-title">{{ selectedActivity ? `活动房间 · ${selectedActivity.title}` : '活动房间' }}</span><div><el-button :disabled="!selectedActivity || joined" @click="joinRally">参加</el-button><el-button :disabled="!selectedActivity || !joined || isOrganizer" @click="quitRally">退出</el-button><el-button type="danger" plain :disabled="!selectedActivity || !isOrganizer" @click="endRally">结束</el-button></div></div>
        </template>

        <div class="members">
          <template v-if="selectedActivity">
            <template v-if="joined && members.length"><span v-for="m in members" :key="m.userId" class="pill">{{ m.role === 0 ? '👑' : '🙋' }}{{ m.nickname }}</span></template>
            <span v-else class="hint">你尚未加入该活动，点击“参加”后可进入聊天室。</span>
          </template>
          <span v-else class="hint">请先从左侧选择活动</span>
        </div>

        <div class="messages">
          <div v-if="!messageList.length" class="hint">暂无消息</div>
          <div v-for="msg in messageList" :key="msg.id" class="msg-item">
            <div class="msg-head"><span class="name">{{ msg.senderNickname }}</span><span class="time">{{ formatTime(msg.createdAt) }}</span></div>
            <div class="content">{{ msg.content }}</div>
          </div>
        </div>

        <div class="input-row"><el-input v-model="messageText" maxlength="1000" placeholder="输入消息，回车发送" :disabled="!joined || !roomConnected" @keyup.enter="sendMessage" /><el-button type="primary" :disabled="!joined || !roomConnected || !messageText.trim()" @click="sendMessage">发送</el-button></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/format'
import { rallyApi, type RallyCreateDTO, type RallyWsEvent } from '@/api/rally'
import type { RallyActivityVO, RallyMemberVO, RallyMessageVO } from '@/types'

const userStore = useUserStore()
const loading = ref(false)
const creating = ref(false)
const activities = ref<RallyActivityVO[]>([])
const selectedId = ref('')
const members = ref<RallyMemberVO[]>([])
const messageList = ref<RallyMessageVO[]>([])
const joined = ref(false)
const messageText = ref('')
const lobbyConnected = ref(false)
const roomConnected = ref(false)
let lobbyWs: WebSocket | null = null
let roomWs: WebSocket | null = null

const form = reactive({ type: 1 as 1 | 2, title: '', recruitCount: 5, startTime: String(Date.now() + 30 * 60 * 1000), remark: '' })
const selectedActivity = computed(() => activities.value.find(i => i.id === selectedId.value))
const isOrganizer = computed(() => !!selectedActivity.value && selectedActivity.value.organizerId === String(userStore.userInfo?.id))

const wsBase = () => {
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const host = ((import.meta.env.VITE_WS_HOST as string | undefined) || location.host).trim()
  return `${protocol}://${host}`
}

async function loadActivities(keep = true) {
  loading.value = true
  try {
    activities.value = (await rallyApi.list()) || []
    if (keep && selectedId.value && !activities.value.some(i => i.id === selectedId.value)) {
      selectedId.value = ''
      resetRoom()
    }
  } catch (e: any) { ElMessage.error(e.message || '加载活动失败') }
  loading.value = false
}

function resetRoom() { joined.value = false; members.value = []; messageList.value = []; closeRoomWs() }

async function selectActivity(id: string) { selectedId.value = id; resetRoom(); await tryEnterRoomData(id) }

async function tryEnterRoomData(id: string) {
  try {
    const [memberRes, historyRes] = await Promise.all([rallyApi.members(id), rallyApi.history(id)])
    members.value = memberRes || []
    messageList.value = historyRes || []
    joined.value = true
    connectRoomWs(id)
  } catch { joined.value = false }
}

function connectLobbyWs() {
  closeLobbyWs(); if (!userStore.token) return
  lobbyWs = new WebSocket(`${wsBase()}/ws/rally/lobby?token=${encodeURIComponent(userStore.token)}`)
  lobbyWs.onopen = () => (lobbyConnected.value = true)
  lobbyWs.onclose = () => (lobbyConnected.value = false)
  lobbyWs.onmessage = async (event) => {
    const msg: RallyWsEvent = JSON.parse(event.data)
    if (msg.type === 'rally_created' || msg.type === 'rally_updated' || msg.type === 'rally_ended') {
      await loadActivities(true)
      if (msg.type === 'rally_ended' && selectedId.value === msg.rallyId) { resetRoom(); ElMessage.info('当前活动已结束') }
    }
  }
}

function connectRoomWs(rallyId: string) {
  closeRoomWs(); if (!userStore.token) return
  roomWs = new WebSocket(`${wsBase()}/ws/rally/${rallyId}?token=${encodeURIComponent(userStore.token)}`)
  roomWs.onopen = () => (roomConnected.value = true)
  roomWs.onclose = () => (roomConnected.value = false)
  roomWs.onmessage = async (event) => {
    const msg: RallyWsEvent = JSON.parse(event.data)
    if (msg.type === 'message') messageList.value.push(msg)
    else if (msg.type === 'member_joined' || msg.type === 'member_quit') { try { members.value = await rallyApi.members(rallyId) } catch {} }
    else if (msg.type === 'rally_ended') { ElMessage.warning('活动已结束'); await loadActivities(true); resetRoom() }
    else if (msg.type === 'error') ElMessage.error(msg.msg)
  }
}

function closeLobbyWs() { if (lobbyWs) { lobbyWs.close(); lobbyWs = null } }
function closeRoomWs() { if (roomWs) { roomWs.close(); roomWs = null }; roomConnected.value = false }

async function createRally() {
  if (!form.title.trim()) return ElMessage.warning('请填写活动标题')
  if (!form.startTime) return ElMessage.warning('请选择发起时间')
  const payload: RallyCreateDTO = { type: form.type, title: form.title.trim(), recruitCount: form.recruitCount, startTime: Number(form.startTime), remark: form.remark?.trim() || null }
  creating.value = true
  try {
    const created = await rallyApi.create(payload)
    ElMessage.success('发起成功')
    form.title = ''; form.remark = ''
    await loadActivities(true)
    await selectActivity(created.id)
  } catch (e: any) { ElMessage.error(e.message || '创建失败') }
  creating.value = false
}

async function joinRally() {
  if (!selectedActivity.value) return
  try { await rallyApi.join(selectedActivity.value.id); ElMessage.success('参加成功'); await loadActivities(true); await tryEnterRoomData(selectedActivity.value.id) }
  catch (e: any) { ElMessage.error(e.message || '参加失败') }
}

async function quitRally() {
  if (!selectedActivity.value) return
  try { await rallyApi.quit(selectedActivity.value.id); ElMessage.success('已退出活动'); await loadActivities(true); resetRoom() }
  catch (e: any) { ElMessage.error(e.message || '退出失败') }
}

async function endRally() {
  if (!selectedActivity.value) return
  try { await rallyApi.end(selectedActivity.value.id); ElMessage.success('活动已结束'); await loadActivities(true); resetRoom() }
  catch (e: any) { ElMessage.error(e.message || '结束失败') }
}

function sendMessage() {
  const content = messageText.value.trim()
  if (!content) return
  if (!roomWs || roomWs.readyState !== WebSocket.OPEN) return ElMessage.warning('聊天室连接未就绪')
  roomWs.send(JSON.stringify({ content }))
  messageText.value = ''
}

onMounted(async () => { await loadActivities(false); connectLobbyWs() })
onUnmounted(() => { closeLobbyWs(); closeRoomWs() })
</script>

<style scoped lang="scss">
.rally-layout{display:grid;grid-template-columns:320px 330px 1fr;gap:16px}.card-title{font-weight:700}.card-row{display:flex;justify-content:space-between;align-items:center;gap:8px}
.activity-list{max-height:70vh;overflow:auto;display:grid;gap:10px}.activity-item{border:1px solid #3a4556;border-radius:10px;padding:10px;cursor:pointer}.activity-item:hover,.activity-item.active{border-color:#5b9ef5}.activity-item.active{background:rgba(91,158,245,.08)}
.top{display:flex;justify-content:space-between;gap:8px;align-items:center}.title{font-weight:600}.meta{margin-top:6px;font-size:12px;color:#9ca3af}
.members{min-height:38px;border:1px dashed #3a4556;border-radius:10px;padding:8px;margin-bottom:10px;display:flex;gap:8px;flex-wrap:wrap}.pill{font-size:12px;border-radius:999px;padding:2px 10px;background:rgba(129,199,132,.2);color:#81c784}.hint{font-size:12px;color:#9ca3af}
.messages{height:52vh;overflow:auto;border:1px solid #3a4556;border-radius:10px;padding:10px}.msg-item{border:1px solid rgba(58,69,86,.7);border-radius:8px;padding:8px;margin-bottom:8px}.msg-head{display:flex;justify-content:space-between;gap:8px}.name{color:#5b9ef5;font-size:13px}.time{color:#9ca3af;font-size:12px}.content{margin-top:4px;line-height:1.6;white-space:pre-wrap}
.input-row{margin-top:10px;display:grid;grid-template-columns:1fr auto;gap:8px}
@media (max-width:1320px){.rally-layout{grid-template-columns:1fr}.messages{height:40vh}}
</style>
