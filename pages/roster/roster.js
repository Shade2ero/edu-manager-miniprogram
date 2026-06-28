var api = require('../../utils/request')

Page({
  data: {
    scheduleId: null,
    courseName: '', scheduleTime: '',
    students: [], checked: 0, pending: 0, absent: 0, leave: 0,
    showQr: false, qrImage: '', qrToken: ''
  },

  onLoad(options) {
    var sid = options.scheduleId
    var cname = decodeURIComponent(options.courseName || '')
    var stime = decodeURIComponent(options.scheduleTime || '')
    this.setData({ scheduleId: sid, courseName: cname, scheduleTime: stime })
    if (sid) this.loadRoster(sid)
  },

  async loadRoster(sid) {
    try {
      var list = await api.get('/api/schedules/' + sid + '/roster')
      list = (list || []).map(function (s) {
        s._statusTxt = { PENDING: '未签', CHECKED_IN: '✓已签', ABSENT: '缺勤', LEAVE: '请假' }[s.status] || s.status
        s._statusCls = { PENDING: 's-pending', CHECKED_IN: 's-ok', ABSENT: 's-absent', LEAVE: 's-leave' }[s.status] || ''
        return s
      })
      // 预计算统计数据
      var checked = 0, pending = 0, absent = 0, leave = 0
      list.forEach(function (s) {
        if (s.status === 'CHECKED_IN') checked++
        else if (s.status === 'PENDING') pending++
        else if (s.status === 'ABSENT') absent++
        else if (s.status === 'LEAVE') leave++
      })
      this.setData({ students: list, checked: checked, pending: pending, absent: absent, leave: leave })
    } catch (err) { console.error(err) }
  },

  // ====== 二维码 ======
  async showQrCode() {
    if (!this.data.scheduleId) return
    wx.showLoading({ title: '生成二维码...' })
    try {
      var base64 = await api.get('/api/schedules/' + this.data.scheduleId + '/qrcode')
      this.setData({ showQr: true, qrImage: base64 })
    } catch (err) { wx.showToast({ title: '获取失败', icon: 'none' }) }
    finally { wx.hideLoading() }
  },
  closeQr() { this.setData({ showQr: false }) },
  async refreshQr() {
    wx.showLoading({ title: '刷新中...' })
    try {
      await api.post('/api/schedules/' + this.data.scheduleId + '/qrcode/refresh')
      var base64 = await api.get('/api/schedules/' + this.data.scheduleId + '/qrcode')
      this.setData({ qrImage: base64 })
      wx.showToast({ title: '已刷新', icon: 'success' })
    } catch (err) { wx.showToast({ title: '刷新失败', icon: 'none' }) }
    finally { wx.hideLoading() }
  },

  /** 刷新点名册 */
  onShow() {
    if (this.data.scheduleId) this.loadRoster(this.data.scheduleId)
  }
})
