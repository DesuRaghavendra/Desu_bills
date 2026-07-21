import api from './api';

export const getTables = async () => {
  const response = await api.get('/api/tables');
  return response.data;
};

export const deleteTable = async (tableId) => {
  const response = await api.delete(`/api/tables/${tableId}`);
  return response.data;
};

export const createTable = async (payload) => {
  const response = await api.post('/api/tables', payload);
  return response.data;
};

export const previewNewTable = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post('/api/ocr/preview/new-table', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return response.data;
};

export const getTableDetail = async (tableId) => {
  const response = await api.get(`/api/tables/${tableId}`);
  return response.data;
};

export const getTableRecords = async (tableId, page = 0, size = 10) => {
  const response = await api.get(`/api/tables/${tableId}/records`, {
    params: { page, size }
  });
  return response.data;
};

export const appendRecords = async (tableId, records) => {
  const response = await api.post(`/api/tables/${tableId}/records`, { records });
  return response.data;
};

export const previewExistingTable = async (tableId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('tableId', tableId);
  const response = await api.post('/api/ocr/preview/existing-table', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return response.data;
};

export const searchRecords = async (tableId, filters, page = 0, size = 10) => {
  const response = await api.post(`/api/tables/${tableId}/search`, {
    filters,
    page,
    size
  });
  return response.data;
};

export const updateRecord = async (recordId, data) => {
  const response = await api.put(`/api/records/${recordId}`, { data });
  return response.data;
};

export const deleteRecord = async (recordId) => {
  const response = await api.delete(`/api/records/${recordId}`);
  return response.data;
};

export const batchDeleteRecords = async (recordIds) => {
  const response = await api.delete('/api/records/batch', {
    data: { recordIds }
  });
  return response.data;
};
