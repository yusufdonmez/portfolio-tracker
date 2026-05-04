import React, { useState, useEffect, useRef } from 'react';
import { 
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip, 
  BarChart, Bar, XAxis, YAxis, CartesianGrid 
} from 'recharts';
import { 
  TrendingUp, TrendingDown, Wallet, AlertTriangle, 
  ArrowUpRight, Plus, Download, RefreshCw, ExternalLink, Banknote, X, Upload, CheckCircle2
} from 'lucide-react';

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#f43f5e', '#8b5cf6', '#06b6d4', '#f97316', '#a855f7', '#ec4899', '#64748b'];

function App() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [importing, setImporting] = useState(false);
  const [importStatus, setImportStatus] = useState(null);
  const fileInputRef = useRef(null);

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

  const handleFileChange = (e) => {
    setSelectedFiles(Array.from(e.target.files));
  };

  const handleImport = async () => {
    if (selectedFiles.length === 0) return;
    
    setImporting(true);
    setImportStatus(null);
    
    const formData = new FormData();
    selectedFiles.forEach(file => {
      formData.append('files', file);
    });

    try {
      const response = await fetch('http://localhost:8080/api/import', {
        method: 'POST',
        body: formData,
      });
      const result = await response.json();
      if (result.success) {
        setImportStatus({ type: 'success', message: result.message });
        fetchData(); // Refresh dashboard
        setTimeout(() => {
          setIsImportModalOpen(false);
          setImportStatus(null);
          setSelectedFiles([]);
        }, 2000);
      } else {
        setImportStatus({ type: 'error', message: 'İthalat başarısız oldu.' });
      }
    } catch (error) {
      setImportStatus({ type: 'error', message: 'Bir hata oluştu.' });
    } finally {
      setImporting(false);
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  };

  if (loading) {
    return (
      <div className="dashboard-container" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <RefreshCw className="animate-spin" size={48} color="#6366f1" />
      </div>
    );
  }

  const dashboardData = data || {
    totalValue: 0,
    totalPnL: 0,
    totalCash: 0,
    allocation: {},
    assets: []
  };

  const pieData = dashboardData.top10Allocation 
    ? Object.keys(dashboardData.top10Allocation).map(key => ({
        name: key,
        value: dashboardData.top10Allocation[key]
      }))
    : Object.keys(dashboardData.allocation).map(key => ({
        name: key,
        value: dashboardData.allocation[key]
      }));

  return (
    <div className="dashboard-container">
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }}>
        <div>
          <h1 style={{ fontSize: '32px', marginBottom: '8px' }}>Portföy Özeti <span style={{ fontSize: '14px', color: 'var(--text-secondary)', fontWeight: '400' }}>v1.1.2</span></h1>
          <p className="text-secondary">Hoş geldin, yatırım yolculuğun burada.</p>
        </div>
        <div style={{ display: 'flex', gap: '16px' }}>
          <button className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Plus size={20} /> Yeni İşlem
          </button>
          <button 
            className="glass-card" 
            style={{ padding: '12px 16px', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}
            onClick={() => setIsImportModalOpen(true)}
          >
            <Download size={20} /> İçe Aktar
          </button>
        </div>
      </header>

      {/* Import Modal */}
      {isImportModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.8)', backdropFilter: 'blur(8px)',
          display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, padding: '20px'
        }}>
          <div className="glass-card" style={{ width: '100%', maxWidth: '500px', padding: '32px', position: 'relative' }}>
            <button 
              onClick={() => setIsImportModalOpen(false)}
              style={{ position: 'absolute', top: '20px', right: '20px', background: 'none', border: 'none', color: '#64748b', cursor: 'pointer' }}
            >
              <X size={24} />
            </button>
            
            <h2 style={{ marginBottom: '24px' }}>Veri İçe Aktar</h2>
            <p className="text-secondary" style={{ marginBottom: '24px' }}>
              Midas, IBKR veya Akbank CSV dosyalarınızı seçin. Mükerrer kayıtlar otomatik olarak ayıklanacaktır.
            </p>

            <div 
              onClick={() => fileInputRef.current.click()}
              style={{
                border: '2px dashed rgba(99, 102, 241, 0.3)',
                borderRadius: '16px',
                padding: '40px 20px',
                textAlign: 'center',
                cursor: 'pointer',
                marginBottom: '24px',
                background: 'rgba(99, 102, 241, 0.05)'
              }}
            >
              <Upload className="text-primary" size={48} style={{ marginBottom: '16px', margin: '0 auto' }} />
              <p style={{ fontWeight: 500 }}>Dosyaları Seçmek İçin Tıklayın</p>
              <p className="text-secondary" style={{ fontSize: '14px', marginTop: '8px' }}>veya sürükleyip bırakın</p>
              <input 
                type="file" 
                multiple 
                ref={fileInputRef} 
                onChange={handleFileChange} 
                style={{ display: 'none' }}
                accept=".csv"
              />
            </div>

            {selectedFiles.length > 0 && (
              <div style={{ marginBottom: '24px' }}>
                <p style={{ fontWeight: 500, marginBottom: '12px' }}>Seçilen Dosyalar ({selectedFiles.length}):</p>
                <div style={{ maxHeight: '120px', overflowY: 'auto' }}>
                  {selectedFiles.map(file => (
                    <div key={file.name} style={{ fontSize: '14px', padding: '8px 12px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <CheckCircle2 size={14} className="text-success" /> {file.name}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {importStatus && (
              <div style={{ 
                padding: '12px', borderRadius: '8px', marginBottom: '24px',
                background: importStatus.type === 'success' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(244, 63, 94, 0.1)',
                color: importStatus.type === 'success' ? 'var(--success)' : 'var(--danger)',
                display: 'flex', alignItems: 'center', gap: '8px'
              }}>
                {importStatus.type === 'success' ? <CheckCircle2 size={20} /> : <AlertTriangle size={20} />}
                {importStatus.message}
              </div>
            )}

            <button 
              className="btn-primary" 
              style={{ width: '100%', padding: '16px', fontSize: '16px' }}
              onClick={handleImport}
              disabled={selectedFiles.length === 0 || importing}
            >
              {importing ? <RefreshCw className="animate-spin" size={20} style={{ margin: '0 auto' }} /> : 'İçe Aktarmayı Başlat'}
            </button>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '24px', marginBottom: '40px' }}>
        <div className="glass-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '10px', background: 'rgba(99, 102, 241, 0.1)', borderRadius: '12px' }}>
              <Wallet className="text-primary" size={24} />
            </div>
            <span className="text-secondary">Toplam Varlık</span>
          </div>
          <h2 style={{ fontSize: '32px' }}>{formatCurrency(dashboardData.totalValue)}</h2>
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
          <h2 style={{ fontSize: '32px' }} className={dashboardData.totalPnL >= 0 ? "text-success" : "text-danger"}>
            {dashboardData.totalPnL >= 0 ? '+' : ''}{formatCurrency(dashboardData.totalPnL)}
          </h2>
          <p className="text-secondary" style={{ marginTop: '12px' }}>Gerçekleşmemiş PnL</p>
        </div>

        <div className="glass-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '10px', background: 'rgba(245, 158, 11, 0.1)', borderRadius: '12px' }}>
              <Banknote className="text-warning" size={24} />
            </div>
            <span className="text-secondary">Nakit Bakiyesi</span>
          </div>
          <h2 style={{ fontSize: '32px' }}>{formatCurrency(dashboardData.totalCash || 0)}</h2>
          <p className="text-secondary" style={{ marginTop: '12px' }}>Kullanılabilir Nakit</p>
        </div>

        <div className="glass-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <div style={{ padding: '10px', background: 'rgba(244, 63, 94, 0.1)', borderRadius: '12px' }}>
              <AlertTriangle className="text-danger" size={24} />
            </div>
            <span className="text-secondary">En Büyük Risk</span>
          </div>
          <h2 style={{ fontSize: '32px' }}>{dashboardData.topRisk || 'None'}</h2>
          <p className="text-secondary" style={{ marginTop: '12px' }}>Varlık yoğunluğu analizi.</p>
        </div>
      </div>

      <div className="main-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '24px' }}>
        <div className="glass-card" style={{ display: 'flex', flexDirection: 'column' }}>
          <h3>Varlık Dağılımı (Top 10)</h3>
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
                  formatter={(value) => formatCurrency(value)}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div style={{ marginTop: '20px', maxHeight: '300px', overflowY: 'auto' }}>
            {pieData.map((entry, index) => (
              <div key={entry.name} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', paddingRight: '10px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: COLORS[index % COLORS.length] }}></div>
                  <span className="text-secondary" style={{ fontSize: '14px' }}>{entry.name}</span>
                </div>
                <span style={{ fontSize: '14px' }}>%{(entry.value / dashboardData.totalValue * 100).toFixed(1)}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <h3>Varlık Detayları</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>Sembol</th>
                <th>Miktar</th>
                <th>Fiyat</th>
                <th>Değer</th>
                <th>PnL</th>
              </tr>
            </thead>
            <tbody>
              {dashboardData.assets.map((asset) => (
                <tr key={asset.symbol}>
                  <td>
                    <a 
                      href={`https://finance.yahoo.com/quote/${asset.symbol}`} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="ticker-link"
                      style={{ display: 'flex', alignItems: 'center', gap: '4px', textDecoration: 'none', color: 'inherit', fontWeight: 600 }}
                    >
                      {asset.symbol} <ExternalLink size={14} className="text-primary" />
                    </a>
                  </td>
                  <td>{asset.quantity.toFixed(asset.type === 'CRYPTO' ? 4 : 2)}</td>
                  <td style={{ fontWeight: 500 }}>{formatCurrency(asset.currentPrice)}</td>
                  <td>{formatCurrency(asset.marketValue)}</td>
                  <td className={asset.pnl >= 0 ? 'text-success' : 'text-danger'}>
                    {asset.pnl >= 0 ? '+' : ''}{formatCurrency(asset.pnl)} ({asset.pnlPercentage.toFixed(1)}%)
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
