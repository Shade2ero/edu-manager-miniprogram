Page({
  data: {
    inputOpenid: ''
  },

  onLoad() {
    // 如果已经选过角色，直接跳到对应页面
    var role = wx.getStorageSync('role')
    if (role) {
      this.goToMain(role)
    }
  },

  /** 快捷选择 */
  selectRole(e) {
    var openid = e.currentTarget.dataset.openid
    wx.setStorageSync('openid', openid)
    this.doLogin()
  },

  /** 手动输入 openid */
  onInput(e) {
    this.setData({ inputOpenid: e.detail.value })
  },

  customLogin() {
    if (!this.data.inputOpenid) return
    wx.setStorageSync('openid', this.data.inputOpenid.trim())
    this.doLogin()
  },

  doLogin() {
    var app = getApp()
    var openid = wx.getStorageSync('openid') || 'dev_openid_001'
    var role = app.detectRole(openid)
    app.applyRole(role)
    this.goToMain(role)
  },

  goToMain(role) {
    if (role === 'STUDENT') {
      wx.switchTab({ url: '/pages/checkin/checkin' })
    } else {
      wx.switchTab({ url: '/pages/balance/balance' })
    }
  }
})
