<template>
  <view class="balance-page">
    <!-- ==================== 下拉刷新容器 ==================== -->
    <scroll-view
      class="scroll-container"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- ==================== 课时总览卡片 ==================== -->
      <view class="overview-card">
        <!-- 背景装饰 -->
        <view class="overview-card__bg"></view>

        <!-- 剩余课时（核心数据） -->
        <view class="overview-card__main">
          <text class="overview-card__label">剩余课时</text>
          <view class="overview-card__number-row">
            <text class="overview-card__number">{{ balanceData.remaining }}</text>
            <text class="overview-card__unit">课时</text>
          </view>
          <!-- 课时进度条 -->
          <view class="overview-card__progress">
            <view class="progress-bar">
              <view
                class="progress-bar__fill"
                :style="{ width: consumptionPercent + '%' }"
              ></view>
            </view>
            <text class="overview-card__progress-text">
              已用 {{ balanceData.totalConsumed }} / 共 {{ balanceData.totalPurchased }} 课时
            </text>
          </view>
        </view>

        <!-- 统计行 -->
        <view class="overview-card__stats">
          <view class="stat-item">
            <text class="stat-item__value">{{ balanceData.totalPurchased }}</text>
            <text class="stat-item__label">累计购买</text>
          </view>
          <view class="stat-item stat-item--divider">
            <text class="stat-item__value stat-item__value--consume">
              {{ balanceData.totalConsumed }}
            </text>
            <text class="stat-item__label">累计消耗</text>
          </view>
          <view class="stat-item">
            <text class="stat-item__value" :class="expiryClass">
              {{ balanceData.daysUntilExpiry > 0 ? balanceData.daysUntilExpiry + '天' : '已过期' }}
            </text>
            <text class="stat-item__label">剩余有效期</text>
          </view>
        </view>
      </view>

      <!-- ==================== 余额不足预警条 ==================== -->
      <view v-if="isLowBalance" class="low-balance-alert">
        <view class="low-balance-alert__icon">⚠️</view>
        <view class="low-balance-alert__content">
          <text class="low-balance-alert__title">课时即将用完</text>
          <text class="low-balance-alert__desc">
            剩余课时不足 2 节，请尽快续费以免影响正常上课
          </text>
        </view>
        <view class="low-balance-alert__action" @tap="handleRenewal">
          <text>去续费</text>
        </view>
      </view>

      <!-- ==================== 课程列表 ==================== -->
      <!-- 多课程场景下展示每个课程的课时卡片 -->
      <view class="section-header" v-if="courseList.length > 0">
        <text class="section-header__title">各课程课时</text>
      </view>
      <view
        v-for="course in courseList"
        :key="course.courseId"
        class="course-card"
        @tap="handleCourseDetail(course)"
      >
        <image
          v-if="course.coverUrl"
          class="course-card__cover"
          :src="course.coverUrl"
          mode="aspectFill"
        />
        <view class="course-card__info">
          <text class="course-card__name">{{ course.courseName }}</text>
          <text class="course-card__expiry" v-if="course.expiresAt">
            有效期至 {{ course.expiresAt }}
          </text>
        </view>
        <view class="course-card__balance">
          <text class="course-card__remaining">{{ course.remaining }}</text>
          <text class="course-card__unit">课时</text>
        </view>
      </view>

      <!-- ==================== 近期流水列表 ==================== -->
      <view class="section-header">
        <text class="section-header__title">近期记录</text>
        <view class="section-header__tabs">
          <text
            class="section-header__tab"
            :class="{ 'section-header__tab--active': activeTab === 'all' }"
            @tap="switchTab('all')"
          >全部</text>
          <text
            class="section-header__tab"
            :class="{ 'section-header__tab--active': activeTab === 'consume' }"
            @tap="switchTab('consume')"
          >消耗</text>
          <text
            class="section-header__tab"
            :class="{ 'section-header__tab--active': activeTab === 'purchase' }"
            @tap="switchTab('purchase')"
          >充值</text>
        </view>
      </view>

      <!-- 流水列表 -->
      <view v-if="filteredTransactions.length > 0" class="transaction-list">
        <view
          v-for="item in filteredTransactions"
          :key="item.id"
          class="transaction-item"
        >
          <view class="transaction-item__left">
            <!-- 图标根据类型变化 -->
            <view
              class="transaction-item__icon"
              :class="'transaction-item__icon--' + item.changeType"
            >
              <text>{{ getTransactionIcon(item) }}</text>
            </view>
            <view class="transaction-item__info">
              <text class="transaction-item__title">{{ item.bizDescription }}</text>
              <text class="transaction-item__time">{{ item.createdAt }}</text>
              <text class="transaction-item__remark" v-if="item.remark">
                {{ item.remark }}
              </text>
            </view>
          </view>
          <view class="transaction-item__right">
            <text
              class="transaction-item__amount"
              :class="{
                'transaction-item__amount--plus': item.changeType === 'PURCHASE',
                'transaction-item__amount--minus': item.changeType === 'CONSUME',
              }"
            >
              {{ item.changeType === 'PURCHASE' ? '+' : '' }}{{ item.changeAmount }} 课时
            </text>
            <text class="transaction-item__balance">余额 {{ item.balanceAfter }}</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else class="empty-state">
        <text class="empty-state__icon">📋</text>
        <text class="empty-state__text">暂无记录</text>
        <text class="empty-state__hint">签到或购买课程后，记录将在这里显示</text>
      </view>

      <!-- 底部安全距离 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { balanceApi } from '@/api/balance'
