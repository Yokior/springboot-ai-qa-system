import request from '@/utils/request'

// 查询知识库团队列表
export function listQa_team(query) {
  return request({
    url: '/team/qa_team/list',
    method: 'get',
    params: query
  })
}

// 查询知识库团队详细
export function getQa_team(teamId) {
  return request({
    url: '/team/qa_team/' + teamId,
    method: 'get'
  })
}

// 新增知识库团队
export function addQa_team(data) {
  return request({
    url: '/team/qa_team',
    method: 'post',
    data: data
  })
}

// 修改知识库团队
export function updateQa_team(data) {
  return request({
    url: '/team/qa_team',
    method: 'put',
    data: data
  })
}

// 删除知识库团队
export function delQa_team(teamId) {
  return request({
    url: '/team/qa_team/' + teamId,
    method: 'delete'
  })
}

// 导出知识库团队
export function exportQa_team(query) {
  return request({
    url: '/team/qa_team/export',
    method: 'get',
    params: query
  })
}

// 上传团队头像
export function uploadTeamAvatar(data) {
  return request({
    url: '/team/qa_team/avatar',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
