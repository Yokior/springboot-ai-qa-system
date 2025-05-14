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
export function getMy_team(id, query) {
  return request({
    url: '/team/my_team/' + id,
    method: 'get',
    params: query
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
export function delMy_team(data) {
  return request({
    url: '/team/my_team/dissolve',
    method: 'delete',
    data: data
  })
}

// 转让团队
export function transferTeam(data) {
  return request({
    url: '/team/my_team/transfer',
    method: 'put',
    data: data
  })
}

// 踢出团队成员
export function removeMember(data) {
  return request({
    url: '/team/my_team/remove_member',
    method: 'delete',
    data: data
  })
}

// 更新成员角色
export function updateMemberRole(data) {
  return request({
    url: '/team/my_team/update_role',
    method: 'put',
    data: data
  })
}

// 上传团队头像
export function uploadTeamAvatar(data) {
  return request({
    url: '/team/my_team/avatar',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 查询团队成员列表
export function listTeamMembers(query) {
  return request({
    url: '/team/my_team/member',
    method: 'get',
    params: query
  })
}

// 创建团队邀请
export function createTeamInvite(data) {
  return request({
    url: '/team/my_team/invite/create',
    method: 'post',
    data: data
  })
}

// 获取邀请信息
export function getTeamInviteInfo(code) {
  return request({
    url: '/team/my_team/invite/info/' + code,
    method: 'get'
  })
}

// 接受团队邀请
export function acceptTeamInvite(code) {
  return request({
    url: '/team/my_team/invite/accept/' + code,
    method: 'post'
  })
}

// 取消团队邀请
export function cancelTeamInvite(inviteId) {
  return request({
    url: '/team/my_team/invite/cancel/' + inviteId,
    method: 'delete'
  })
}
