# Recommendation 模块说明（自适应推荐 Agent）

> 适用项目：校园任务接单平台  
> 覆盖范围：后端推荐画像/推荐任务/权重更新；前端推荐中心页面与首页推荐面板  
> 说明：本文按“数据 → 接口 → 算法 → 前端页面 → 常见问题”组织，便于维护与二次开发。

---

## 1. 模块目标

- **给接单者提供个性化任务推荐**：综合技能、分类偏好、金额区间、距离、交付方式等因素，为任务打分并排序。
- **支持用户维护画像**：用户可编辑能力标签、偏好分类、交付方式、金额与距离范围等。
- **可自适应调权**：在订单结算后，根据“这次接单是否命中偏好”对权重做微调，并记录日志。

---

## 2. 数据模型（画像与权重）

### 2.1 画像字段（存储在 `user` 表）

后端实体字段见 `campus-task-backend/src/main/java/com/campus/task/module/user/entity/User.java`：

- `abilityTags`：推荐能力标签（JSON 数组字符串）
- `preferredCategories`：偏好分类 ID（JSON 数组字符串）
- `preferredDeliveryType`：偏好交付方式（0 线上 / 1 线下）
- `minAcceptAmount` / `maxAcceptAmount`：可接受金额区间
- `maxDistanceKm`：可接受最远距离（线下任务使用）
- `dailyRecommendLimit`：每日推荐上限（画像字段，后端推荐接口会用 limit 控制返回数量）
- `recommendWeights`：推荐权重（JSON 对象字符串）

### 2.2 权重默认值与归一化

后端默认权重（见 `RecommendationServiceImpl#def()`）：

- ability：0.30
- category：0.25
- amount：0.20
- distance：0.15
- delivery：0.10

权重会被 **normalize** 到总和约等于 1（最后一个 key 用 $1-acc$ 补齐，避免浮点/精度累积误差）。

---

## 3. 后端接口设计

推荐模块 Controller：`campus-task-backend/src/main/java/com/campus/task/module/recommendation/controller/RecommendationController.java`  
统一前缀：`/api/recommendation`

### 3.1 获取画像

- **GET** `/api/recommendation/profile`
- **鉴权**：需要登录（`@AuthenticationPrincipal`）
- **返回**：`RecommendationProfileVO`

### 3.2 保存画像

- **PUT** `/api/recommendation/profile`
- **鉴权**：需要登录
- **请求体**：`RecommendationProfileDTO`
  - `abilityTags`：最多 12
  - `preferredCategoryIds`：最多 8
  - 其余字段：金额/距离/交付方式/推荐上限
- **返回**：保存后的 `RecommendationProfileVO`

### 3.3 拉取推荐任务

- **GET** `/api/recommendation/tasks?limit=10`
- **鉴权**：需要登录
- **返回**：`List<RecommendedTaskVO>`
  - `task`：任务卡片信息
  - `score`：总分（0~1）
  - `recommendReason`：推荐理由（用于 UI 展示）
  - `scoreBreakdown`：各子项得分（便于调参/排查）

### 3.4 手动触发权重更新（结算后）

- **POST** `/api/recommendation/tasks/{taskId}/weights/refresh`
- **鉴权**：需要登录
- **说明**：用于订单结算后手动触发权重自适应更新（也可由后端在结算流程里自动调用）。

---

## 4. 后端推荐逻辑（核心算法）

核心实现：`campus-task-backend/src/main/java/com/campus/task/module/recommendation/service/impl/RecommendationServiceImpl.java`

### 4.1 推荐前过滤（可接单判定）

对于候选任务，先过滤掉明显不满足画像约束的任务（`receivable(Task, User)`）：

- 任务已过截止时间 → 过滤（`deadline` 为 `null` 时视为可接，兼容历史数据）
- 任务金额 `< minAcceptAmount` 或 `> maxAcceptAmount` → 过滤
- 线下任务且 `distanceKm > maxDistanceKm` → 过滤

此外存在一个 **日限制拦截**（`blocked(userId)`）：

