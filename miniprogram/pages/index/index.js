const api = require('../../utils/api');
const auth = require('../../utils/auth');

Page({
  data: {
    categories: [],
    activeCategoryId: null,
    dishes: [],
    loading: false
  },
  onShow() {
    this.loadCategories();
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
    api.post('/api/user/cart/items', { dishId })
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'success' }))
      .catch(() => {});
  },
  imageError() {}
});
