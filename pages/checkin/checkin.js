const api = require('../../utils/request')

Page({
  onLoad() {
    var role = wx.getStorageSync('role') || 'STUDENT'
    if (role !== 'STUDENT') {
      wx.switchTab({ url: '/pages/balance/balance' })
    }
  },

  data: {
    isCheckingIn: false,
    showResult: false,
    checkInResult: {
      checkInSuccess: false,
      checkInTime: '',
      riskLevel: 'SAFE',
      riskDescription: '',
      lowBalanceWarning: false,
      consumedHours: 1,
      remainingHours: 0,
      consumptionSuccess: false,
      consumptionFailReason: '',
      displayMessage: ''
    }
  },

  /**
   * 点击扫码签到 — 完整流程
   */
  async handleScanCode() {
    if (this.data.isCheckingIn) return
    this.setData({ isCheckingIn: true })

    try {
      // 步骤1：获取位置
      const location = await this.getLocation()
      if (!location) { this.setData({ isCheckingIn: false }); return }

      // 步骤2：获取设备信息
      const deviceInfo = this.getDeviceInfo()

      // 步骤3：扫码
      const scanResult = await this.scanCode()
      if (!scanResult) { this.setData({ isCheckingIn: false }); return }

      // 步骤4：解析二维码 {scheduleId, token}
      let scheduleId, qrToken
      try {
        const qr = JSON.parse(scanResult)
        scheduleId = qr.scheduleId
        qrToken = qr.token
      } catch (e) {
        const parts = scanResult.split(',')
        if (parts.length >= 2) { scheduleId = Number(parts[0]); qrToken = parts[1] }
      }

      if (!scheduleId || !qrToken) {
        wx.showToast({ title: '二维码格式错误', icon: 'none' })
        this.setData({ isCheckingIn: false })
        return
      }

      // 步骤5：调用后端签到接口
      const result = await api.post('/api/checkin/scan', {
        scheduleId,
        studentId: wx.getStorageSync('studentId') || 1,
        qrToken,
        latitude: location.latitude,
        longitude: location.longitude,
        deviceBrand: deviceInfo.brand,
        deviceModel: deviceInfo.model,
        systemVersion: deviceInfo.system,
        wxVersion: deviceInfo.wxVersion
      })

      // 步骤6：展示结果
      this.setData({
        checkInResult: result,
        showResult: true
      })

    } catch (err) {
      console.error('签到失败:', err)
    } finally {
      this.setData({ isCheckingIn: false })
    }
  },

  /** 获取定位 */
  getLocation() {
    return new Promise((resolve) => {
      wx.getLocation({
        type: 'gcj02',
        isHighAccuracy: true,
        timeout: 10000,
        success: (res) => resolve({ latitude: res.latitude, longitude: res.longitude }),
        fail: (err) => {
          if (err.errMsg && err.errMsg.includes('auth deny')) {
            wx.showModal({
              title: '需要位置权限',
              content: '签到需要获取位置信息，请在设置中开启。',
              confirmText: '去设置',
              success: (m) => { if (m.confirm) wx.openSetting() }
            })
          } else {
            wx.showToast({ title: '定位失败，请重试', icon: 'none' })
          }
          resolve(null)
        }
      })
    })
  },

  /** 扫码 */
  scanCode() {
    return new Promise((resolve) => {
      wx.scanCode({
        onlyFromCamera: true,
        scanType: ['qrCode'],
        success: (res) => resolve(res.result),
        fail: (err) => {
          if (!(err.errMsg && err.errMsg.includes('cancel'))) {
            wx.showToast({ title: '扫码失败', icon: 'none' })
          }
          resolve(null)
        }
      })
    })
  },

  /** 获取设备信息 */
  getDeviceInfo() {
    try {
      const d = wx.getDeviceInfo()
      const a = wx.getAppBaseInfo()
      return { brand: d.brand || '', model: d.model || '', system: d.system || '', wxVersion: a.SDKVersion || '' }
    } catch (e) {
      return { brand: '', model: '', system: '', wxVersion: '' }
    }
  },

  /** 关闭结果弹窗 */
  closeResult() {
    this.setData({ showResult: false })
  }
})
