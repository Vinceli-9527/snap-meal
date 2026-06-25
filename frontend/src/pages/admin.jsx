import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { jsonOptions, readJson, requestJson } from '../api.js';
import '../styles.css';

const views = [
  ['dashboard', '经营概览'],
  ['dishes', '菜品管理'],
  ['categories', '分类管理'],
  ['orders', '订单管理']
];

function AdminApp() {
  const [token, setToken] = useState(() => localStorage.adminToken || '');
  const [loggedIn, setLoggedIn] = useState(false);
  const [message, setMessage] = useState('');
  const [view, setView] = useState('dashboard');

  const showLogin = useCallback((notice = '') => {
    localStorage.removeItem('adminToken');
    setToken('');
    setLoggedIn(false);
    setMessage(notice);
  }, []);

  const api = useCallback(
    async (path, options = {}) => {
      try {
        return await requestJson(`/api/admin${path}`, jsonOptions(options, { token }));
      } catch (error) {
        if (error.status === 401) {
          showLogin('登录状态已过期，请重新登录。');
          error.auth = true;
        }
        throw error;
      }
    },
    [showLogin, token]
  );

  useEffect(() => {
    if (new URLSearchParams(location.search).has('logout')) {
      history.replaceState(null, '', location.pathname);
      showLogin();
      return;
    }
    if (!token) {
      showLogin();
      return;
    }
    api('/auth/session')
      .then(() => setLoggedIn(true))
      .catch((error) => {
        if (!error.auth) showLogin(error.message);
      });
  }, [api, showLogin, token]);

  async function handleLogin(event) {
    event.preventDefault();
    setMessage('');
    const form = Object.fromEntries(new FormData(event.currentTarget));
    try {
      const response = await fetch('/api/admin/auth/login', jsonOptions({
        method: 'POST',
        body: JSON.stringify(form)
      }));
      const result = await readJson(response);
      if (!response.ok || !result.success) throw new Error(result.message || '登录失败');
      localStorage.adminToken = result.data.token;
      setToken(result.data.token);
      setLoggedIn(true);
      setView('dashboard');
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function logout() {
    try {
      await api('/auth/logout', { method: 'POST' });
      showLogin('你已安全退出。');
    } catch (error) {
      if (!error.auth) alert(`退出失败：${error.message}`);
    }
  }

  async function downloadReport() {
    try {
      const response = await fetch('/api/admin/reports/export', { headers: { token } });
      if (response.status === 401) {
        showLogin('登录状态已过期，请重新登录。');
        return;
      }
      if (!response.ok) throw new Error('报表导出失败');
      const blob = await response.blob();
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = '运营数据报表.xlsx';
      link.click();
      URL.revokeObjectURL(link.href);
    } catch (error) {
      alert(error.message);
    }
  }

  if (!loggedIn) {
    return <AdminLogin message={message} onSubmit={handleLogin} />;
  }

  const title = views.find(([key]) => key === view)?.[1] || '经营概览';
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          snap-meal
          <small>OPERATION CONSOLE</small>
        </div>
        <nav>
          {views.map(([key, label]) => (
            <button key={key} className={view === key ? 'active' : ''} onClick={() => setView(key)}>
              {label}
            </button>
          ))}
          <button onClick={downloadReport}>导出运营报表</button>
          <button onClick={logout}>退出登录</button>
          <a className="sidebar-link" href="/">回到主页</a>
        </nav>
      </aside>
      <main className="content">
        <div className="topline">
          <h1>{title}</h1>
          <span className="pill">● 店铺营业中</span>
        </div>
        <AdminView view={view} api={api} />
      </main>
    </div>
  );
}

function AdminLogin({ message, onSubmit }) {
  const [loading, setLoading] = useState(false);
  async function submit(event) {
    setLoading(true);
    await onSubmit(event);
    setLoading(false);
  }
  return (
    <div className="login">
      <form className="login-card" onSubmit={submit}>
        <div className="eyebrow">OPERATION CONSOLE</div>
        <h1>登录运营台</h1>
        {message ? <p className="auth-message" role="alert">{message}</p> : null}
        <div className="field">
          <label>用户名</label>
          <input name="username" defaultValue="admin" autoComplete="username" required />
        </div>
        <div className="field">
          <label>密码</label>
          <input type="password" name="password" defaultValue="123456" autoComplete="current-password" required />
        </div>
        <button className="btn primary" disabled={loading}>{loading ? '正在登录...' : '进入系统'}</button>
        <p className="notice">实验默认账号：admin / 123456</p>
        <a className="btn home-link" href="/">回到主页</a>
      </form>
    </div>
  );
}

function AdminView({ view, api }) {
  const [state, setState] = useState({ view: '', data: null });
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const reload = () => setReloadKey((value) => value + 1);

  useEffect(() => {
    let alive = true;
    setState({ view, data: null });
    setError('');
    const loaders = {
      dashboard: () => api('/reports/overview'),
      dishes: () => api('/dishes'),
      categories: () => api('/categories?type=1'),
      orders: () => api('/orders')
    };
    loaders[view]()
      .then((result) => alive && setState({ view, data: result }))
      .catch((err) => alive && !err.auth && setError(err.message));
    return () => {
      alive = false;
    };
  }, [api, reloadKey, view]);

  if (error) return <div className="panel">{error}</div>;
  const data = state.view === view ? state.data : null;
  if (!data) return <div className="panel">正在加载...</div>;
  if (view === 'dashboard') return <Dashboard data={data} />;
  if (view === 'dishes') return <Dishes rows={Array.isArray(data) ? data : []} api={api} reload={reload} />;
  if (view === 'categories') return <Categories rows={Array.isArray(data) ? data : []} api={api} reload={reload} />;
  return <Orders rows={Array.isArray(data) ? data : []} api={api} reload={reload} />;
}

function Dashboard({ data }) {
  useEffect(() => {
    window.SnapMealDashboard?.render();
    return () => window.SnapMealDashboard?.detach();
  }, [data]);

  return <div id="dashboard-mount" />;
}

function LegacyDashboard({ data }) {
  return (
    <>
      <div className="grid">
        <Metric label="累计营业额" value={`￥${data.turnover || 0}`} />
        <Metric label="已完成订单" value={data.validOrders} />
        <Metric label="注册用户" value={data.users} />
        <Metric label="待接单" value={data.pendingOrders} />
      </div>
      <div className="panel">
        <div className="panel-head"><h2>系统状态</h2></div>
        <p>数据库、订单服务、文件存储均已连接。微信支付与 OSS 当前运行在本地模拟模式。</p>
      </div>
    </>
  );
}

function Metric({ label, value }) {
  return <div className="metric"><span>{label}</span><strong>{value}</strong></div>;
}

function Dishes({ rows, api, reload }) {
  const [categories, setCategories] = useState([]);
  const [editing, setEditing] = useState(null);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let alive = true;
    api('/categories?type=1')
      .then((result) => alive && setCategories(Array.isArray(result) ? result : []))
      .catch((error) => alive && !error.auth && setFormError(error.message));
    return () => {
      alive = false;
    };
  }, [api]);

  function startAdd() {
    setFormError('');
    setEditing({
      id: null,
      name: '',
      price: '',
      categoryId: categories[0]?.id ? String(categories[0].id) : '',
      status: 1,
      image: '',
      description: ''
    });
  }

  function startEdit(row) {
    setFormError('');
    setEditing({
      id: row.id,
      name: row.name || '',
      price: row.price ?? '',
      categoryId: String(row.category_id || row.categoryId || ''),
      status: Number(row.status) === 0 ? 0 : 1,
      image: row.image || '',
      description: row.description || ''
    });
  }

  function updateDraft(field, value) {
    setEditing((draft) => ({ ...draft, [field]: value }));
  }

  async function saveDish(event) {
    event.preventDefault();
    if (!editing) return;
    const name = editing.name.trim();
    const price = Number(editing.price);
    const categoryId = Number(editing.categoryId);
    if (!name) {
      setFormError('请输入菜品名称');
      return;
    }
    if (!categoryId) {
      setFormError('请选择菜品分类');
      return;
    }
    if (!Number.isFinite(price) || price < 0) {
      setFormError('请输入正确的菜品价格');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const payload = {
        name,
        price,
        categoryId,
        status: Number(editing.status),
        image: editing.image || '',
        description: editing.description || ''
      };
      await api(editing.id ? `/dishes/${editing.id}` : '/dishes', {
        method: editing.id ? 'PUT' : 'POST',
        body: JSON.stringify(payload)
      });
      setEditing(null);
      reload();
    } catch (error) {
      if (!error.auth) setFormError(error.message);
    } finally {
      setSaving(false);
    }
  }

  async function toggleDish(id, value) {
    await api(`/dishes/${id}/status?value=${value}`, { method: 'PATCH' });
    reload();
  }

  return (
    <div className="panel">
      <div className="panel-head">
        <h2>菜品目录</h2>
        <button className="btn primary small" onClick={startAdd}>新增菜品</button>
      </div>
      {editing ? (
        <form className="dish-editor" onSubmit={saveDish}>
          <div className="field">
            <label>菜品名称</label>
            <input value={editing.name} onChange={(event) => updateDraft('name', event.target.value)} required />
          </div>
          <div className="field">
            <label>分类</label>
            <select value={editing.categoryId} onChange={(event) => updateDraft('categoryId', event.target.value)} required>
              <option value="" disabled>请选择分类</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>价格</label>
            <input type="number" min="0" step="0.01" value={editing.price} onChange={(event) => updateDraft('price', event.target.value)} required />
          </div>
          <div className="field">
            <label>状态</label>
            <select value={editing.status} onChange={(event) => updateDraft('status', event.target.value)}>
              <option value="1">起售</option>
              <option value="0">停售</option>
            </select>
          </div>
          {formError ? <p className="form-error" role="alert">{formError}</p> : null}
          <div className="form-actions">
            <button className="btn primary small" disabled={saving || !categories.length}>{saving ? '保存中...' : '保存'}</button>
            <button className="btn small" type="button" onClick={() => setEditing(null)} disabled={saving}>取消</button>
          </div>
        </form>
      ) : formError ? <p className="form-error" role="alert">{formError}</p> : null}
      <table>
        <thead><tr><th>名称</th><th>分类</th><th>价格</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.name}</td>
              <td>{row.category_name}</td>
              <td>￥{row.price}</td>
              <td><span className={`status ${row.status ? '' : 'off'}`}>{row.status ? '起售' : '停售'}</span></td>
              <td>
                <div className="table-actions">
                  <button className="btn small" onClick={() => startEdit(row)}>编辑</button>
                  <button className="btn small" onClick={() => toggleDish(row.id, row.status ? 0 : 1)}>{row.status ? '停售' : '起售'}</button>
                </div>
              </td>
            </tr>
          ))}
          {!rows.length ? (
            <tr>
              <td colSpan="5"><div className="empty compact">还没有菜品，先新增一个菜品。</div></td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </div>
  );
}

