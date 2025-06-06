import request from '@/utils/request';

// 获取文档列表
export function listDocuments(query) {
  return request({
    url: '/knowledge/document/list',
    method: 'get',
    params: query
  });
}

// 获取文档详情
export function getDocument(docId) {
  return request({
    url: `/knowledge/document/${docId}`,
    method: 'get'
  });
}

// 删除文档
export function deleteDocument(docId, teamId) {
  return request({
    url: `/knowledge/document/${docId}`,
    method: 'delete',
    params: { teamId }
  });
}

// 上传文档
export function uploadDocument(data, teamId) {
  return request({
    url: `/knowledge/document/upload`,
    method: 'post',
    data: data,
    params: { teamId },
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

// 获取文档段落列表
export function listDocumentParagraphs(docId) {
  return request({
    url: `/knowledge/document/paragraphs/${docId}`,
    method: 'get'
  });
}

// 获取文档详情
export function getDocumentDetail(docId, teamId) {
  return request({
    url: `/knowledge/document/detail/${docId}`,
    method: 'get',
    params: { teamId }
  });
}
