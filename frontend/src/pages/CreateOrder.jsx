import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Trash2, ArrowLeft } from 'lucide-react'
import { orderApi, customerApi, productApi } from '../api'
import LoadingSpinner from '../components/LoadingSpinner'

export default function CreateOrder() {
  const navigate = useNavigate()
  const [customers, setCustomers] = useState([])
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const [form, setForm] = useState({
    customerId: '',
    items: [{ productId: '', quantity: 1 }],
    shippingAddressLine1: '',
    shippingCity: '',
    shippingState: '',
    shippingZipCode: '',
    shippingCountry: '',
    notes: '',
  })

  useEffect(() => {
    async function fetchData() {
      try {
        const [custRes, prodRes] = await Promise.all([
          customerApi.getAll(0, 100),
          productApi.getActive(0, 100),
        ])
        setCustomers(custRes.data.data?.content || [])
        setProducts(prodRes.data.data?.content || [])
      } catch (err) {
        console.error(err)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  const addItem = () => {
    setForm({ ...form, items: [...form.items, { productId: '', quantity: 1 }] })
  }

  const removeItem = (index) => {
    setForm({ ...form, items: form.items.filter((_, i) => i !== index) })
  }

  const updateItem = (index, field, value) => {
    const items = [...form.items]
    items[index] = { ...items[index], [field]: value }
    setForm({ ...form, items })
  }

  const getTotal = () => {
    return form.items.reduce((sum, item) => {
      const product = products.find((p) => p.id === item.productId)
      return sum + (product?.price || 0) * (item.quantity || 0)
    }, 0)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.items.some((i) => !i.productId)) {
      alert('Please select a product for all items')
      return
    }
    setSubmitting(true)
    try {
      const res = await orderApi.create({
        ...form,
        items: form.items.map((i) => ({ productId: i.productId, quantity: parseInt(i.quantity) })),
      })
      navigate(`/orders/${res.data.data.id}`)
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create order')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <LoadingSpinner />

  return (
    <div>
      <button onClick={() => navigate('/orders')} className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-4">
        <ArrowLeft size={16} /> Back to Orders
      </button>

      <h1 className="text-2xl font-bold mb-6">Create Order</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Customer */}
        <div className="card">
          <h2 className="text-lg font-semibold mb-4">Customer</h2>
          <select
            className="input"
            required
            value={form.customerId}
            onChange={(e) => setForm({ ...form, customerId: e.target.value })}
          >
            <option value="">Select a customer...</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>{c.firstName} {c.lastName} ({c.email})</option>
            ))}
          </select>
        </div>

        {/* Items */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">Order Items</h2>
            <button type="button" onClick={addItem} className="btn-secondary text-xs">
              <Plus size={14} /> Add Item
            </button>
          </div>

          <div className="space-y-3">
            {form.items.map((item, index) => {
              const product = products.find((p) => p.id === item.productId)
              return (
                <div key={index} className="flex items-center gap-3">
                  <select
                    className="input flex-1"
                    value={item.productId}
                    onChange={(e) => updateItem(index, 'productId', e.target.value)}
                    required
                  >
                    <option value="">Select product...</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} — ${p.price?.toFixed(2)} (Stock: {p.stockQuantity})
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    min="1"
                    max={product?.stockQuantity || 999}
                    className="input w-24"
                    value={item.quantity}
                    onChange={(e) => updateItem(index, 'quantity', e.target.value)}
                  />
                  {form.items.length > 1 && (
                    <button type="button" onClick={() => removeItem(index)} className="p-2 text-red-500 hover:bg-red-50 rounded">
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>
              )
            })}
          </div>

          <div className="mt-4 pt-4 border-t text-right">
            <p className="text-sm text-gray-500">Estimated Total</p>
            <p className="text-2xl font-bold">${getTotal().toFixed(2)}</p>
          </div>
        </div>

        {/* Shipping */}
        <div className="card">
          <h2 className="text-lg font-semibold mb-4">Shipping Address</h2>
          <div className="space-y-4">
            <div>
              <label className="label">Address Line 1</label>
              <input className="input" value={form.shippingAddressLine1} onChange={(e) => setForm({ ...form, shippingAddressLine1: e.target.value })} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label">City</label>
                <input className="input" value={form.shippingCity} onChange={(e) => setForm({ ...form, shippingCity: e.target.value })} />
              </div>
              <div>
                <label className="label">State</label>
                <input className="input" value={form.shippingState} onChange={(e) => setForm({ ...form, shippingState: e.target.value })} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label">Zip Code</label>
                <input className="input" value={form.shippingZipCode} onChange={(e) => setForm({ ...form, shippingZipCode: e.target.value })} />
              </div>
              <div>
                <label className="label">Country</label>
                <input className="input" value={form.shippingCountry} onChange={(e) => setForm({ ...form, shippingCountry: e.target.value })} />
              </div>
            </div>
          </div>
        </div>

        {/* Notes */}
        <div className="card">
          <h2 className="text-lg font-semibold mb-4">Notes</h2>
          <textarea
            className="input"
            rows={3}
            maxLength={500}
            placeholder="Optional order notes..."
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
        </div>

        {/* Submit */}
        <div className="flex justify-end gap-3">
          <button type="button" onClick={() => navigate('/orders')} className="btn-secondary">Cancel</button>
          <button type="submit" disabled={submitting} className="btn-primary">
            {submitting ? 'Creating...' : 'Create Order'}
          </button>
        </div>
      </form>
    </div>
  )
}
