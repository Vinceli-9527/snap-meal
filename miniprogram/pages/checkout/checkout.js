const api = require('../../utils/api');
const auth = require('../../utils/auth');

Page({
  data: { items: [], address: null, total: '0.00', submitting: false },
  onShow() {
    if (!auth.requireLogin()) return;
    this.load();
  },
  load() {
    Promise.all([api.get('/api/user/cart'), api.get('/api/user/addresses')])
      .then(([items, addresses]) => {
        const mapped = items.map((item) => ({
          ...item,
          lineTotal: (Number(item.amount || 0) * Number(item.number || 0)).toFixed(2)
        }));
        const total = mapped.reduce((sum, item) => sum + Number(item.lineTotal), 0);
        this.setData({
          items: mapped,
          total: total.toFixed(2),
          address: addresses[0] || null
        });
      })
      .catch(() => {});
  },
  chooseAddress() {
    wx.navigateTo({ url: '/pages/address/address' });
  },
  submitOrder() {
    if (!this.data.address) {
      wx.showToast({ title: '请先添加收货地址', icon: 'none' });
      return;
    }
    if (!this.data.items.length) {
      wx.showToast({ title: '购物车为空', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    api.post('/api/user/orders', {
      addressBookId: this.data.address.id,
      payMethod: 1,
      remark: ''
    }).then((order) => api.post('/api/user/orders/' + order.id + '/pay'))
      .then(() => {
        wx.showToast({ title: '支付成功', icon: 'success' });
        wx.redirectTo({ url: '/pages/order/order' });
      })
      .catch(() => {})
      .finally(() => this.setData({ submitting: false }));
  }
});
