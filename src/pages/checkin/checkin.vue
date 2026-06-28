<template>
  <view class="checkin-page">
    <!-- ==================== 顶部状态卡片 ==================== -->
    <view class="status-card">
      <view class="status-card__inner">
        <view class="status-card__icon">
          <text class="iconfont icon-qrcode">📱</text>
        </view>
        <view class="status-card__text">
          <text class="status-card__title">扫码签到</text>
          <text class="status-card__desc">扫描教师端二维码完成签到，系统将自动扣除对应课时</text>
        </view>
      </view>
      <!-- 当前课程提示条 -->
      <view v-if="currentSchedule" class="current-class-bar">
        <text class="current-class-bar__dot">●</text>
        <text class="current-class-bar__text">
          当前课程：{{ currentSchedule.courseName }}（{{ currentSchedule.startTime }}-{{ currentSchedule.endTime }}）
        </text>
      </view>
    </view>

    <!-- ==================== 签到按钮区域 ==================== -->
    <view class="scan-area">
      <!-- 主动扫码按钮（推荐方式） -->
      <view class="scan-btn-wrapper">
        <view
          class="scan-btn"
          :class="{ 'scan-btn--loading': isCheckingIn }"
          @tap="handleScanCode"
        >
          <view class="scan-btn__ripple" v-if="!isCheckingIn"></view>
          <text class="scan-btn__icon">📷</text>
          <text class="scan-btn__text">
            {{ isCheckingIn ? '签到处理中...' : '点击扫码签到' }}
          </text>
          <text class="scan-btn__hint" v-if="!isCheckingIn">
            请对准教师端展示的二维码
          </text>
        </view>
      </view>

      <!-- 备选：手动输入签到码（网络异常时兜底） -->
      <view class="manual-area">
        <text class="manual-area__label">无法扫码？</text>
        <text class="manual-area__link" @tap="showManualInput = true">
          手动输入签到码
        </text>
      </view>
    </view>

    <!-- ==================== 签到说明卡片 ==================== -->
    <view class="tips-card">
      <view class="tips-card__title">签到须知</view>
      <view class="tips-card__list">
        <view class="tips-card__item">
          <text class="tips-card__dot">·</text>
          <text>请在上课前30分钟至下课后30分钟内完成签到</text>
        </view>
        <view class="tips-card__item">
          <text class="tips-card__dot">·</text>
          <text>需开启定位权限，在机构范围内方可签到成功</text>
        </view>
        <view class="tips-card__item">
          <text class="tips-card__dot">·</text>
          <text>签到成功后系统将自动扣除1课时</text>
        </view>
        <view class="tips-card__item">
          <text class="tips-card__dot">·</text>
          <text>距离签到仅限本人使用，请勿替他人签到</text>
        </view>
      </view>
    </view>

    <!-- ==================== 手动输入签到码弹窗 ==================== -->
    <view v-if="showManualInput" class="modal-mask" @tap="showManualInput = false">
      <view class="manual-modal" @tap.stop>
        <view class="manual-modal__header">
          <text class="manual-modal__title">手动输入签到码</text>
          <text class="manual-modal__close" @tap="showManualInput = false">✕</text>
        </view>
        <view class="manual-modal__body">
          <input
            class="manual-modal__input"
            v-model="manualCode"
            placeholder="请输入教师提供的 6 位签到码"
            maxlength="6"
            type="number"
            focus
          />
          <view class="manual-modal__hint">
            签到码由教师在课程开始时生成，请向教师索取
          </view>
        </view>
        <view class="manual-modal__footer">
          <button class="btn btn--cancel" @tap="showManualInput = false">取消</button>
          <button
            class="btn btn--primary"
            :disabled="manualCode.length < 6"
            @tap="handleManualCheckIn"
          >
            确认签到
          </button>
        </view>
      </view>
    </view>

    <!-- ==================== 签到结果弹窗 ==================== -->
    <view v-if="showResult" class="modal-mask" @tap="closeResult">
      <view class="result-modal" @tap.stop>
        <!-- 成功状态 -->
        <template v-if="checkInResult.checkInSuccess">
          <view class="result-modal__icon result-modal__icon--success">✅</view>
          <view class="result-modal__title result-modal__title--success">
            {{ checkInResult.riskLevel === 'HIGH_RISK' ? '等待教师确认' : '签到成功' }}
          </view>
          <view class="result-modal__time">{{ checkInResult.checkInTime }}</view>

          <!-- 风控提示（SUSPICIOUS / WARNING 级别时展示） -->
          <view
            v-if="checkInResult.riskLevel === 'SUSPICIOUS' || checkInResult.riskLevel === 'WARNING'"
            class="result-modal__risk-warning"
          >
            <text>⚠️ {{ checkInResult.riskDescription || '签到位置略有偏差，已记录' }}</text>
          </view>

          <!-- 课消详情 -->
          <view class="result-modal__detail" v-if="checkInResult.consumptionTriggered">
            <view class="detail-row">
              <text class="detail-row__label">本次消耗</text>
              <text class="detail-row__value detail-row__value--consume">
                -{{ checkInResult.consumedHours || 1 }} 课时
              </text>
            </view>
            <view class="detail-row" v-if="checkInResult.remainingHours !== null && checkInResult.remainingHours !== undefined">
              <text class="detail-row__label">剩余课时</text>
              <text
                class="detail-row__value"
                :class="{
                  'detail-row__value--low': checkInResult.lowBalanceWarning,
                  'detail-row__value--normal': !checkInResult.lowBalanceWarning
                }"
              >
                {{ checkInResult.remainingHours }} 课时
              </text>
            </view>
            <!-- 课消失败提示 -->
            <view v-if="!checkInResult.consumptionSuccess" class="result-modal__consume-fail">
              <text>⚠️ 课时扣减失败：{{ checkInResult.consumptionFailReason }}</text>
            </view>
            <!-- 余额不足预警 -->
            <view v-if="checkInResult.lowBalanceWarning" class="result-modal__balance-warning">
              <text>⚠️ 课时即将用完，请及时续费</text>
            </view>
          </view>

          <!-- 高危状态额外提示 -->
          <view v-if="checkInResult.riskLevel === 'HIGH_RISK'" class="result-modal__high-risk-hint">
            <text>您的签到存在安全风险，需教师确认后生效</text>
          </view>
        </template>

        <!-- 失败状态 -->
        <template v-else>
          <view class="result-modal__icon result-modal__icon--fail">❌</view>
          <view class="result-modal__title result-modal__title--fail">签到失败</view>
          <view class="result-modal__fail-reason">{{ checkInResult.displayMessage || '未知错误' }}</view>
          <view class="result-modal__retry-hint">请确认信息后重新扫码尝试</view>
        </template>

        <button class="btn btn--primary result-modal__btn" @tap="closeResult">
          {{ checkInResult.checkInSuccess ? '完成' : '知道了' }}
        </button>
      </view>
    </view>

    <!-- ==================== 权限提示弹窗 ==================== -->
    <view v-if="showPermissionTip" class="modal-mask" @tap="showPermissionTip = false">
      <view class="permission-modal" @tap.stop>
        <view class="permission-modal__icon">📍</view>
        <view class="permission-modal__title">需要位置权限</view>
        <view class="permission-modal__body">
          <text>签到需要获取您的位置信息，用于验证您是否在机构范围内。请前往「设置」开启定位权限。</text>
        </view>
        <view class="permission-modal__footer">
          <button class="btn btn--cancel" @tap="showPermissionTip = false">暂不开启</button>
          <button class="btn btn--primary" @tap="openSetting">前往设置</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { checkinApi } from '@/api/checkin'

