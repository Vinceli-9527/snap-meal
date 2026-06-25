const auth = require('./auth');

function baseUrl() {
  return getApp().globalData.baseUrl || 'http://localhost:8080';
}

function request(method, path, data) {
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' };
    const token = auth.getToken();
    if (token) header.authentication = token;

    wx.request({
      url: baseUrl() + path,
      method,
      data: data || {},
      header,
      success(res) {
        const body = res.data || {};
        if (res.statusCode === 401) {
          auth.clearSession();
          wx.showToast({ title: '请先登录', icon: 'none' });
          wx.navigateTo({ url: '/pages/login/login' });
          reject(new Error('请先登录'));
          return;
        }
        if (res.statusCode < 200 || res.statusCode >= 300 || body.success === false) {
          const message = body.message || '请求处理失败';
          wx.showToast({ title: message, icon: 'none' });
          reject(new Error(message));
          return;
        }
        resolve(body.data);
      },
      fail() {
        wx.showToast({ title: '服务暂不可用，请稍后重试', icon: 'none' });
        reject(new Error('服务暂不可用，请稍后重试'));
      }
    });
  });
}

module.exports = {
  request,
  get(path, data) { return request('GET', path, data); },
  post(path, data) { return request('POST', path, data); },
  patch(path, data) { return request('PATCH', path, data); },
  delete(path, data) { return request('DELETE', path, data); }
};
