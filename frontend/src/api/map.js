import request from '@/utils/request'

export const uploadMapImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/map/image/upload', formData)
}

export const getMapImageList = () => request.get('/map/image/list')

export function getMapImageUrl(id) {
  return `/api/map/image/${id}`
}

export const deleteMapImage = (id) => request.delete(`/map/image/${id}`)
