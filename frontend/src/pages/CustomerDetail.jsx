import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, Mail, Phone, MapPin } from 'lucide-react'
import { customerApi, orderApi } from '../api'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'

export default function CustomerDetail() {
  const { id } = useParams()
  const [customer, setCustomer] = useState(null)
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function fetch() {
      try {
        const [custRes, ordersRes] = await Promise.all([
          customerApi.getById(id),
          orderApi.getByCustomer(id, 0, 10),
        ])
        setCustomer(custRes.data.data)
        setOrders(ordersRes.data.data?.content || [])
      } catch (err) {
        console.error(err)
      } finally {
        setLoading(false)
      }
    }
    fetch()
  }, [id])

  if (loading) return <LoadingSpinner />
  if (!customer) return <p className="text-gray-500">Customer not found.</p>

  return (
    <div>
      <Link to="/customers" className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-4">
        <ArrowLeft size={16} /> Back to Customers
      </Link>

      <div className="card mb-6">
        <h1 className="text-2xl font-bold">{customer.firstName} {customer.lastName}</h1>
        <div className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          <div className="flex items-center gap-2 text-gray-600">
            <Mail size={16} /> {customer.email}
          </div>
          {customer.phone && (
            <div className="flex items-center gap-2 text-gray-600">
              <Phone size={16} /> {customer.phone}
            </div>
          )}
          {customer.city && (
            <div className="flex items-center gap-2 text-gray-600">
              <MapPin size={16} /> {[customer.city, customer.state, customer.country].filter(Boolean).join(', ')}
            </div>
          )}
        </div>
        <p className="text-xs text-gray-400 mt-4">
          Created: {new Date(customer.createdAt).toLocaleDateString()}
        </p>
      </div>

      <div className="card">
        <h2 className="text-lg font-semibold mb-4">Order History</h2>
        {orders.length === 0 ? (
          <p className="text-sm text-gray-500">No orders for this customer.</p>
        ) : (
          <div className="space-y-3">
            {orders.map((order) => (
              <Link
                key={order.id}
                to={`/orders/${order.id}`}
                className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 border"
              >
                <div>
                  <p className="font-medium text-sm">{order.orderNumber}</p>
                  <p className="text-xs text-gray-500">{new Date(order.createdAt).toLocaleDateString()}</p>
                </div>
                <div className="text-right">
                  <StatusBadge status={order.status} />
                  <p className="text-sm font-medium mt-1">${order.totalAmount?.toFixed(2)}</p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
