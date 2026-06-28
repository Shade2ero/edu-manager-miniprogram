/**
 * 排课管理 API
 */
import { get, post, request } from './request'

const BASE = '/api/schedules'

export const scheduleApi = {
  /** 查询排课列表 */
  list(params) {
    return get(BASE, params)
  },

  /** 排课详情（含点名册） */
  detail(scheduleId) {
    return get(BASE + '/' + scheduleId)
  },

  /** 创建排课 */
  create(institutionId, data) {
    return post(BASE + '?institutionId=' + institutionId, data, {
      showLoading: true,
      loadingText: '创建中...',
    })
  },

  /** 更新排课 */
  update(scheduleId, data) {
    return request({
      url: BASE + '/' + scheduleId,
      method: 'PUT',
      data,
      showLoading: true,
    })
  },

  /** 取消排课 */
  cancel(scheduleId) {
    return request({
      url: BASE + '/' + scheduleId,
      method: 'DELETE',
    })
  },

  /** 获取点名册学员列表 */
  getRoster(scheduleId) {
    return get(BASE + '/' + scheduleId + '/roster')
  },

  /** 添加学员到点名册 */
  addStudent(scheduleId, studentId) {
    return post(BASE + '/' + scheduleId + '/roster?studentId=' + studentId)
  },

  /** 从点名册移除学员 */
  removeStudent(scheduleId, studentId) {
    return request({
      url: BASE + '/' + scheduleId + '/roster?studentId=' + studentId,
      method: 'DELETE',
    })
  },

  /** 刷新二维码 Token */
  refreshQrCode(scheduleId) {
    return post(BASE + '/' + scheduleId + '/qrcode/refresh')
  },
}
