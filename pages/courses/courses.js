var api = require('../../utils/request')

Page({
  data: {
    courses: [],
    selectedPkg: null, selectedCourse: null,
    showBar: false, showConfirm: false,
    paying: false, showPayResult: false, paySuccess: false,
    isAdmin: false, adminMode: false,
    showAdd: false,
    addForm: { name: '', category: '', duration: '60', desc: '', packages: [{ name: '', hours: '16', price: '', origPrice: '' }] }
  },

  onLoad() {
    var role = wx.getStorageSync('role') || 'STUDENT'
    this.setData({ isAdmin: role === 'ADMIN' })
    this.loadCourses()
  },

  onShow() { this.loadCourses() },

  async loadCourses() {
    try {
      var params = { institutionId: 1 }
      if (this.data.adminMode) params.admin = true
      var courses = await api.get('/api/courses', params)
      // 管理员模式：下架即可删除
      if (this.data.adminMode) {
        ;(courses || []).forEach(function (c) {
          c._canDelete = c.status === 0
        })
      }
      this.setData({ courses: courses || [] })
    } catch (err) { console.error(err) }
  },

  // ====== 选择课时包 ======
  selectPkg(e) {
    var ds = e.currentTarget.dataset
    var course = this.data.courses[ds.courseidx]
    var pkg = course && course.packages ? course.packages[ds.pkgidx] : null
    if (pkg) this.setData({ selectedPkg: pkg, selectedCourse: course, showBar: true })
  },

  handleBuy() {
    if (!this.data.selectedPkg) return
    this.setData({ showConfirm: true })
  },
  closeConfirm() { if (!this.data.paying) this.setData({ showConfirm: false }) },

  async handlePay() {
    if (this.data.paying) return
    this.setData({ paying: true })
    try {
      var studentId = wx.getStorageSync('studentId') || 1
      var openid = wx.getStorageSync('openid') || 'dev_openid_001'
      var order = await api.post('/api/orders', { studentId: studentId, packageId: this.data.selectedPkg.id, openid: openid })
      this.setData({ showConfirm: false })
      wx.requestPayment({
        timeStamp: order.payParams.timeStamp, nonceStr: order.payParams.nonceStr,
        package: order.payParams.package, signType: order.payParams.signType || 'RSA', paySign: order.payParams.paySign,
        success: function () { this.setData({ paySuccess: true, showPayResult: true }) }.bind(this),
        fail: function (err) {
          if (!(err.errMsg && err.errMsg.indexOf('cancel') >= 0)) {
            this.setData({ paySuccess: false, showPayResult: true })
          }
        }.bind(this),
        complete: function () { this.setData({ paying: false }) }.bind(this)
      })
    } catch (err) { this.setData({ paying: false, showConfirm: false }) }
  },

  /** 开发模式：模拟支付，跳过微信 */
  async handleMockPay() {
    var studentId = wx.getStorageSync('studentId') || 1
    var openid = wx.getStorageSync('openid') || 'dev_openid_001'
    var that = this
    try {
      var order = await api.post('/api/orders', { studentId: studentId, packageId: this.data.selectedPkg.id, openid: openid })
      if (!order || !order.orderNo) {
        wx.showToast({ title: '下单失败，课时包可能已下架', icon: 'none' })
        return
      }
      await api.post('/api/orders/mock-pay?orderNo=' + order.orderNo)
      this.setData({ showConfirm: false, paySuccess: true, showPayResult: true })
    } catch (err) { wx.showToast({ title: '支付失败，请重试', icon: 'none' }) }
  },

  closePayResult() {
    this.setData({ showPayResult: false })
    if (this.data.paySuccess) wx.switchTab({ url: '/pages/balance/balance' })
  },

  // ====== 管理员功能 ======
  toggleAdmin() {
    this.setData({ adminMode: !this.data.adminMode, showBar: false, selectedPkg: null })
    this.loadCourses()
  },

  toggleCourse(e) {
    var cid = e.currentTarget.dataset.cid
    var that = this
    wx.showModal({
      title: '确认操作', content: '切换该课程的上下架状态？',
      success: function (r) {
        if (r.confirm) {
          api.put('/api/courses/' + cid + '/toggle').then(function () {
            wx.showToast({ title: '操作成功', icon: 'success' })
            that.loadCourses()
          })
        }
      }
    })
  },

  deleteCourse(e) {
    var cid = e.currentTarget.dataset.cid
    var that = this
    wx.showModal({
      title: '确认删除', content: '删除后不可恢复，确定删除此课程？',
      success: function (r) {
        if (r.confirm) {
          api.del('/api/courses/' + cid).then(function () {
            wx.showToast({ title: '已删除', icon: 'success' })
            that.loadCourses()
          }).catch(function () {})
        }
      }
    })
  },

  // ====== 新增课程 ======
  showAddForm() {
    this.setData({ showAdd: true, addForm: { name: '', category: '', duration: '60', desc: '', packages: [{ name: '', hours: '16', price: '', origPrice: '' }] } })
  },
  closeAdd() { this.setData({ showAdd: false }) },

  // 课时包动态增删
  addPkgItem() {
    var pkgs = this.data.addForm.packages.concat([{ name: '', hours: '16', price: '', origPrice: '' }])
    this.setData({ 'addForm.packages': pkgs })
  },
  delPkgItem(e) {
    var idx = e.currentTarget.dataset.idx
    var pkgs = this.data.addForm.packages.filter(function(_, i) { return i !== idx })
    this.setData({ 'addForm.packages': pkgs })
  },
  onPkgField(e) {
    var idx = e.currentTarget.dataset.idx
    var field = e.currentTarget.dataset.field
    this.setData({ ['addForm.packages[' + idx + '].' + field]: e.detail.value })
  },

  onAddField(e) {
    var f = e.currentTarget.dataset.field
    this.setData({ ['addForm.' + f]: e.detail.value })
  },

  doAddCourse() {
    var f = this.data.addForm
    if (!f.name) { wx.showToast({ title: '请输入课程名', icon: 'none' }); return }
    var that = this
    // 组装课时包数组
    var pkgList = []
    ;(f.packages || []).forEach(function (p) {
      if (p.name && p.hours && p.price) {
        pkgList.push({
          name: p.name, totalHours: Number(p.hours), price: Math.round(parseFloat(p.price) * 100),
          originalPrice: p.origPrice ? Math.round(parseFloat(p.origPrice) * 100) : null
        })
      }
    })
    var body = { name: f.name, category: f.category, defaultDuration: Number(f.duration) || 60, description: f.desc, packages: pkgList.length > 0 ? pkgList : null }
    api.post('/api/courses?institutionId=1', body).then(function () {
      wx.showToast({ title: '创建成功', icon: 'success' })
      that.setData({ showAdd: false })
      that.loadCourses()
    })
  }
})
