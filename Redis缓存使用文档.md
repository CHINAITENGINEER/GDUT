# 校园任务接单平台 — Redis 缓存使用文档

> 版本：v1.0 | 日期：2026-03-12 | 基于实际代码整理

---

## 一、Redis Key 总览

| Key 格式 | 数据类型 | TTL | 说明 |
|---------|---------|-----|------|
| `token:{userId}` | String | 7天（记住我）/ 1天 | 用户登录 Token |
| `sms:code:{phone}` | String | 5分钟 | 短信验证码 |
| `user:role:{userId}` | String | 7天 | 用户当前角色（publisher/acceptor） |
| `user:level:{userId}` | String | — | 结算后主动删除，触发刷新 |
| `user:credit:{userId}` | String | — | 结算后主动删除，触发刷新 |
| `fee:config` | String | 永久（手动） | 手续费配置缓存 |
| `category:list` | String | 永久（手动） | 分类标签列表缓存 |

> 注：`user:level` 和 `user:credit` 当前只做**删除操作**（缓存失效），读取直接查库；`fee:config` 和 `category:list` 仅做删除，读取也走库。如需完整 Cache-Aside 模式可后续补充写入逻辑。

---

## 二、各 Key 详细说明

### 2.1 `token:{userId}` — 登录 Token

**所在类**：`UserServiceImpl`

| 操作 | 代码位置 | 触发时机 |
|------|---------|----------|
| **写入** | `buildLoginVO()` | 用户登录（`login`）/ 注册（`register`）成功 |
| **删除** | `logout()` | 用户主动登出 |
| **删除** | `changePassword()` | 修改密码后强制登出 |
| **删除** | `AdminController.updateUserStatus()` | 管理员禁用用户时强制登出 |

**写入示例**：
```java
// 记住我：TTL 7天；否则 1天
redisTemplate.opsForValue().set("token:" + user.getId(), token, ttlDays, TimeUnit.DAYS);
```

**读取位置**：`JwtAuthFilter`（每次请求校验 Token 有效性，防止登出后重用）

**TTL 规则**：
- 勾选「记住我」：7 天
- 未勾选：1 天

---

### 2.2 `sms:code:{phone}` — 短信验证码

**所在类**：`UserServiceImpl`

| 操作 | 代码位置 | 触发时机 |
|------|---------|----------|
| **写入** | `sendSmsCode()` | 发送短信验证码 |
| **读取+删除** | `verifySmsCode()` | 注册、登录（验证码登录）、重置密码时校验 |

**写入示例**：
```java
String key = "sms:code:" + phone;
redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
```

**特殊规则**：验证码 `666666` 为万能码（演示用），直接通过校验并删除 Key。

**TTL**：5 分钟，每次发送覆盖旧值。

---

### 2.3 `user:role:{userId}` — 用户当前角色

**所在类**：`UserServiceImpl`

| 操作 | 代码位置 | 触发时机 |
|------|---------|----------|
| **写入** | `switchRole()` | 用户切换角色（发布者 ↔ 承接者） |
| **读取** | `toProfileVO()` | 获取个人信息时读取当前角色，Key 不存在默认返回 `publisher` |

**写入示例**：
```java
redisTemplate.opsForValue().set("user:role:" + userId, role, 7, TimeUnit.DAYS);
```

**说明**：角色状态不入库，仅存 Redis，重新登录或 Key 过期后默认为 `publisher`。

**TTL**：7 天。

---

### 2.4 `user:level:{userId}` — 用户等级缓存失效标记

**所在类**：`TaskServiceImpl`（`settle()` 方法）

| 操作 | 代码位置 | 触发时机 |
|------|---------|----------|
| **删除** | `settle()` | 任务结算成功，接单者经验/等级变更后删除 |

**删除示例**：
```java
redisTemplate.delete("user:level:" + acceptor.getId());
```

**说明**：当前只做缓存失效（删除），下次读取等级信息直接查库获取最新值。后续如需加速可补充写入逻辑（Cache-Aside 模式）。

---

### 2.5 `user:credit:{userId}` — 用户信誉分缓存失效标记