// ==================== 响应式状态 ====================

/** 是否正在签到处理中（防重复点击） */
const isCheckingIn = ref(false)

/** 当前课程信息（可从首页传递或接口获取） */
const currentSchedule = ref(null)

/** 是否显示手动输入弹窗 */
const showManualInput = ref(false)

/** 手动输入的签到码 */
const manualCode = ref('')

/** 是否显示签到结果弹窗 */
const showResult = ref(false)

/** 签到结果数据 */
const checkInResult = reactive({
  checkInSuccess: false,
  checkInTime: '',
  riskLevel: 'SAFE',
  riskDescription: '',
  consumptionTriggered: false,
  consumptionSuccess: false,
  consumedHours: 0,
  remainingHours: 0,
  lowBalanceWarning: false,
  consumptionFailReason: '',
  displayMessage: '',
})

/** 是否显示权限提示弹窗 */
const showPermissionTip = ref(false)

// ==================== 生命周期 ====================

onMounted(() => {
  // 尝试从页面参数中获取当前课程信息
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  if (currentPage && currentPage.options) {
    const { scheduleId, courseName, startTime, endTime } = currentPage.options
    if (scheduleId) {
      currentSchedule.value = {
        scheduleId: Number(scheduleId),
        courseName: courseName || '未指定课程',
        startTime: startTime || '--:--',
        endTime: endTime || '--:--',
      }
    }
  }
})

// ==================== 核心方法：扫码签到 ====================

