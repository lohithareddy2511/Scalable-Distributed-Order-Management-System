import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Customers
export const customerApi = {
  getAll: (page = 0, size = 10, sortBy = 'createdAt', direction = 'desc') =>
    api.get('/customers', { params: { page, size, sortBy, direction } }),
  getById: (id) => api.get(`/customers/${id}`),
  search: (query, page = 0, size = 10) =>
    api.get('/customers/search', { params: { query, page, size } }),
  create: (data) => api.post('/customers', data),
  update: (id, data) => api.put(`/customers/${id}`, data),
  delete: (id) => api.delete(`/customers/${id}`),
}

// Products
export const productApi = {
  getAll: (page = 0, size = 10, sortBy = 'createdAt', direction = 'desc') =>
    api.get('/products', { params: { page, size, sortBy, direction } }),
  getById: (id) => api.get(`/products/${id}`),
  getActive: (page = 0, size = 10) =>
    api.get('/products/active', { params: { page, size } }),
  getByCategory: (category, page = 0, size = 10) =>
    api.get(`/products/category/${category}`, { params: { page, size } }),
  search: (query, page = 0, size = 10) =>
    api.get('/products/search', { params: { query, page, size } }),
  getLowStock: (threshold = 10) =>
    api.get('/products/low-stock', { params: { threshold } }),
  create: (data) => api.post('/products', data),
  update: (id, data) => api.put(`/products/${id}`, data),
  delete: (id) => api.delete(`/products/${id}`),
}

// Orders
export const orderApi = {
  getAll: (page = 0, size = 10, sortBy = 'createdAt', direction = 'desc') =>
    api.get('/orders', { params: { page, size, sortBy, direction } }),
  getById: (id) => api.get(`/orders/${id}`),
  getByCustomer: (customerId, page = 0, size = 10) =>
    api.get(`/orders/customer/${customerId}`, { params: { page, size } }),
  getByStatus: (status, page = 0, size = 10) =>
    api.get(`/orders/status/${status}`, { params: { page, size } }),
  create: (data) => api.post('/orders', data),
  updateStatus: (id, data) => api.patch(`/orders/${id}/status`, data),
  cancel: (id, reason) =>
    api.post(`/orders/${id}/cancel`, null, { params: { reason } }),
}

// Payments
export const paymentApi = {
  getById: (id) => api.get(`/payments/${id}`),
  getByOrder: (orderId) => api.get(`/payments/order/${orderId}`),
  process: (data) => api.post('/payments', data),
  refund: (id) => api.post(`/payments/${id}/refund`),
}

// Inventory
export const inventoryApi = {
  addStock: (productId, quantity, note) =>
    api.post(`/inventory/${productId}/add`, null, { params: { quantity, note } }),
  adjustStock: (productId, newQuantity, note) =>
    api.post(`/inventory/${productId}/adjust`, null, { params: { newQuantity, note } }),
}

export default api
