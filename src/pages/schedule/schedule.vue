<template>
  <view class="schedule-page">
    <!-- ==================== 月份选择器 ==================== -->
    <view class="month-picker">
      <view class="month-picker__nav" @tap="prevMonth">◀</view>
      <text class="month-picker__label">{{ currentMonth }}</text>
      <view class="month-picker__nav" @tap="nextMonth">▶</view>
      <view class="month-picker__today" @tap="goToday">今</view>
    </view>

    <!-- ==================== 统计摘要条 ==================== -->
    <view class="summary-bar">
      <view class="summary-item">
        <text class="summary-item__num">{{ schedules.length }}</text>
        <text class="summary-item__label">排课数</text>
      </view>
      <view class="summary-item">
        <text class="summary-item__num summary-item__num--success">{{ totalCheckedIn }}</text>
        <text class="summary-item__label">已签到</text>
      </view>
      <view class="summary-item">
        <text class="summary-item__num summary-item__num--warning">{{ totalPending }}</text>
        <text class="summary-item__label">待签到</text>
      </view>
      <view class="summary-item">
        <text class="summary-item__num summary-item__num--danger">{{ totalAbsent }}</text>
        <text class="summary-item__label">缺勤</text>
      </view>
    </view>

    <!-- ==================== 排课列表 ==================== -->
    <scroll-view class="list-scroll" scroll-y @scrolltolower="loadMore">
      <view v-if="schedules.length > 0">
        <view
          v-for="item in schedules"
          :key="item.scheduleId"
          class="schedule-card"
          @tap="handleDetail(item)"
        >
          <!-- 日期标记 -->
          <view class="schedule-card__date">
            <text class="schedule-card__day">{{ fmtDay(item.scheduleDate) }}</text>
            <text class="schedule-card__weekday">{{ fmtWeekday(item.scheduleDate) }}</text>
          </view>

          <!-- 课程信息 -->
          <view class="schedule-card__body">
            <view class="schedule-card__header">
              <text class="schedule-card__name">{{ item.courseName }}</text>
              <view
                class="schedule-card__status"
                :class="'schedule-card__status--' + item.status"
              >
                {{ statusText(item.status) }}
              </view>
            </view>
            <view class="schedule-card__info">
              <text>🕐 {{ item.startTime }}-{{ item.endTime }}</text>
              <text>📍 {{ item.classroom || '未指定教室' }}</text>
              <text>👨‍🏫 {{ item.teacherName }}</text>
            </view>

            <!-- 点名册统计条 -->
            <view class="schedule-card__roster">
              <view class="roster-bar">
                <view
                  class="roster-bar__fill roster-bar__fill--checked"
                  :style="{ width: rosterPercent(item, 'checked') + '%' }"
                ></view>
                <view
                  class="roster-bar__fill roster-bar__fill--pending"
                  :style="{ width: rosterPercent(item, 'pending') + '%' }"
                ></view>
                <view
                  class="roster-bar__fill roster-bar__fill--absent"
                  :style="{ width: rosterPercent(item, 'absent') + '%' }"
                ></view>
              </view>
              <view class="roster-stats">
                <text class="roster-stats__item roster-stats__item--checked">
                  ✓{{ item.checkedInCount }}
                </text>
                <text class="roster-stats__item roster-stats__item--pending">
                  待{{ item.pendingCount }}
                </text>
                <text class="roster-stats__item roster-stats__item--absent">
                  缺{{ item.absentCount }}
                </text>
                <text class="roster-stats__item">共{{ item.totalStudents }}人</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else-if="!loading" class="empty-state">
        <text class="empty-state__icon">📅</text>
        <text class="empty-state__text">本月暂无排课</text>
      </view>

      <view v-if="loading" class="loading-hint">加载中...</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { scheduleApi } from '@/api/schedule'
import dayjs from 'dayjs'

// ==================== 状态 ====================
const currentDate = ref(dayjs())
const schedules = ref([])
const loading = ref(false)

// ==================== 计算属性 ====================
const currentMonth = computed(() => currentDate.value.format('YYYY年M月'))

const totalCheckedIn = computed(() =>
  schedules.value.reduce((s, i) => s + i.checkedInCount, 0))

const totalPending = computed(() =>
  schedules.value.reduce((s, i) => s + i.pendingCount, 0))

const totalAbsent = computed(() =>
  schedules.value.reduce((s, i) => s + i.absentCount, 0))

// ==================== 生命周期 ====================
onMounted(() => loadSchedules())

// ==================== 月份切换 ====================
function prevMonth() { currentDate.value = currentDate.value.subtract(1, 'month'); loadSchedules() }
function nextMonth() { currentDate.value = currentDate.value.add(1, 'month'); loadSchedules() }
function goToday() { currentDate.value = dayjs(); loadSchedules() }

// ==================== 数据加载 ====================
async function loadSchedules() {
  loading.value = true
  try {
    const start = currentDate.value.startOf('month').format('YYYY-MM-DD')
    const end = currentDate.value.endOf('month').format('YYYY-MM-DD')
    schedules.value = await scheduleApi.list({ institutionId: 1, startDate: start, endDate: end })
  } catch { schedules.value = [] }
  finally { loading.value = false }
}