/**
 * 处理扫码签到 — 完整流程
 *
 * 流程：
 * 1. 获取位置授权 + 获取 GPS 坐标
 * 2. 调用 wx.scanCode 扫描二维码
 * 3. 获取设备信息（品牌、型号、系统、微信版本）
 * 4. 调用后端签到接口
 * 5. 展示结果弹窗
 */
async function handleScanCode() {
  // 防重复点击
  if (isCheckingIn.value) return
  isCheckingIn.value = true

  try {
    // ===== 步骤1：获取位置信息 =====
    const location = await getLocationWithPermission()
    if (!location) {
      isCheckingIn.value = false
      return // 权限被拒绝，已在 getLocationWithPermission 中处理
    }

    // ===== 步骤2：扫码 =====
    const scanResult = await scanQRCode()
    if (!scanResult) {
      isCheckingIn.value = false
      return // 扫码取消或失败
    }

    // ===== 步骤3：解析二维码内容 =====
    // 二维码格式约定：{ "scheduleId": 123, "token": "uuid-string" }
    let scheduleId, qrToken
    try {
      const qrData = JSON.parse(scanResult)
      scheduleId = qrData.scheduleId
      qrToken = qrData.token
    } catch {
      // 兼容纯文本格式：scheduleId,token
      const parts = scanResult.split(',')
      if (parts.length >= 2) {
        scheduleId = Number(parts[0])
        qrToken = parts[1]
      }
    }

    if (!scheduleId || !qrToken) {
      uni.showToast({ title: '二维码内容格式错误，请扫描正确的签到码', icon: 'none' })
      isCheckingIn.value = false
      return
    }

    // ===== 步骤4：获取设备信息 =====
    const deviceInfo = getDeviceInfo()

    // ===== 步骤5：组装请求参数 =====
    const params = {
      scheduleId,
      studentId: getStudentId(),      // 从登录态或本地存储获取
      qrToken,
      latitude: location.latitude,
      longitude: location.longitude,
      deviceBrand: deviceInfo.brand,
      deviceModel: deviceInfo.model,
      systemVersion: deviceInfo.system,
      wxVersion: deviceInfo.wxVersion,
    }

    // ===== 步骤6：调用后端签到接口 =====
    const result = await checkinApi.scanCheckIn(params)

    // ===== 步骤7：展示结果 =====
    Object.assign(checkInResult, result)
    showResult.value = true

  } catch (error) {
    // 网络异常或业务异常已在 request.js 拦截器中 toast 提示
    console.error('签到流程异常:', error)
  } finally {
    isCheckingIn.value = false
  }
}

// ==================== 手动输入签到码 ====================

async function handleManualCheckIn() {
  if (manualCode.value.length < 6) return

  isCheckingIn.value = true
  showManualInput.value = false

  try {
    const location = await getLocationWithPermission()
    if (!location) {
      isCheckingIn.value = false
      return
    }

    const deviceInfo = getDeviceInfo()

    const params = {
      scheduleId: 0, // 手动签到需由后端根据签到码反查排课
      studentId: getStudentId(),
      qrToken: manualCode.value,
      latitude: location.latitude,
      longitude: location.longitude,
      deviceBrand: deviceInfo.brand,
      deviceModel: deviceInfo.model,
      systemVersion: deviceInfo.system,
      wxVersion: deviceInfo.wxVersion,
    }

    const result = await checkinApi.scanCheckIn(params)
    Object.assign(checkInResult, result)
    showResult.value = true
    manualCode.value = ''
  } catch (error) {
    console.error('手动签到异常:', error)
  } finally {
    isCheckingIn.value = false
  }
}

// ==================== 辅助方法 ====================

/**
 * 获取定位信息（带权限处理）
 * @returns {{ latitude: number, longitude: number } | null}
 */
function getLocationWithPermission() {
  return new Promise((resolve) => {
    uni.getLocation({
      type: 'gcj02',        // 国测局坐标（微信默认）
      isHighAccuracy: true, // 开启高精度模式
      timeout: 10000,       // 10秒超时
      success: (res) => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude,
        })
      },
      fail: (err) => {
        console.error('获取位置失败:', err)
        if (err.errMsg && err.errMsg.includes('auth deny')) {
          // 用户拒绝授权 → 展示引导弹窗
          showPermissionTip.value = true
        } else if (err.errMsg && err.errMsg.includes('auth denied')) {
          // 权限已被永久拒绝 → 引导去设置页
          uni.showModal({
            title: '位置权限未开启',
            content: '签到需要获取您的位置信息，请在系统设置中允许小程序访问位置。',
            confirmText: '去设置',
            success: (modalRes) => {
              if (modalRes.confirm) {
                uni.openSetting()
              }
            },
          })
        } else {
          // 其他定位失败（室内信号差等）
          uni.showToast({ title: '定位失败，请尝试在开阔地带签到', icon: 'none' })
        }
        resolve(null)
      },
    })
  })
}