import dayjs from 'dayjs'

// ==================== 响应式状态 ====================

/** 是否正在下拉刷新 */
const isRefreshing = ref(false)

/** 当前激活的 Tab */
const activeTab = ref('all')

/** 课时总览数据（第一门课为主展示） */
const balanceData = reactive({
  remaining: 0,
  totalPurchased: 0,
  totalConsumed: 0,
  daysUntilExpiry: 0,
  expiresAt: '',
})

/** 各课程课时列表 */
const courseList = ref([])

/** 交易/流水列表（原始数据） */
const transactions = ref([])

// ==================== 计算属性 ====================

/** 课时消耗百分比 */
const consumptionPercent = computed(() => {
  if (balanceData.totalPurchased === 0) return 0
  const pct = (balanceData.totalConsumed / balanceData.totalPurchased) * 100
  return Math.min(Math.round(pct), 100)
})

/** 是否余额不足（剩余 <= 2 课时） */
const isLowBalance = computed(() => {
  return balanceData.remaining > 0 && balanceData.remaining <= 2
})

/** 有效期样式 */
const expiryClass = computed(() => {
  if (balanceData.daysUntilExpiry <= 0) return 'stat-item__value--danger'
  if (balanceData.daysUntilExpiry <= 30) return 'stat-item__value--warning'
  return ''
})

/** 按 Tab 筛选后的流水 */
const filteredTransactions = computed(() => {
  if (activeTab.value === 'all') return transactions.value
  if (activeTab.value === 'consume') {
    return transactions.value.filter(t => t.changeType === 'CONSUME')
  }
  if (activeTab.value === 'purchase') {
    return transactions.value.filter(t => t.changeType === 'PURCHASE')
  }
  return transactions.value
})

// ==================== 生命周期 ====================

onMounted(() => {
  loadAllData()
})

// ==================== 数据加载 ====================

/**
 * 并行加载课程余额和流水列表（真实 API）
 */
async function loadAllData() {
  const { studentId } = getQueryParams()
  if (!studentId) return

  try {
    // 并行请求：课程余额列表 + 近期流水
    const [balances, txList] = await Promise.all([
      balanceApi.listBalances(studentId),
      balanceApi.listTransactions({ studentId, page: 1, pageSize: 20 }),
    ])

    // 设置课程列表
    courseList.value = balances || []

    // 以第一门课为主展示在顶部卡片
    if (balances && balances.length > 0) {
      const main = balances[0]
      balanceData.remaining = main.remaining || 0
      balanceData.totalPurchased = main.totalPurchased || 0
      balanceData.totalConsumed = main.totalConsumed || 0
      balanceData.expiresAt = main.expiresAt || ''
      balanceData.daysUntilExpiry = main.daysUntilExpiry || 0
    }

    // 设置流水列表
    transactions.value = txList || []

  } catch (error) {
    console.error('加载课时数据失败:', error)
    // 网络异常时保留旧数据不清空
  }
}

