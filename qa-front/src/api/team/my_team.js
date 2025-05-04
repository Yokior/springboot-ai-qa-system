import request from '@/utils/request'

// 查询我的团队列表
export function listMy_team(query) {
  return request({
    url: '/team/my_team/list',
    method: 'get',
    params: query
  })
}

// 查询我的团队详细
export function getMy_team(id) {
  return request({
    url: '/team/my_team/' + id,
    method: 'get'
  })
}

// 新增我的团队
export function addMy_team(data) {
  return request({
    url: '/team/my_team',
    method: 'post',
    data: data
  })
}

// 修改我的团队
export function updateMy_team(data) {
  return request({
    url: '/team/my_team',
    method: 'put',
    data: data
  })
}

// 删除我的团队
export function delMy_team(id) {
  return request({
    url: '/team/my_team/' + id,
    method: 'delete'
  })
}
