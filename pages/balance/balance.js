const api = require('../../utils/request')

Page({
  data: {
    // 总览
    profile: { realName: '', gender: '', phone: '' },
    remaining: 0,
    totalPurchased: 0,
    totalConsumed: 0,
    daysUntilExpiry: 0,
    consumptionPercent: 0,
    isLowBalance: false,
    expiryClass: '',

    // Tab
    activeTab: 'all',

    // 课程列表
    courseList: [],

    // 流水
    transactions: [],
    filteredTransactions: []
  },

  onLoad() {
    this.loadAllData()
  },

  onPullDownRefresh() {
    this.loadAllData().then(() => wx.stopPullDownRefresh())
  },

  async loadAllData() {
    const studentId = wx.getStorageSync('studentId') || 1
    // 模拟学生信息（生产环境需调后端接口）
    this.setData({ profile: { realName: '张三', gender: '男', phone: '13900001111' } })

    try {
      const [balances, txList] = await Promise.all([
        api.get('/api/balance/list', { studentId }),
        api.get('/api/balance/transactions', { studentId, page: 1, pageSize: 20 })
      ])

      // 课程列表
      this.setData({ courseList: balances || [] })

      // 以第一门课为主展示
      if (balances && balances.length > 0) {
        const m = balances[0]
        const pct = m.totalPurchased > 0 ? Math.round((m.totalConsumed / m.totalPurchased) * 100) : 0
        this.setData({
          remaining: m.remaining || 0,
          totalPurchased: m.totalPurchased || 0,
          totalConsumed: m.totalConsumed || 0,
          daysUntilExpiry: m.daysUntilExpiry || 0,
          consumptionPercent: Math.min(pct, 100),
          isLowBalance: m.remaining > 0 && m.remaining <= 2,
          expiryClass: m.daysUntilExpiry <= 0 ? 'danger' : (m.daysUntilExpiry <= 30 ? 'warning' : '')
        })
      }

      // 流水
      this.setData({ transactions: txList || [] })
      this.filterTransactions()

    } catch (err) {
      console.error('加载课时数据失败:', err)
    }
  },

  /** 切换 Tab */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
    this.filterTransactions()
  },

  filterTransactions() {
    const { transactions, activeTab } = this.data
    let list = transactions
    if (activeTab === 'consume') list = transactions.filter(t => t.changeType === 'CONSUME')
    if (activeTab === 'purchase') list = transactions.filter(t => t.changeType === 'PURCHASE')
    this.setData({ filteredTransactions: list })
  },

  /** 跳转购课页 */
  goCourses() {
    wx.switchTab({ url: '/pages/courses/courses' })
  },

  /** 获取流水图标 */
  getTxIcon(type) {
    const map = { PURCHASE: '💰', CONSUME: '📝', REFUND: '↩️', ADJUST: '🔧' }
    return map[type] || '📌'
  }
})
