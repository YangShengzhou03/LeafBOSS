# LEAF-BOSS API 接口文档

所有接口以 `/api` 开头。需认证的接口 Header 携带 `Authorization: Bearer <Token>`。

统一响应结构：
```json
{ "code": 200, "message": "提示信息", "data": ..., "timestamp": 1765430400000 }
```
`code` 为 `200` 表示成功，其他值表示失败，失败原因见 `message`。

---

## 认证规则

鉴权流程（`JwtInterceptor` + `CorsConfig`）：
1. `CorsConfig` 拦截器排除的路径直接放行（`/api/auth/login` `/api/auth/register` `/api/user-auth/login` `/api/user-auth/register` `/api/public/**` `/api/admins/send-reset-code` `/api/admins/reset-password`）
2. 其余路径进入拦截器，`isPublicPath` 白名单放行（POST `/api/user-auth/send-reset-code` `/api/user-auth/send-register-code` `/api/user-auth/reset-password`）
3. 非白名单路径要求携带有效 Token（`currentUserId` 非空）
4. `isUserAccessiblePath` 允许任何登录用户访问
5. `isAdminOnlyPath` 要求 `admin` 角色
6. 带 Token 的请求若即将过期，响应头返回 `New-Token`（滑动续期）
7. 单设备登录：新登录挤掉旧 Token，旧 Token 校验 session_token 失败返回 401 "账号已在其他设备登录"

| 路径模式 | 认证要求 |
|----------|----------|
| `/api/auth/login`、`/api/auth/register` | 公开 |
| `/api/user-auth/login`、`/api/user-auth/register`、`/api/user-auth/send-register-code`、`/api/user-auth/send-reset-code`、`/api/user-auth/reset-password` | 公开 |
| `/api/user-auth/logout`、`/api/user-auth/me`、`/api/user-auth/send-email-change-code` | 需登录 |
| `/api/public/*` | 完全公开（跳过拦截器） |
| `/api/admins` | 需 admin Token |
| `/api/admins/send-reset-code`、`/api/admins/reset-password` | 公开 |
| `/api/admins/admin-reset-password` | 需 admin Token |
| `/api/users/*` | 需 admin Token |
| `/api/products`、`/api/specifications`、`/api/companies` | GET 需登录，写操作需 admin Token |
| `/api/v1/company-reviews` | 需登录（app 客户端专用） |
| `/api/card-keys/with-details`、`/api/card-keys/remaining-generate-count` | 需登录（admin 或 agent，控制器内做数据隔离） |
| `/api/card-keys/batch` | 需登录（agent 导入） |
| `/api/card-keys/verify` | 需登录 |
| `/api/card-keys/redeem` | 需登录 |
| `/api/card-keys/agent/revoke`、`/api/card-keys/agent/restore`、`/api/card-keys/agent/by-card-key/{cardKey}` | 需登录（agent 自助操作本人卡密） |
| `/api/card-keys/*` 其余端点 | 需 admin Token |
| `/api/agent-authorizations`、`/api/agent-authorizations/{id}/*` | 需 admin Token |
| `/api/agent-authorizations/products` | 需登录（agent 查看自己授权的商品） |
| `/api/user-products/user/me` | 需登录 |
| `/api/user-products/*` 其余端点 | 需 admin Token |
| `/api/operation-logs/*` | 需 admin Token |
| `/api/admin/*` | 需 admin Token |
| `/api/notices`、`/api/notices/{id}` | 需 admin Token（GET 也需要） |
| `/api/public/notices` | 公开 |
| `/api/feedbacks/*` | 需 admin Token |

---

## 1. 管理员认证 `/api/auth`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| POST | `/api/auth/login` | 公开 | 管理员登录，返回 `{ token, user }` |
| POST | `/api/auth/register` | 公开 | 管理员注册（新管理员 status 强制 `inactive`） |
| GET  | `/api/auth/me` | 需登录 | 获取当前管理员信息（查 Admin 表，非 admin 角色返回"用户不存在"） |
| PUT  | `/api/auth/me` | 需登录 | 更新当前管理员用户名/邮箱（字段白名单） |
| GET  | `/api/auth/storage` | 需登录 | 获取存储配额信息（固定值，预留字段） |

**登录请求体：**
```json
{ "email": "admin@example.com", "password": "123456" }
```

---

## 2. 用户/代理认证 `/api/user-auth`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| POST | `/api/user-auth/login` | 公开 | 用户/代理登录（拒绝 admin 角色） |
| POST | `/api/user-auth/register` | 公开 | 用户注册（需验证码，role 强制 `user`，status 强制 `active`） |
| GET  | `/api/user-auth/me` | 登录 | 获取当前用户信息 |
| PUT  | `/api/user-auth/me` | 登录 | 更新当前用户用户名/邮箱/密码（邮箱变更需验证码） |
| POST | `/api/user-auth/logout` | 登录 | 登出 |
| POST | `/api/user-auth/send-email-change-code` | 登录 | 发送邮箱变更验证码 |
| POST | `/api/user-auth/send-register-code` | 公开 | 发送注册验证码 |
| POST | `/api/user-auth/send-reset-code` | 公开 | 发送密码重置验证码 |
| POST | `/api/user-auth/reset-password` | 公开 | 通过验证码重置密码 |

