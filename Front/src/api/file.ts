import request from '@/utils/request'

export const fileApi = {
  upload: (file: FormData) =>
    request.post<{ url: string }>('/file/upload', file, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  uploadBatch: (files: FormData) =>
    request.post<{ urls: string[] }>('/file/upload/batch', files, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
}
