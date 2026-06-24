import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { jsonOptions, requestJson } from '../api.js';
import AnimatedList from '../components/AnimatedList.jsx';
import Dock from '../components/Dock.jsx';
import '../styles.css';

const getLoginMethod = (profile) => profile.loginMethod || profile.loginmethod;
const methodName = (method) => (method === 'PHONE' ? '手机号码登录' : '微信登录');

function UserApp() {
  const [token, setToken] = useState(() => localStorage.userToken || '');
  const [profile, setProfile] = useState(() => {
    try {
      return JSON.parse(localStorage.userProfile || 'null');
    } catch {
      return null;
    }
  });
  const [message, setMessage] = useState('');
  const [cart, setCart] = useState([]);

  const showLogin = useCallback((notice = '') => {
    localStorage.removeItem('userToken');
    localStorage.removeItem('userProfile');
    setToken('');
    setProfile(null);
    setCart([]);
    setMessage(notice);
  }, []);

  const api = useCallback(
    async (path, options = {}) => {
      try {
        return await requestJson(`/api/user${path}`, jsonOptions(options, { authentication: token }));
      } catch (error) {
        if (error.status === 401) {
          showLogin('登录状态已过期，请重新选择登录方式。');
          error.auth = true;
        }
        throw error;
      }
    },
    [showLogin, token]
  );

  const refreshCart = useCallback(async () => {
    try {
      setCart(await api('/cart'));
    } catch (error) {
      if (!error.auth) alert(error.message);
    }
  }, [api]);

  useEffect(() => {
    if (!token) {
      showLogin();
      return;
    }

    api('/auth/session')
      .then((session) => {
        localStorage.userProfile = JSON.stringify(session);
        setProfile(session);
        refreshCart();
      })
      .catch((error) => {
        if (!error.auth) showLogin(error.message);
      });
  }, [api, refreshCart, showLogin, token]);

  async function login(payload) {
    setMessage('');
    try {
      const nextProfile = await requestJson('/api/user/auth/login', jsonOptions({
        method: 'POST',
        body: JSON.stringify(payload)
      }));
      localStorage.userToken = nextProfile.token;
      localStorage.userProfile = JSON.stringify(nextProfile);
      setToken(nextProfile.token);
      setProfile(nextProfile);
    } catch (error) {
      setMessage(error.message);
    }
  }

  if (!profile) return <UserLogin message={message} onLogin={login} />;

  return <MealBrowser profile={profile} api={api} cart={cart} refreshCart={refreshCart} />;
}

function UserLogin({ message, onLogin }) {
  const [phoneVisible, setPhoneVisible] = useState(false);
  const [loading, setLoading] = useState('');

  async function submit(payload, key) {
    setLoading(key);
    await onLogin(payload);
    setLoading('');
  }

  function submitPhone(event) {
    event.preventDefault();
    submit({ loginMethod: 'PHONE', phone: new FormData(event.currentTarget).get('phone') }, 'phone');
  }

  return (
    <section className="user-auth">
      <div className="user-auth-card">
        <a className="text-link" href="/">← 回到主页</a>
        <div className="user-auth-brand">snap-meal</div>
        <h1>选择登录方式</h1>
        <p>本页面使用模拟账号，不会发起短信验证或真实微信授权。</p>
        {message ? <p className="auth-message" role="alert">{message}</p> : null}
        <div className="login-methods">
          <button className="login-method" type="button" onClick={() => setPhoneVisible(true)}>
            <strong>手机号码登录</strong>
            <span>输入任意手机号码</span>
          </button>
          <button className="login-method" type="button" disabled={loading === 'wechat'} onClick={() => submit({ loginMethod: 'WECHAT' }, 'wechat')}>
            <strong>微信登录</strong>
            <span>{loading === 'wechat' ? '正在进入...' : '使用“微信体验用户”'}</span>
          </button>
        </div>
        {phoneVisible ? (
          <form className="phone-login" onSubmit={submitPhone}>
            <div className="field">
              <label>手机号码</label>
              <input name="phone" type="tel" autoComplete="tel" placeholder="请输入任意手机号码" required />
            </div>
            <button className="btn primary" disabled={loading === 'phone'}>{loading === 'phone' ? '正在进入...' : '以手机体验用户登录'}</button>
          </form>
        ) : null}
      </div>
    </section>
  );
}

