# 智能课消与教务管理小程序

> 为教培机构提供一站式的教务与商业化解决方案 — 课程购买 → 智能签到 → 自动课消 → 数据看板

---

## 📋 目录

- [项目概览](#项目概览)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [数据库设计](#数据库设计)
- [API 文档](#api-文档)
- [快速启动](#快速启动)
- [业务流程图](#业务流程图)
- [核心设计](#核心设计)
- [模块清单](#模块清单)
- [待开发](#待开发)

---

## 项目概览

本系统覆盖教培机构的核心业务闭环：

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  课程购买  │ →  │  智能签到  │ →  │  自动课消  │ →  │  数据看板  │
│ 微信支付   │    │ 扫码+LBS  │    │ CAS扣减   │    │ 考勤/课消  │
│ 课时下发   │    │ 6维风控   │    │ 流水审计   │    │ 趋势分析   │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
       ↑                                              │
       └────────── 续费提醒（余额不足） ◀──────────────┘
```

| 角色 | 功能 |
|------|------|
| **学生** | 扫码签到、查看剩余课时、购买课程 |
| **教师** | 管理排课、点名册、手动签到、刷新二维码 |
| **管理员** | 数据看板（出勤率/课消率/预警） |

---

## 技术架构

```
┌─────────────────────────────────────────────────┐
│                   小程序前端                      │
│         Uni-app (Vue3) + 微信原生 API             │
│    checkin │ courses │ balance │ schedule │ dashboard │
└─────────────────┬───────────────────────────────┘
                  │ HTTP REST
┌─────────────────▼───────────────────────────────┐
│                  Spring Boot                     │
│  Controller → Service → Mapper (MyBatis-Plus)    │
│       │                                │         │
│  ┌────▼────┐                     ┌───▼────┐      │
│  │  MySQL   │                     │  Redis  │     │
│  │ 9 张表   │                     │ 缓存/锁  │     │
│  └─────────┘                     └────────┘      │
└─────────────────────────────────────────────────┘
         │              │
    ┌────▼────┐   ┌────▼────┐
    │ 微信支付  │   │ 腾讯LBS  │
    │  V3 API  │   │ 位置服务  │
    └─────────┘   └─────────┘
```

### 技术栈

| 层 | 技术 | 说明 |
|----|------|------|
| 前端框架 | Uni-app (Vue3) | 编译为微信小程序 |
| 前端构建 | Vite 5 | @dcloudio/vite-plugin-uni |
| 后端框架 | Spring Boot 2.7 | Java 8 |
| ORM | MyBatis-Plus 3.5 | Lambda 查询 + 乐观锁插件 |
| 数据库 | MySQL 8.0 | InnoDB，utf8mb4 |
| 缓存 | Redis | 签到码过期 / 设备指纹去重 |
| 工具库 | Hutool 5.8 | 雪花ID / JSON / 日期 |
| 支付 | 微信支付 V3 | JSAPI 统一下单 |
| 位置 | 腾讯位置服务 | Haversine 围栏计算 |

---

## 项目结构

```
miniprogram-1/
│
├── backend/                          # Spring Boot 后端 (62 个 Java 文件)
│   ├── pom.xml                       # Maven 依赖
│   └── src/main/
│       ├── java/com/edumanager/
│       │   ├── EduManagerApplication.java
│       │   ├── common/               # Result 响应体, GlobalExceptionHandler
│       │   ├── config/               # MybatisPlusConfig (自动填充+乐观锁插件)
│       │   ├── controller/           # 8 个 Controller
│       │   │   ├── CheckInController       # 签到 (扫码/LBS/手动)
│       │   │   ├── StudentBalanceController # 课时账户 CRUD
│       │   │   ├── TransactionController   # 流水查询 + 多课程余额
│       │   │   ├── CourseController        # 课程与课时包列表
│       │   │   ├── OrderController         # 创建订单 + 查单
│       │   │   ├── PayController           # 微信支付回调
│       │   │   ├── ScheduleController      # 排课 CRUD + 点名册
│       │   │   └── DashboardController     # 数据看板
│       │   ├── dto/                 # 14 个 DTO/VO
│       │   ├── entity/              # 9 个数据库实体
│       │   ├── event/               # LowBalanceEvent (余额预警事件)
│       │   ├── exception/           # BusinessException (业务异常)
│       │   ├── mapper/              # 9 个 Mapper (含手写 CAS SQL)
│       │   ├── service/             # 7 个接口 + 7 个实现
│       │   └── util/                # 3 个工具类
│       │       ├── DeviceFingerprintUtil  # 设备指纹 + 模拟器检测
│       │       ├── GeoDistanceUtil        # Haversine 距离计算
│       │       └── RiskAssessmentEngine   # 6 维风控打分引擎
│       └── resources/
│           └── application.yml      # 数据源/Redis/微信支付 配置
│
├── src/                              # Uni-app 前端 (16 个文件)
│   ├── App.vue                       # 根组件 (全局CSS变量)
│   ├── main.js                       # Vue3 入口
│   ├── pages.json                    # 页面路由 + TabBar 配置
│   ├── manifest.json                 # 微信小程序配置
│   ├── api/                          # API 封装层
│   │   ├── request.js                # HTTP 拦截器 (Token/异常/Toast)
│   │   ├── checkin.js                # 签到 API
│   │   ├── balance.js                # 课时 API
│   │   ├── course.js                 # 课程 API
│   │   ├── order.js                  # 订单 API
│   │   ├── schedule.js               # 排课 API
│   │   └── dashboard.js              # 看板 API
│   └── pages/                        # 5 个页面
│       ├── checkin/checkin.vue       # 扫码签到页 (~480行)
│       ├── courses/courses.vue       # 课程购买页 (~400行)
│       ├── balance/balance.vue       # 我的课时页 (~500行)
│       ├── schedule/schedule.vue     # 排课管理页 (~280行)
│       └── dashboard/dashboard.vue   # 数据看板页 (~250行)
│
├── package.json                      # 前端依赖
├── vite.config.js                    # Vite 配置 (含 API 代理)
├── index.html                        # Vite 入口
└── README.md
```

---

## 数据库设计

### E-R 关系图

```
 institution ──┬── student ──┬── student_balance ─── balance_transaction
               │              │        │
               │              ├── course_order
               │              └── attendance_log
               │
               ├── teacher
               │
               ├── course ─── course_package
               │    │
               │    └── student_balance
               │
               ├── class_schedule ── class_student ── attendance_log
               │
               └── wx_subscribe_message_log
```

### 核心表 (9 张)

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `institution` | 机构/租户 | `lbs_latitude`, `lbs_longitude`, `lbs_radius` (签到围栏) |
| `student` | 学员 | `openid`, `phone` |
| `teacher` | 教师/管理员 | `role` (TEACHER/ADMIN) |
| `course` | 课程定义 | `name`, `category` |
| `course_package` | 课时包/SKU | `total_hours`, `price` (分), `valid_days` |
| `course_order` | 购买订单 | `order_no`, `status` (PENDING→PAID), `wx_transaction_id` |
| `student_balance` | **课时账户** | `remaining`, `total_consumed`, `version` (乐观锁) |
| `balance_transaction` | 课时流水 | `change_type`, `change_amount`, `balance_before/after` |
| `class_schedule` | 排课计划 | `qr_code_token` (动态UUID), `qr_code_expire_at` |
| `class_student` | 点名册 | `status` (PENDING/CHECKED_IN/ABSENT/LEAVE) |
| `attendance_log` | 签到审计日志 | `risk_score`, `consume_result`, `device_info` |

### 关键索引

```sql
-- 课时账户唯一约束 (防重复)
UNIQUE KEY `uk_student_course` (`institution_id`, `student_id`, `course_id`)

-- 点名册唯一约束
UNIQUE KEY `uk_schedule_student` (`schedule_id`, `student_id`)

-- 排课二维码 Token 唯一
UNIQUE KEY `uk_qr_token` (`qr_code_token`)

-- 订单号唯一
UNIQUE KEY `uk_order_no` (`order_no`)
```

---

## API 文档

### 完整 API 列表 (14 个接口)

#### 🔵 签到模块

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/checkin/scan` | 学生扫码签到 |
| `POST` | `/api/checkin/lbs` | LBS 定位签到 |
| `POST` | `/api/checkin/manual` | 教师手动签到 |
| `GET` | `/api/checkin/status` | 查询签到状态 |

#### 🟢 课时模块

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/balance` | 查单个课程余额 |
| `GET` | `/api/balance/list` | 查多课程余额列表 |
| `GET` | `/api/balance/transactions` | 查流水（分页+按类型筛选） |
| `POST` | `/api/balance/add` | 增加课时（支付回调） |
| `POST` | `/api/balance/consume` | 扣减课时（签到课消） |

#### 🟡 购课模块

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/courses` | 课程+课时包列表 |
| `GET` | `/api/courses/{id}` | 课程详情 |
| `POST` | `/api/orders` | 创建订单 → 返回微信支付参数 |
| `GET` | `/api/orders/status` | 查询支付状态 |

#### 🟣 排课模块

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/schedules` | 创建排课 + 生成二维码 |
| `GET` | `/api/schedules` | 排课列表（按月份/教师/课程筛选） |
| `GET` | `/api/schedules/{id}` | 排课详情（含点名册统计） |
| `PUT` | `/api/schedules/{id}` | 更新排课 |
| `DELETE` | `/api/schedules/{id}` | 取消排课 |
| `GET` | `/api/schedules/{id}/roster` | 点名册学员列表 |
| `POST` | `/api/schedules/{id}/roster` | 添加学员到点名册 |
| `DELETE` | `/api/schedules/{id}/roster` | 移除学员 |
| `POST` | `/api/schedules/{id}/qrcode/refresh` | 刷新二维码 Token |

#### 🔴 支付模块

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/pay/notify` | 微信支付回调通知 |

#### ⚪ 数据模块

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/dashboard` | 月度数据看板（考勤率/课消率/趋势） |

### 统一响应格式

```json
{
  "code": 0,           // 0=成功, 非0=业务错误码
  "message": "ok",
  "data": { ... }      // 业务数据
}
```

| code | 说明 |
|------|------|
| 0 | 成功 |
| 10001 | 课时余额不足 |
| 10002 | 课时账户不存在 |
| 10003 | 并发冲突，请重试 |
| 20001 | 课时包已下架 |
| 30001 | 排课不存在 |
| 30002 | 学员已在点名册中 |

---

## 快速启动

### 前置条件

- JDK 8+
- Maven 3.6+
- MySQL 8.0
- Redis 6.0+
- Node.js 18+
- 微信开发者工具

### 1. 数据库初始化

```bash
mysql -u root -p
CREATE DATABASE edu_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE edu_manager;

# 执行建表 DDL（见下方：核心建表 SQL）
```

### 2. 后端启动

```bash
cd backend

# 修改 application.yml 中的数据库连接信息
# spring.datasource.url, username, password

mvn spring-boot:run
# 启动在 http://localhost:8080
```

### 3. 前端启动

```bash
cd ..  # 回到项目根目录

npm install
npm run dev:mp-weixin
# 编译产物在 dist/dev/mp-weixin
```

### 4. 导入微信开发者工具

1. 打开微信开发者工具
2. 导入项目 → 选择 `dist/dev/mp-weixin` 目录
3. 在 `manifest.json` 中填入你的微信小程序 AppID
4. 点击编译预览

### 5. 接口测试

```bash
# ① 加课时
curl -X POST http://localhost:8080/api/balance/add \
  -H "Content-Type: application/json" \
  -d '{"studentId":1,"courseId":1,"amount":16,"orderId":1}'

# ② 查余额
curl "http://localhost:8080/api/balance?studentId=1&courseId=1"

# ③ 模拟签到
curl -X POST http://localhost:8080/api/checkin/scan \
  -H "Content-Type: application/json" \
  -d '{"scheduleId":1,"studentId":1,"qrToken":"test-token","latitude":31.2304,"longitude":121.4737,"deviceBrand":"Apple","deviceModel":"iPhone 15","systemVersion":"iOS 17","wxVersion":"8.0.48"}'

# ④ 查流水
curl "http://localhost:8080/api/balance/transactions?studentId=1&page=1&pageSize=10"

# ⑤ 看板
curl "http://localhost:8080/api/dashboard?institutionId=1"
```

---

## 业务流程图

### 扫码签到 → 自动课消完整链路

```
 小程序前端                    后端 Spring Boot                    数据库
 ──────────                   ────────────────                    ──────
     │                              │                               │
     │ 1. wx.scanCode() 扫码        │                               │
     │    → 解析 {scheduleId,token} │                               │
     │                              │                               │
     │ 2. wx.getLocation() 定位     │                               │
     │    wx.getDeviceInfo() 设备   │                               │
     │                              │                               │
     │ 3. POST /api/checkin/scan ──►│                               │
     │    {scheduleId, token,       │                               │
     │     lat, lng, device...}    │                               │
     │                              │                               │
     │                              │ 【阶段A: 身份核验】              │
     │                              │  ├─ A1. Token 匹配 + 过期校验   │
     │                              │  ├─ A2. 点名册归属校验          │
     │                              │  └─ A3. 防重复签到             │
     │                              │                               │
     │                              │ 【阶段B: 6维风控】              │
     │                              │  ├─ 时间窗口 (±30min)          │
     │                              │  ├─ LBS围栏 (Haversine)       │
     │                              │  ├─ 模拟器检测                 │
     │                              │  ├─ 同设备多账号               │
     │                              │  ├─ 频繁换设备                 │
     │                              │  └─ RiskAssessmentEngine       │
     │                              │      → 0-30 SAFE              │
     │                              │      → 31-60 SUSPICIOUS       │
     │                              │      → 61-80 WARNING          │
     │                              │      → 81-100 HIGH_RISK       │
     │                              │                               │
     │                              │ 【阶段C: 写入签到】             │
     │                              │  ├─ UPDATE class_student ────►│
     │                              │  │   status=CHECKED_IN        │
     │                              │  └─ INSERT attendance_log ───►│
     │                              │                               │
     │                              │ 【阶段D: 自动课消】             │
     │                              │  ├─ consumeWithVersion() ────►│
     │                              │  │   UPDATE ...               │
     │                              │  │   WHERE remaining >= ?     │
     │                              │  │   AND version = ?          │
     │                              │  │   (CAS 乐观锁)              │
     │                              │  │                             │
     │                              │  ├─ affected=0?               │
     │                              │  │  → 余额不足 → 标记+提醒     │
     │                              │  │  → 版本冲突 → 重试(最多3次) │
     │                              │  │                             │
     │                              │  └─ INSERT balance_transaction►│
     │                              │                               │
     │ ◄── CheckInResult ───────────│                               │
     │  {checkInSuccess, riskLevel, │                               │
     │   consumptionSuccess,        │                               │
     │   remainingHours, ...}       │                               │
```

### 课程购买 → 支付 → 课时下发

```
 小程序端                       后端                          微信支付
 ────────                      ──────                        ────────
     │                            │                              │
     │ GET /api/courses ─────────►│ 查课程+课时包                 │
     │ ◄── 课程列表 ──────────────│                              │
     │                            │                              │
     │ 选择课时包 → 确认订单        │                              │
     │                            │                              │
     │ POST /api/orders ─────────►│                              │
     │  {studentId, packageId}   │ 查价格 → 创建订单(PENDING)     │
     │                            │ 调用 JSAPI 统一下单 ────────►│
     │                            │ ◄── prepay_id ─────────────│
     │ ◄── {orderNo, payParams} ──│ RSA签名 → 返回支付参数        │
     │                            │                              │
     │ wx.requestPayment() ───────────────────────────────────►│
     │ ◄── 支付成功 ──────────────────────────────────────────│
     │                            │                              │
     │                            │  ◄── POST /api/pay/notify ──│
     │                            │  验签 → 解密 → 更新PAID       │
     │                            │  调用 addBalance() 下发课时   │
     │                            │  INSERT balance_transaction   │
     │                            │                              │
     │ 查看我的课时 → 课时已到账    │                              │
```

---

## 核心设计

### 1. 并发安全的课时扣减

```sql
-- 手写 CAS SQL（MyBatis Mapper XML 或 @Update 注解）
-- 两条防线合并在一个原子 UPDATE 中：

UPDATE student_balance
SET remaining = remaining - #{amount},
    total_consumed = total_consumed + #{amount},
    version = version + 1
WHERE id = #{balanceId}
  AND remaining >= #{amount}       -- 防线①：余额防线
  AND version = #{expectedVersion} -- 防线②：并发防线
```

**为什么不直接用 MyBatis-Plus 的 `@Version`？**
`@Version` 只在 `updateById` 时生效，无法在 SQL 中同时加入 `remaining >= amount` 的业务校验。手写 CAS 将两重校验合并为原子操作。

**重试策略：**
```
affected=0 → 查最新记录 → remaining < amount? → 余额不足，返回失败
                        → remaining >= amount? → 版本冲突，用新version重试（最多3次）
```

### 2. 6维防作弊风控

| 维度 | 分值 | 检出方式 |
|------|------|----------|
| 签到时间窗口 | +20 | 签到时间 vs 排课时间 ±30min |
| LBS 围栏越界 | +30 | Haversine 距离 > 围栏半径 + 30m容错 |
| LBS 严重越界 | +60 | 距离 > 围栏半径 × 3 |
| 模拟器检测 | +40 | 品牌/型号/系统含模拟器关键字 |
| 同设备多账号 | +30 | Redis KEY `device:{fp}:students` 5min TTL |
| 频繁换设备 | +15 | 7天内设备指纹去重计数 > 3 |

**分级处理（不"一刀切"拒绝）：**

| 总分 | 等级 | 策略 |
|------|------|------|
| 0-30 | SAFE | 正常签到 |
| 31-60 | SUSPICIOUS | 正常签到 + 后台标记人工抽查 |
| 61-80 | WARNING | 签到成功 + 推送教师预警 |
| 81-100 | HIGH_RISK | 需教师二次确认 |

### 3. 支付回调幂等设计

```
handlePaymentNotify(notifyJson):
  1. 解析订单号 out_trade_no
  2. SELECT order WHERE order_no = ?
  3. if order.status == "PAID" → return "已处理" (幂等)
  4. if trade_state != "SUCCESS" → return (非成功不处理)
  5. UPDATE order SET status="PAID"
  6. addBalance(student, course, hours)
```

---

## 模块清单

| # | 模块 | 后端 | 前端 | 状态 |
|---|------|------|------|------|
| 1 | 数据库设计 | 9 张表 DDL | — | ✅ |
| 2 | 课时账户服务 | CAS 扣减 + 流水 | 我的课时页 | ✅ |
| 3 | 扫码签到 + 风控 | 6维风控 + 二维码校验 | 签到页 | ✅ |
| 4 | 自动课消 | 签到→扣课→预警 | 签到结果弹窗 | ✅ |
| 5 | 课程购买 | 课程包列表 + 下单 | 购课页 | ✅ |
| 6 | 微信支付 | V3 JSAPI + 回调 | wx.requestPayment | ✅ |
| 7 | 排课管理 | CRUD + 二维码 + 点名册 | 排课页 | ✅ |
| 8 | 数据看板 | 聚合统计 + 趋势 | 看板页 | ✅ |
| 9 | 微信订阅消息 | — | — | ⬜ 待开发 |

---

## 核心建表 SQL

```sql
-- 机构表
CREATE TABLE institution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(500),
    contact_phone VARCHAR(20),
    address VARCHAR(300),
    lbs_latitude DECIMAL(10,7),
    lbs_longitude DECIMAL(10,7),
    lbs_radius INT DEFAULT 300,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学员表
CREATE TABLE student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    openid VARCHAR(64) NOT NULL,
    unionid VARCHAR(64),
    real_name VARCHAR(50),
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    parent_phone VARCHAR(20),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_institution_openid (institution_id, openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课程表
CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    cover_url VARCHAR(500),
    description TEXT,
    category VARCHAR(50),
    default_duration INT DEFAULT 60,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课时包表
CREATE TABLE course_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    total_hours INT NOT NULL,
    price INT NOT NULL COMMENT '售价(分)',
    original_price INT COMMENT '原价(分)',
    valid_days INT DEFAULT 365,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课时账户表（核心表）
CREATE TABLE student_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    total_purchased DECIMAL(8,1) NOT NULL DEFAULT 0,
    total_consumed DECIMAL(8,1) NOT NULL DEFAULT 0,
    remaining DECIMAL(8,1) NOT NULL DEFAULT 0,
    expires_at DATE,
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_course (institution_id, student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课时流水表（只追加不修改）
CREATE TABLE balance_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    balance_id BIGINT NOT NULL,
    change_type VARCHAR(20) NOT NULL COMMENT 'PURCHASE/CONSUME/REFUND/ADJUST',
    change_amount DECIMAL(8,1) NOT NULL,
    balance_before DECIMAL(8,1) NOT NULL,
    balance_after DECIMAL(8,1) NOT NULL,
    biz_type VARCHAR(30),
    biz_id BIGINT,
    remark VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 购买订单表
CREATE TABLE course_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    institution_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    total_amount INT NOT NULL,
    paid_amount INT,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PAID/REFUNDED/CLOSED',
    pay_time DATETIME,
    wx_transaction_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 排课表
CREATE TABLE class_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    classroom VARCHAR(100),
    schedule_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_students INT DEFAULT 20,
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    qr_code_token VARCHAR(64),
    qr_code_expire_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_qr_token (qr_code_token),
    INDEX idx_date (schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 点名册表
CREATE TABLE class_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/CHECKED_IN/ABSENT/LEAVE',
    check_in_time DATETIME,
    check_in_method VARCHAR(20),
    check_in_lat DECIMAL(10,7),
    check_in_lng DECIMAL(10,7),
    check_in_device VARCHAR(200),
    risk_flag TINYINT DEFAULT 0,
    risk_reason VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_schedule_student (schedule_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 签到日志表
CREATE TABLE attendance_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    class_student_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    check_in_method VARCHAR(20) NOT NULL,
    check_in_time DATETIME NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    distance_from_center DECIMAL(8,2),
    device_info VARCHAR(500),
    wx_location_verify TINYINT DEFAULT 0,
    ip_address VARCHAR(45),
    risk_score INT DEFAULT 0,
    risk_detail VARCHAR(500),
    consume_result TINYINT DEFAULT 0 COMMENT '0未触发 1成功 2余额不足 3跳过',
    consume_transaction_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_schedule (schedule_id),
    INDEX idx_student (student_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 待开发

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P0 | 微信订阅消息 | 余额不足/签到成功/开课提醒 |
| P1 | 单元测试 + 集成测试 | 覆盖率 > 80% |
| P1 | 教师端管理后台 | 教师/管理员专用页面 |
| P2 | 排课重复/冲突检测 | 同教室同时间不可排课 |
| P2 | 多机构 SaaS 租户隔离 | 数据权限隔离 |
| P2 | ECharts 集成 | 替换纯 CSS 图表 |
| P3 | Docker 部署 | Dockerfile + docker-compose |
| P3 | CI/CD 流水线 | GitHub Actions 自动构建 |
