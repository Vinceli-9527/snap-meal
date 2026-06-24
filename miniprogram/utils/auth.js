function getToken() {
  return wx.getStorageSync('token') || '';
}

function setSession(data) {
  const app = getApp();
  wx.setStorageSync('token', data.token);
  wx.setStorageSync('user', data);
  app.globalData.token = data.token;
  app.globalData.user = data;
}

function clearSession() {
  const app = getApp();
  wx.removeStorageSync('token');
  wx.removeStorageSync('user');
  app.globalData.token = null;
  app.globalData.user = null;
}

function requireLogin() {
  if (getToken()) return true;
  wx.navigateTo({ url: '/pages/login/login' });
  return false;
}

module.exports = { getToken, setSession, clearSession, requireLogin };
