import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Users, Package, ShoppingCart, AlertTriangle, TrendingUp, DollarSign } from 'lucide-react'
import { customerApi, productApi, orderApi } from '../api'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [recentOrders, setRecentOrders] = useState([])
  const [lowStock, setLowStock] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function fetchData() {
      try {
        const [customersRes, productsRes, ordersRes, lowStockRes] = await Promise.all([
          customerApi.getAll(0, 1),
          productApi.getAll(0, 1),
          orderApi.getAll(0, 5),
          productApi.getLowStock(10),
        ])

        setStats({
          customers: customersRes.data.data?.totalElements || 0,
          products: productsRes.data.data?.totalElements || 0,
          orders: ordersRes.data.data?.totalElements || 0,
        })
        setRecentOrders(ordersRes.data.data?.content || [])
        setLowStock(lowStockRes.data.data || [])
      } catch (err) {
        console.error('Failed to fetch dashboard data:', err)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  if (loading) return <LoadingSpinner />

  const statCards = [
    { label: 'Customers', value: stats?.customers, icon: Users, color: 'text-blue-600 bg-blue-50', to: '/customers' },
    { label: 'Products', value: stats?.products, icon: Package, color: 'text-green-600 bg-green-50', to: '/products' },
    { label: 'Orders', value: stats?.orders, icon: ShoppingCart, color: 'text-purple-600 bg-purple-50', to: '/orders' },
    { label: 'Low Stock Items', value: lowStock.length, icon: AlertTriangle, color: 'text-orange-600 bg-orange-50', to: '/products' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Dashboard</h1>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {statCards.map(({ label, value, icon: Icon, color, to }) => (
          <Link key={label} to={to} className="card hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{label}</p>
                <p className="text-2xl font-bold mt-1">{value}</p>
              </div>
              <div className={`p-3 rounded-lg ${color}`}>
                <Icon size={24} />
              </div>
            </div>
          </Link>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Orders */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">Recent Orders</h2>
            <Link to="/orders" className="text-sm text-blue-600 hover:underline">View all</Link>
          </div>
          {recentOrders.length === 0 ? (
            <p className="text-sm text-gray-500">No orders yet.</p>
          ) : (
            <div className="space-y-3">
              {recentOrders.map((order) => (
                <Link
                  key={order.id}
                  to={`/orders/${order.id}`}
                  className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <div>
                    <p className="text-sm font-medium">{order.orderNumber}</p>
                    <p className="text-xs text-gray-500">{order.customerName}</p>
                  </div>
                  <div className="text-right">
                    <StatusBadge status={order.status} />
                    <p className="text-xs text-gray-500 mt-1">${order.totalAmount?.toFixed(2)}</p>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Low Stock Alert */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">Low Stock Alerts</h2>
            <Link to="/products" className="text-sm text-blue-600 hover:underline">View all</Link>
          </div>
          {lowStock.length === 0 ? (
            <p className="text-sm text-gray-500">All products are well-stocked.</p>
          ) : (
            <div className="space-y-3">
              {lowStock.slice(0, 5).map((product) => (
                <div key={product.id} className="flex items-center justify-between p-3 rounded-lg bg-orange-50">
                  <div>
                    <p className="text-sm font-medium">{product.name}</p>
                    <p className="text-xs text-gray-500">SKU: {product.sku}</p>
                  </div>
                  <span className="badge bg-orange-100 text-orange-800">
                    {product.stockQuantity} left
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
