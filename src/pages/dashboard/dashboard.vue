<template>
  <view class="dashboard-page">
    <scroll-view class="scroll-container" scroll-y refresher-enabled
      :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

      <!-- ==================== 日期选择 ==================== -->
      <view class="header">
        <text class="header__title">数据看板</text>
        <text class="header__subtitle">{{ currentMonth }} 月度统计</text>
      </view>

      <!-- ==================== 核心指标卡片 ==================== -->
      <view class="kpi-grid">
        <view class="kpi-card kpi-card--primary">
          <text class="kpi-card__value">{{ data.attendanceRate }}%</text>
          <text class="kpi-card__label">出勤率</text>
          <text class="kpi-card__detail">{{ data.totalCheckedIn }}/{{ data.totalShouldCheckIn }} 人次</text>
        </view>
        <view class="kpi-card kpi-card--success">
          <text class="kpi-card__value">{{ data.consumptionRate }}%</text>
          <text class="kpi-card__label">课消率</text>
          <text class="kpi-card__detail">{{ data.totalConsumedHours }} / {{ data.totalPurchasedHours }} 课时</text>
        </view>
        <view class="kpi-card kpi-card--warning">
          <text class="kpi-card__value">{{ data.lowBalanceStudentCount }}</text>
          <text class="kpi-card__label">课时不足</text>
          <text class="kpi-card__detail">需续费提醒</text>
        </view>
        <view class="kpi-card kpi-card--danger">
          <text class="kpi-card__value">{{ data.expiringSoonStudentCount }}</text>
          <text class="kpi-card__label">即将到期</text>
          <text class="kpi-card__detail">30天内</text>
        </view>
      </view>

      <!-- ==================== 近7天签到趋势 ==================== -->
      <view class="chart-card">
        <view class="chart-card__header">
          <text class="chart-card__title">近7天签到趋势</text>
          <text class="chart-card__subtitle">人次</text>
        </view>
        <view class="bar-chart">
          <view
            v-for="(val, idx) in data.dailyCheckInCounts"
            :key="idx"
            class="bar-item"
          >
            <view class="bar-item__value">{{ val }}</view>
            <view class="bar-item__bar-wrap">
              <view
                class="bar-item__bar"
                :style="{ height: barHeight(val, data.dailyCheckInCounts) + 'px' }"
              ></view>
            </view>
            <text class="bar-item__label">{{ data.dailyLabels?.[idx] || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- ==================== 近7天课消趋势 ==================== -->
      <view class="chart-card">
        <view class="chart-card__header">
          <text class="chart-card__title">近7天课消趋势</text>
          <text class="chart-card__subtitle">课时</text>
        </view>
        <view class="bar-chart">
          <view
            v-for="(val, idx) in data.dailyConsumedHours"
            :key="idx"
            class="bar-item"
          >
            <view class="bar-item__value bar-item__value--consume">{{ fmtDecimal(val) }}</view>
            <view class="bar-item__bar-wrap">
              <view
                class="bar-item__bar bar-item__bar--consume"
                :style="{ height: barHeightNum(val, data.dailyConsumedHours) + 'px' }"
              ></view>
            </view>
            <text class="bar-item__label">{{ data.dailyLabels?.[idx] || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- ==================== 月度概览 ==================== -->
      <view class="overview-card">
        <view class="overview-card__header">
          <text class="overview-card__title">月度概览</text>
        </view>
        <view class="overview-grid">
          <view class="overview-item">
            <text class="overview-item__num">{{ data.totalSchedules }}</text>
            <text class="overview-item__label">排课总数</text>
          </view>
          <view class="overview-item">
            <text class="overview-item__num">{{ data.totalConsumedHours }}</text>
            <text class="overview-item__label">消耗课时</text>
          </view>
          <view class="overview-item">
            <text class="overview-item__num">{{ data.leaveCount }}</text>
            <text class="overview-item__label">请假人次</text>
          </view>
          <view class="overview-item">
            <text class="overview-item__num">{{ data.absentCount }}</text>
            <text class="overview-item__label">缺勤人次</text>
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { dashboardApi } from '@/api/dashboard'
import dayjs from 'dayjs'

// ==================== 状态 ====================
const refreshing = ref(false)
const currentMonth = ref(dayjs().format('YYYY年M月'))

const data = reactive({
  attendanceRate: 0,
  totalCheckedIn: 0,
  totalShouldCheckIn: 0,
  consumptionRate: 0,
  totalConsumedHours: 0,
  totalPurchasedHours: 0,
  lowBalanceStudentCount: 0,
  expiringSoonStudentCount: 0,
  totalSchedules: 0,
  leaveCount: 0,
  absentCount: 0,
  dailyCheckInCounts: [],
  dailyConsumedHours: [],
  dailyLabels: [],
})

// ==================== 生命周期 ====================
onMounted(() => loadDashboard())

// ==================== 数据加载 ====================
async function loadDashboard() {
  try {
    const result = await dashboardApi.getDashboard()
    Object.assign(data, result)
  } catch { /* 错误已在拦截器处理 */ }
}

async function onRefresh() {
  refreshing.value = true
  await loadDashboard()
  refreshing.value = false
}

// ==================== 图表辅助 ====================
function barHeight(val, arr) {
  const max = Math.max(...(arr || []), 1)
  return Math.max((val / max) * 140, 4)
}

function barHeightNum(val, arr) {
  if (!arr || arr.length === 0) return 4
  const nums = arr.map(v => Number(v) || 0)
  const max = Math.max(...nums, 1)
  return Math.max((Number(val || 0) / max) * 140, 4)
}

function fmtDecimal(v) {
  if (v === null || v === undefined) return '0'
  return Number(v).toFixed(1)
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  min-height: 100vh;
  background: #F5F5F5;
}

.scroll-container {
  height: 100vh;
  padding-bottom: env(safe-area-inset-bottom);
}

// ==================== 标题 ====================
.header {
  padding: 36rpx 32rpx 20rpx;
  background: linear-gradient(180deg, #e8f4fd, #f5f5f5);

  &__title {
    display: block;
    font-size: 40rpx; font-weight: 700; color: #333;
    margin-bottom: 8rpx;
  }

  &__subtitle {
    font-size: 24rpx; color: #999;
  }
}

// ==================== KPI 卡片 ====================
.kpi-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin: 0 24rpx 24rpx;
}

.kpi-card {
  padding: 28rpx;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);

  &__value {
    display: block;
    font-size: 48rpx; font-weight: 800;
  }

  &__label {
    display: block;
    font-size: 24rpx; color: #666;
    margin-top: 4rpx;
  }

  &__detail {
    display: block;
    font-size: 20rpx; color: #BBB;
    margin-top: 8rpx;
  }

  &--primary .kpi-card__value { color: #4A90D9; }
  &--success .kpi-card__value { color: #52C41A; }
  &--warning .kpi-card__value { color: #FAAD14; }
  &--danger .kpi-card__value { color: #FF4D4F; }
}

// ==================== 图表卡片 ====================
.chart-card {
  margin: 0 24rpx 24rpx;
  padding: 28rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);

  &__header {
    display: flex; justify-content: space-between; align-items: baseline;
    margin-bottom: 24rpx;
  }

  &__title {
    font-size: 28rpx; font-weight: 600; color: #333;
  }

  &__subtitle {
    font-size: 22rpx; color: #999;
  }
}

.bar-chart {
  display: flex; justify-content: space-around; align-items: flex-end;
  height: 200rpx;
}

.bar-item {
  display: flex; flex-direction: column; align-items: center;
  flex: 1;

  &__value {
    font-size: 20rpx; color: #4A90D9; font-weight: 600;
    margin-bottom: 6rpx;
    &--consume { color: #FF4D4F; }
  }

  &__bar-wrap {
    width: 40rpx; height: 140rpx;
    display: flex; flex-direction: column; justify-content: flex-end;
    background: #F8FAFC; border-radius: 8rpx; overflow: hidden;
  }

  &__bar {
    width: 100%;
    background: linear-gradient(180deg, #4A90D9, #8BB8E8);
    border-radius: 8rpx 8rpx 0 0;
    transition: height 0.6s ease;
    min-height: 4rpx;

    &--consume {
      background: linear-gradient(180deg, #FF7875, #FFA39E);
    }
  }

  &__label {
    font-size: 18rpx; color: #BBB; margin-top: 8rpx;
  }
}

// ==================== 月度概览 ====================
.overview-card {
  margin: 0 24rpx 24rpx;
  padding: 28rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);

  &__title {
    font-size: 28rpx; font-weight: 600; color: #333;
    margin-bottom: 20rpx;
  }
}

.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.overview-item {
  text-align: center;
  padding: 16rpx;

  &__num {
    display: block;
    font-size: 36rpx; font-weight: 700; color: #333;
  }

  &__label {
    font-size: 22rpx; color: #999; margin-top: 4rpx;
  }
}

.bottom-safe { height: 40rpx; }
</style>