/** 下拉刷新 */
async function onRefresh() {
  isRefreshing.value = true
  try {
    await loadAllData()
  } finally {
    isRefreshing.value = false
  }
}

// ==================== 交互方法 ====================

/** 切换流水 Tab — 重新请求对应类型 */
async function switchTab(tab) {
  activeTab.value = tab
  const { studentId } = getQueryParams()
  if (!studentId) return

  try {
    const changeType = tab === 'all' ? null : tab === 'consume' ? 'CONSUME' : 'PURCHASE'
    const txList = await balanceApi.listTransactions({
      studentId,
      changeType,
      page: 1,
      pageSize: 20,
    })
    transactions.value = txList || []
  } catch (error) {
    console.error('切换Tab加载失败:', error)
  }
}

/** 点击续费 → 跳转课程购买页 */
function handleRenewal() {
  uni.switchTab({ url: '/pages/courses/courses' })
}

/** 点击课程卡片跳转购买页 */
function handleCourseDetail(_course) {
  uni.switchTab({ url: '/pages/courses/courses' })
}

// ==================== 辅助方法 ====================

/** 从页面参数获取 studentId */
function getQueryParams() {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const query = currentPage?.options || {}

  return {
    studentId: Number(query.studentId || uni.getStorageSync('studentId') || 0),
  }
}

/** 根据交易类型返回图标 */
function getTransactionIcon(item) {
  switch (item.changeType) {
    case 'PURCHASE': return '💰'
    case 'CONSUME': return '📝'
    case 'REFUND': return '↩️'
    case 'ADJUST': return '🔧'
    default: return '📌'
  }
}
</script>

<style lang="scss" scoped>
.balance-page {
  min-height: 100vh;
  background: #F5F5F5;
}

.scroll-container {
  height: 100vh;
  padding-bottom: env(safe-area-inset-bottom);
}

// ==================== 课时总览卡片 ====================
.overview-card {
  position: relative;
  margin: 24rpx;
  padding: 36rpx;
  background: linear-gradient(135deg, #4A90D9 0%, #3A7BC8 50%, #2563A0 100%);
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(74, 144, 217, 0.35);
  color: #fff;

  &__bg {
    position: absolute;
    top: -60rpx;
    right: -40rpx;
    width: 240rpx;
    height: 240rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
  }

  &__main {
    position: relative;
    z-index: 1;
    text-align: center;
    padding-bottom: 28rpx;
    border-bottom: 1rpx solid rgba(255, 255, 255, 0.2);
  }

  &__label {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: 12rpx;
    display: block;
  }

  &__number-row {
    display: flex;
    align-items: baseline;
    justify-content: center;
    margin-bottom: 20rpx;
  }

  &__number {
    font-size: 96rpx;
    font-weight: 800;
    line-height: 1;
    letter-spacing: -4rpx;
  }

  &__unit {
    font-size: 32rpx;
    font-weight: 500;
    margin-left: 12rpx;
    color: rgba(255, 255, 255, 0.9);
  }

  &__progress {
    padding: 0 40rpx;
  }

  &__progress-text {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.7);
    margin-top: 8rpx;
    display: block;
    text-align: center;
  }

  &__stats {
    position: relative;
    z-index: 1;
    display: flex;
    margin-top: 24rpx;
  }
}

.progress-bar {
  width: 100%;
  height: 8rpx;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 4rpx;
  overflow: hidden;

  &__fill {
    height: 100%;
    background: rgba(255, 255, 255, 0.85);
    border-radius: 4rpx;
    transition: width 0.6s ease;
  }
}

