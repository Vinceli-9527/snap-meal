import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { jsonOptions, readJson, requestJson } from '../api.js';
import '../styles.css';

marked.setOptions({ gfm: true, breaks: true });

// 经营问答的回答按 Markdown 渲染（表格/加粗/列表等）。
// 大模型输出属不可信输入：marked 转 HTML 后必须过 DOMPurify 消毒，
// 并禁掉对纯文本回答无意义的激活/嵌入标签与行内样式。
function MarkdownText({ text }) {
  const html = useMemo(
    () =>
      DOMPurify.sanitize(marked.parse(text || ''), {
        FORBID_TAGS: ['img', 'iframe', 'object', 'embed', 'form', 'input', 'button', 'textarea', 'select', 'video', 'audio', 'svg', 'math'],
        FORBID_ATTR: ['style']
      }),
    [text]
  );
  return <div className="qa-text qa-md" dangerouslySetInnerHTML={{ __html: html }} />;
}

const views = [
  ['dashboard', '经营概览'],
  ['dishes', '菜品管理'],
  ['categories', '分类管理'],
  ['orders', '订单管理'],
  ['agent', '经营问答']
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
      orders: () => api('/orders'),
      agent: () => Promise.resolve(null)
    };
    loaders[view]()
      .then((result) => alive && setState({ view, data: result }))
      .catch((err) => alive && !err.auth && setError(err.message));
    return () => {
      alive = false;
    };
  }, [api, reloadKey, view]);

  if (error) return <div className="panel">{error}</div>;
  if (view === 'orders') return <Orders api={api} />;
  if (view === 'agent') return <AgentQA api={api} />;
  const data = state.view === view ? state.data : null;
  if (!data) return <div className="panel">正在加载...</div>;
  if (view === 'dashboard') return <Dashboard data={data} />;
  if (view === 'dishes') return <Dishes rows={Array.isArray(data) ? data : []} api={api} reload={reload} />;
  return <Categories rows={Array.isArray(data) ? data : []} api={api} reload={reload} />;
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

const ORDER_STATUS_TEXT = ['', '待付款', '待接单', '已接单', '派送中', '已完成', '已取消'];
const ORDER_FILTERS = [
  ['', '全部'],
  ['1', '待付款'],
  ['2', '待接单'],
  ['3', '已接单'],
  ['4', '派送中'],
  ['5', '已完成'],
  ['6', '已取消']
];

