const state = {
  token: "",
  userId: null,
  activities: [],
  selectedId: null,
  lobbyWs: null,
  roomWs: null
};

const $ = (id) => document.getElementById(id);

const dom = {
  tokenInput: $("tokenInput"),
  connectBtn: $("connectBtn"),
  createForm: $("createForm"),
  typeInput: $("typeInput"),
  titleInput: $("titleInput"),
  countInput: $("countInput"),
  startInput: $("startInput"),
  remarkInput: $("remarkInput"),
  refreshBtn: $("refreshBtn"),
  activityList: $("activityList"),
  roomTitle: $("roomTitle"),
  memberBar: $("memberBar"),
  joinBtn: $("joinBtn"),
  quitBtn: $("quitBtn"),
  endBtn: $("endBtn"),
  chatMessages: $("chatMessages"),
  chatForm: $("chatForm"),
  chatInput: $("chatInput")
};

function showToast(text) {
  const el = document.createElement("div");
  el.className = "toast";
  el.textContent = text;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 2200);
}

function decodeUserId(token) {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return Number(payload.sub);
  } catch (e) {
    return null;
  }
}

async function request(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;

  const res = await fetch(path, { ...options, headers });
  const json = await res.json();
  if (json.code !== 200) {
    throw new Error(json.message || "请求失败");
  }
  return json.data;
}