**注册请求体：**
```json
{ "email": "user@example.com", "password": "123456", "verificationCode": "xxxx" }
```

---

## 3. 管理员管理 `/api/admins`（需 admin Token）

| 方法 | URL | 说明 |
|------|-----|------|
| GET    | `/api/admins` | 管理员列表（分页，支持 `keyword`/`status`） |
| POST   | `/api/admins` | 创建管理员（用户名默认 `leafAdmin`，密码随机 8 位） |
| PUT    | `/api/admins/{id}` | 更新管理员信息（字段白名单：username/email/status/password） |
| DELETE | `/api/admins/{id}` | 删除管理员 |
| POST   | `/api/admins/reset-password` | 公开：验证码方式重置密码 |
| POST   | `/api/admins/admin-reset-password` | 需 admin：管理员重置他人密码（需操作者自身密码确认） |
| POST   | `/api/admins/send-reset-code` | 公开：发送管理员密码重置验证码 |

---

## 4. 用户管理 `/api/users`（需 admin Token）

| 方法 | URL | 说明 |
|------|-----|------|
| GET    | `/api/users` | 用户列表（分页，支持 `keyword`/`status`/`role` 筛选） |
| POST   | `/api/users` | 创建用户（禁止创建 admin 角色，自动处理用户名重复） |
| PUT    | `/api/users/{id}` | 更新用户（禁止提升为 admin，字段白名单更新） |
| DELETE | `/api/users/{id}` | 删除用户 |
| POST   | `/api/users/reset-password` | 管理员重置用户密码 |

---

## 5. 商品管理 `/api/products`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| GET    | `/api/products` | 登录 | 商品列表（分页，支持 `name`/`status` 筛选） |
| POST   | `/api/products` | admin | 创建商品（名称唯一，默认 status `active`） |
| PUT    | `/api/products/{id}` | admin | 更新商品 |
| DELETE | `/api/products/{id}` | admin | 删除商品 |

> 搜索参数名是 **`name`**，不是 `keyword`。

---

## 6. 规格管理 `/api/specifications`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| POST   | `/api/specifications` | admin | 创建规格（名称唯一，默认 status `active`） |
| PUT    | `/api/specifications/{id}` | admin | 更新规格 |
| DELETE | `/api/specifications/{id}` | admin | 删除规格 |
| GET    | `/api/specifications/dto` | 登录 | 规格 DTO 列表（全量） |
| GET    | `/api/specifications/dto/pagination` | 登录 | 规格 DTO 分页 |

---

## 7. 卡密管理 `/api/card-keys`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| GET    | `/api/card-keys/with-details` | 登录 | 卡密详情列表（分页，含商品/规格/使用用户名及邮箱/激活与到期时间，agent 自动隔离为本人数据） |
| GET    | `/api/card-keys/agents` | admin | 获取代理商列表（role=agent 的用户） |
| GET    | `/api/card-keys/remaining-generate-count?productId=` | 登录 | 当前用户剩余可生成卡密数（按商品授权余额） |
| POST   | `/api/card-keys/batch` | 登录 | 批量导入卡密（单次最多 10000 条，受商品授权余额限制，原子扣减） |
| POST   | `/api/card-keys/verify` | 登录 | 验证卡密有效性（返回完整商品/规格信息） |
| POST   | `/api/card-keys/redeem` | 登录 | 兑换卡密（激活卡密并创建用户商品授权） |
| POST   | `/api/card-keys/status` | admin | 切换卡密状态（`已使用`/`未使用`/`已禁用`） |
| DELETE | `/api/card-keys/by-card-key/{cardKey}` | admin | 删除卡密（按卡密值） |