function Orders({ api }) {
  const [filter, setFilter] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [appliedNumber, setAppliedNumber] = useState('');
  const [rows, setRows] = useState(null);
  const [statistics, setStatistics] = useState({});
  const [expandedId, setExpandedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const params = new URLSearchParams();
    if (filter) params.set('status', filter);
    if (appliedNumber) params.set('number', appliedNumber);
    const query = params.toString();
    Promise.all([api(`/orders${query ? `?${query}` : ''}`), api('/orders/statistics')])
      .then(([list, stats]) => {
        setRows(list);
        setStatistics(Object.fromEntries(stats.map((row) => [String(row.status), row.count])));
        setError('');
      })
      .catch((err) => {
        if (!err.auth) setError(err.message);
      });
  }, [api, filter, appliedNumber]);

  useEffect(load, [load]);

  async function orderAction(id, action) {
    try {
      await api(`/orders/${id}/${action}`, { method: 'POST' });
      setExpandedId(null);
      load();
    } catch (err) {
      if (!err.auth) alert(err.message);
    }
  }

  async function orderActionWithReason(id, action, label) {
    const reason = window.prompt(`${label}原因`);
    if (reason === null) return;
    if (!reason.trim()) {
      alert(`请填写${label}原因`);
      return;
    }
    try {
      await api(`/orders/${id}/${action}`, { method: 'POST', body: JSON.stringify({ reason: reason.trim() }) });
      setExpandedId(null);
      load();
    } catch (err) {
      if (!err.auth) alert(err.message);
    }
  }

  async function toggleDetail(id) {
    if (expandedId === id) {
      setExpandedId(null);
      setDetail(null);
      return;
    }
    try {
      const data = await api(`/orders/${id}`);
      setDetail(data);
      setExpandedId(id);
    } catch (err) {
      if (!err.auth) alert(err.message);
    }
  }

  return (
    <div className="panel">
      <div className="panel-head"><h2>订单管理</h2></div>
      <div className="order-toolbar">
        <div className="order-filters">
          {ORDER_FILTERS.map(([value, label]) => (
            <button
              key={value || 'all'}
              className={`btn small${filter === value ? ' primary' : ''}`}
              onClick={() => setFilter(value)}
            >
              {label}{value && statistics[value] ? ` (${statistics[value]})` : ''}
            </button>
          ))}
        </div>
        <form
          className="order-search"
          onSubmit={(event) => {
            event.preventDefault();
            setAppliedNumber(searchInput.trim());
          }}
        >
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="按订单号搜索"
          />
          <button className="btn small" type="submit">搜索</button>
        </form>
      </div>
      {error ? <div className="empty">{error}</div> : null}
      {!error && rows === null ? <div className="empty">正在加载...</div> : null}
      {!error && rows && rows.length ? (
        <table>
          <thead><tr><th>订单号</th><th>收货人</th><th>金额</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            {rows.map((row) => (
              <React.Fragment key={row.id}>
                <tr>
                  <td>{row.number}</td>
                  <td>{row.consignee}</td>
                  <td>￥{row.amount}</td>
                  <td>{ORDER_STATUS_TEXT[row.status]}</td>
                  <td className="order-actions">
                    {row.status === 2 ? <button className="btn small" onClick={() => orderAction(row.id, 'confirm')}>接单</button> : null}
                    {row.status === 2 ? <button className="btn small" onClick={() => orderActionWithReason(row.id, 'reject', '拒单')}>拒单</button> : null}
                    {row.status === 3 ? <button className="btn small" onClick={() => orderAction(row.id, 'deliver')}>派送</button> : null}
                    {row.status === 4 ? <button className="btn small" onClick={() => orderAction(row.id, 'complete')}>完成</button> : null}
                    {row.status === 2 || row.status === 3 ? <button className="btn small" onClick={() => orderActionWithReason(row.id, 'cancel', '取消')}>取消</button> : null}
                    <button className="btn small" onClick={() => toggleDetail(row.id)}>{expandedId === row.id ? '收起' : '详情'}</button>
                  </td>
                </tr>
                {expandedId === row.id ? (
                  <tr className="order-detail-row">
                    <td colSpan="5"><OrderDetail detail={detail} /></td>
                  </tr>
                ) : null}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      ) : null}
      {!error && rows && !rows.length ? <div className="empty">没有符合条件的订单</div> : null}
    </div>
  );
}

function OrderDetail({ detail }) {
  if (!detail) return <div className="empty compact">正在加载...</div>;
  const items = Array.isArray(detail.details) ? detail.details : [];
  return (
    <div className="order-detail">
      <div><strong>收货信息</strong>：{detail.consignee} {detail.phone} {detail.address}</div>
      {detail.remark ? <div><strong>备注</strong>：{detail.remark}</div> : null}
      <div><strong>下单时间</strong>：{detail.order_time}</div>
      {detail.estimated_delivery_time ? <div><strong>预计送达</strong>：{detail.estimated_delivery_time}</div> : null}
      {detail.delivery_time ? <div><strong>送达时间</strong>：{detail.delivery_time}</div> : null}
      {detail.rejection_reason ? <div><strong>拒单原因</strong>：{detail.rejection_reason}</div> : null}
      {detail.cancel_reason ? <div><strong>取消原因</strong>：{detail.cancel_reason}</div> : null}
      <table>
        <thead><tr><th>菜品</th><th>数量</th><th>单价</th></tr></thead>
        <tbody>
          {items.map((item, index) => (
            <tr key={index}>
              <td>{item.name}</td>
              <td>×{item.number}</td>
              <td>￥{item.amount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AgentQA({ api }) {
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState([]);
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState(null);
  const [error, setError] = useState('');
  const [keyDraft, setKeyDraft] = useState('');
  const [showKey, setShowKey] = useState(false);
  const [keyOpen, setKeyOpen] = useState(false);
  const [keyMsg, setKeyMsg] = useState(null);
  const [testing, setTesting] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let alive = true;
    api('/agent/status')
      .then((data) => {
        if (!alive) return;
        setStatus(data);
        if (!data.configured) setKeyOpen(true);
      })
      .catch((err) => alive && !err.auth && setError(err.message));
    return () => {
      alive = false;
    };
  }, [api]);

  async function refreshStatus() {
    try {
      setStatus(await api('/agent/status'));
    } catch (err) {
      if (!err.auth) setError(err.message);
    }
  }

  async function saveKey() {
    const apiKey = keyDraft.trim();
    if (!apiKey || saving) return;
    setSaving(true);
    setKeyMsg(null);
    try {
      await api('/agent/key', { method: 'POST', body: JSON.stringify({ apiKey }) });
      setKeyMsg({ ok: true, text: `已保存并即时生效，无需重启服务` });
      setKeyDraft('');
      setShowKey(false);
      await refreshStatus();
    } catch (err) {
      if (!err.auth) setKeyMsg({ ok: false, text: err.message });
    } finally {
      setSaving(false);
    }
  }

  async function testKey() {
    const apiKey = keyDraft.trim();
    if (!apiKey || testing) return;
    setTesting(true);
    setKeyMsg(null);
    try {
      const data = await api('/agent/key/test', { method: 'POST', body: JSON.stringify({ apiKey }) });
      setKeyMsg({ ok: data.ok, text: data.message });
    } catch (err) {
      if (!err.auth) setKeyMsg({ ok: false, text: err.message });
    } finally {
      setTesting(false);
    }
  }

  async function changeModel(event) {
    const model = event.target.value;
    if (!model || !status || model === status.model) return;
    setSaving(true);
    setKeyMsg(null);
    try {
      await api('/agent/model', { method: 'POST', body: JSON.stringify({ model }) });
      setKeyMsg({ ok: true, text: `已切换模型：${model}，即时生效无需重启` });
      await refreshStatus();
    } catch (err) {
      if (!err.auth) setKeyMsg({ ok: false, text: err.message });
    } finally {
      setSaving(false);
    }
  }

  async function ask(event) {
    event.preventDefault();
    const question = input.trim();
    if (!question || busy) return;
    setInput('');
    setError('');
    setMessages((list) => [...list, { role: 'user', content: question }]);
    setBusy(true);
    try {
      const data = await api('/agent/chat', {
        method: 'POST',
        body: JSON.stringify({ question })
      });
      setMessages((list) => [...list, { role: 'assistant', data }]);
    } catch (err) {
      if (!err.auth) setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function clearChat() {
    setMessages([]);
    setError('');
  }

  return (
    <div className="panel qa-panel">
      <div className="panel-head">
        <h2>经营问答</h2>
        <div className="qa-actions">
          <span className="pill">
            {status ? (status.configured ? `● ${status.model}` : '● 未配置 API Key') : '● 检测中...'}
          </span>
          <button className="btn small" onClick={clearChat}>清空对话</button>
        </div>
      </div>
      <div className="qa-keycard">
        <button type="button" className="qa-keycard-head" onClick={() => setKeyOpen((v) => !v)}>
          <span>Agent 设置</span>
          <span className="qa-keycard-meta">
            {status ? (status.configured ? `已配置 ${status.masked}` : '未配置') : '检测中'}
          </span>
          <span className="qa-keycard-caret">{keyOpen ? '收起' : '展开'}</span>
        </button>
        {keyOpen ? (
          <div className="qa-keycard-body">
            <div className="qa-keycard-row">
              <label className="qa-keycard-label">模型</label>
              <select value={status?.model || ''} onChange={changeModel} disabled={saving || testing}>
                {(status?.models || []).map((m) => (
                  <option key={m} value={m}>{m}</option>
                ))}
              </select>
            </div>
            <div className="qa-keycard-row">
              <input
                type={showKey ? 'text' : 'password'}
                value={keyDraft}
                onChange={(event) => setKeyDraft(event.target.value)}
                placeholder="sk- 开头，粘贴 DeepSeek API Key"
                disabled={testing || saving}
              />
              <button type="button" className="btn small" onClick={() => setShowKey((v) => !v)} disabled={testing || saving}>
                {showKey ? '隐藏' : '显示'}
              </button>
              <button type="button" className="btn small" onClick={testKey} disabled={testing || saving || !keyDraft.trim()}>
                {testing ? '测试中…' : '测试连接'}
              </button>
              <button type="button" className="btn small primary" onClick={saveKey} disabled={saving || testing || !keyDraft.trim()}>
                {saving ? '保存中…' : '保存'}
              </button>
            </div>
            {status ? (
              <div className="qa-keycard-info">
                当前 Key：{status.masked || '未配置'}　保存位置：{status.keyfile}
              </div>
            ) : null}
            {keyMsg ? (
              <p className={`qa-keymsg ${keyMsg.ok ? 'ok' : 'fail'}`}>{keyMsg.text}</p>
            ) : null}
          </div>
        ) : null}
      </div>
      {status && !status.configured ? (
        <p className="form-error">
          尚未配置 DeepSeek API Key，请展开上方「Agent 设置」卡片粘贴真实 Key 并保存。
        </p>
      ) : null}
      {error ? <p className="form-error">{error}</p> : null}
      <div className="qa-history">
        {messages.length === 0 && !busy ? (
          <div className="empty compact">
            用自然语言提问，例如「最近7天营收多少？」「哪个菜卖得最好？」「有多少待接单订单？」
          </div>
        ) : null}
        {messages.map((item, index) => (
          <div key={index} className={`qa-message ${item.role === 'user' ? 'qa-user' : 'qa-assistant'}`}>
            {item.role === 'user' ? (
              <div className="qa-question">{item.content}</div>
            ) : (
              <div className="qa-answer">
                {item.data && item.data.error ? (
                  <p className="qa-error">{item.data.error}</p>
                ) : (
                  <>
                    <MarkdownText text={item.data?.answer} />
                    {item.data?.sql ? (
                      <div className="qa-sql">
                        <div className="qa-sql-label">
                          执行 SQL{item.data.attempts > 1 ? `（纠错 ${item.data.attempts} 次后成功）` : ''}
                        </div>
                        <pre>{item.data.sql}</pre>
                      </div>
                    ) : null}
                    {Array.isArray(item.data?.rows) && item.data.rows.length ? (
                      <div className="qa-result">
                        <table>
                          <thead>
                            <tr>
                              {(item.data.columns || []).map((col, j) => <th key={j}>{col}</th>)}
                            </tr>
                          </thead>
                          <tbody>
                            {item.data.rows.map((row, j) => (
                              <tr key={j}>
                                {row.map((cell, k) => (
                                  <td key={k}>{cell == null ? '' : String(cell)}</td>
                                ))}
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        {item.data.truncated ? (
                          <p className="qa-note">结果较多，仅显示前 {item.data.rows.length} 行</p>
                        ) : null}
                      </div>
                    ) : null}
                  </>
                )}
              </div>
            )}
          </div>
        ))}
        {busy ? (
          <div className="qa-message qa-assistant"><div className="qa-text">正在查询分析…</div></div>
        ) : null}
      </div>
      <form className="qa-input" onSubmit={ask}>
        <input
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="输入经营问题，回车发送"
          disabled={busy}
        />
        <button className="btn primary" type="submit" disabled={busy || !input.trim()}>
          {busy ? '分析中…' : '发送'}
        </button>
      </form>
    </div>
  );
}

createRoot(document.getElementById('root')).render(<AdminApp />);
