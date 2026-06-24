const api = require('../../utils/api');
const auth = require('../../utils/auth');

const statusText = {
  1: '待付款',
  2: '待接单',
  3: '待派送',
  4: '配送中',
  5: '已完成',
  6: '已取消'
};

Page({
  data: { orders: [] },
  onShow() {
    if (!auth.requireLogin()) return;
    this.load();
  },
  load() {
    api.get('/api/user/orders')
      .then((orders) => this.setData({
        orders: orders.map((item) => ({ ...item, statusText: statusText[item.status] || '待接单' }))
      }))
      .catch(() => {});
  }
});
