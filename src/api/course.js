/**
 * 课程与课时包 API
 */
import { get } from './request'

export const courseApi = {
  /**
   * 获取机构所有课程及课时包列表
   * @param {number} institutionId - 机构ID
   * @returns {Promise<Array>} 课程列表，每个课程含 packages 数组
   */
  listCourses(institutionId = 1) {
    return get('/api/courses', { institutionId })
  },

  /**
   * 获取单个课程详情
   * @param {number} courseId - 课程ID
   * @returns {Promise<{course, packages}>}
   */
  getCourseDetail(courseId) {
    return get(`/api/courses/${courseId}`)
  },
}
