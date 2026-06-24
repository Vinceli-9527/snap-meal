const api = require('../../utils/api');
const auth = require('../../utils/auth');

Page({
  data: { id: null, title: '分类详情', dishes: [] },
  onLoad(query) {
    const id = Number(query.id);
    this.setData({ id });
    this.load(id);
  },
  load(id) {
    api.get('/api/user/catalog/dishes?categoryId=' + id)
      .then((dishes) => this.setData({ dishes }))
      .catch(() => {});
  },
  addCart(event) {
    if (!auth.requireLogin()) return;
    api.post('/api/user/cart/items', { dishId: Number(event.currentTarget.dataset.id) })
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'success' }))
      .catch(() => {});
  }
});