- 当日抢单数 `>= platform.grab-daily-limit` 或
- 当日违规数 `>= platform.violation-daily-limit`

会阻止继续推荐（用于保护平台规则）。

> 这些平台参数来自 `application.yml` 的 `platform.*` 配置。

### 4.2 五维打分 + 加权求和（已优化）

对每条候选任务计算子分：

- `ability`：技能/能力标签 Jaccard 相似度，映射到 [0.20, 1.00]。提取时过滤中文停用词（「的」「了」等），消除噪声 token，提升匹配精度。
- `category`：命中 → 1.0；**未命中 → 0.20**（原 0.35，差异更明显，非偏好任务排名明显下降）；画像空 → 0.50。
- `amount`：梯形函数，落在 [min,max] 内 → 1.0；超出按**区间宽度**衰减（修正原版边界接近 0 时除零风险）。
- `distance`：线上 → 1.0；线下 dist=0 → 1.0，dist=maxDist → 0.60，超出后线性衰减到 0.10（修正原版方向 bug）。
- `delivery`：命中 → 1.0；**未命中 → 0.40**（原 0.50，增强偏好信号）；未设置 → 0.70。

总分（四位小数）：

$$
score = \sum_{k} subScore_k \times weight_k + urgencyBonus + creditBonus + levelBonus
$$

三项奖励加成（最终 clamp 到 [0,1]）：

- `urgencyBonus [0, 0.10]`：三段式时效奖励（修正原版方向错误）。0~24h → [0.07,0.10]，24~72h → [0.02,0.07)，72~168h → [0,0.02)，>168h → 0。越临近截止奖励越高。
- `creditBonus [0, 0.05]`：发布者信誉超过基线（100分）时按比例奖励，从预加载缓存取，无 N+1 查询。
- `levelBonus [0, 0.05]`：任务金额落在接单者等级参考区间内满分，偏离越大奖励越少。

并返回 `scoreBreakdown` 便于调参/排查。

### 4.3 推荐理由生成（Explainability）

`recommendReason` 基于各维度子分阈值拼接，最多展示前 3 条最强信号：

- 命中偏好分类 → "符合你的偏好分类"
- ability ≥ 0.75 → "技能高度契合"；≥ 0.50 → "技能标签相关"
- amount ≥ 0.90 → "佣金在你的期望区间内"；≥ 0.70 → "佣金较为合理"
- 线上任务 distance=1.0 → "线上完成，无需出行"；distance ≥ 0.80 → "任务距离较近"
- delivery 命中（且未被前面覆盖）→ "符合你的交付偏好"
- urgencyBonus ≥ 0.07 → "截止日期临近，尽快接单"；≥ 0.03 → "任务即将截止"
- levelBonus ≥ 0.04 → "契合你当前的接单段位"
- 全不满足 → "与你的画像整体匹配"

### 4.4 权重自适应更新（结算后）

`updateWeightsAfterSettlement(userId, taskId)` 的核心思路：

- 校验任务存在、当前用户是接单者、且任务状态已结算
- 读取旧权重 `before`
- 调用 `computeGradient()` 计算各维度梯度：
  - **连续型**（ability/amount/distance）：子分中心化（-0.5）后 ×2，映射到 [-1, 1]
  - **二值型**（category/delivery）：命中给强正信号（+1.0/+0.80），未命中给中等负信号（-0.60/-0.40），避免一次未命中大幅拉低权重
- 各维度权重 += `LEARNING_RATE(0.04) × gradient`，clamp 到 [WEIGHT_MIN(0.05), WEIGHT_MAX(0.55)]
- `normalize()` 得到新权重并写回，记录日志（`trigger_type=1`）

### 4.5 冷启动优化

画像（abilityTags + preferredCategories）为空时：

- 多拉 `limit × 3`（至少 30）条候选，先通过 `receivable()` 过滤过期/不合金额距离的任务
- 按 `urgencyBonus + creditBonus` 排序，截止临近优先
- 同样使用 `batchLoadPublishers()` 批量预加载，无 N+1

