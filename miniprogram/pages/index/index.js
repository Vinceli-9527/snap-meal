const api = require('../../utils/api');
const auth = require('../../utils/auth');

Page({
  data: {
    categories: [],
    activeCategoryId: null,
    dishes: [],
    cart: [],
    cartCount: 0,
    cartTotal: '0.00',
    cartSummary: '还没有选择菜品',
    cartPanelSummary: '购物车为空',
    cartOpen: false,
    updatingCartId: null,
    addingId: null,
    loading: false
  },
  onShow() {
    this.loadCategories();
    this.loadCart();
  },
  loadCategories() {
    api.get('/api/user/catalog/categories?type=1')
      .then((categories) => {
        const active = this.data.activeCategoryId || (categories[0] && categories[0].id);
        this.setData({ categories, activeCategoryId: active });
        if (active) this.loadDishes(active);
      })
      .catch(() => {});
  },
  loadDishes(categoryId) {
    this.setData({ loading: true });
    api.get('/api/user/catalog/dishes?categoryId=' + categoryId)
      .then((dishes) => this.setData({ dishes }))
      .catch(() => this.setData({ dishes: [] }))
      .finally(() => this.setData({ loading: false }));
  },
  loadCart() {
    if (!auth.getToken()) {
      this.setData({ cart: [], cartCount: 0, cartTotal: '0.00', cartSummary: '还没有选择菜品', cartPanelSummary: '购物车为空', cartOpen: false });
      return;
    }
    api.get('/api/user/cart')
      .then((cart) => {
        const mapped = cart.map((item) => {
          const price = Number(item.amount || 0);
          const number = Number(item.number || 0);
          return {
            ...item,
            priceText: price.toFixed(2),
            lineTotal: (price * number).toFixed(2)
          };
        });
        const cartCount = mapped.reduce((sum, item) => sum + Number(item.number || 0), 0);
        const total = mapped.reduce((sum, item) => sum + Number(item.lineTotal || 0), 0);
        const cartTotal = total.toFixed(2);
        this.setData({
          cart: mapped,
          cartCount,
          cartTotal,
          cartSummary: cartCount ? cartCount + ' 件 · ￥' + cartTotal : '还没有选择菜品',
          cartPanelSummary: cartCount ? mapped.length + ' 种菜品' : '购物车为空',
          cartOpen: cartCount ? this.data.cartOpen : false
        });
      })
      .catch(() => {});
  },
  selectCategory(event) {
    const id = Number(event.currentTarget.dataset.id);
    this.setData({ activeCategoryId: id });
    this.loadDishes(id);
  },
  openCategory() {
    if (!this.data.activeCategoryId) return;
    wx.navigateTo({ url: '/pages/category/category?id=' + this.data.activeCategoryId });
  },
  addCart(event) {
    if (!auth.requireLogin()) return;
    const dishId = Number(event.currentTarget.dataset.id);
    this.setData({ addingId: dishId });
    api.post('/api/user/cart/items', { dishId })
      .then(() => {
        this.loadCart();
        wx.showToast({ title: '已加入购物车', icon: 'success' });
      })
      .catch(() => {})
      .finally(() => {
        setTimeout(() => this.setData({ addingId: null }), 420);
      });
  },
  toggleCartPanel() {
    if (!auth.requireLogin()) return;
    this.setData({ cartOpen: !this.data.cartOpen });
  },
  closeCartPanel() {
    this.setData({ cartOpen: false });
  },
  incrementCart(event) {
    const item = this.findCartItem(event.currentTarget.dataset.id);
    if (!item || this.data.updatingCartId) return;
    this.setData({ updatingCartId: item.id });
    api.post('/api/user/cart/items', {
      dishId: item.dish_id || null,
      setmealId: item.setmeal_id || null,
      flavor: item.dish_flavor || ''
    })
      .then(() => this.loadCart())
      .catch(() => {})
      .finally(() => this.setData({ updatingCartId: null }));
  },
  decrementCart(event) {
    const item = this.findCartItem(event.currentTarget.dataset.id);
    if (!item || this.data.updatingCartId) return;
    const nextNumber = Math.max(0, Number(item.number || 0) - 1);
    this.setData({ updatingCartId: item.id });
    api.patch('/api/user/cart/items/' + item.id + '?number=' + nextNumber)
      .then(() => this.loadCart())
      .catch(() => {})
      .finally(() => this.setData({ updatingCartId: null }));
  },
  clearCart() {
    if (!this.data.cart.length) return;
    wx.showModal({
      title: '清空购物车',
      content: '确认删除全部商品？',
      success: (res) => {
        if (!res.confirm) return;
        api.delete('/api/user/cart').then(() => this.loadCart()).catch(() => {});
      }
    });
  },
  findCartItem(id) {
    const itemId = Number(id);
    return this.data.cart.find((item) => Number(item.id) === itemId);
  },
  checkout() {
    if (!auth.requireLogin()) return;
    if (!this.data.cart.length) {
      wx.showToast({ title: '先选择一份菜品', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/checkout/checkout' });
  },
  noop() {},
  imageError() {}
});