**代理商自助操作（需登录，仅限本人卡密）：**

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/api/card-keys/agent/revoke` | 吊销卡密 |
| POST | `/api/card-keys/agent/restore` | 恢复已吊销卡密 |
| DELETE | `/api/card-keys/agent/by-card-key/{cardKey}` | 删除卡密 |

**批量导入请求体：**
```json
{ "specId": 1, "keys": ["KEY-001", "KEY-002"], "agentId": "optional" }
```

**兑换请求体：**
```json
{ "cardKey": "KEY-001" }
```

**兑换成功响应：**
```json
{
  "code": 200,
  "data": {
    "success": true,
    "message": "兑换成功",
    "productName": "商品A",
    "specificationName": "月卡",
    "validDays": 30,
    "expiresAt": "2026-10-25T12:00:00"
  }
}
```

**兑换失败响应：**
```json
{ "code": 400, "message": "卡密已被使用或已禁用" }
```

---

## 8. 代理授权 `/api/agent-authorizations`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| GET    | `/api/agent-authorizations` | admin | 代理授权列表（分页，支持 `keyword`/`status`） |
| GET    | `/api/agent-authorizations/products` | 登录 | 当前代理商已授权的商品列表 |
| POST   | `/api/agent-authorizations` | admin | 创建代理授权（必填 agentId/productId/remainingCount） |
| PUT    | `/api/agent-authorizations/{id}` | admin | 更新代理授权 |
| PUT    | `/api/agent-authorizations/{id}/revoke` | admin | 吊销代理授权 |
| PUT    | `/api/agent-authorizations/{id}/restore` | admin | 恢复代理授权 |
| DELETE | `/api/agent-authorizations/{id}` | admin | 删除代理授权 |

---

## 9. 用户商品授权 `/api/user-products`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| GET    | `/api/user-products` | admin | 授权列表（分页，支持 `keyword`/`status`） |
| GET    | `/api/user-products/user/me` | 登录 | 当前登录用户的授权列表 |
| POST   | `/api/user-products` | admin | 创建授权（必填 userId/productId/expiresAt） |
| PUT    | `/api/user-products/{id}` | admin | 更新授权（字段白名单） |
| PUT    | `/api/user-products/{id}/revoke` | admin | 吊销授权（status 置 0） |
| PUT    | `/api/user-products/{id}/restore` | admin | 恢复授权（status 置 1） |
| DELETE | `/api/user-products/{id}` | admin | 删除授权 |

---

## 10. 公司管理 `/api/companies`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| GET    | `/api/companies` | 登录 | 公司列表（分页，支持 `name` 模糊筛选） |
| POST   | `/api/companies` | admin | 创建公司（commentCount 默认 0） |
| PUT    | `/api/companies/{id}` | admin | 更新公司 |
| DELETE | `/api/companies/{id}` | admin | 删除公司 |

---

## 11. 公司评论 `/api/v1/company-reviews`（app 客户端专用，需登录）

> ⚠️ **外部客户端依赖 — 不可删除**
> 以下四个接口由外部项目 `D:\code\js\jobs_helper\extension`（浏览器插件）调用，并非死代码。清理死代码时请勿移除本节任何接口及其对应的 Controller、Service、Entity、Mapper。
> - `GET /api/v1/company-reviews` — 获取评论列表（支持 `company_name`/`page`/`size`）
> - `POST /api/v1/company-reviews` — 发布评论
> - `DELETE /api/v1/company-reviews/{id}` — 删除评论（仅本人）
> - `POST /api/v1/company-reviews/{id}/vote` — 点赞/点踩

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| GET    | `/api/v1/company-reviews?company_name=&page=&size=` | 登录 | 分页查评论，`company_name` 为可选过滤（不传或为空返回全部评论） |
| POST   | `/api/v1/company-reviews` | 登录 | 发布评论 |
| DELETE | `/api/v1/company-reviews/{id}` | 登录 | 删除评论（仅评论者本人） |
| POST   | `/api/v1/company-reviews/{id}/vote` | 登录 | 点赞/点踩（一用户一票，重复取消，改票切换） |

**列表响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "userId": "user-uuid",
        "companyId": 5,
        "content": "评论内容",
        "likeCount": 3,
        "dislikeCount": 1,
        "createdAt": "2026-10-03T10:30:00",
        "myVote": "like",
        "companyName": "xxx公司"
      }
    ],
    "total": 10,
    "page": 1,
    "size": 10
  }
}
```
> `myVote`：当前登录用户对该评论的投票，`like`/`dislike`/`null`（未投票或未登录）。
> 客户端可通过 `userId` 与自身 ID 比对判断是否是自己评论的。

**提交评论请求体：**
```json
{ "company_name": "xxx", "content": "评论内容" }
```

**投票请求体：**
```json
{ "vote": "like" }
```
可选值：`like`（点赞）或 `dislike`（点踩）。

