import React, { useState, useEffect } from 'react';
import { 
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip, 
  BarChart, Bar, XAxis, YAxis, CartesianGrid 
} from 'recharts';
import { 
  TrendingUp, TrendingDown, Wallet, AlertTriangle, 
  ArrowUpRight, Plus, Download, RefreshCw 
} from 'lucide-react';

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#f43f5e', '#8b5cf6'];

function App() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/portfolio/summary');
      const result = await response.json();
      setData(result);
    } catch (error) {
      console.error('Error fetching data:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="dashboard-container" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <RefreshCw className="animate-spin" size={48} color="#6366f1" />
      </div>
    );
  }

  // Mock data if server is not running or empty
  const dashboardData = data || {
    totalValue: 125430.50,
    totalPnL: 15200.25,
    allocation: { STOCK: 85000, CRYPTO: 25000, OPTION: 5000, COMMODITY: 10430.50 },
    assets: [
      { symbol: 'TSLA', quantity: 10, marketValue: 2500.0, pnl: 450.0, pnlPercentage: 18.5, type: 'STOCK' },
      { symbol: 'BTC', quantity: 0.5, marketValue: 32000.0, pnl: 12000.0, pnlPercentage: 60.0, type: 'CRYPTO' },
      { symbol: 'GLD', quantity: 50, marketValue: 10430.5, pnl: 300.0, pnlPercentage: 2.8, type: 'COMMODITY' }
    ]
  };

  const pieData = dashboardData.top5Allocation 
    ? Object.keys(dashboardData.top5Allocation).map(key => ({
        name: key,
        value: dashboardData.top5Allocation[key]
      }))
    : Object.keys(dashboardData.allocation).map(key => ({
        name: key,
        value: dashboardData.allocation[key]
      }));

  return (
    <div className="dashboard-container">
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }}>
        <div>
          <h1 style={{ fontSize: '32px', marginBottom: '8px' }}>Portföy Özeti</h1>
          <p className="text-secondary">Hoş geldin, yatırım yolculuğun burada.</p>
        </div>
        <div style={{ display: 'flex', gap: '16px' }}>
          <button className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Plus size={20} /> Yeni İşlem
          </button>
          <button className="glass-card" style={{ padding: '12px 16px', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Download size={20} /> İçe Aktar
          </button>
        </div>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px', marginBottom: '40px' }}>
        <div className="glass-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '10px', background: 'rgba(99, 102, 241, 0.1)', borderRadius: '12px' }}>
              <Wallet className="text-primary" size={24} />
            </div>
            <span className="text-secondary">Toplam Varlık</span>
          </div>
          <h2 style={{ fontSize: '36px' }}>${dashboardData.totalValue.toLocaleString()}</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', marginTop: '12px' }}>
            <TrendingUp className="text-success" size={16} />
            <span className="text-success">+12.5%</span>
            <span className="text-secondary" style={{ marginLeft: '8px' }}>YTD</span>
          </div>
        </div>

        <div className="glass-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '10px', background: 'rgba(16, 185, 129, 0.1)', borderRadius: '12px' }}>
              <TrendingUp className="text-success" size={24} />
            </div>
            <span className="text-secondary">Toplam Kar/Zarar</span>
          </div>
          <h2 style={{ fontSize: '36px' }} className="text-success">+${dashboardData.totalPnL.toLocaleString()}</h2>
          <p className="text-secondary" style={{ marginTop: '12px' }}>Gerçekleşmemiş PnL</p>
        </div>

        <div className="glass-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '10px', background: 'rgba(244, 63, 94, 0.1)', borderRadius: '12px' }}>
              <AlertTriangle className="text-danger" size={24} />
            </div>
            <span className="text-secondary">En Büyük Risk</span>
          </div>
          <h2 style={{ fontSize: '36px' }}>{dashboardData.topRisk || 'Hesaplanıyor...'}</h2>
          <p className="text-secondary" style={{ marginTop: '12px' }}>Varlık yoğunluğu bazlı risk analizi.</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '24px' }}>
        <div className="glass-card">
          <h3>Varlık Dağılımı</h3>
          <div style={{ height: '300px', marginTop: '20px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={100}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {pieData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ background: '#1e293b', border: 'none', borderRadius: '12px', boxShadow: '0 10px 20px rgba(0,0,0,0.2)' }}
                  itemStyle={{ color: '#fff' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div style={{ marginTop: '20px' }}>
            {pieData.map((entry, index) => (
              <div key={entry.name} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: COLORS[index % COLORS.length] }}></div>
                  <span className="text-secondary">{entry.name}</span>
                </div>
                <span>%{(entry.value / dashboardData.totalValue * 100).toFixed(1)}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>Varlık Detayları</h3>
            <span className="text-primary" style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}>
              Tümünü Gör <ArrowUpRight size={16} />
            </span>
          </div>
          <table>
            <thead>
              <tr>
                <th>Sembol</th>
                <th>Miktar</th>
                <th>Değer</th>
                <th>PnL</th>
                <th>Risk Skoru</th>
              </tr>
            </thead>
            <tbody>
              {dashboardData.assets.map((asset) => (
                <tr key={asset.symbol}>
                  <td style={{ fontWeight: 600 }}>{asset.symbol}</td>
                  <td>{asset.quantity}</td>
                  <td>${asset.marketValue.toLocaleString()}</td>
                  <td className={asset.pnl >= 0 ? 'text-success' : 'text-danger'}>
                    {asset.pnl >= 0 ? '+' : ''}{asset.pnl.toLocaleString()} ({asset.pnlPercentage.toFixed(1)}%)
                  </td>
                  <td>
                    <div style={{ width: '60px', height: '6px', background: 'rgba(255,255,255,0.1)', borderRadius: '3px' }}>
                      <div style={{ 
                        width: asset.marketValue / dashboardData.totalValue * 100 + '%', 
                        height: '100%', 
                        background: asset.pnl >= 0 ? 'var(--success)' : 'var(--danger)',
                        borderRadius: '3px'
                      }}></div>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default App;