---

## 5. 前端实现（页面与数据流）

### 5.1 API 封装

文件：`Front/src/api/recommendation.ts`（基于 `src/utils/request.ts`）

- `getProfile()` → `GET /recommendation/profile`
- `saveProfile(data)` → `PUT /recommendation/profile`
- `recommendTasks(limit)` → `GET /recommendation/tasks?limit=...`
- `refreshWeights(taskId)` → `POST /recommendation/tasks/{taskId}/weights/refresh`

> 注意：前端 `request.ts` 的 `baseURL` 最终会拼成后端的 `/api/...`。

### 5.2 首页推荐面板（轻量入口）

文件：`Front/src/views/Home.vue`

- 条件：`userStore.isAcceptor && recommendList.length`
- 挂载时 `loadRecommendations()` 调用 `recommendTasks(6)`，展示推荐任务卡片。
- 只做展示与刷新，画像编辑入口在"推荐中心"。

### 5.3 推荐中心页面（完整功能）

文件：`Front/src/views/Recommendation.vue`

- Tab1「为你推荐」
  - `loadTasks()` → `recommendTasks(12)`
  - 展示 `score`、`recommendReason` 与 `TaskCard`
- Tab2「我的画像」
  - `loadProfile()` → `getProfile()`，回填表单
  - `save()` → `saveProfile()` 保存画像后刷新推荐列表

### 5.4 路由与入口

- 路由：`Front/src/router/index.ts` 新增 `/recommendation`
- 入口：`Front/src/views/Home.vue` 头像下拉菜单（仅接单者可见）

---

## 6. 典型时序（从登录到推荐）

1. 用户登录成功，前端持久化 `campus_token`
2. 进入首页（接单者身份）：
   - `GET /api/recommendation/tasks?limit=6` 拉取推荐
3. 进入推荐中心：
   - `GET /api/recommendation/profile` 回填画像
   - 用户修改画像 → `PUT /api/recommendation/profile`
   - 保存成功后刷新推荐列表
4. 任务结算后（可选）：
   - 后端自动或前端手动 `POST /api/recommendation/tasks/{taskId}/weights/refresh` 调整权重

---

## 7. 常见问题与排查

- **推荐为空**
  - 任务是否都已过期/金额不在区间/线下距离超限（`receivable` 过滤）
  - 后端会仅取最近一批候选任务做评分（默认最多 300 条），若任务量非常大且你希望更"全量"的推荐，可调整该上限或改为分页+SQL 侧筛选。
  - 当日抢单/违规是否达到限制（`blocked`）
  - 画像是否过于严格（把 maxDistance、金额区间放宽试试）

- **权重看起来"乱跳"**
  - `normalize()` 会保证总和接近 1，但每次更新会微调；如需更稳定可以调小 `LEARNING_RATE` 或收窄 [WEIGHT_MIN, WEIGHT_MAX]。
  - 二值型维度（category/delivery）现在使用非对称梯度，命中正信号强于未命中负信号，整体更稳定。

- **推荐理由不符合预期**
  - 由阈值拼接（如 ability≥0.75），可在 `buildReason()` 中调整阈值或文案。

- **冷启动用户看到过期任务**
  - 已修复：冷启动现在也经过 `receivable()` 过滤。

---

## 8. 代码位置索引

- 后端
  - Controller：`campus-task-backend/src/main/java/com/campus/task/module/recommendation/controller/RecommendationController.java`
  - Service：`campus-task-backend/src/main/java/com/campus/task/module/recommendation/service/impl/RecommendationServiceImpl.java`
  - DTO/VO：`campus-task-backend/src/main/java/com/campus/task/module/recommendation/dto/*`、`vo/*`
  - 用户画像字段：`campus-task-backend/src/main/java/com/campus/task/module/user/entity/User.java`
- 前端
  - API：`Front/src/api/recommendation.ts`
  - 首页面板：`Front/src/views/Home.vue`
  - 推荐中心：`Front/src/views/Recommendation.vue`
  - 路由：`Front/src/router/index.ts`