/**
 * 调用微信扫码
 * @returns {string | null} 扫码结果字符串
 */
function scanQRCode() {
  return new Promise((resolve) => {
    uni.scanCode({
      onlyFromCamera: true,  // 只允许从相机扫码（不允许从相册选图）
      scanType: ['qrCode'],  // 仅识别二维码
      success: (res) => {
        console.log('扫码结果:', res.result)
        resolve(res.result)
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.includes('cancel')) {
          // 用户主动取消，不提示错误
          resolve(null)
        } else {
          uni.showToast({ title: '扫码失败，请重试', icon: 'none' })
          resolve(null)
        }
      },
    })
  })
}

/**
 * 获取设备信息
 *
 * 微信小程序提供的设备信息接口：
 * - wx.getDeviceInfo() → brand, model, system, platform
 * - wx.getAppBaseInfo() → SDKVersion（微信版本）
 */
function getDeviceInfo() {
  let brand = '', model = '', system = '', wxVersion = ''

  // #ifdef MP-WEIXIN
  try {
    const deviceRes = wx.getDeviceInfo()
    brand = deviceRes.brand || ''
    model = deviceRes.model || ''
    system = deviceRes.system || ''

    const appRes = wx.getAppBaseInfo()
    wxVersion = appRes.SDKVersion || ''
  } catch (e) {
    console.warn('获取设备信息失败:', e)
  }
  // #endif

  return { brand, model, system, wxVersion }
}

/**
 * 获取当前学员ID
 * TODO: 从登录态/全局 store 获取
 */
function getStudentId() {
  // 开发阶段从本地存储读取，生产环境应通过登录接口获取
  return Number(uni.getStorageSync('studentId') || 0)
}

/**
 * 关闭结果弹窗
 */
function closeResult() {
  showResult.value = false
}

/**
 * 打开系统设置页
 */
function openSetting() {
  showPermissionTip.value = false
  uni.openSetting()
}
</script>

<style lang="scss" scoped>
.checkin-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e8f4fd 0%, #f5f5f5 40%);
  padding-bottom: env(safe-area-inset-bottom);
}

// ==================== 顶部状态卡片 ====================
.status-card {
  margin: 24rpx;
  padding: 32rpx;
  background: linear-gradient(135deg, #4A90D9, #5BA0E8);
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(74, 144, 217, 0.3);

  &__inner {
    display: flex;
    align-items: center;
  }

  &__icon {
    width: 80rpx;
    height: 80rpx;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 20rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    margin-right: 24rpx;
  }

  &__text {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: 36rpx;
    font-weight: 700;
    color: #fff;
    margin-bottom: 8rpx;
  }

  &__desc {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.8);
    line-height: 1.5;
  }
}

.current-class-bar {
  margin-top: 20rpx;
  padding: 16rpx 20rpx;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12rpx;
  display: flex;
  align-items: center;

  &__dot {
    color: #52C41A;
    font-size: 24rpx;
    margin-right: 12rpx;
    animation: pulse 2s infinite;
  }

  &__text {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.95);
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

// ==================== 签到按钮区域 ====================
.scan-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0;
}

.scan-btn-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
}

.scan-btn {
  position: relative;
  width: 320rpx;
  height: 320rpx;
  background: #fff;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;

  &::after {
    content: '';
    position: absolute;
    top: -4rpx;
    left: -4rpx;
    right: -4rpx;
    bottom: -4rpx;
    border-radius: 50%;
    border: 6rpx dashed #4A90D9;
    animation: rotate-border 20s linear infinite;
  }

  &--loading {
    opacity: 0.7;
    transform: scale(0.95);
  }

  &__ripple {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: rgba(74, 144, 217, 0.05);
    animation: ripple 2s ease-out infinite;
  }

  &__icon {
    font-size: 72rpx;
    margin-bottom: 16rpx;
    position: relative;
    z-index: 1;
  }

  &__text {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    position: relative;
    z-index: 1;
  }

  &__hint {
    font-size: 22rpx;
    color: #999;
    margin-top: 8rpx;
    position: relative;
    z-index: 1;
  }
}

@keyframes rotate-border {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes ripple {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(1.4); opacity: 0; }
}

