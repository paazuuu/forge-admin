import { request } from '@/utils'

export function lowcodeAppPage(params) {
  return request.get('/ai/lowcode/app/page', { params })
}

export function lowcodeDomainPage(params) {
  return request.get('/ai/lowcode/domain/page', { params })
}

export function lowcodeDomainTree(params) {
  return request.get('/ai/lowcode/domain/tree', { params })
}

export function lowcodeDomainDetail(id) {
  return request.get(`/ai/lowcode/domain/${id}`)
}

export function lowcodeCreateDomain(data) {
  return request.post('/ai/lowcode/domain', data)
}

export function lowcodeUpdateDomain(data) {
  return request.put('/ai/lowcode/domain', data)
}

export function lowcodeUpdateDomainStatus(id, status) {
  return request.put(`/ai/lowcode/domain/${id}/status`, { status })
}

export function lowcodeDomainWorkspace(id) {
  return request.get(`/ai/lowcode/domain/${id}/workspace`)
}

export function lowcodeDomainDefaults(id) {
  return request.get(`/ai/lowcode/domain/${id}/defaults`)
}

export function lowcodeDeleteDomain(id) {
  return request.delete(`/ai/lowcode/domain/${id}`)
}

export function lowcodeAppDetail(id) {
  return request.get(`/ai/lowcode/app/${id}`)
}

export function lowcodeDeleteApp(id) {
  return request.delete(`/ai/lowcode/app/${id}`)
}

export function lowcodeSaveDraft(data) {
  return request.post('/ai/lowcode/app/draft', data)
}

export function lowcodeMoveDomain(id, data) {
  return request.put(`/ai/lowcode/app/${id}/move-domain`, data)
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

export function lowcodeModelPage(params) {
  return request.get('/ai/lowcode/model/page', { params })
}

export function lowcodeModelList(params) {
  return request.get('/ai/lowcode/model/list', { params })
}

export function lowcodeModelDetail(id) {
  return request.get(`/ai/lowcode/model/${id}`)
}

export function lowcodeCreateModel(data) {
  return request.post('/ai/lowcode/model', data)
}

export function lowcodeUpdateModel(data) {
  return request.put('/ai/lowcode/model', data)
}

export function lowcodeUpdateModelStatus(id, status) {
  return request.put(`/ai/lowcode/model/${id}/status`, { status })
}

export function lowcodeDeleteModel(id) {
  return request.delete(`/ai/lowcode/model/${id}`)
}

export function lowcodeDdlPreview(data) {
  return request.post('/ai/lowcode/model/ddl/preview', { modelSchema: data })
}

export function genTablePage(params) {
  return request.get('/generator/list', { params })
}

export function genTableColumnList(tableId) {
  return request.get(`/generator/column/list/${tableId}`)
}

export function genDatasourceEnabled() {
  return request.get('/generator/datasource/enabled')
}