function formatTs(ts) {
  if (!ts) return "-";
  const d = new Date(ts);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function getSelectedActivity() {
  return state.activities.find((a) => String(a.id) === String(state.selectedId));
}

function renderActivities() {
  dom.activityList.innerHTML = "";
  if (!state.activities.length) {
    dom.activityList.innerHTML = `<div class="meta">当前暂无进行中的召集活动</div>`;
    return;
  }

  state.activities.forEach((a) => {
    const card = document.createElement("article");
    card.className = `activity-card ${String(a.id) === String(state.selectedId) ? "active" : ""}`;
    card.innerHTML = `
      <div class="activity-top">
        <strong>${a.title}</strong>
        <span class="badge">${a.typeName}</span>
      </div>
      <div class="meta">
        发起人：${a.organizerNickname}<br/>
        人数：${a.currentCount}/${a.recruitCount}<br/>
        时间：${formatTs(a.startTime)}
      </div>
    `;
    card.onclick = () => selectActivity(a.id);
    dom.activityList.appendChild(card);
  });
}

function renderMembers(members) {
  if (!members?.length) {
    dom.memberBar.textContent = "暂无成员信息";
    return;
  }
  dom.memberBar.textContent = members
    .map((m) => `${m.role === 0 ? "👑" : "🙋"}${m.nickname}`)
    .join("  ·  ");
}

function addMessage({ senderNickname, content, createdAt }) {
  const item = document.createElement("div");
  item.className = "msg";
  item.innerHTML = `
    <div><span class="name">${senderNickname || "系统"}</span><span class="time">${formatTs(createdAt)}</span></div>
    <div class="content">${content || ""}</div>
  `;
  dom.chatMessages.appendChild(item);
  dom.chatMessages.scrollTop = dom.chatMessages.scrollHeight;
}

function addSystemMessage(text) {
  addMessage({ senderNickname: "系统", content: text, createdAt: Date.now() });
}

async function loadActivities(keepSelection = true) {
  state.activities = await request("/api/rally/list");
  if (keepSelection && state.selectedId) {
    const exists = state.activities.some((a) => String(a.id) === String(state.selectedId));
    if (!exists) {
      state.selectedId = null;
      closeRoomSocket();
      dom.roomTitle.textContent = "活动聊天室";
      dom.memberBar.textContent = "请选择活动";
      dom.chatMessages.innerHTML = "";
    }
  }
  renderActivities();
}

async function selectActivity(id) {
  state.selectedId = id;
  renderActivities();
  const selected = getSelectedActivity();
  dom.roomTitle.textContent = selected ? `聊天室：${selected.title}` : "活动聊天室";
  dom.chatMessages.innerHTML = "";

  try {
    const [members, history] = await Promise.all([
      request(`/api/rally/${id}/members`),
      request(`/api/rally/${id}/history`)
    ]);
    renderMembers(members);
    history.forEach(addMessage);
    connectRoomSocket(id);
  } catch (e) {
    renderMembers([]);
    addSystemMessage("你尚未加入该活动，仅可查看大厅信息。可先点击参加。");
    closeRoomSocket();
  }
}

function connectLobbySocket() {
  if (state.lobbyWs) state.lobbyWs.close();
  state.lobbyWs = new WebSocket(`${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws/rally/lobby?token=${encodeURIComponent(state.token)}`);
  state.lobbyWs.onopen = () => showToast("大厅实时连接成功");
  state.lobbyWs.onmessage = async (event) => {
    const msg = JSON.parse(event.data);
    if (["rally_created", "rally_updated", "rally_ended"].includes(msg.type)) {
      await loadActivities(true);
      if (msg.type === "rally_ended" && String(msg.rallyId) === String(state.selectedId)) {
        addSystemMessage("该活动已结束");
      }
    }
  };
  state.lobbyWs.onclose = () => showToast("大厅实时连接已断开");
}

function closeRoomSocket() {
  if (state.roomWs) {
    state.roomWs.close();
    state.roomWs = null;
  }
}

function connectRoomSocket(rallyId) {
  closeRoomSocket();
  state.roomWs = new WebSocket(`${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws/rally/${rallyId}?token=${encodeURIComponent(state.token)}`);
  state.roomWs.onopen = () => addSystemMessage("已进入活动聊天室");
  state.roomWs.onmessage = async (event) => {
    const msg = JSON.parse(event.data);
    if (msg.type === "message") {
      addMessage(msg);
    } else if (msg.type === "member_joined") {
      addSystemMessage(`${msg.nickname} 加入了活动`);
      try {
        renderMembers(await request(`/api/rally/${rallyId}/members`));
      } catch (e) {}
    } else if (msg.type === "member_quit") {
      addSystemMessage(`${msg.nickname} 退出了活动`);
      try {
        renderMembers(await request(`/api/rally/${rallyId}/members`));
      } catch (e) {}
    } else if (msg.type === "rally_ended") {
      addSystemMessage("活动已结束");
      await loadActivities(true);
      closeRoomSocket();
    } else if (msg.type === "error") {
      showToast(msg.msg || "房间连接异常");
    }
  };
  state.roomWs.onclose = () => {};
}

async function connectAll() {
  const token = dom.tokenInput.value.trim();
  if (!token) return showToast("请先输入 Token");
  state.token = token;
  state.userId = decodeUserId(token);
  try {
    await loadActivities(false);
    connectLobbySocket();
    showToast("连接完成");
  } catch (e) {
    showToast(e.message);
  }
}

async function createActivity(e) {
  e.preventDefault();
  if (!state.token) return showToast("请先连接实时服务");

  const startValue = dom.startInput.value;
  if (!startValue) return showToast("请选择发起时间");

  const payload = {
    type: Number(dom.typeInput.value),
    title: dom.titleInput.value.trim(),
    recruitCount: Number(dom.countInput.value),
    startTime: new Date(startValue).getTime(),
    remark: dom.remarkInput.value.trim() || null
  };

  try {
    const data = await request("/api/rally", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    showToast("召集创建成功");
    await loadActivities(true);
    await selectActivity(data.id);
  } catch (err) {
    showToast(err.message);
  }
}

async function joinSelected() {
  const selected = getSelectedActivity();
  if (!selected) return showToast("请先选择活动");
  try {
    await request(`/api/rally/${selected.id}/join`, { method: "POST", body: "{}" });
    showToast("参加成功");
    await loadActivities(true);
    await selectActivity(selected.id);
  } catch (e) {
    showToast(e.message);
  }
}

async function quitSelected() {
  const selected = getSelectedActivity();
  if (!selected) return showToast("请先选择活动");
  try {
    await request(`/api/rally/${selected.id}/quit`, { method: "POST", body: "{}" });
    showToast("已退出活动");
    closeRoomSocket();
    dom.chatMessages.innerHTML = "";
    dom.memberBar.textContent = "你已退出该活动";
    await loadActivities(true);
  } catch (e) {
    showToast(e.message);
  }
}

async function endSelected() {
  const selected = getSelectedActivity();
  if (!selected) return showToast("请先选择活动");
  try {
    await request(`/api/rally/${selected.id}/end`, { method: "POST", body: "{}" });
    showToast("活动已结束");
    closeRoomSocket();
    await loadActivities(true);
  } catch (e) {
    showToast(e.message);
  }
}

function sendChat(e) {
  e.preventDefault();
  const text = dom.chatInput.value.trim();
  if (!text) return;
  if (!state.roomWs || state.roomWs.readyState !== WebSocket.OPEN) {
    showToast("尚未连接活动聊天室");
    return;
  }
  state.roomWs.send(JSON.stringify({ content: text }));
  dom.chatInput.value = "";
}

function initDefaultTime() {
  const now = new Date(Date.now() + 30 * 60 * 1000);
  const pad = (n) => String(n).padStart(2, "0");
  dom.startInput.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
}

dom.connectBtn.onclick = connectAll;
dom.refreshBtn.onclick = () => loadActivities(true).catch((e) => showToast(e.message));
dom.createForm.addEventListener("submit", createActivity);
dom.joinBtn.onclick = joinSelected;
dom.quitBtn.onclick = quitSelected;
dom.endBtn.onclick = endSelected;
dom.chatForm.addEventListener("submit", sendChat);

initDefaultTime();