.manual-area {
  margin-top: 40rpx;
  text-align: center;

  &__label {
    font-size: 26rpx;
    color: #999;
  }

  &__link {
    font-size: 26rpx;
    color: #4A90D9;
    margin-left: 8rpx;
    text-decoration: underline;
  }
}

// ==================== 签到须知 ====================
.tips-card {
  margin: 0 24rpx 40rpx;
  padding: 28rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  &__title {
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 16rpx;
  }

  &__item {
    font-size: 24rpx;
    color: #666;
    line-height: 2;
    display: flex;
  }

  &__dot {
    color: #4A90D9;
    margin-right: 12rpx;
    font-weight: 700;
  }
}

// ==================== 通用弹窗遮罩 ====================
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

// ==================== 签到结果弹窗 ====================
.result-modal {
  width: 600rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 36rpx 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: modal-pop-in 0.3s ease;

  &__icon {
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  &__title {
    font-size: 34rpx;
    font-weight: 700;
    margin-bottom: 12rpx;

    &--success { color: #333; }
    &--fail { color: #FF4D4F; }
  }

  &__time {
    font-size: 24rpx;
    color: #999;
    margin-bottom: 24rpx;
  }

  &__fail-reason {
    font-size: 28rpx;
    color: #666;
    text-align: center;
    margin-bottom: 16rpx;
    line-height: 1.6;
  }

  &__retry-hint {
    font-size: 24rpx;
    color: #999;
    margin-bottom: 24rpx;
  }

  &__risk-warning {
    width: 100%;
    padding: 16rpx 20rpx;
    background: #FFF7E6;
    border-radius: 12rpx;
    font-size: 24rpx;
    color: #FAAD14;
    text-align: center;
    margin-bottom: 20rpx;
  }

  &__detail {
    width: 100%;
    padding: 24rpx;
    background: #F8FAFC;
    border-radius: 16rpx;
    margin-bottom: 24rpx;
  }

  &__consume-fail {
    margin-top: 16rpx;
    padding: 12rpx 16rpx;
    background: #FFF1F0;
    border-radius: 8rpx;
    font-size: 24rpx;
    color: #FF4D4F;
  }

  &__balance-warning {
    margin-top: 16rpx;
    padding: 12rpx 16rpx;
    background: #FFF7E6;
    border-radius: 8rpx;
    font-size: 24rpx;
    color: #FAAD14;
  }

  &__high-risk-hint {
    width: 100%;
    padding: 16rpx 20rpx;
    background: #FFF1F0;
    border-radius: 12rpx;
    font-size: 24rpx;
    color: #FF4D4F;
    text-align: center;
    margin-bottom: 20rpx;
  }

  &__btn {
    width: 320rpx;
    margin-top: 8rpx;
  }
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8rpx 0;

  &__label {
    font-size: 26rpx;
    color: #999;
  }

  &__value {
    font-size: 30rpx;
    font-weight: 600;

    &--consume { color: #FF4D4F; }
    &--normal { color: #52C41A; }
    &--low { color: #FAAD14; }
  }
}

@keyframes modal-pop-in {
  from { transform: scale(0.8); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

// ==================== 手动输入弹窗 ====================
.manual-modal {
  width: 560rpx;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 32rpx 32rpx 0;
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

  &__input {
    width: 100%;
    height: 80rpx;
    border: 2rpx solid #E5E5E5;
    border-radius: 12rpx;
    padding: 0 24rpx;
    font-size: 32rpx;
    letter-spacing: 12rpx;
    text-align: center;
    box-sizing: border-box;

    &:focus {
      border-color: #4A90D9;
    }
  }

  &__hint {
    font-size: 24rpx;
    color: #999;
    margin-top: 16rpx;
    text-align: center;
  }

  &__footer {
    display: flex;
    border-top: 1rpx solid #F0F0F0;
  }
}

// ==================== 权限提示弹窗 ====================
.permission-modal {
  width: 560rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 36rpx 36rpx;
  text-align: center;

  &__icon {
    font-size: 72rpx;
    margin-bottom: 20rpx;
  }

  &__title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 20rpx;
  }

  &__body {
    font-size: 26rpx;
    color: #666;
    line-height: 1.8;
    margin-bottom: 32rpx;
  }

  &__footer {
    display: flex;
    gap: 20rpx;
  }
}

// ==================== 通用按钮 ====================
.btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 12rpx;
  font-size: 28rpx;
  font-weight: 500;
  border: none;
  padding: 0;

  &::after { border: none; }

  &--primary {
    background: linear-gradient(135deg, #4A90D9, #5BA0E8);
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
</style>
