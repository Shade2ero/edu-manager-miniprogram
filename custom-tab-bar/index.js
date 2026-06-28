Component({
  data: { selected: 0, list: [] },

  lifetimes: {
    attached() {
      var app = getApp()
      var allowed = app.globalData.allowedTabs || ['checkin', 'courses', 'balance']
      var all = [
        { pagePath: 'pages/checkin/checkin',   text: '签到', icon: '📷', key: 'checkin' },
        { pagePath: 'pages/courses/courses',   text: '购课', icon: '🛒', key: 'courses' },
        { pagePath: 'pages/balance/balance',   text: '课时', icon: '📊', key: 'balance' },
        { pagePath: 'pages/schedule/schedule', text: '排课', icon: '📅', key: 'schedule' },
      ]
      var list = all.filter(function (t) { return allowed.indexOf(t.key) >= 0 })
      this.setData({ list: list })

      // 初始选中：匹配当前路由
      var pages = getCurrentPages()
      if (pages.length > 0) {
        var route = pages[pages.length - 1].route
        this.highlightByRoute(route)
      }
    }
  },

  pageLifetimes: {
    show() {
      // 页面切换时更新高亮（不重建列表，只改选中）
      var pages = getCurrentPages()
      if (pages.length > 0) {
        this.highlightByRoute(pages[pages.length - 1].route)
      }
    }
  },

  methods: {
    highlightByRoute(route) {
      var idx = -1
      for (var i = 0; i < this.data.list.length; i++) {
        if (this.data.list[i].pagePath === route) { idx = i; break }
      }
      if (idx >= 0) this.setData({ selected: idx })
    },

    switchTab(e) {
      var idx = Number(e.currentTarget.dataset.index)
      var path = e.currentTarget.dataset.path
      this.setData({ selected: idx })
      wx.switchTab({ url: '/' + path })
    }
  }
})