function loadMore() { /* 分页加载 */ }

// ==================== 交互 ====================
function handleDetail(item) {
  // 弹窗展示点名册详情
  uni.showModal({
    title: item.courseName,
    content: `签到 ${item.checkedInCount}/${item.totalStudents}\n待签 ${item.pendingCount}\n缺勤 ${item.absentCount}\n请假 ${item.leaveCount}`,
    confirmText: '查看点名册',
    success: (res) => {
      if (res.confirm) {
        // TODO: 跳转到点名册详情页
        uni.showToast({ title: '点名册功能开发中', icon: 'none' })
      }
    },
  })
}

// ==================== 格式化 ====================
function fmtDay(dateStr) { return dayjs(dateStr).format('DD') }
function fmtWeekday(dateStr) {
  const d = dayjs(dateStr).day()
  return ['日', '一', '二', '三', '四', '五', '六'][d]
}
function statusText(s) {
  const map = { SCHEDULED: '待上课', IN_PROGRESS: '进行中', FINISHED: '已结束', CANCELLED: '已取消' }
  return map[s] || s
}
function rosterPercent(item, type) {
  if (item.totalStudents === 0) return 0
  if (type === 'checked') return (item.checkedInCount / item.totalStudents) * 100
  if (type === 'pending') return (item.pendingCount / item.totalStudents) * 100
  if (type === 'absent') return ((item.absentCount + item.leaveCount) / item.totalStudents) * 100
  return 0
}
</script>

<style lang="scss" scoped>
.schedule-page {
  min-height: 100vh;
  background: #F5F5F5;
}

// ==================== 月份选择器 ====================
.month-picker {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx;
  background: #fff;

  &__nav {
    width: 64rpx; height: 64rpx;
    display: flex; align-items: center; justify-content: center;
    font-size: 28rpx; color: #4A90D9;
  }

  &__label {
    font-size: 32rpx; font-weight: 600; color: #333;
    margin: 0 32rpx; min-width: 180rpx; text-align: center;
  }

  &__today {
    margin-left: 20rpx;
    padding: 8rpx 20rpx;
    background: #4A90D9; color: #fff;
    font-size: 22rpx; border-radius: 20rpx;
  }
}

// ==================== 统计摘要 ====================
.summary-bar {
  display: flex;
  margin: 16rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.summary-item {
  flex: 1; text-align: center;

  &__num {
    display: block;
    font-size: 36rpx; font-weight: 700; color: #333;
    &--success { color: #52C41A; }
    &--warning { color: #FAAD14; }
    &--danger { color: #FF4D4F; }
  }

  &__label {
    font-size: 22rpx; color: #999; margin-top: 4rpx;
  }
}

// ==================== 排课卡片 ====================
.list-scroll { height: calc(100vh - 220rpx); }

.schedule-card {
  display: flex;
  margin: 0 24rpx 16rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);

  &__date {
    width: 100rpx;
    display: flex; flex-direction: column;
    align-items: center; justify-content: center;
    background: #F8FAFC;
  }

  &__day {
    font-size: 36rpx; font-weight: 700; color: #333;
  }

  &__weekday {
    font-size: 22rpx; color: #999; margin-top: 4rpx;
  }

  &__body { flex: 1; padding: 20rpx; min-width: 0; }

  &__header {
    display: flex; justify-content: space-between; align-items: center;
  }

  &__name {
    font-size: 28rpx; font-weight: 600; color: #333;
  }

  &__status {
    font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 12rpx;
    &--SCHEDULED { background: #E8F4FD; color: #4A90D9; }
    &--IN_PROGRESS { background: #F0FFF4; color: #52C41A; }
    &--FINISHED { background: #F5F5F5; color: #999; }
    &--CANCELLED { background: #FFF1F0; color: #FF4D4F; }
  }

  &__info {
    display: flex; gap: 16rpx;
    font-size: 22rpx; color: #999; margin-top: 8rpx; flex-wrap: wrap;
  }

  &__roster { margin-top: 16rpx; }
}

.roster-bar {
  display: flex; height: 10rpx; border-radius: 5rpx; overflow: hidden; background: #F0F0F0;

  &__fill {
    height: 100%;
    &--checked { background: #52C41A; }
    &--pending { background: #E5E5E5; }
    &--absent { background: #FFD666; }
  }
}

.roster-stats {
  display: flex; gap: 20rpx; margin-top: 8rpx; font-size: 22rpx;

  &__item--checked { color: #52C41A; }
  &__item--pending { color: #999; }
  &__item--absent { color: #FAAD14; }
}

// ==================== 通用 ====================
.empty-state {
  display: flex; flex-direction: column; align-items: center;
  padding: 120rpx 0;
  &__icon { font-size: 72rpx; margin-bottom: 20rpx; }
  &__text { font-size: 28rpx; color: #999; }
}

.loading-hint {
  text-align: center; padding: 24rpx; font-size: 24rpx; color: #999;
}
</style>
