var api = require('../../utils/request')

Page({
  data: {
    currentMonth: '',
    schedules: [],
    totalCheckedIn: 0, totalPending: 0, totalAbsent: 0,
    isStaff: false,
    showQr: false, qrImage: '', qrCourseName: '', qrTime: '', qrScheduleId: null,
    showCreate: false, courseList: [], courseIndex: 0,
    form: { courseId: '1', date: '', start: '14:00', end: '14:45', room: '' }
  },

  onLoad() {
    var role = wx.getStorageSync('role') || 'STUDENT'
    this.setData({ isStaff: role === 'TEACHER' || role === 'ADMIN' })
    var now = new Date()
    this.setData({ currentMonth: now.getFullYear() + '年' + (now.getMonth() + 1) + '月' })
    this.loadSchedules()
  },

  async loadSchedules() {
    var now = new Date()
    var start = new Date(now.getFullYear(), now.getMonth(), 1)
    var end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
    var fmt = function (d) { return d.toISOString().slice(0, 10) }

    try {
      var list = await api.get('/api/schedules', {
        institutionId: 1, startDate: fmt(start), endDate: fmt(end)
      })
      // 过滤掉已取消的排课
      list = (list || []).filter(function (s) { return s.status !== 'CANCELLED' })
      var weekdays = ['日', '一', '二', '三', '四', '五', '六']

      list.forEach(function (item) {
        if (item.scheduleDate) {
          item._day = item.scheduleDate.slice(8, 10) || '--'
          item._weekday = weekdays[new Date(item.scheduleDate).getDay()]
        } else { item._day = '--'; item._weekday = '-' }
        var t = item.totalStudents || 0
        if (t > 0) {
          item._pctChecked = ((item.checkedInCount || 0) / t * 100).toFixed(1)
          item._pctPending = ((item.pendingCount || 0) / t * 100).toFixed(1)
          item._pctAbsent = (((item.absentCount || 0) + (item.leaveCount || 0)) / t * 100).toFixed(1)
        } else { item._pctChecked = 0; item._pctPending = 0; item._pctAbsent = 0 }
      })

      this.setData({
        schedules: list,
        totalCheckedIn: list.reduce(function (s, i) { return s + (i.checkedInCount || 0) }, 0),
        totalPending: list.reduce(function (s, i) { return s + (i.pendingCount || 0) }, 0),
        totalAbsent: list.reduce(function (s, i) { return s + (i.absentCount || 0) }, 0)
      })
    } catch (err) { console.error(err) }
  },

  prevMonth() {
    var p = this.data.currentMonth.split(/[年月]/)
    var y = +p[0], m = +p[1]
    if (--m < 1) { m = 12; y-- }
    this.setData({ currentMonth: y + '年' + m + '月' })
    this.loadSchedules()
  },
  nextMonth() {
    var p = this.data.currentMonth.split(/[年月]/)
    var y = +p[0], m = +p[1]
    if (++m > 12) { m = 1; y++ }
    this.setData({ currentMonth: y + '年' + m + '月' })
    this.loadSchedules()
  },

  // ====== 二维码 ======
  /** 点击卡片 → 跳转点名册详情页 */
  goRoster(e) {
    var item = e.currentTarget.dataset.item
    if (!item || !item.scheduleId) return
    wx.navigateTo({
      url: '/pages/roster/roster?scheduleId=' + item.scheduleId
        + '&courseName=' + encodeURIComponent(item.courseName || '')
        + '&scheduleTime=' + encodeURIComponent((item.scheduleDate || '') + ' ' + (item.startTime || '') + '-' + (item.endTime || ''))
    })
  },

  async showQrCode(e) {
    var item = e.currentTarget.dataset.item
    if (!item || !item.scheduleId) return
    wx.showLoading({ title: '生成二维码...' })
    try {
      var base64 = await api.get('/api/schedules/' + item.scheduleId + '/qrcode')
      this.setData({
        showQr: true, qrImage: base64,
        qrCourseName: item.courseName,
        qrTime: (item.scheduleDate || '') + ' ' + (item.startTime || '') + '-' + (item.endTime || ''),
        qrScheduleId: item.scheduleId
      })
    } catch (err) { wx.showToast({ title: '二维码获取失败', icon: 'none' }) }
    finally { wx.hideLoading() }
  },
  closeQr() { this.setData({ showQr: false }) },
  async refreshQr() {
    if (!this.data.qrScheduleId) return
    wx.showLoading({ title: '刷新中...' })
    try {
      await api.post('/api/schedules/' + this.data.qrScheduleId + '/qrcode/refresh')
      var base64 = await api.get('/api/schedules/' + this.data.qrScheduleId + '/qrcode')
      this.setData({ qrImage: base64 })
      wx.showToast({ title: '二维码已刷新', icon: 'success' })
    } catch (err) { wx.showToast({ title: '刷新失败', icon: 'none' }) }
    finally { wx.hideLoading() }
  },

  // ====== 新建排课 ======
  showCreateForm() {
    var today = new Date().toISOString().slice(0, 10)
    var that = this
    api.get('/api/courses', { institutionId: 1 }).then(function (list) {
      that.setData({ courseList: list || [], showCreate: true, 'form.date': today, 'form.courseId': list && list[0] ? String(list[0].courseId) : '1', courseIndex: 0 })
    })
  },
  closeCreate() { this.setData({ showCreate: false }) },
  onFormField(e) { var f = e.currentTarget.dataset.field; this.setData({ ['form.' + f]: e.detail.value }) },
  onDateChange(e) { this.setData({ 'form.date': e.detail.value }) },
  onStartTime(e) { this.setData({ 'form.start': e.detail.value }) },
  onEndTime(e) { this.setData({ 'form.end': e.detail.value }) },
  onCourseChange(e) {
    var idx = e.detail.value
    var c = this.data.courseList[idx]
    this.setData({ courseIndex: idx, 'form.courseId': c ? String(c.courseId) : '1' })
  },

  doCreate() {
    var f = this.data.form
    if (!f.date || !f.start || !f.end) { wx.showToast({ title: '请填写完整信息', icon: 'none' }); return }
    var that = this
    api.post('/api/schedules?institutionId=1', {
      courseId: Number(f.courseId) || 1,
      teacherId: Number(wx.getStorageSync('teacherId') || 1),
      scheduleDate: f.date,
      startTime: f.start + ':00',
      endTime: f.end + ':00',
      classroom: f.room,
      maxStudents: 20
    }).then(function () {
      wx.showToast({ title: '创建成功', icon: 'success' })
      that.setData({ showCreate: false })
      that.loadSchedules()
    }).catch(function () { wx.showToast({ title: '创建失败', icon: 'none' }) })
  },

  // ====== 删除排课 ======
  deleteSchedule(e) {
    var sid = e.currentTarget.dataset.sid
    var that = this
    wx.showModal({
      title: '确认删除', content: '删除后排课不可恢复',
      success: function (r) {
        if (r.confirm) {
          api.del('/api/schedules/' + sid).then(function () {
            wx.showToast({ title: '已删除', icon: 'success' })
            that.loadSchedules()
          }).catch(function () {
            wx.showToast({ title: '删除失败', icon: 'none' })
          })
        }
      }
    })
  }
})
