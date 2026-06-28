/**
 * 签到相关 API
 */
import { get, post } from './request'

export const checkinApi = {
  /**
   * 学生扫码签到（主接口）
   *
   * @param {object} params - 签到参数
   * @param {number} params.scheduleId - 排课ID
   * @param {number} params.studentId - 学员ID
   * @param {string} params.qrToken - 二维码中解析的 token
   * @param {number} params.latitude - 签到纬度
   * @param {number} params.longitude - 签到经度
   * @param {string} params.deviceBrand - 设备品牌
   * @param {string} params.deviceModel - 设备型号
   * @param {string} params.systemVersion - 系统版本
   * @param {string} params.wxVersion - 微信版本
   * @returns {Promise<CheckInResult>}
   */
  scanCheckIn(params) {
    return post('/api/checkin/scan', params, { showLoading: true, loadingText: '签到中...' })
  },

  /**
   * LBS 定位签到（无扫码，纯位置签到）
   */
  lbsCheckIn(params) {
    return post('/api/checkin/lbs', params, { showLoading: true, loadingText: '签到中...' })
  },

  /**
   * 查询签到状态
   *
   * @param {number} scheduleId - 排课ID
   * @param {number} studentId - 学员ID
   * @returns {Promise<string>} PENDING / CHECKED_IN / ABSENT / LEAVE
   */
  getCheckInStatus(scheduleId, studentId) {
    return get('/api/checkin/status', { scheduleId, studentId })
  },
}
