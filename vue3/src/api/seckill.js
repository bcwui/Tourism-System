import request from '@/utils/request'

/**
 * 获取门票的秒杀活动信息
 */
export function getSeckillActivity(ticketId) {
  return request.get(`/seckill/activity/${ticketId}`, {}, {
    showDefaultMsg: false
  })
}

/**
 * 获取当前所有进行中的秒杀活动列表（门票列表页用）
 */
export function getSeckillList() {
  return request.get('/seckill/list', {}, {
    showDefaultMsg: false
  })
}

/**
 * 执行秒杀
 */
export function executeSeckill(activityId, data) {
  return request.post(`/seckill/${activityId}`, data, {
    showDefaultMsg: false
  })
}

/**
 * 获取秒杀库存（Redis实时库存）
 */
export function getSeckillStock(activityId) {
  return request.get(`/seckill/stock/${activityId}`, {}, {
    showDefaultMsg: false
  })
}

// ==================== 管理端 API ====================

/**
 * 管理端：分页获取所有秒杀活动
 */
export function adminGetSeckillList(params) {
  return request.get('/seckill/admin/list', params, {
    showDefaultMsg: false
  })
}

/**
 * 管理端：获取秒杀活动详情
 */
export function adminGetSeckillDetail(id) {
  return request.get(`/seckill/admin/${id}`, {}, {
    showDefaultMsg: false
  })
}

/**
 * 管理端：创建秒杀活动
 */
export function adminCreateSeckill(data) {
  return request.post('/seckill', data, {
    successMsg: '秒杀活动创建成功'
  })
}

/**
 * 管理端：更新秒杀活动
 */
export function adminUpdateSeckill(id, data) {
  return request.put(`/seckill/${id}`, data, {
    successMsg: '秒杀活动更新成功'
  })
}

/**
 * 管理端：删除秒杀活动
 */
export function adminDeleteSeckill(id) {
  return request.delete(`/seckill/${id}`, null, {
    successMsg: '秒杀活动删除成功'
  })
}

/**
 * 管理端：预热秒杀库存到Redis
 */
export function adminPreloadSeckillStock(activityId) {
  return request.post(`/seckill/preload/${activityId}`, {}, {
    successMsg: '库存预热成功'
  })
}