function Categories({ rows, api, reload }) {
  const [editing, setEditing] = useState(null);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  function startAdd() {
    setFormError('');
    setEditing({
      id: null,
      name: '',
      status: 1
    });
  }

  function startEdit(row) {
    setFormError('');
    setEditing({
      id: row.id,
      name: row.name || '',
      status: Number(row.status) === 0 ? 0 : 1
    });
  }

  function updateDraft(field, value) {
    setEditing((draft) => ({ ...draft, [field]: value }));
  }

  async function saveCategory(event) {
    event.preventDefault();
    if (!editing) return;
    const name = editing.name.trim();
    if (!name) {
      setFormError('请输入分类名称');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const payload = {
        name,
        status: Number(editing.status)
      };
      await api(editing.id ? `/categories/${editing.id}` : '/categories', {
        method: editing.id ? 'PUT' : 'POST',
        body: JSON.stringify(payload)
      });
      setEditing(null);
      reload();
    } catch (error) {
      if (!error.auth) setFormError(error.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="panel">
      <div className="panel-head">
        <h2>分类</h2>
        <button className="btn primary small" onClick={startAdd}>新增分类</button>
      </div>
      {editing ? (
        <form className="management-editor category-editor" onSubmit={saveCategory}>
          <div className="field">
            <label>分类名称</label>
            <input value={editing.name} onChange={(event) => updateDraft('name', event.target.value)} required />
          </div>
          <div className="field">
            <label>状态</label>
            <select value={editing.status} onChange={(event) => updateDraft('status', event.target.value)}>
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </select>
          </div>
          {formError ? <p className="form-error" role="alert">{formError}</p> : null}
          <div className="form-actions">
            <button className="btn primary small" disabled={saving}>{saving ? '保存中...' : '保存'}</button>
            <button className="btn small" type="button" onClick={() => setEditing(null)} disabled={saving}>取消</button>
          </div>
        </form>
      ) : formError ? <p className="form-error" role="alert">{formError}</p> : null}
      <table>
        <thead><tr><th>名称</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.name}</td>
              <td>{row.status ? '启用' : '禁用'}</td>
              <td><button className="btn small" onClick={() => startEdit(row)}>编辑</button></td>
            </tr>
          ))}
          {!rows.length ? (
            <tr>
              <td colSpan="3"><div className="empty compact">还没有分类，先新增一个分类。</div></td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </div>
  );
}

function Orders({ rows, api, reload }) {
  const status = useMemo(() => ['', '待付款', '待接单', '已接单', '派送中', '已完成', '已取消'], []);
  async function orderAction(id, action) {
    await api(`/orders/${id}/${action}`, { method: 'POST' });
    reload();
  }
  return (
    <div className="panel">
      <div className="panel-head"><h2>最近订单</h2></div>
      {rows.length ? (
        <table>
          <thead><tr><th>订单号</th><th>收货人</th><th>金额</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{row.number}</td>
                <td>{row.consignee}</td>
                <td>￥{row.amount}</td>
                <td>{status[row.status]}</td>
                <td>
                  {row.status === 2 ? <button className="btn small" onClick={() => orderAction(row.id, 'confirm')}>接单</button> : null}
                  {row.status === 3 ? <button className="btn small" onClick={() => orderAction(row.id, 'deliver')}>派送</button> : null}
                  {row.status === 4 ? <button className="btn small" onClick={() => orderAction(row.id, 'complete')}>完成</button> : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : <div className="empty">还没有订单</div>}
    </div>
  );
}

createRoot(document.getElementById('root')).render(<AdminApp />);
