import { useEffect, useState } from 'react'
import { Plus, Search, Trash2, Edit, Package } from 'lucide-react'
import { productApi, inventoryApi } from '../api'
import Pagination from '../components/Pagination'
import LoadingSpinner from '../components/LoadingSpinner'
import Modal from '../components/Modal'

export default function Products() {
  const [products, setProducts] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [showStockModal, setShowStockModal] = useState(false)
  const [editProduct, setEditProduct] = useState(null)
  const [stockProduct, setStockProduct] = useState(null)
  const [stockQuantity, setStockQuantity] = useState('')
  const [form, setForm] = useState({ sku: '', name: '', description: '', price: '', stockQuantity: '', category: '' })

  const fetchProducts = async (p = 0) => {
    setLoading(true)
    try {
      const res = searchQuery
        ? await productApi.search(searchQuery, p, 10)
        : await productApi.getAll(p, 10)
      setProducts(res.data.data?.content || [])
      setTotalPages(res.data.data?.totalPages || 0)
      setPage(p)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchProducts() }, [])

  const handleSearch = (e) => {
    e.preventDefault()
    fetchProducts(0)
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this product?')) return
    try {
      await productApi.delete(id)
      fetchProducts(page)
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to delete')
    }
  }

  const openCreate = () => {
    setEditProduct(null)
    setForm({ sku: '', name: '', description: '', price: '', stockQuantity: '', category: '' })
    setShowModal(true)
  }

  const openEdit = (p) => {
    setEditProduct(p)
    setForm({
      sku: p.sku || '',
      name: p.name || '',
      description: p.description || '',
      price: p.price?.toString() || '',
      stockQuantity: p.stockQuantity?.toString() || '',
      category: p.category || '',
    })
    setShowModal(true)
  }

  const openStockModal = (p) => {
    setStockProduct(p)
    setStockQuantity('')
    setShowStockModal(true)
  }

  const handleStockAdd = async (e) => {
    e.preventDefault()
    try {
      await inventoryApi.addStock(stockProduct.id, parseInt(stockQuantity), 'Stock added from UI')
      setShowStockModal(false)
      fetchProducts(page)
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update stock')
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const data = {
      ...form,
      price: parseFloat(form.price),
      stockQuantity: parseInt(form.stockQuantity) || 0,
    }
    try {
      if (editProduct) {
        await productApi.update(editProduct.id, data)
      } else {
        await productApi.create(data)
      }
      setShowModal(false)
      fetchProducts(page)
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to save')
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Products</h1>
        <button onClick={openCreate} className="btn-primary">
          <Plus size={16} /> Add Product
        </button>
      </div>

      <form onSubmit={handleSearch} className="flex gap-2 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
          <input
            type="text"
            placeholder="Search products..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input pl-9"
          />
        </div>
        <button type="submit" className="btn-secondary">Search</button>
      </form>

      {loading ? (
        <LoadingSpinner />
      ) : products.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-500">No products found.</p>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Product</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">SKU</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Category</th>
                  <th className="text-right px-4 py-3 font-medium text-gray-600">Price</th>
                  <th className="text-right px-4 py-3 font-medium text-gray-600">Stock</th>
                  <th className="text-right px-4 py-3 font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {products.map((p) => (
                  <tr key={p.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <p className="font-medium">{p.name}</p>
                      {p.description && <p className="text-xs text-gray-500 truncate max-w-xs">{p.description}</p>}
                    </td>
                    <td className="px-4 py-3 text-gray-600 font-mono text-xs">{p.sku}</td>
                    <td className="px-4 py-3 text-gray-600">{p.category || '—'}</td>
                    <td className="px-4 py-3 text-right font-medium">${p.price?.toFixed(2)}</td>
                    <td className="px-4 py-3 text-right">
                      <span className={`font-medium ${p.stockQuantity <= 10 ? 'text-red-600' : 'text-green-600'}`}>
                        {p.stockQuantity}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right whitespace-nowrap">
                      <button onClick={() => openStockModal(p)} className="p-1.5 rounded hover:bg-green-50 text-green-600" title="Add Stock">
                        <Package size={16} />
                      </button>
                      <button onClick={() => openEdit(p)} className="p-1.5 rounded hover:bg-gray-100 text-gray-500 ml-1">
                        <Edit size={16} />
                      </button>
                      <button onClick={() => handleDelete(p.id)} className="p-1.5 rounded hover:bg-red-50 text-red-500 ml-1">
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={totalPages} onPageChange={fetchProducts} />
        </>
      )}

      {/* Create/Edit Modal */}
      <Modal open={showModal} onClose={() => setShowModal(false)} title={editProduct ? 'Edit Product' : 'New Product'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          {!editProduct && (
            <div>
              <label className="label">SKU *</label>
              <input className="input" required value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} />
            </div>
          )}
          <div>
            <label className="label">Name *</label>
            <input className="input" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div>
            <label className="label">Description</label>
            <textarea className="input" rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">Price *</label>
              <input className="input" type="number" step="0.01" min="0" required value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
            </div>
            <div>
              <label className="label">Stock Quantity</label>
              <input className="input" type="number" min="0" value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} />
            </div>
          </div>
          <div>
            <label className="label">Category</label>
            <input className="input" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={() => setShowModal(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">{editProduct ? 'Update' : 'Create'}</button>
          </div>
        </form>
      </Modal>

      {/* Stock Modal */}
      <Modal open={showStockModal} onClose={() => setShowStockModal(false)} title={`Add Stock — ${stockProduct?.name}`}>
        <form onSubmit={handleStockAdd} className="space-y-4">
          <p className="text-sm text-gray-600">Current stock: <strong>{stockProduct?.stockQuantity}</strong></p>
          <div>
            <label className="label">Quantity to Add *</label>
            <input className="input" type="number" min="1" required value={stockQuantity} onChange={(e) => setStockQuantity(e.target.value)} />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={() => setShowStockModal(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-success">Add Stock</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
