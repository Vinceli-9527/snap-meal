App({
  globalData: {
    token: null,
    user: null,
    baseUrl: 'http://localhost:8080'
  },
  onLaunch() {
    const token = wx.getStorageSync('token');
    const user = wx.getStorageSync('user');
    if (token) this.globalData.token = token;
    if (user) this.globalData.user = user;
  }
});
