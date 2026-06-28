<template>
  <view class="courses-page">
    <!-- ==================== 顶部标题栏 ==================== -->
    <view class="header">
      <text class="header__title">课程购买</text>
      <text class="header__subtitle">选择适合的课程和课时包</text>
    </view>

    <!-- ==================== 课程列表 ==================== -->
    <view v-if="courses.length > 0" class="course-list">
      <view v-for="course in courses" :key="course.courseId" class="course-card">
        <!-- 课程封面 -->
        <image
          v-if="course.coverUrl"
          class="course-card__cover"
          :src="course.coverUrl"
          mode="aspectFill"
        />
        <view v-else class="course-card__cover course-card__cover--placeholder">
          <text class="course-card__cover-icon">📚</text>
        </view>

        <!-- 课程信息 -->
        <view class="course-card__body">
          <view class="course-card__header">
            <text class="course-card__name">{{ course.courseName }}</text>
            <text class="course-card__category" v-if="course.category">
              {{ course.category }}
            </text>
          </view>
          <text class="course-card__desc" v-if="course.description">
            {{ course.description }}
          </text>
          <view class="course-card__meta">
            <text class="course-card__duration">
              每课时 {{ course.defaultDuration || 60 }} 分钟
            </text>
          </view>

          <!-- 课时包列表 -->
          <view class="package-list">
            <view
              v-for="pkg in course.packages"
              :key="pkg.id"
              class="package-item"
              :class="{ 'package-item--selected': selectedPackage?.id === pkg.id }"
              @tap="selectPackage(pkg, course)"
            >
              <view class="package-item__main">
                <view class="package-item__name-row">
                  <text class="package-item__name">{{ pkg.name }}</text>
                  <text
                    v-if="pkg.originalPrice && pkg.originalPrice > pkg.price"
                    class="package-item__tag"
                  >优惠</text>
                </view>
                <text class="package-item__hours">{{ pkg.totalHours }} 课时</text>
                <text class="package-item__validity" v-if="pkg.validDays">
                  有效期 {{ pkg.validDays }} 天
                </text>
              </view>
              <view class="package-item__price-area">
                <view class="package-item__price">
                  <text class="package-item__currency">¥</text>
                  <text class="package-item__amount">{{ fmtPrice(pkg.price) }}</text>
                </view>
                <text
                  v-if="pkg.originalPrice && pkg.originalPrice > pkg.price"
                  class="package-item__original"
                >¥{{ fmtPrice(pkg.originalPrice) }}</text>
                <view class="package-item__select-btn">
                  <text>{{ selectedPackage?.id === pkg.id ? '已选' : '选择' }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 加载中 -->
    <view v-else-if="loading" class="loading-state">
      <text>加载中...</text>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <text class="empty-state__icon">📭</text>
      <text class="empty-state__text">暂无可用课程</text>
    </view>

    <!-- ==================== 底部购买栏 ==================== -->
    <view v-if="selectedPackage" class="bottom-bar">
      <view class="bottom-bar__summary">
        <text class="bottom-bar__label">已选</text>
        <text class="bottom-bar__name">{{ selectedCourse?.courseName }}</text>
        <text class="bottom-bar__pkg">{{ selectedPackage.name }}</text>
        <text class="bottom-bar__price">¥{{ fmtPrice(selectedPackage.price) }}</text>
      </view>
      <button class="bottom-bar__btn" @tap="handleBuy">
        立即购买
      </button>
    </view>

    <!-- ==================== 购买确认弹窗 ==================== -->
    <view v-if="showConfirm" class="modal-mask" @tap="closeConfirm">
      <view class="confirm-modal" @tap.stop>
        <view class="confirm-modal__header">
          <text class="confirm-modal__title">确认订单</text>
          <text class="confirm-modal__close" @tap="closeConfirm">✕</text>
        </view>

        <view class="confirm-modal__body">
          <!-- 订单明细 -->
          <view class="confirm-row">
            <text class="confirm-row__label">课程</text>
            <text class="confirm-row__value">{{ selectedCourse?.courseName }}</text>
          </view>
          <view class="confirm-row">
            <text class="confirm-row__label">课时包</text>
            <text class="confirm-row__value">{{ selectedPackage?.name }}</text>
          </view>
          <view class="confirm-row">
            <text class="confirm-row__label">课时数</text>
            <text class="confirm-row__value confirm-row__value--highlight">
              {{ selectedPackage?.totalHours }} 课时
            </text>
          </view>
          <view class="confirm-row" v-if="selectedPackage?.validDays">
            <text class="confirm-row__label">有效期</text>
            <text class="confirm-row__value">购买后 {{ selectedPackage?.validDays }} 天</text>
          </view>

          <view class="confirm-divider"></view>

          <view class="confirm-row confirm-row--total">
            <text class="confirm-row__label">应付金额</text>
            <text class="confirm-row__price">¥{{ fmtPrice(selectedPackage?.price) }}</text>
          </view>
        </view>

        <view class="confirm-modal__footer">
          <button class="btn btn--cancel" @tap="closeConfirm">取消</button>
          <button
            class="btn btn--pay"
            :disabled="paying"
            @tap="handleConfirmPay"
          >
            {{ paying ? '支付中...' : '微信支付' }}
          </button>
        </view>
      </view>
    </view>

    <!-- ==================== 支付结果弹窗 ==================== -->
    <view v-if="showPayResult" class="modal-mask" @tap="showPayResult = false">
      <view class="result-modal" @tap.stop>
        <view
          class="result-modal__icon"
          :class="paySuccess ? 'result-modal__icon--success' : 'result-modal__icon--fail'"
        >
          {{ paySuccess ? '✅' : '❌' }}
        </view>
        <text class="result-modal__title">
          {{ paySuccess ? '支付成功' : '支付失败' }}
        </text>
        <text class="result-modal__desc" v-if="paySuccess">
          {{ selectedPackage?.totalHours }} 课时已到账，请前往「我的课时」查看
        </text>
        <text class="result-modal__desc" v-else>
          {{ payErrorMsg || '支付未完成，请重试' }}
        </text>
        <button class="btn btn--primary result-modal__btn" @tap="handleAfterPay">
          {{ paySuccess ? '查看我的课时' : '重新选择' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { courseApi } from '@/api/course'
import { orderApi } from '@/api/order'

// ==================== 状态 ====================

const loading = ref(false)
const courses = ref([])
const selectedPackage = ref(null)
const selectedCourse = ref(null)

const showConfirm = ref(false)
const paying = ref(false)

const showPayResult = ref(false)
const paySuccess = ref(false)
const payErrorMsg = ref('')

// ==================== 生命周期 ====================

onMounted(() => {
  loadCourses()
})

// ==================== 数据加载 ====================

async function loadCourses() {
  loading.value = true
  try {
    courses.value = await courseApi.listCourses()
  } catch {
    // 错误已在拦截器中处理
    courses.value = []
  } finally {
    loading.value = false
  }
}

// ==================== 选择课时包 ====================

function selectPackage(pkg, course) {
  selectedPackage.value = pkg
  selectedCourse.value = course
}

// ==================== 购买流程 ====================

function handleBuy() {
  if (!selectedPackage.value) {
    uni.showToast({ title: '请先选择课时包', icon: 'none' })
    return
  }
  showConfirm.value = true
}

function closeConfirm() {
  if (paying.value) return // 支付中不允许关闭
  showConfirm.value = false
}

/**
 * 确认支付 — 完整购买链路
 *
 * 流程：
 * 1. 调用后端 POST /api/orders 创建订单 → 获取 payParams
 * 2. 调用 wx.requestPayment(payParams) 拉起微信支付
 * 3. 支付成功 → 弹窗提示
 * 4. 支付失败 → 弹窗提示 + 支持重试
 */
async function handleConfirmPay() {
  if (paying.value) return
  paying.value = true

  try {
    // ===== 步骤1：获取 OpenID（从本地存储或登录态） =====
    const openid = uni.getStorageSync('openid') || ''
    const studentId = Number(uni.getStorageSync('studentId') || 0)

    if (!openid || !studentId) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      paying.value = false
      return
    }

    // ===== 步骤2：创建订单 + 获取支付参数 =====
    const orderResult = await orderApi.createOrder({
      studentId,
      packageId: selectedPackage.value.id,
      openid,
    })

    // ===== 步骤3：关闭确认弹窗，拉起微信支付 =====
    showConfirm.value = false

    // #ifdef MP-WEIXIN
    wx.requestPayment({
      timeStamp: orderResult.payParams.timeStamp,
      nonceStr: orderResult.payParams.nonceStr,
      package: orderResult.payParams.package,
      signType: orderResult.payParams.signType || 'RSA',
      paySign: orderResult.payParams.paySign,
      success: () => {
        // 支付成功
        paySuccess.value = true
        showPayResult.value = true
      },
      fail: (err) => {
        console.error('支付失败:', err)
        // 用户取消不算错误
        if (err.errMsg && err.errMsg.includes('cancel')) {
          paying.value = false
          return
        }
        paySuccess.value = false
        payErrorMsg.value = '支付未完成，请重试'
        showPayResult.value = true
      },
      complete: () => {
        paying.value = false
      },
    })
    // #endif

    // #ifndef MP-WEIXIN
    // 非微信环境：模拟支付成功（开发调试）
    uni.showToast({ title: '请在微信小程序中完成支付', icon: 'none' })
    paying.value = false
    // #endif

  } catch (error) {
    console.error('下单失败:', error)
    paying.value = false
    showConfirm.value = false
  }
}

// ==================== 支付结果处理 ====================

function handleAfterPay() {
  showPayResult.value = false
  if (paySuccess.value) {
    // 跳转到我的课时页
    uni.switchTab({ url: '/pages/balance/balance' })
  }
  paySuccess.value = false
}

// ==================== 工具方法 ====================

/** 格式化价格（分→元） */
function fmtPrice(cents) {
  if (!cents) return '0.00'
  const yuan = cents / 100
  return yuan % 1 === 0 ? yuan.toFixed(0) : yuan.toFixed(2)
}
</script>

<style lang="scss" scoped>
.courses-page {
  min-height: 100vh;
  background: #F5F5F5;
  padding-bottom: 140rpx;
}

// ==================== 顶部标题栏 ====================
.header {
  padding: 40rpx 32rpx 28rpx;
  background: linear-gradient(180deg, #e8f4fd 0%, #f5f5f5 100%);

  &__title {
    display: block;
    font-size: 40rpx;
    font-weight: 700;
    color: #333;
    margin-bottom: 8rpx;
  }

  &__subtitle {
    font-size: 26rpx;
    color: #999;
  }
}

// ==================== 课程卡片 ====================
.course-card {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);

  &__cover {
    width: 100%;
    height: 280rpx;
    background: linear-gradient(135deg, #e8f4fd, #d4e8fb);

    &--placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    &-icon {
      font-size: 72rpx;
    }
  }

  &__body {
    padding: 24rpx;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12rpx;
  }

  &__name {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
  }

  &__category {
    font-size: 22rpx;
    color: #4A90D9;
    background: rgba(74, 144, 217, 0.1);
    padding: 4rpx 16rpx;
    border-radius: 12rpx;
  }

  &__desc {
    font-size: 24rpx;
    color: #999;
    line-height: 1.6;
    margin-bottom: 12rpx;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__duration {
    font-size: 22rpx;
    color: #BBB;
  }
}

// ==================== 课时包列表 ====================
.package-list {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.package-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  border: 2rpx solid #F0F0F0;
  border-radius: 16rpx;
  transition: all 0.2s;

  &--selected {
    border-color: #4A90D9;
    background: rgba(74, 144, 217, 0.05);
  }

  &__name-row {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  &__name {
    font-size: 28rpx;
    font-weight: 500;
    color: #333;
  }

  &__tag {
    font-size: 20rpx;
    color: #fff;
    background: #FF4D4F;
    padding: 2rpx 10rpx;
    border-radius: 8rpx;
  }

  &__hours {
    font-size: 24rpx;
    color: #666;
    margin-top: 6rpx;
  }

  &__validity {
    font-size: 22rpx;
    color: #BBB;
    margin-top: 4rpx;
  }

  &__price-area {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    flex-shrink: 0;
  }

  &__price {
    color: #FF4D4F;
  }

  &__currency {
    font-size: 24rpx;
    font-weight: 600;
  }

  &__amount {
    font-size: 36rpx;
    font-weight: 700;
  }

  &__original {
    font-size: 22rpx;
    color: #BBB;
    text-decoration: line-through;
  }

  &__select-btn {
    margin-top: 8rpx;
    padding: 6rpx 24rpx;
    border-radius: 24rpx;
    font-size: 22rpx;
    color: #4A90D9;
    border: 1rpx solid #4A90D9;
  }
}

// ==================== 底部购买栏 ====================
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #F0F0F0;
  box-shadow: 0 -2rpx 16rpx rgba(0, 0, 0, 0.04);
  z-index: 100;

  &__summary {
    flex: 1;
    display: flex;
    align-items: center;
    min-width: 0;
    gap: 8rpx;
  }

  &__label {
    font-size: 22rpx;
    color: #999;
    padding: 4rpx 12rpx;
    background: #F5F5F5;
    border-radius: 8rpx;
  }

  &__name {
    font-size: 26rpx;
    color: #333;
    font-weight: 500;
  }

  &__pkg {
    font-size: 24rpx;
    color: #999;
  }

  &__price {
    font-size: 32rpx;
    font-weight: 700;
    color: #FF4D4F;
    margin-left: auto;
    flex-shrink: 0;
  }

  &__btn {
    width: 200rpx;
    height: 72rpx;
    line-height: 72rpx;
    text-align: center;
    background: linear-gradient(135deg, #4A90D9, #5BA0E8);
    color: #fff;
    border-radius: 36rpx;
    font-size: 28rpx;
    font-weight: 600;
    border: none;
    margin-left: 20rpx;

    &::after { border: none; }
  }
}

// ==================== 确认弹窗 ====================
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.confirm-modal {
  width: 600rpx;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 32rpx;
    border-bottom: 1rpx solid #F0F0F0;
  }

  &__title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
  }

  &__close {
    font-size: 36rpx;
    color: #999;
    padding: 8rpx;
  }

  &__body {
    padding: 32rpx;
  }

  &__footer {
    display: flex;
    border-top: 1rpx solid #F0F0F0;
  }
}

.confirm-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;

  &__label {
    font-size: 26rpx;
    color: #999;
  }

  &__value {
    font-size: 26rpx;
    color: #333;

    &--highlight {
      color: #4A90D9;
      font-weight: 600;
    }
  }

  &--total {
    margin-top: 8rpx;
  }

  &__price {
    font-size: 36rpx;
    font-weight: 700;
    color: #FF4D4F;
  }
}

.confirm-divider {
  height: 1rpx;
  background: #F0F0F0;
  margin: 16rpx 0;
}

// ==================== 支付结果弹窗 ====================
.result-modal {
  width: 560rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 36rpx 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  &__icon {
    font-size: 72rpx;
    margin-bottom: 20rpx;
  }

  &__title {
    font-size: 34rpx;
    font-weight: 700;
    color: #333;
    margin-bottom: 16rpx;
  }

  &__desc {
    font-size: 26rpx;
    color: #666;
    text-align: center;
    line-height: 1.6;
    margin-bottom: 32rpx;
  }

  &__btn {
    width: 360rpx;
  }
}

// ==================== 通用按钮 ====================
.btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 500;
  border: none;
  padding: 0;

  &::after { border: none; }

  &--primary {
    background: linear-gradient(135deg, #4A90D9, #5BA0E8);
    color: #fff;
  }

  &--pay {
    background: linear-gradient(135deg, #07C160, #06AD56);
    color: #fff;
  }

  &--cancel {
    background: #F5F5F5;
    color: #666;
  }

  &[disabled] {
    opacity: 0.5;
  }
}

// ==================== 状态 ====================
.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-state {
  &__icon { font-size: 72rpx; margin-bottom: 20rpx; }
  &__text { font-size: 28rpx; color: #999; }
}
</style>
