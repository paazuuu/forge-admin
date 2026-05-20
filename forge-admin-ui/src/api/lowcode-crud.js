import { request } from '@/utils'

export function lowcodeAppPage(params) {
  return request.get('/ai/lowcode/app/page', { params })
}

export function lowcodeAppDetail(id) {
  return request.get(`/ai/lowcode/app/${id}`)
}

export function lowcodeSaveDraft(data) {
  return request.post('/ai/lowcode/app/draft', data)
}

export function lowcodePreview(id, data) {
  return request.post(`/ai/lowcode/app/${id}/preview`, data)
}

export function lowcodePublish(id, data) {
  return request.post(`/ai/lowcode/app/${id}/publish`, data)
}

export function lowcodeVersions(id) {
  return request.get(`/ai/lowcode/app/${id}/versions`)
}

export function lowcodeRollback(id, versionId) {
  return request.post(`/ai/lowcode/app/${id}/rollback/${versionId}`)
}

export function lowcodeValidateModel(data) {
  return request.post('/ai/lowcode/model/validate', data)
}

export function lowcodeDdlPreview(data) {
  return request.post('/ai/lowcode/model/ddl/preview', { modelSchema: data })
}