**成功响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "userId": "user-uuid",
        "companyId": 5,
        "content": "评论内容",
        "likeCount": 3,
        "dislikeCount": 1,
        "createdAt": "2026-10-03T10:30:00",
        "myVote": "like",
        "companyName": "某公司名称",
        "canDelete": true
      }
    ],
    "total": 10,
    "page": 1,
    "size": 10,
    "currentUserId": "user-uuid"
  },
  "timestamp": 1765430400000
}
```

**字段说明：**
| 字段 | 类型 | 说明 |
|------|------|------|
| `records` | array | 评论列表 |
| `records[].id` | int | 评论 ID |
| `records[].userId` | string | 评论者用户 ID |
| `records[].companyId` | int | 公司 ID |
| `records[].content` | string | 评论内容 |
| `records[].likeCount` | int | 点赞数 |
| `records[].dislikeCount` | int | 点踩数 |
| `records[].createdAt` | datetime | 创建时间 |
| `records[].myVote` | string/null | 当前用户对该评论的投票：`like`/`dislike`，未投票时为 `null` |
| `records[].companyName` | string | 公司名称（联表查询） |
| `records[].canDelete` | boolean | 当前登录用户是否有权限删除此评论 |
| `total` | int | 总评论数 |
| `page` | int | 当前页码 |
| `size` | int | 每页条数 |
| `currentUserId` | string/null | 当前登录用户 ID，未登录时为 `null` |

---

## 12. 仪表盘统计 `/api/admin`（需 admin Token）

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/api/admin/stats` | 核心指标（用户数、卡密数、激活率、仓库总值、各时段收入等） |
| GET | `/api/admin/daily-revenue-trend?days=30` | 每日收入趋势（参数 `days` 1-90，默认 30） |
| GET | `/api/admin/today-sales-distribution` | 今日销售分布（按规格） |

**`/stats` 响应字段：**
```json
{
  "dailySales": 10, "dailyRevenue": 299.0,
  "yesterdaySales": 5, "yesterdayRevenue": 149.5,
  "totalOrders": 1000, "monthlyRevenue": 5999.0,
  "lastMonthRevenue": 3999.0, "weeklyRevenue": 1999.0,
  "lastWeekRevenue": 999.0, "totalCardKeys": 5000,
  "activationRate": 20.0, "cardKeyCount": 4000,
  "stockValue": 119600.0, "userCount": 150
}
```

---

## 13. 操作日志 `/api/operation-logs`（需 admin Token）

| 方法 | URL | 说明 |
|------|-----|------|
| GET    | `/api/operation-logs` | 日志列表（分页，支持 `startDate`/`endDate`/`operationType`/`operationTypes`） |
| DELETE | `/api/operation-logs` | 清空所有日志 |

---

## 14. 通知管理 `/api/notices`

| 方法 | URL | 认证 | 说明 |
|------|-----|------|------|
| GET    | `/api/public/notices?productName=` | 公开 | 按商品名获取通知（无需鉴权） |
| GET    | `/api/notices` | admin | 通知列表（分页，默认 size=10） |
| POST   | `/api/notices` | admin | 创建通知 |
| PUT    | `/api/notices/{id}` | admin | 更新通知（字段：productName/content/level） |
| DELETE | `/api/notices/{id}` | admin | 删除通知 |

**通知实体字段：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productName | string | 是 | 目标受众（"管理员"/"普通用户"/"代理商"/具体商品名） |
| content | string | 是 | 通知内容 |
| level | string | 否 | 级别：`info`（默认）/`important`/`urgent` |

---

## 15. 商品反馈 `/api/feedbacks`（需 admin Token）

| 方法 | URL | 说明 |
|------|-----|------|
| GET    | `/api/feedbacks` | 反馈列表（按时间倒序） |
| DELETE | `/api/feedbacks/{id}` | 删除反馈 |

---

## 16. 公开接口 `/api/public/*`（跳过拦截器，无需认证）

### 16.1 卡密验证并激活（安装验证）

| 方法 | 说明 |
|------|------|
| GET | `/api/public/card-keys/verify/{cardKey}` | 验证卡密，若"未使用"则自动激活，返回 `"商品名-规格名"` |

状态流转：
- `未使用` → 自动激活为 `已使用`，返回商品规格信息（200）
- `已使用` → 400 "该卡密已被使用"
- `已禁用`/`已吊销` → 400 "该卡密已被吊销，请联系开发者了解原因"
- 不存在 → 404

---

## 附录：分页查询通用参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |

分页响应结构（部分接口返回 `IPage` 原始结构，部分返回自定义结构）：
```json
{
  "code": 200,
  "data": {
    "page": 1,
    "size": 10,
    "total": 100,
    "records": [...]
  }
}
```

---

## 附录：实体状态枚举

**卡密状态：**
| 值 | 说明 |
|-----|------|
| 未使用 | 已生成，等待首次验证 |
| 已使用 | 已激活 |
| 已禁用 | 被管理员禁用 |
| 已吊销 | 被代理商吊销 |

**管理员/用户状态：**
| 值 | 说明 |
|-----|------|
| active | 正常可用 |
| inactive | 新注册默认，需激活 |

**用户商品授权 status：**
| 值 | 说明 |
|-----|------|
| 1 | 激活 |
| 0 | 已吊销 |

**通知 level：**
| 值 | 说明 |
|-----|------|
| info | 普通（默认） |
| important | 重要 |
| urgent | 紧急 |
