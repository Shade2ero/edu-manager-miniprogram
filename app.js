App({
  onLaunch() {
    // 根据 openid 立即判定角色（同步，不等后端）
    var openid = wx.getStorageSync('openid') || 'dev_openid_001'
    var role = this.detectRole(openid)
    wx.setStorageSync('role', role)
    this.applyRole(role)

    // 异步登录（刷新后端信息，不影响 TabBar）
    this.login(openid)
  },

  /** 根据 openid 推断角色 */
  detectRole: function (openid) {
    if (openid === 'dev_teacher_001') return 'TEACHER'
    if (openid === 'dev_admin_001') return 'ADMIN'
    return 'STUDENT'
  },

  /** 根据角色设置 TabBar */
  applyRole: function (role) {
    var tabs
    if (role === 'ADMIN')      tabs = ['courses', 'balance', 'schedule']
    else if (role === 'TEACHER') tabs = ['balance', 'schedule']
    else                        tabs = ['checkin', 'courses', 'balance']

    this.globalData.role = role
    this.globalData.allowedTabs = tabs
    wx.setStorageSync('role', role)
    console.log('角色: ' + role + ' → ' + tabs.join(','))
  },

  /** 后台登录 */
  login: function (openid) {
    var that = this
    wx.request({
      url: 'http://192.168.1.17:8080/api/auth/login',
      method: 'POST',
      data: { openid: openid },
      success: function (r) {
        if (r.data.code === 0) {
          var d = r.data.data
          wx.setStorageSync('token', d.token)
          wx.setStorageSync('studentId', d.role === 'STUDENT' ? d.userId : 0)
          wx.setStorageSync('teacherId', d.role !== 'STUDENT' ? d.userId : 0)
          console.log('后端登录完成: ' + d.role)
        }
      },
      fail: function (err) { console.log('后端登录失败（不影响使用）', err) }
    })
  },

  globalData: {
    role: 'STUDENT',
    allowedTabs: ['checkin', 'courses', 'balance'],
    token: '',
    userId: 0
  }
})