.stat-item {
  flex: 1;
  text-align: center;

  &--divider {
    border-left: 1rpx solid rgba(255, 255, 255, 0.2);
    border-right: 1rpx solid rgba(255, 255, 255, 0.2);
  }

  &__value {
    font-size: 36rpx;
    font-weight: 700;
    display: block;
    margin-bottom: 4rpx;

    &--consume { color: #FFD666; }
    &--warning { color: #FFD666; }
    &--danger { color: #FF7875; }
  }

  &__label {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.7);
  }
}

// ==================== 余额不足预警 ====================
.low-balance-alert {
  display: flex;
  align-items: center;
  margin: 0 24rpx 16rpx;
  padding: 20rpx 24rpx;
  background: linear-gradient(135deg, #FFF7E6, #FFFBE6);
  border-radius: 16rpx;
  border: 1rpx solid #FFE58F;

  &__icon {
    font-size: 36rpx;
    margin-right: 20rpx;
  }

  &__content {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: 26rpx;
    font-weight: 600;
    color: #D48806;
  }

  &__desc {
    font-size: 22rpx;
    color: #D48806;
    opacity: 0.8;
    margin-top: 4rpx;
  }

  &__action {
    padding: 10rpx 24rpx;
    background: #FAAD14;
    border-radius: 24rpx;
    font-size: 24rpx;
    color: #fff;
    font-weight: 500;
    white-space: nowrap;
  }
}

// ==================== 区域标题 ====================
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 32rpx 24rpx 16rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
  }

  &__tabs {
    display: flex;
    gap: 8rpx;
  }

  &__tab {
    padding: 8rpx 20rpx;
    font-size: 24rpx;
    color: #999;
    background: #F0F0F0;
    border-radius: 20rpx;
    transition: all 0.2s;

    &--active {
      color: #fff;
      background: #4A90D9;
    }
  }
}

// ==================== 课程卡片 ====================
.course-card {
  display: flex;
  align-items: center;
  margin: 0 24rpx 12rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  &__cover {
    width: 80rpx;
    height: 80rpx;
    border-radius: 12rpx;
    background: #F0F0F0;
    margin-right: 20rpx;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: 28rpx;
    font-weight: 500;
    color: #333;
  }

  &__expiry {
    font-size: 22rpx;
    color: #999;
    margin-top: 4rpx;
  }

  &__balance {
    display: flex;
    align-items: baseline;
    flex-shrink: 0;
  }

  &__remaining {
    font-size: 40rpx;
    font-weight: 700;
    color: #4A90D9;
  }

  &__unit {
    font-size: 24rpx;
    color: #999;
    margin-left: 4rpx;
  }
}

// ==================== 流水列表 ====================
.transaction-list {
  margin: 0 24rpx;
}

.transaction-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  background: #fff;
  margin-bottom: 2rpx;

  &:first-child {
    border-radius: 16rpx 16rpx 0 0;
  }

  &:last-child {
    border-radius: 0 0 16rpx 16rpx;
    margin-bottom: 0;
  }

  &:only-child {
    border-radius: 16rpx;
  }

  &__left {
    display: flex;
    align-items: flex-start;
    flex: 1;
    min-width: 0;
  }

  &__icon {
    width: 64rpx;
    height: 64rpx;
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    margin-right: 20rpx;
    flex-shrink: 0;

    &--PURCHASE { background: #F0FFF4; }
    &--CONSUME { background: #FFF7E6; }
    &--REFUND { background: #FFF1F0; }
    &--ADJUST { background: #F0F5FF; }
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: 26rpx;
    font-weight: 500;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__time {
    font-size: 22rpx;
    color: #999;
    margin-top: 4rpx;
  }

  &__remark {
    font-size: 22rpx;
    color: #BBB;
    margin-top: 2rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    flex-shrink: 0;
    margin-left: 16rpx;
  }

  &__amount {
    font-size: 28rpx;
    font-weight: 600;

    &--plus { color: #52C41A; }
    &--minus { color: #333; }
  }

  &__balance {
    font-size: 22rpx;
    color: #999;
    margin-top: 4rpx;
  }
}

// ==================== 空状态 ====================
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;

  &__icon {
    font-size: 64rpx;
    margin-bottom: 20rpx;
  }

  &__text {
    font-size: 28rpx;
    color: #999;
    margin-bottom: 8rpx;
  }

  &__hint {
    font-size: 24rpx;
    color: #BBB;
  }
}

// ==================== 底部安全距离 ====================
.bottom-safe {
  height: 40rpx;
}
</style>
