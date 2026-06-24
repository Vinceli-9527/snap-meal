const api = require('../../utils/api');
const auth = require('../../utils/auth');

Page({
  data: { items: [], total: '0.00' },
  onShow() {
    if (!auth.requireLogin()) return;
    this.load();
  },
  load() {
    api.get('/api/user/cart')
      .then((items) => {
        const total = items.reduce((sum, item) => sum + Number(item.amount || 0) * Number(item.number || 0), 0);
        this.setData({ items, total: total.toFixed(2) });
      })
      .catch(() => {});
  },
  clearCart() {
    if (!this.data.items.length) return;
    wx.showModal({
      title: '清空购物车',
      content: '确认删除全部商品？',
      success: (res) => {
        if (!res.confirm) return;
        api.delete('/api/user/cart').then(() => this.load()).catch(() => {});
      }
    });
  },
  checkout() {
    wx.navigateTo({ url: '/pages/checkout/checkout' });
  }
});