**所在类**：`TaskServiceImpl`（`settle()` 方法）

| 操作 | 代码位置 | 触发时机 |
|------|---------|----------|
| **删除** | `settle()` | 任务结算成功，接单者信誉分变更后删除 |

**删除示例**：
```java
redisTemplate.delete("user:credit:" + acceptor.getId());
```

**说明**：与 `user:level` 相同，只做失效处理。

---

### 2.6 `fee:config` — 手续费配置

**所在类**：`AdminController`

| 操作 | 代码位置 | 触发时机 |
|------|---------|----------|
| **删除** | `updateFeeConfig()` | 管理员更新手续费配置后立即删除 |

**删除示例**：
```java
// 更新数据库后刷新缓存
redisTemplate.delete("fee:config");
```

**说明**：这是**动态手续费**的核心亮点。管理员在后台修改费率后，无需重启服务，下次结算时自动从数据库读取最新配置。

> 当前 `TaskServiceImpl.getBaseFeeRate()` 是硬编码（100元以下5%，以上3%），如需完整动态效果，需将该方法改为先查 Redis `fee:config`，命中则用缓存，未命中则查 `fee_config` 表并写入缓存。

---

### 2.7 `category:list` — 分类标签列表

**所在类**：`AdminController`

| 操作 | 代码位置 | 触发时机 |
|------|---------|----------|
| **删除** | `addCategory()` | 新增分类标签 |
| **删除** | `updateCategory()` | 修改分类标签 |
| **删除** | `deleteCategory()` | 删除分类标签 |

**删除示例**：
```java
redisTemplate.delete("category:list");
```

**说明**：`TaskCategoryController.list()` 当前直接查库，Key 存在但未被读取。如需缓存加速，需在 `list()` 方法中先读 Redis，未命中再查库并写入。

---

## 三、缓存操作汇总（按类）

### UserServiceImpl

```
登录/注册     → SET  token:{userId}          TTL 7天/1天
登出          → DEL  token:{userId}
修改密码      → DEL  token:{userId}
发送验证码    → SET  sms:code:{phone}        TTL 5分钟
验证验证码    → GET + DEL  sms:code:{phone}
切换角色      → SET  user:role:{userId}      TTL 7天
获取个人信息  → GET  user:role:{userId}（不存在默认 publisher）
```

### TaskServiceImpl

```
任务结算      → DEL  user:level:{userId}
任务结算      → DEL  user:credit:{userId}
```

### AdminController

```
禁用用户      → DEL  token:{userId}
更新手续费    → DEL  fee:config
新增分类      → DEL  category:list
修改分类      → DEL  category:list
删除分类      → DEL  category:list
```

---

## 四、待完善项（比赛后优化建议）

| 缺口 | 当前状态 | 建议补充 |
|------|---------|----------|
| `fee:config` 读取 | `getBaseFeeRate()` 硬编码 | 改为查 Redis，未命中查库并 SET |
| `category:list` 读取 | `TaskCategoryController` 直接查库 | 改为查 Redis，未命中查库并 SET |
| `user:level` 读取 | 直接查库 | 结算后 SET 最新值，读取先查 Redis |
| `user:credit` 读取 | 直接查库 | 评价提交后 DEL，读取先查 Redis |
| 热门任务列表缓存 | 未实现 | `task:list:hot` TTL 5分钟，任务状态变更时 DEL |
| 任务详情缓存 | 未实现 | `task:detail:{id}` TTL 10分钟，状态变更时 DEL |
| 当日抢单计数 | 查数据库 | `grab:daily:{userId}:{date}` INCR，TTL至23:59 |
| 当日违规计数 | 查数据库 | `violation:daily:{userId}:{date}` INCR，TTL至23:59 |

---

## 五、Redis 连接配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:        # 无密码留空
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          min-idle: 2
          max-idle: 8
          max-active: 16
```

序列化方式：`StringRedisTemplate`（Key 和 Value 均为纯字符串），配置见 `RedisConfig.java`。

---

*文档版本：v1.0 | 生成时间：2026-03-12 | 基于代码实际扫描整理*
