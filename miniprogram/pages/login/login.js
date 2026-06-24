const api = require('../../utils/api');
const auth = require('../../utils/auth');

Page({
  data: { loading: false },
  login() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    api.post('/api/user/auth/login', { loginMethod: 'WECHAT' })
      .then((data) => {
        auth.setSession(data);
        wx.switchTab({ url: '/pages/index/index' });
      })
      .catch(() => {})
      .finally(() => this.setData({ loading: false }));
  }
});
