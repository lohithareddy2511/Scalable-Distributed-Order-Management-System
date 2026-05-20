import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, CreditCard } from 'lucide-react'
import { orderApi, paymentApi } from '../api'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'
import Modal from '../components/Modal'

const STATUS_TRANSITIONS = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
}

const PAYMENT_METHODS = ['CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'BANK_TRANSFER']

export default function OrderDetail() {
  const { id } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [showPayment, setShowPayment] = useState(false)
  const [paymentForm, setPaymentForm] = useState({ paymentMethod: 'CREDIT_CARD', amount: '' })

  const fetchOrder = async () => {
    try {
      const res = await orderApi.getById(id)
      setOrder(res.data.data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchOrder() }, [id])

  const handleStatusUpdate = async (newStatus) => {
    if (!confirm(`Update status to ${newStatus}?`)) return
    try {
      await orderApi.updateStatus(id, { status: newStatus })
      fetchOrder()
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update status')
    }
  }

  const handleCancel = async () => {
    const reason = prompt('Reason for cancellation:')
    if (reason === null) return
    try {
      await orderApi.cancel(id, reason)
      fetchOrder()
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to cancel')
    }
  }

  const handlePayment = async (e) => {
    e.preventDefault()
    try {
      await paymentApi.process({
        orderId: id,
        paymentMethod: paymentForm.paymentMethod,
        amount: parseFloat(paymentForm.amount),
      })
      setShowPayment(false)
      fetchOrder()
    } catch (err) {
      alert(err.response?.data?.message || 'Payment failed')
    }
  }

  if (loading) return <LoadingSpinner />
  if (!order) return <p className="text-gray-500">Order not found.</p>

  const availableTransitions = STATUS_TRANSITIONS[order.status] || []

  return (
    <div>
      <Link to="/orders" className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-4">
        <ArrowLeft size={16} /> Back to Orders
      </Link>

      {/* Order Header */}
      <div className="card mb-6">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold">{order.orderNumber}</h1>
            <p className="text-sm text-gray-500 mt-1">Customer: {order.customerName}</p>
          </div>
          <div className="text-right">
            <StatusBadge status={order.status} />
            <p className="text-2xl font-bold mt-2">${order.totalAmount?.toFixed(2)}</p>
          </div>
        </div>

        {/* Shipping Address */}
        {order.shippingAddressLine1 && (
          <div className="mt-4 pt-4 border-t text-sm text-gray-600">
            <p className="font-medium text-gray-700 mb-1">Shipping Address</p>
            <p>{order.shippingAddressLine1}</p>
            {order.shippingAddressLine2 && <p>{order.shippingAddressLine2}</p>}
            <p>{[order.shippingCity, order.shippingState, order.shippingZipCode].filter(Boolean).join(', ')}</p>
            {order.shippingCountry && <p>{order.shippingCountry}</p>}
          </div>
        )}

        {order.notes && (
          <div className="mt-4 pt-4 border-t text-sm">
            <p className="font-medium text-gray-700 mb-1">Notes</p>
            <p className="text-gray-600">{order.notes}</p>
          </div>
        )}

        {/* Actions */}
        <div className="mt-4 pt-4 border-t flex flex-wrap gap-2">
          {availableTransitions.filter(s => s !== 'CANCELLED').map((status) => (
            <button key={status} onClick={() => handleStatusUpdate(status)} className="btn-primary text-xs">
              Mark as {status}
            </button>
          ))}
          {availableTransitions.includes('CANCELLED') && (
            <button onClick={handleCancel} className="btn-danger text-xs">Cancel Order</button>
          )}
          {order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
            <button onClick={() => { setPaymentForm({ ...paymentForm, amount: order.totalAmount }); setShowPayment(true) }} className="btn-success text-xs">
              <CreditCard size={14} /> Process Payment
            </button>
          )}
        </div>
      </div>

      {/* Order Items */}
      <div className="card mb-6">
        <h2 className="text-lg font-semibold mb-4">Items</h2>
        <table className="w-full text-sm">
          <thead className="border-b">
            <tr>
              <th className="text-left py-2 font-medium text-gray-600">Product</th>
              <th className="text-left py-2 font-medium text-gray-600">SKU</th>
              <th className="text-right py-2 font-medium text-gray-600">Qty</th>
              <th className="text-right py-2 font-medium text-gray-600">Unit Price</th>
              <th className="text-right py-2 font-medium text-gray-600">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {order.items?.map((item) => (
              <tr key={item.id}>
                <td className="py-2 font-medium">{item.productName}</td>
                <td className="py-2 text-gray-500 font-mono text-xs">{item.productSku}</td>
                <td className="py-2 text-right">{item.quantity}</td>
                <td className="py-2 text-right">${item.unitPrice?.toFixed(2)}</td>
                <td className="py-2 text-right font-medium">${item.totalPrice?.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot className="border-t">
            <tr>
              <td colSpan={4} className="py-2 text-right font-semibold">Total:</td>
              <td className="py-2 text-right font-bold">${order.totalAmount?.toFixed(2)}</td>
            </tr>
          </tfoot>
        </table>
      </div>

      {/* Payments */}
      {order.payments && order.payments.length > 0 && (
        <div className="card">
          <h2 className="text-lg font-semibold mb-4">Payments</h2>
          <div className="space-y-3">
            {order.payments.map((payment) => (
              <div key={payment.id} className="flex items-center justify-between p-3 rounded-lg border">
                <div>
                  <p className="text-sm font-medium">{payment.paymentMethod?.replace('_', ' ')}</p>
                  <p className="text-xs text-gray-500">{payment.transactionReference || 'No reference'}</p>
                </div>
                <div className="text-right">
                  <StatusBadge status={payment.status} />
                  <p className="text-sm font-medium mt-1">${payment.amount?.toFixed(2)}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Payment Modal */}
      <Modal open={showPayment} onClose={() => setShowPayment(false)} title="Process Payment">
        <form onSubmit={handlePayment} className="space-y-4">
          <div>
            <label className="label">Payment Method *</label>
            <select
              className="input"
              value={paymentForm.paymentMethod}
              onChange={(e) => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })}
            >
              {PAYMENT_METHODS.map((m) => (
                <option key={m} value={m}>{m.replace('_', ' ')}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label">Amount *</label>
            <input
              className="input"
              type="number"
              step="0.01"
              min="0.01"
              required
              value={paymentForm.amount}
              onChange={(e) => setPaymentForm({ ...paymentForm, amount: e.target.value })}
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={() => setShowPayment(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-success">Process Payment</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