function GoodCard({ row, feedback, onAdd }) {
  const stateClass = feedback ? ` ${feedback}` : '';

  return (
    <article className={`good animated-good${stateClass}`}>
      <div className="photo">{row.image ? <img src={row.image} alt={row.name} /> : '🍱'}</div>
      <div>
        <h3>{row.name}</h3>
        <p>{row.description || '当日新鲜制作'}</p>
        <div className="price">￥{row.price}</div>
      </div>
      <button className={`add${stateClass}`} aria-label={`添加${row.name}`} onClick={(event) => onAdd(row, event.currentTarget.getBoundingClientRect())}>
        {feedback === 'success' ? '✓' : '+'}
      </button>
    </article>
  );
}

function CartPanel({ open, cart, total, onClose, onIncrement, onDecrement, onCheckout }) {
  if (!open) return null;

  return (
    <section className="cart-panel" aria-label="购物车明细">
      <div className="cart-panel-head">
        <div>
          <strong>已选菜品</strong>
          <small>{cart.length ? `${cart.length} 种菜品` : '购物车为空'}</small>
        </div>
        <button type="button" className="cart-panel-close" onClick={onClose} aria-label="关闭购物车">×</button>
      </div>
      {cart.length ? (
        <div className="cart-panel-list">
          {cart.map((item) => {
            const quantity = Number(item.number) || 0;
            const price = Number(item.amount) || 0;
            return (
              <div className="cart-panel-item" key={item.id}>
                <div className="cart-item-main">
                  <strong>{item.name}</strong>
                  <span>￥{price.toFixed(2)} × {quantity}</span>
                </div>
                <div className="cart-stepper" aria-label={`${item.name}数量调整`}>
                  <button type="button" onClick={() => onDecrement(item)} aria-label={`减少${item.name}`}>−</button>
                  <span>{quantity}</span>
                  <button type="button" onClick={() => onIncrement(item)} aria-label={`增加${item.name}`}>+</button>
                </div>
                <div className="cart-item-subtotal">￥{(price * quantity).toFixed(2)}</div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="cart-panel-empty">还没有选择菜品，先从上面的菜单添加一份。</div>
      )}
      <div className="cart-panel-foot">
        <div>
          <small>合计</small>
          <strong>￥{total.toFixed(2)}</strong>
        </div>
        <button type="button" className="btn primary small" onClick={onCheckout} disabled={!cart.length}>去结算</button>
      </div>
    </section>
  );
}

function MealBrowser({ profile, api, cart, refreshCart }) {
  const [categories, setCategories] = useState([]);
  const [activeCategory, setActiveCategory] = useState(null);
  const [goods, setGoods] = useState([]);
  const [error, setError] = useState('');
  const [addFeedback, setAddFeedback] = useState({});
  const [flyEffect, setFlyEffect] = useState(null);
  const [cartOpen, setCartOpen] = useState(false);
  const cartbarRef = useRef(null);

  useEffect(() => {
    requestJson('/api/user/catalog/categories?type=1')
      .then((rows) => {
        setCategories(rows);
        if (rows.length) setActiveCategory(rows[0].id);
      })
      .catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    if (!activeCategory) return;
    requestJson(`/api/user/catalog/dishes?categoryId=${activeCategory}`)
      .then(setGoods)
      .catch((err) => setError(err.message));
  }, [activeCategory]);

  function setDishFeedback(id, value) {
    setAddFeedback((current) => ({ ...current, [id]: value }));
    window.setTimeout(() => {
      setAddFeedback((current) => {
        const next = { ...current };
        delete next[id];
        return next;
      });
    }, 760);
  }

  function launchCartFly(source) {
    const target = cartbarRef.current?.getBoundingClientRect();
    if (!source || !target) return;

    const id = Date.now();
    setFlyEffect({
      id,
      fromX: source.left + source.width / 2,
      fromY: source.top + source.height / 2,
      toX: target.left + Math.min(84, target.width / 2),
      toY: target.top + target.height / 2
    });
    window.setTimeout(() => setFlyEffect((current) => (current?.id === id ? null : current)), 720);
  }

  async function add(row, sourceRect) {
    try {
      await api('/cart/items', {
        method: 'POST',
        body: JSON.stringify({ dishId: row.id })
      });
      setDishFeedback(row.id, 'success');
      launchCartFly(sourceRect);
      refreshCart();
    } catch (error) {
      setDishFeedback(row.id, 'error');
      if (!error.auth) alert(error.message);
    }
  }

  async function incrementCartItem(item) {
    try {
      await api('/cart/items', {
        method: 'POST',
        body: JSON.stringify({
          dishId: item.dish_id || null,
          setmealId: item.setmeal_id || null,
          flavor: item.dish_flavor || ''
        })
      });
      refreshCart();
    } catch (error) {
      if (!error.auth) alert(error.message);
    }
  }

  async function decrementCartItem(item) {
    const nextNumber = Math.max(0, Number(item.number || 0) - 1);
    try {
      await api(`/cart/items/${item.id}?number=${nextNumber}`, { method: 'PATCH' });
      refreshCart();
    } catch (error) {
      if (!error.auth) alert(error.message);
    }
  }

  async function checkout() {
    if (!cart.length) {
      alert('购物车还是空的');
      return;
    }

    try {
      let addresses = await api('/addresses');
      if (!addresses.length) {
        const consignee = prompt('收货人', '体验用户');
        const phone = prompt('手机号', '13800000001');
        const detail = prompt('详细地址', '大学路 100 号');
        if (!consignee || !phone || !detail) return;
        await api('/addresses', {
          method: 'POST',
          body: JSON.stringify({ consignee, phone, detail, cityName: '上海市', districtName: '杨浦区', label: '学校' })
        });
        addresses = await api('/addresses');
      }
      const order = await api('/orders', {
        method: 'POST',
        body: JSON.stringify({ addressBookId: addresses[0].id, payMethod: 1, remark: 'Web 实验订单' })
      });
      await api(`/orders/${order.id}/pay`, { method: 'POST' });
      alert(`支付成功\n订单号：${order.number}`);
      refreshCart();
    } catch (error) {
      if (!error.auth) alert(error.message);
    }
  }

  const count = cart.reduce((sum, item) => sum + item.number, 0);
  const total = cart.reduce((sum, item) => sum + Number(item.amount) * item.number, 0);
  const availableGoods = useMemo(() => goods.filter((row) => row.status), [goods]);
  const categoryItems = useMemo(
    () => categories.map((category) => ({
      id: category.id,
      label: category.name,
      icon: category.name,
      active: activeCategory === category.id,
      onClick: () => setActiveCategory(category.id)
    })),
    [activeCategory, categories]
  );

  return (
    <div className="mobile">
      <header>
        <div className="user-account">
          <div>
            <span>{profile.nickname}</span>
            <small>{methodName(getLoginMethod(profile))}</small>
          </div>
          <a href="/">主页</a>
        </div>
        <small>今日营业 · 预计 40 分钟送达</small>
        <h1>snap-meal</h1>
        <div>山野食材，认真做饭</div>
      </header>
      <main>
        <Dock items={categoryItems} className="category-dock" />
        <div className="goods">
          {error ? <div className="empty">{error}</div> : null}
          {!error && availableGoods.length ? (
            <AnimatedList
              items={availableGoods}
              getKey={(row) => row.id}
              className="goods-list"
              itemClassName="goods-list-item"
              showGradients
              enableArrowNavigation
              renderItem={(row) => <GoodCard row={row} feedback={addFeedback[row.id]} onAdd={add} />}
            />
          ) : null}
          {!error && !availableGoods.length ? <div className="empty">这个分类还没有上架菜品</div> : null}
        </div>
      </main>
      {flyEffect ? (
        <span
          className="cart-fly-dot"
          style={{
            '--from-x': `${flyEffect.fromX}px`,
            '--from-y': `${flyEffect.fromY}px`,
            '--to-x': `${flyEffect.toX}px`,
            '--to-y': `${flyEffect.toY}px`
          }}
          aria-hidden="true"
        />
      ) : null}
      <CartPanel
        open={cartOpen}
        cart={cart}
        total={total}
        onClose={() => setCartOpen(false)}
        onIncrement={incrementCartItem}
        onDecrement={decrementCartItem}
        onCheckout={checkout}
      />
      <div className="cartbar" ref={cartbarRef}>
        <div className="cartbar-summary" onClick={() => setCartOpen((value) => !value)} role="button" tabIndex={0} onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            setCartOpen((value) => !value);
          }
        }}>
          <small>购物车</small>
          <div>{cart.length ? `${count} 件 · ￥${total.toFixed(2)}` : '还没有选择菜品'}</div>
        </div>
        <button className="btn primary small" onClick={checkout}>去结算</button>
      </div>
    </div>
  );
}

createRoot(document.getElementById('root')).render(<UserApp />);
