const api = require('../../utils/api');
const auth = require('../../utils/auth');

Page({
  data: { addresses: [] },
  onShow() {
    if (!auth.requireLogin()) return;
    this.load();
  },
  load() {
    api.get('/api/user/addresses')
      .then((addresses) => this.setData({ addresses }))
      .catch(() => {});
  },
  addAddress() {
    wx.navigateTo({ url: '/pages/address-edit/address-edit' });
  }
});
