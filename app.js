App({
  onLaunch() {
    // 不自动登录，由 pages/login/login 页面处理角色选择
  },

  /** 根据 openid 推断角色 */
  detectRole: function (openid) {
    if (openid === 'dev_teacher_001') return 'TEACHER'
    if (openid === 'dev_admin_001') return 'ADMIN'
    return 'STUDENT'
  },

  /** 根据角色设置 Tab */
  applyRole: function (role) {
    var tabs
    if (role === 'ADMIN')      tabs = ['courses', 'balance', 'schedule']
    else if (role === 'TEACHER') tabs = ['balance', 'schedule']
    else                        tabs = ['checkin', 'courses', 'balance']

    this.globalData.role = role
    this.globalData.allowedTabs = tabs
    wx.setStorageSync('role', role)
    wx.setStorageSync('studentId', role === 'STUDENT' ? 1 : 0)
    wx.setStorageSync('teacherId', role !== 'STUDENT' ? 1 : 0)
    console.log('角色: ' + role + ' → ' + tabs.join(','))
  },

  globalData: {
    role: 'STUDENT',
    allowedTabs: ['checkin', 'courses', 'balance'],
    token: '',
    userId: 0
  }
})
